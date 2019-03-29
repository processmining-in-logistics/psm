package org.processmining.scala.log.common.enhancment.segments.parallel

import java.io.File

import org.processmining.scala.log.common.csv.common.CsvWriter
import org.processmining.scala.log.common.enhancment.segments.common.{AbstractAggregationFunction, InventoryAggregation, PreprocessingSession}
import org.processmining.scala.log.common.types.{Segment, SegmentWithClazz}
import org.processmining.scala.log.common.unified.event.{CommonAttributeSchemas, UnifiedEvent}
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog

import scala.collection.immutable.SortedMap
import scala.collection.parallel.{ParMap, ParSeq}


case class SegmentVisualizationItem(index: Long, key: String, clazz: Int, count: Long) {
  def toCsv() = s""""$index";"$key";"$clazz";"$count""""


}

object SegmentVisualizationItem {
  val csvHeader = s""""index";"key";"${CommonAttributeSchemas.AttrNameClazz}";"count""""
}

case class SegmentProcessorConfig(
                                   segments: UnifiedEventLog,
                                   timestamp1Ms: Long,
                                   timestamp2Ms: Long,
                                   twSize: Long,
                                   aaf: AbstractAggregationFunction = InventoryAggregation
                                 )

abstract class SegmentProcessor(val config: SegmentProcessorConfig, clazzCount: Int) {

  def getVisualizationDataset(export: => ParSeq[(Long, SegmentWithClazz)] => Unit): (ParSeq[SegmentVisualizationItem], PreprocessingSession) = {

    val session = PreprocessingSession(config.timestamp1Ms, config.timestamp2Ms, config.twSize, clazzCount, config.aaf.getClass.getSimpleName, "")
    val segments = getClassifiedSegments()

    export(segments.map(x =>
      (config.aaf.getTws(x.timestamp, x.duration, config.timestamp1Ms, config.timestamp2Ms, config.twSize)
        , x))
      .filter(_._1.nonEmpty)
      .map(x => (x._1.head, x._2))
    )

    (segments
      .flatMap(x =>
        config.aaf.getTws(x.timestamp, x.duration, config.timestamp1Ms, config.timestamp2Ms, config.twSize)
          .map((_, x))
      )
      .groupBy(x => (x._1, x._2.key, x._2.clazz))
      .mapValues(x => x.size)
      .map(x => SegmentVisualizationItem(x._1._1, x._1._2, x._1._3, x._2))
      .toSeq, PreprocessingSession.commit(session))
  }

  def getClassifiedSegments(): ParSeq[SegmentWithClazz]

  def getClassifiedSegmentLog(): UnifiedEventLog =
    UnifiedEventLog.create(
      getClassifiedSegments()
        .map(x => (x.id,
          UnifiedEvent(
            x.timestamp,
            x.key,
            SortedMap(CommonAttributeSchemas.AttrNameDuration -> x.duration, CommonAttributeSchemas.AttrNameClass -> x.clazz.toByte),
            None
          )
        )))


}


object SegmentProcessor {

  type GroupedSegmentsWithClazz = ParMap[(Long, String, Int), ParSeq[(Long, SegmentWithClazz)]]

  val ProcessedDataFileName = "time_diff.csv"
  val StatDataFileName = "max.csv"
  val SessionFileName = "session.psm"
  val SegmentsFileName = "segments.csv"


  def getMax(groupedSegTw: ParSeq[SegmentVisualizationItem]): ParMap[String, Long] = {
    groupedSegTw
      .groupBy(x => (x.index, x.key))
      .mapValues(_.map(_.count).reduce(_ + _))
      .groupBy(_._1._2)
      .map(x => x._1 -> x._2.values.max)
  }

  def toCsvV2(proc: SegmentProcessor, outDir: String, legend: String) = {
    new File(outDir).mkdirs()
    val (dataset, session) = proc.getVisualizationDataset(DataExporter.segmentsToDirs(s"${outDir}/segments", _: ParSeq[(Long, SegmentWithClazz)]))
    DataExporter.toDirs(dataset, s"${outDir}/data")
    val maxSeg = getMax(dataset)
    CsvWriter.eventsToCsvLocalFilesystem[(String, Long)](maxSeg.seq, """"key";"max"""", x => s""""${x._1}";"${x._2}"""", outDir + StatDataFileName)
    PreprocessingSession.toDisk(session.copy(legend = legend), outDir + SessionFileName, false)
  }


  @deprecated
  def toCsv(proc: SegmentProcessor, outDir: String, legend: String) = {
    new File(outDir).mkdirs()
    val (groupedSegTw, session) = proc.getVisualizationDataset(_ => ())
    PreprocessingSession.toDisk(session, outDir + SessionFileName, false)
    DataExporter.toFile(groupedSegTw, outDir + ProcessedDataFileName)
    val maxSeg = getMax(groupedSegTw)
    CsvWriter.eventsToCsvLocalFilesystem[(String, Long)](maxSeg.seq, """"key";"max"""", x => s""""${x._1}";"${x._2}"""", outDir + StatDataFileName)
    //    val w = new PrintWriter(outDir + ConfigFileName)
    //    try {
    //
    //      w.println("[GENERAL]")
    //      w.println(s"""twSizeMs = ${proc.config.twSize}""")
    //      //w.println(s"""startTime = ${proc.config.csvExportHelper.timestamp2String(proc.config.timestamp1Ms)}""")
    //      //w.println(s"""endTime = ${proc.config.csvExportHelper.timestamp2String(proc.config.timestamp2Ms)}""")
    //      w.println(s"""startTimeMs = ${proc.config.timestamp1Ms}""")
    //      w.println(s"""endTimeMs = ${proc.config.timestamp2Ms}""")
    //      if (!legend.isEmpty) w.println(s"""legend = ${legend}""")
    //    } finally {
    //      w.close()
    //    }
    groupedSegTw
  }


  def getTracesWithSegments(log: UnifiedEventLog): ParSeq[(String, List[Segment])] =
    log
      .traces()
      .map(x => (x._1, x._2.filter(_.hasAttribute(CommonAttributeSchemas.AttrNameDuration))))
      .filter(_._2.nonEmpty)
      .map(x => (x._1.id, x._2.map(e => Segment(x._1.id, e.activity, e.timestamp, e.getAs[Long](CommonAttributeSchemas.AttrNameDuration)))))


  def getSegments(log: UnifiedEventLog): ParSeq[Segment] =
    getTracesWithSegments(log)
      .flatMap(_._2)

}

//  def dfToRdd(df: DataFrame): RDD[SegmentWithClazz] =
//    df
//      .rdd
//      .map { x =>
//        SegmentWithClazz(
//          x.getAs[String]("id"),
//          x.getAs[String]("key"),
//          x.getAs[Long]("timestamp"),
//          x.getAs[Long](CommonAttributeSchemas.AttrNameDuration),
//          x.getAs[Int](CommonAttributeSchemas.AttrNameClazz))
//      }
//
//
//  def createSegmentDataframe(log: UnifiedEventLog, spark: SparkSession, segmentClass: String): DataFrame =
//    spark.createDataFrame(log.selectEventsAs[Segment](segmentClass))

