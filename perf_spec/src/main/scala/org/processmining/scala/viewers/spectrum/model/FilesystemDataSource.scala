package org.processmining.scala.viewers.spectrum.model

import java.io.IOException
import java.util.Date
import org.processmining.scala.log.common.csv.parallel.CsvReader
import org.processmining.scala.log.common.enhancment.segments.common.PreprocessingSession
import org.processmining.scala.log.common.enhancment.segments.parallel.SegmentProcessor
import org.processmining.scala.log.common.types.SegmentWithClazz
import org.processmining.scala.log.common.unified.event.CommonAttributeSchemas
import org.processmining.scala.log.common.utils.common.EventAggregator
import org.slf4j.LoggerFactory

private[viewers] class FilesystemDataSource(dir: String, aggregator: EventAggregator, lt: (String, String) => Boolean) extends AbstractDataSource {

  val logger = LoggerFactory.getLogger(classOf[FilesystemDataSource])

  val session = PreprocessingSession(s"$dir/${SegmentProcessor.SessionFileName}")

  override val twSizeMs: Long = session.twSizeMs

  override val startTimeMs: Long = session.startMs

  override val twCount: Int = ((session.endMs - startTimeMs) / twSizeMs).toInt

  override val legend = session.legend

  override def classesCount: Int = session.classCount

  private val csvReader = new CsvReader()

  private val maxSegmentsCounts: Map[String, Long] = {
    def factory(keyIndex: Int, maxIndex: Int)(row: Array[String]): (String, Long) =
      (row(keyIndex), row(maxIndex).toLong)

    val (header, lines) = csvReader.read(s"$dir/${SegmentProcessor.StatDataFileName}")
    csvReader.parse(lines, factory(header.indexOf("key"), header.indexOf("max")))
      .map(x => ( aggregator.aggregateSegmentKey(x._1), x._2))
      .groupBy(_._1)
      .mapValues(_.map(_._2).reduce(_+_))
      .toMap
      .seq
  }

  override def maxSegmentsCount(name: String): Long = maxSegmentsCounts(name)

  override val segmentNames: Array[String] = maxSegmentsCounts.keys.toArray.sortWith(lt)

  override def goingToRequest(startTwIndex: Int, endTwIndex: Int, includingIds: Boolean, includingSegments: Boolean): Unit = ()

  private var segmentCountStorage: Map[Int, Map[String, List[(Int, Long)]]] = Map()

  private var segmentCountStorageUseTimestamps: Map[Int, Long] = Map()

  private def timeDiffFactory(keyIndex: Int, clazzIndex: Int, countIndex: Int)(row: Array[String]): (String, Int, Long) =
    (aggregator.aggregateSegmentKey(row(keyIndex)), row(clazzIndex).toInt, row(countIndex).toLong)

  //TODO: move to unparallel!!!
  private def loadSegmentCounts(twIndex: Int): Map[String, List[(Int, Long)]] = {
    try {
      val (header, lines) = csvReader.read(s"$dir/data/$twIndex/${SegmentProcessor.ProcessedDataFileName}")
      csvReader
        .parse(lines, timeDiffFactory(header.indexOf("key"), header.indexOf(CommonAttributeSchemas.AttrNameClazz), header.indexOf("count")))
        .seq
        .groupBy(_._1)
        .mapValues(_.groupBy(_._2).mapValues(_.map(_._3).reduce(_ + _)).toList.sortBy(_._1))
    }catch{
      case ex: IOException => {
        logger.debug(s"cannot load segment counts for tw $twIndex")
        Map()
      }
    }
  }

  override def segmentsCount(twIndex: Int): Map[String, List[(Int, Long)]] = {
    if (segmentCountStorage.contains(twIndex)) segmentCountStorage(twIndex) else {
      val data = loadSegmentCounts(twIndex)
      segmentCountStorage = segmentCountStorage + (twIndex -> data)
      segmentCountStorageUseTimestamps - twIndex + (twIndex -> new Date().getTime)
      data
    }
  }



  private var segmentIdsStorage: Map[Int, Map[String, Map[String, Int]]] = Map()

  private var segmentIdsStorageUseTimestamps: Map[Int, Long] = Map()

  private def segmentFactory(idIndex: Int, keyIndex: Int, timestampIndex: Int, durationIndex: Int, clazzIndex: Int)(row: Array[String]): SegmentWithClazz =
    SegmentWithClazz(row(idIndex), aggregator.aggregateSegmentKey(row(keyIndex)), row(timestampIndex).toLong, row(durationIndex).toLong, row(clazzIndex).toInt)


  //TODO: move to unparallel!!!
  private def loadSegmentIds(twIndex: Int): Map[String, Seq[SegmentWithClazz]] = {
    try {
      val (header, lines) = csvReader.read(s"$dir/segments/$twIndex/${SegmentProcessor.SegmentsFileName}")
      csvReader
        .parse(lines, segmentFactory(header.indexOf("id"), header.indexOf("key"), header.indexOf("timestamp"), header.indexOf(CommonAttributeSchemas.AttrNameDuration), header.indexOf(CommonAttributeSchemas.AttrNameClazz)))
        .seq
        .groupBy(_.key)
    }catch{
      case ex: IOException => {
        logger.debug(s"cannot load segment IDs for tw $twIndex")
        Map()
      }
    }

  }

  override def segmentIds(twIndex: Int): Map[String, Map[String, Int]] = {
    if (segmentIdsStorage.contains(twIndex)) segmentIdsStorage(twIndex) else {
      val data = loadSegmentIds(twIndex)
        .mapValues(x => x.map(s => (s.id -> s.clazz)).toMap)
      segmentIdsStorage = segmentIdsStorage + (twIndex -> data)
      segmentIdsStorageUseTimestamps - twIndex + (twIndex -> new Date().getTime)
      data
    }
  }


  private var segmentStorage: Map[Int, Map[String, Map[Int, List[(String, Long, Long)]]]] = Map()

  private var segmentStorageUseTimestamps: Map[Int, Long] = Map()


  override def segments(twIndex: Int): Map[String, Map[Int, List[(String, Long, Long)]]] = {
    if (segmentStorage.contains(twIndex)) segmentStorage(twIndex) else {
      val data = loadSegmentIds(twIndex)
        .mapValues(_
          .groupBy(_.clazz)
          .map(x => x._1 -> x._2.map(s => (s.id, s.timestamp, s.duration)).toList))

      segmentStorage = segmentStorage + (twIndex -> data)
      segmentStorageUseTimestamps - twIndex + (twIndex -> new Date().getTime)
      data
    }
  }


}
