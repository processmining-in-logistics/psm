package org.processmining.scala.viewers.spectrum2.model

import java.io.File
import java.util.concurrent.Callable

import org.processmining.scala.log.common.csv.parallel.CsvReader
import org.processmining.scala.log.common.enhancment.segments.common.SegmentUtils
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.processmining.scala.log.utils.common.csv.common.{CsvExportHelper, CsvImportHelper}
import org.processmining.scala.viewers.spectrum.view.SortingOrderUtils
import org.processmining.scala.viewers.spectrum2.view.SortingOrderEntry
import org.slf4j.LoggerFactory

import scala.swing.Color

abstract class CallableWithLevel[T] extends Callable[T] {

  def call(level: Int): T

  def levels: Vector[String]

  val levelCount = levels.size

  def call(): T = call(levelCount)
}


class UnifiedEventLogBasedBuilder(logFactory: CallableWithLevel[UnifiedEventLog], isSingleLevel: Boolean, val dir: String) extends CallableWithLevel[AbstractDataSource] {

  private val logger = LoggerFactory.getLogger(classOf[UnifiedEventLogBasedBuilder])

  def loadSortingOrder(level: Int) = {
    val sortingOrderFilename = s"$dir/sorting_order_$level.txt"
    val sortingOrderFile = new File(sortingOrderFilename)
    AbstractDataSource.buildSortingOrderFromSegmentNames(
      (if (sortingOrderFile.exists && sortingOrderFile.isFile)
        SortingOrderUtils.readSortingOrderNonDistinct(sortingOrderFilename)
          .map(x => x.split(SegmentUtils.DefaultSeparator))
      else Array()).map(x => SegmentName(x(0), x(1))).toVector)
  }

  def loadOverlaidSegments(level: Int): Map[SegmentName, Vector[OverlaidSegment]] = {
    val loadOverlaidSegmentsFilename = s"$dir/overlaid_segments_$level.csv"
    val loadOverlaidSegmentsFile = new File(loadOverlaidSegmentsFilename)
    if (loadOverlaidSegmentsFile.exists && loadOverlaidSegmentsFile.isFile) {
      val csvReader = new CsvReader(",", "\"")
      val importCsvHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)
      val lines = csvReader.readNoHeaderSeq(loadOverlaidSegmentsFilename)

      def factory(a: Array[String]) = {
        val colorHex =
          if (a(9).length != 8) {
            logger.warn(s"Wrong color '${a(9)}'. Colors must be represented as a 8-letter string 'RRGGBBAA' with hex values for each color and alpha")
            "00000000"
          } else a(9)
        val r = Integer.valueOf(colorHex.substring(0, 2), 16)
        val g = Integer.valueOf(colorHex.substring(2, 4), 16)
        val b = Integer.valueOf(colorHex.substring(4, 6), 16)
        val alpha = Integer.valueOf(colorHex.substring(6, 8), 16)
        OverlaidSegment(
          a(0),
          SegmentName(a(5), a(6)),
          SegmentName(a(7), a(8)),
          SegmentName(a(1), a(2)),
          importCsvHelper.extractTimestamp(a(3)),
          importCsvHelper.extractTimestamp(a(4)),
          new Color(r, g, b, alpha))
      }

      lines
        .map(factory)
        .groupBy(x => x.srcSegment)
        .map(x => (x._1, x._2.toVector))

    } else Map()
  }

  override def call(level: Int): AbstractDataSource = {
    val traces = logFactory.call(level)
      .traces()
    val originsWoIndices = traces.map(t => (t._1.id, t._2.head.activity)).seq.toMap
    val uniqueOriginsToIndex = originsWoIndices.values.toList.distinct.sorted.zipWithIndex.map(x => x._1 -> x._2).toMap
    val origins = originsWoIndices.map(x => x._1 -> (x._2, uniqueOriginsToIndex(x._2)))
    val segments = traces
      .flatMap(t => t._2.foldLeft((None: Option[UnifiedEvent], List[(SegmentName, Segment)]()))((p: (Option[UnifiedEvent], List[(SegmentName, Segment)]), e: UnifiedEvent) => {
        if (p._1.isEmpty) (Some(e), p._2)
        else {
          //val resourceUtilOpt = resourceUtil.get(SegmentName(p._1.get.activity, e.activity))
          val startEvent = SegmentEventImpl(p._1.get)
          val endEvent = SegmentEventImpl(e)

          (Some(e),
            (SegmentName(p._1.get.activity, e.activity),
              Segment(t._1.id,
                startEvent,
                endEvent,
                0)) :: p._2
          )
        }
      })._2
      )


    val segmentMap = segments
      .groupBy(_._1)
      .map(x => (x._1, x._2.map(_._2).toVector.sortBy(_.start.minMs)))
      .seq
    val segmentNamesUnsorted = segmentMap.keys.toVector

    val sortingOrderFromFile = loadSortingOrder(level)
    val overlaidSegmentsFile = loadOverlaidSegments(level)

    val tmp = overlaidSegmentsFile.values.flatMap(_.map(x => (x.timestamp1, x.timestamp2)))

    val segmentBasedMin = segments.map(_._2.start.minMs).min
    val segmentBasedMax = segments.map(_._2.end.maxMs).max


    val oMin = if(tmp.nonEmpty) tmp.map(_._1).min else segmentBasedMin
    val oMax = if(tmp.nonEmpty) tmp.map(_._2).max else segmentBasedMax

    val startTime = Math.min(segmentBasedMin, oMin)
    val endTime = Math.max(segmentBasedMax, oMax)


    val ret = new AbstractDataSource {
      override def startTimeMs: Long = startTime

      override def endTimeMs: Long = endTime

      override def segmentNames: Vector[SegmentName] = segmentNamesUnsorted

      override def segments: Map[SegmentName, Vector[Segment]] = segmentMap

      override def caseIdToOriginActivityAndIndex: Map[String, (String, Int)] = origins

      override def sortingOrder(): Vector[SortingOrderEntry] = if (level == 1) {
        if (sortingOrderFromFile(0).segmentNames.isEmpty) super.sortingOrder() else sortingOrderFromFile
      } else FakeDataSource.sortingOrder()


      override def overlaidSegments(srcSegmentName: SegmentName): Vector[OverlaidSegment] = {
        val opt = overlaidSegmentsFile.get(srcSegmentName)
        if (opt.isDefined) opt.get else Vector()
      }
    }
    ret
  }

  override val levelCount: Int = if (isSingleLevel) 1 else logFactory.levelCount

  override def levels: Vector[String] = if (isSingleLevel) Vector(FakeDataSource.FirstLevel) else FakeDataSource.Levels
}
