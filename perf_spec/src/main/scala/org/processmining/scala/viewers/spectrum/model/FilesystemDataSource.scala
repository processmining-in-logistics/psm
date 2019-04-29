package org.processmining.scala.viewers.spectrum.model

import java.util.Date
import java.util.function.Consumer

import org.processmining.scala.log.common.csv.parallel.CsvReader
import org.processmining.scala.log.common.enhancment.segments.common.PreprocessingSession
import org.processmining.scala.log.common.enhancment.segments.parallel.SegmentProcessor
import org.processmining.scala.log.common.types.SegmentWithClazz
import org.processmining.scala.log.common.utils.common.EventAggregator
import org.slf4j.LoggerFactory

class FilesystemDataSource(dir: String,
                           aggregator: EventAggregator,
                           lt: (String, String) => Boolean,
                           segmentsFilter: String => Boolean = _ => true
                          ) extends AbstractDataSource {

  private val logger = LoggerFactory.getLogger(classOf[FilesystemDataSource])

  val session = PreprocessingSession(s"$dir/${SegmentProcessor.SessionFileName}")

  override val twSizeMs: Long = session.twSizeMs

  override val startTimeMs: Long = session.startMs

  override val twCount: Int = ((session.endMs - startTimeMs) / twSizeMs).toInt

  override val legend: String = session.legend

  override val classifierName: String = session.durationClassifier

  override def classesCount: Int = session.classCount

  private val csvReader = new CsvReader()

  private val maxSegmentsCounts: Map[String, (Long, Long, Long, Long)] = {
    def factory(keyIndex: Int, maxIndex: Int, maxIntersect: Int, maxStop: Int, maxSum: Int)(row: Array[String]): (String, Long, Long, Long, Long) =
      (row(keyIndex), row(maxIndex).toLong, row(maxIntersect).toLong, row(maxStop).toLong, row(maxSum).toLong)

    val (header, lines) = csvReader.read(s"$dir/${SegmentProcessor.StatDataFileName}")
    csvReader.parse(lines, factory(header.indexOf("key"), header.indexOf("max"), header.indexOf("maxIntersect"), header.indexOf("maxStop"), header.indexOf("maxSum")))
      .map(x => (aggregator.aggregateSegmentKey(x._1), x._2, x._3, x._4, x._5))
      .groupBy(_._1)
      .mapValues(x => (x.map(_._2).sum, x.map(_._3).sum, x.map(_._4).sum, x.map(_._5).sum))
      .toMap
      .filter(x => segmentsFilter(x._1))
      .seq

  }

  override def maxSegmentsCount(name: String): (Long, Long, Long, Long) = maxSegmentsCounts(name)

  override val segmentNames: Array[String] = maxSegmentsCounts.keys.toArray.sortWith(lt)

  private var fileStorage: Option[FileStorage] = None

  override def initialize(callback: Consumer[Int]): Unit = fileStorage = Some(new FileStorage(dir, segmentNames, callback))

  private var segmentCountStorage: Map[Int, Map[String, List[(Int, Long, Long, Long)]]] = Map()

  private var segmentCountStorageUseTimestamps: Map[Int, Long] = Map()

  //  private def timeDiffFactory(keyIndex: Int, clazzIndex: Int, countIndex: Int, countStartIndex: Int, countStopIndex: Int)(row: Array[String]): (String, Int, Long, Long, Long) =
  //    if (row.length >= 6) (aggregator.aggregateSegmentKey(row(keyIndex)), row(clazzIndex).toInt, row(countIndex).toLong, row(countStartIndex).toLong, row(countStopIndex).toLong)
  //    else (aggregator.aggregateSegmentKey(row(keyIndex)), row(clazzIndex).toInt, row(countIndex).toLong, 0, 0)
  //

  //TODO: move to unparallel!!!
  private def loadSegmentCounts(twIndex: Int): Map[String, List[(Int, Long, Long, Long)]] =
    segmentNames
      .map(s => (s, fileStorage.get.readData(twIndex, s)))
      .filter(_._2.isDefined)
      .map(x => x._1 ->
        x._2.get
          .sortBy(_.clazz)
          .map(y => (y.clazz, y.countStart.toLong, y.countIntersect.toLong, y.countStop.toLong)))
      .toMap

  override def segmentsCount(twIndex: Int): Map[String, List[(Int, Long, Long, Long)]] = {
    val opt = segmentCountStorage.get(twIndex)
    if (opt.isDefined) opt.get else {
      val data = loadSegmentCounts(twIndex)
      segmentCountStorage = segmentCountStorage + (twIndex -> data)
      segmentCountStorageUseTimestamps = segmentCountStorageUseTimestamps - twIndex + (twIndex -> new Date().getTime)
      data
    }
  }

  override def forgetSegmentsCount(twIndex: Int): Unit = {
    if (segmentCountStorage.contains(twIndex)) {
      segmentCountStorage = segmentCountStorage - twIndex
      segmentCountStorageUseTimestamps = segmentCountStorageUseTimestamps - twIndex
    }
  }

  private var segmentIdsStorage: Map[Int, Map[String, Map[String, Int]]] = Map()

  private var segmentIdsStorageUseTimestamps: Map[Int, Long] = Map()

//  private def segmentFactory(idIndex: Int, keyIndex: Int, timestampIndex: Int, durationIndex: Int, clazzIndex: Int)(row: Array[String]): SegmentWithClazz =
//    SegmentWithClazz(row(idIndex), aggregator.aggregateSegmentKey(row(keyIndex)), row(timestampIndex).toLong, row(durationIndex).toLong, row(clazzIndex).toInt)


  //TODO: move to unparallel!!!
  private def loadSegmentIds(twIndex: Int): Map[String, Seq[SegmentWithClazz]] = {
    segmentNames
      .map(s => (s, fileStorage.get.readStarted(twIndex, s)))
      .filter(_._2.isDefined)
      .map(x => x._1 ->
        x._2.get
          .map(y => SegmentWithClazz(y.caseId, x._1, y.timeMs, y.duration, y.clazz)))
      .toMap
  }

  override def segmentIds(twIndex: Int): Map[String, Map[String, Int]] = {
    val opt = segmentIdsStorage.get(twIndex)
    if (opt.isDefined) opt.get else {
      val data = loadSegmentIds(twIndex)
        .mapValues(x => x.map(s => (s.id -> s.clazz)).toMap)
      segmentIdsStorage = segmentIdsStorage + (twIndex -> data)
      segmentIdsStorageUseTimestamps = segmentIdsStorageUseTimestamps - twIndex + (twIndex -> new Date().getTime)
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

  override def logFilteredSegments(s: Array[String]): Unit = {
    logger.info(s""""${s.mkString("""", """")}"""")
  }

}
