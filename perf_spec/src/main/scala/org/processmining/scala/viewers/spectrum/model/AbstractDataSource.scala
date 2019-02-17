package org.processmining.scala.viewers.spectrum.model

import java.util.function.Consumer
import java.util.regex.Pattern


// Prerequisite: all the data for at least 1 time window must fit into the heap of JVM and prefferably into RAM
trait AbstractDataSource {

  //  require(twSizeMs > 0)
  //
  //  require(twCount > 0)
  //
  //  require(classesCount > 0)

  def twCount: Int

  def twSizeMs: Long

  def startTimeMs: Long //abs. time (preferrably UTC)

  def segmentNames: Array[String] // all possible segments (after aggregation) sorted ASC

  def legend: String

  def getTimeWindowByAbsTime(timeMs: Long): Int = ((timeMs - startTimeMs) / twSizeMs).toInt

  def initialize(): Unit = {}

  def maxSegmentCountForAggregation(segmentName: String, aggregationCode: Int): Long =
    if (aggregationCode == AbstractDataSource.AggregationCodeZero) throw new IllegalArgumentException(s"aggregationCode=$aggregationCode")
    else maxSegmentCountForAggregationImpl(segmentName, aggregationCode)


  private def maxSegmentCountForAggregationImpl(segmentName: String, aggregationCode: Int): Long =
    aggregationCode match {
      case AbstractDataSource.AggregationCodeZero => maxSegmentsCount(segmentName)._4 //sum when no aggregation function selected Int.MaxValue // for filtering
      case AbstractDataSource.AggregationCodeStart => maxSegmentsCount(segmentName)._1
      case AbstractDataSource.AggregationCodeIntersect => maxSegmentsCount(segmentName)._2
      case AbstractDataSource.AggregationCodeStop => maxSegmentsCount(segmentName)._3
      case AbstractDataSource.AggregationCodeSum => maxSegmentsCount(segmentName)._4
      case _ => throw new IllegalArgumentException(s"aggregationCode=$aggregationCode")
    }


  //sorted
  def segmentNames(whiteList: Array[Pattern], blackList: Array[Pattern], minCount: Int, maxCount: Int, aggregationCode: Int): Array[String] = {

    val filteredByCount = segmentNames.filter(x => (minCount <= 0 || maxSegmentCountForAggregationImpl(x, aggregationCode) >= minCount) && (maxCount == 0 || maxSegmentCountForAggregationImpl(x, aggregationCode) <= maxCount))
    val filteredByCountAndBothLists =
      if (whiteList.nonEmpty) filteredByCount.filter(x => whiteList.map(p => p.matcher(x)).exists(_.matches())) else filteredByCount

    val ret = if (blackList.nonEmpty) {
      filteredByCountAndBothLists
        .filter(x => !blackList.map(p => p.matcher(x)).exists(_.matches()))
    } else filteredByCountAndBothLists
    logFilteredSegments(ret)
    ret
  }

  def logFilteredSegments(s: Array[String]): Unit = {}

  def classesCount: Int // classes are zero-based integers (0, 1, 2 etc)

  def maxSegmentsCount(name: String): (Long, Long, Long, Long) // max. sum of counts of all classes for a time window (to scale heights of charts for a segment)

  // for allowing parallel requests to a storage and caching (if implemented)
  def goingToRequest(startTwIndex: Int, endTwIndex: Int, includingIds: Boolean, includingSegments: Boolean): Unit //endTwIndex excluding

  def segmentsCount(twIndex: Int): Map[String, List[(Int, Long, Long, Long)]] // name -> class -> count

  def segmentIds(twIndex: Int): Map[String, Map[String, Int]] // name -> ID -> class

  def segments(twIndex: Int): Map[String, Map[Int, List[(String, Long, Long)]]] // name -> class ->{(ID, startTimeMs, durationMs)}

  def fetchAll(callback: Consumer[Int]): Unit =
    (0 until twCount)
      .foreach { tw =>
        segmentIds(tw)
        segments(tw)
        if (tw % 10 == 0) callback.accept(tw)
      }

  def forgetSegmentsCount(twIndex: Int): Unit

  def forgetSegmentCounts(): Unit = (0 until twCount).foreach(forgetSegmentsCount)


}

object AbstractDataSource {
  private val AggregationCodeZero = 0

  val AggregationCodeFirstValue = 1
  val AggregationCodeStart = AggregationCodeFirstValue
  val AggregationCodeIntersect = AggregationCodeStart + 1
  val AggregationCodeStop = AggregationCodeIntersect + 1
  val AggregationCodeSum = AggregationCodeStop + 1

  val AggregationCodeLastValue = AggregationCodeSum

  val AggregationCodes = AggregationCodeLastValue - AggregationCodeFirstValue + 1
}