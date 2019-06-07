package org.processmining.scala.viewers.spectrum.patterns

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.processmining.scala.viewers.spectrum.features.LabelForOneSegmentPerformancePattern
import org.processmining.scala.viewers.spectrum.model.AbstractDataSource
import org.slf4j.LoggerFactory


class OneSegmentPatternsDetector(ds: AbstractDataSource, classes: (Int, Int), minPower: Long, percentileForMaxIntervalsBetweenStartsOrEndsPercent: Int) extends SegmentHelper(ds) {

  private val logger = LoggerFactory.getLogger(classOf[OneSegmentPatternsDetector].getName)

  private def isClassOk(clazz: Int) = clazz >= classes._1 && clazz < classes._2


  private def tryToAddPattern(list: List[OneSegmentPerformancePattern], segments: List[(String, Long, Long, Int)], maxIntervalBetweenStartStopMs: Long): List[OneSegmentPerformancePattern] = {
    if (segments.length < minPower) {
      //logger.debug(s"Too small power ${segments.length}")
      List()
    } else {
      //split into groups where endings satisfy the conditions
      val initialZ: (List[List[(String, Long, Long, Int)]], List[(String, Long, Long, Int)]) = (List(), List())
      val finalZ = segments.foldLeft(initialZ)((z, s) =>
        if (z._2.isEmpty) (z._1, s :: z._2)
        else if (s._3 - z._2.last._3 <= maxIntervalBetweenStartStopMs) (z._1, s :: z._2)
        else (z._2 :: z._1, List())
      )
      val groups = if (finalZ._2.nonEmpty) finalZ._2 :: finalZ._1 else finalZ._1

      //      logger.debug("Candidates:")
      //      groups.foreach(x => logger.debug(x.mkString("; ")))

      val result = groups
        .filter(_.length >= minPower)
        .map(_.reverse)

      //      logger.debug("Result:")
      //      result.foreach(x => logger.debug(x.mkString("; ")))
      result.map(x => OneSegmentPerformancePattern(x.head._2, x.last._2, x.head._3, x.last._3, x.length, classes))
    }
  }

  def getDescriptiveStatisticsOnIntervalsBetweenStartsAndDurations(sortedByStart: Seq[(String, Long, Long, Int)]): (DescriptiveStatistics, DescriptiveStatistics) = {
    val triple = sortedByStart
      .foldLeft((new DescriptiveStatistics(), new DescriptiveStatistics(), -1L))((z, x) => {
        if (z._3 >= 0) {
          z._1.addValue(x._2 - z._3)
          z._2.addValue(x._3 - x._2)
        }
        (z._1, z._2, x._2)
      }
      )
    (triple._1, triple._2)
  }

  case class IntersectionState(nFaster: Int, nSlower: Int, t1: Long, lastFaster: (String, Long, Long, Int), lastSlower: (String, Long, Long, Int))

  def processSegmentWithIntersections(sortedByStart: Seq[(String, Long, Long, Int)]): List[(String, Long, Long, Int)] = {

    val segments = sortedByStart
      .zipWithIndex
      .toArray

    segments.foldLeft((segments, List[(String, Long, Long, Int)]()))((z, head) => {
      val tail = z._1.tail
      val stopIndex = tail.find(x => x._1._2 >= head._1._3 && x._1._3 >= head._1._3)
      if (stopIndex.isEmpty) (tail, z._2)
      else {
        val power = (head._2 until stopIndex.get._2)
          .map(segments(_))
          .count(x => x._1._3 < head._1._3)
        if (power > 0) (tail, head._1 :: z._2)
        else (z._1.tail, z._2)
      }
    }
    )._2.reverse
  }

  def processSegmentWithoutIntersections(sortedByStart: Seq[(String, Long, Long, Int)]): Seq[OneSegmentPerformancePattern] = {
    val (_, descrStatDurations) = getDescriptiveStatisticsOnIntervalsBetweenStartsAndDurations(sortedByStart)
    val maxIntervalBetweenStartStopMs: Long = descrStatDurations.getPercentile(percentileForMaxIntervalsBetweenStartsOrEndsPercent).toLong
    logger.debug(s"maxIntervalBetweenStartStopMs $maxIntervalBetweenStartStopMs")
    val initialZ: (List[OneSegmentPerformancePattern], List[(String, Long, Long, Int)]) = (List(), List())
    sortedByStart.foldLeft(initialZ)((z, s) =>
      if (z._2.isEmpty) {
        if (isClassOk(s._4)) {
          //logger.debug(s"Added 1st $s")
          (z._1, s :: z._2)
        } else z
      }
      else if (isClassOk(s._4) && (s._2 - z._2.last._2) <= maxIntervalBetweenStartStopMs) {
        //logger.debug(s"Added next $s")
        (z._1, s :: z._2)
      }
      else {
        //logger.debug(s"Bad next $s")
        (tryToAddPattern(z._1, z._2.reverse, maxIntervalBetweenStartStopMs) ::: z._1,
          if (isClassOk(s._4)) List(s) else List())
      }
    )._1
    //TODO: try to add the last list
  }

  def processSegmentForQueues(name: String): Seq[OneSegmentPerformancePattern] = {
    val segments = getOrderedByStartSegments(name)
    if (!hasIntersections(segments)) {
      logger.info(s"SEGMENT $name does not have intersections")
      if (segments.filter(x => isClassOk(x._4)).length < segments.length * 0.65) {
        processSegmentWithoutIntersections(segments)
      } else Seq()
    }
    else {
      logger.info(s"SEGMENT $name has intersections")
      Seq()
    }
  }

  def processSegmentForIntersections(name: String): List[(String, Long, Long, Int)] = {
    val segments = getOrderedByStartSegments(name)
    if (!hasIntersections(segments)) {
      logger.info(s"SEGMENT $name does not have intersections")
      List()
    }
    else {
      logger.info(s"SEGMENT $name has intersections")
      processSegmentWithIntersections(segments)
    }
  }

  def splitPatterns(list: List[(String, Long, Long, Int)]): List[OneSegmentPerformancePattern] = {

    val initialValue: List[List[(String, Long, Long, Int)]] = List()
    val groups = list.foldLeft((initialValue, List[(String, Long, Long, Int)]()))((z, s) =>
      if (z._2.isEmpty) (z._1, s :: z._2)
      else {
        if (z._2.head._3 > s._2) (z._1, s :: z._2) else (z._2 :: z._1, List(s))
      }
    )._1

    val ret = groups.map(x => {
      val sortedByStart = x.sortBy(_._2)
      val sortedByEnd = x.sortBy(_._3)
      OneSegmentPerformancePattern(sortedByStart.head._2, sortedByEnd.last._3, sortedByStart.head._2, sortedByEnd.last._3, x.length, (0, 0))
    })
    ret
  }


  def processQueues(): Map[String, Seq[OneSegmentPerformancePattern]] =
    ds.segmentNames.map(name => name -> processSegmentForQueues(name))
      .filter(_._2.nonEmpty)
      .toMap

  def processIntersections(): Map[String, List[(String, Long, Long, Int)]] =
    ds.segmentNames.map(name => name -> processSegmentForIntersections(name))
      .filter(_._2.nonEmpty)
      .toMap

  def processIntersections2(): Map[String, Seq[OneSegmentPerformancePattern]] =
    ds.segmentNames.map(name => name -> processSegmentForIntersections(name))
      .filter(_._2.nonEmpty)
      .map(x => x._1 -> splitPatterns(x._2))
      .toMap

  def convert(patterns: Seq[OneSegmentPerformancePattern]): Seq[LabelForOneSegmentPerformancePattern] =
    patterns.map(x => {
      val left = ds.getTimeWindowByAbsTime(x.leftTop)
      val right = ds.getTimeWindowByAbsTime(x.rightBottom)
      val middle = (right+ left)/2
      LabelForOneSegmentPerformancePattern(left, right, middle, classes._1)
    })

}
