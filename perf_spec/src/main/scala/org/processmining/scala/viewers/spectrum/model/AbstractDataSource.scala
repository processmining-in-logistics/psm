package org.processmining.scala.viewers.spectrum.model

import java.util.function.Consumer
import java.util.regex.Pattern


// Prerequisite: all the data for at least 1 time window must fit into the heap of JVM and prefferably into RAM
private[viewers] trait AbstractDataSource {

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

  //sorted
  def segmentNames(whiteList: Array[Pattern], blackList: Array[Pattern], minCount: Int, maxCount: Int): Array[String] = {
    val filteredByCount = segmentNames.filter(x => (minCount <= 0 || maxSegmentsCount(x) >= minCount) && (maxCount == 0 || maxSegmentsCount(x) <= maxCount))
    val filteredByCountAndBothLists =
      if (whiteList.nonEmpty) filteredByCount.filter(x => whiteList.map(p => p.matcher(x)).exists(_.matches())) else filteredByCount

    if (blackList.nonEmpty) {
      filteredByCountAndBothLists
        .filter(x => !blackList.map(p => p.matcher(x)).exists(_.matches()))
    } else filteredByCountAndBothLists
  }

  def classesCount: Int // classes are zero-based integers (0, 1, 2 etc)

  def maxSegmentsCount(name: String): Long // max. sum of counts of all classes for a time window (to scale heights of charts for a segment)

  // for allowing parallel requests to a storage and caching (if implemented)
  def goingToRequest(startTwIndex: Int, endTwIndex: Int, includingIds: Boolean, includingSegments: Boolean): Unit //endTwIndex excluding

  def segmentsCount(twIndex: Int): Map[String, List[(Int, Long)]] // name -> class -> count

  def segmentIds(twIndex: Int): Map[String, Map[String, Int]] // name -> ID -> class

  def segments(twIndex: Int): Map[String, Map[Int, List[(String, Long, Long)]]] // name -> class ->{(ID, startTimeMs, durationMs)}

  def fetchAll(callback: Consumer[Int]): Unit =
    (0 until twCount)
      .foreach { tw =>
        segmentIds(tw)
        segments(tw)
        if (tw % 10 == 0) callback.accept(tw)
      }


}
