package org.processmining.scala.viewers.spectrum.model

import java.util.function.Consumer
import java.util.regex.Pattern

/**
  * The datasource interface of a Performance Spectrum to be visualized in the PSM
  */
abstract class AbstractDataSource {

  /**
    *
    * @return number of bins in the Performance Spectrum (PS)
    */
  def twCount: Int

  /**
    *
    * @return size of one bin, in ms
    */
  def twSizeMs: Long

  /**
    *
    * @return UNIX time of the beggining of very first PS bin (in ms, UTC time zone)
    */
  def startTimeMs: Long

  /**
    *
    * @return all the segment presented in the PS after segment aggregation. Provide here a sorting order for the view
    */
  def segmentNames: Array[String]

  /**
    *
    * @return a string of the legend in format "TITLE%CLASS0_NAME%CLASS1_NAME%CLASSn_NAME"
    */
  def legend: String

  /**
    *
    * @return classifier name
    */
  def classifierName: String

  /**
    *
    * @param timeMs absoulte time (UNIX time, ms)
    * @return a corresponding bin (time window) index
    */
  def getTimeWindowByAbsTime(timeMs: Long): Int = ((timeMs - startTimeMs) / twSizeMs).toInt

  /**
    * Initialization of the data source (if required)
    *
    * @param callback an optional callback to indicate progress for the UI
    */
  def initialize(callback: Consumer[Int]): Unit = {}

  /**
    *
    * @param segmentName     segment name
    * @param aggregationCode grouping
    * @return max. observed value for the given aggregation (grouping) for the given segment
    */
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


  /**
    * Provides segments to be shown in the view
    *
    * @param whiteList       regex for the segment white list
    * @param blackList       regex for the segment black list
    * @param minCount        filtering: min. allowed value of the max. observed value for the given aggregation (grouping)
    * @param maxCount        filtering: max. allowed value of the max. observed value for the given aggregation (grouping)
    * @param aggregationCode aggregation (grouping)
    * @return sorted array of segement names
    */
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

  /**
    * Hook for logging of segment filtering
    *
    * @param segmentNames array of segment names to be logged
    */
  def logFilteredSegments(segmentNames: Array[String]): Unit = {}

  /**
    *
    * @return class number (min. allowed value is 1)
    */
  def classesCount: Int

  /**
    * Provides max. observed value for supported aggregation (grouping) for the given segment
    *
    * @param name segment name
    * @return values for grouping start, pending, end, sum of all the previous
    */
  def maxSegmentsCount(name: String): (Long, Long, Long, Long) //

  /**
    * Provides aggregate PS for a bin
    *
    * @param twIndex bin index
    * @return a map: segment name  ->  {class, start, pending, end)
    */
  def segmentsCount(twIndex: Int): Map[String, List[(Int, Long, Long, Long)]]

  /**
    * Provides segment occurrences that start in the given bin
    *
    * @param twIndex bin index
    * @return two maps: segment name  -> caseID -> class_value. Class values are zero-based
    */
  def segmentIds(twIndex: Int): Map[String, Map[String, Int]] // name -> ID -> class

  /**
    * Provides detailed PS for a bin
    *
    * @param twIndex bin index
    * @return two maps: segment name -> class ->{(caseID, startTimeMs, durationMs)}
    */
  def segments(twIndex: Int): Map[String, Map[Int, List[(String, Long, Long)]]]

  /**
    * Helper for cache management that forces reading all the data from disk into memory
    *
    * @param callback a callback for reporting progress in the UI
    */
  def fetchAll(callback: Consumer[Int]): Unit =
    (0 until twCount)
      .foreach { tw =>
        segmentIds(tw)
        segments(tw)
        if (tw % 10 == 0) callback.accept(tw)
      }

  /**
    * Forces forgetting all the cached data for the given bin
    *
    * @param twIndex bin index
    */
  def forgetSegmentsCount(twIndex: Int): Unit

  /**
    * Forces forgetting all the cached data
    */
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