package org.processmining.scala.viewers.spectrum.features

import org.processmining.scala.viewers.spectrum.model.AbstractDataSource
import org.processmining.scala.viewers.spectrum.patterns.SegmentHelper
import org.slf4j.LoggerFactory

case class PositionsInCell(data: Map[Int, Map[Int, Long]], positions: Int, classes: Int) { // pos -> class -> count

  def extractFeatures(): Seq[Int] = // for each position number of classes p0c0p0c p1c0p1c1 ...
    (0 until positions).map(p => {
      val x = data.get(p)
      if (x.isDefined) x.get else Map[Int, Long]()
    }).flatMap(p => {
      (0 until classes).map(clazz => {
        val x = p.get(clazz)
        if (x.isDefined) x.get.toInt else 0
      })
    })
}

case class OneCellSegmentData(twIndex: Int, position: Int, clazz: Int)

class PositionsExtractor(ds: AbstractDataSource, positionsNumber: Int, classNumber: Int) extends SegmentHelper(ds) {
  private val logger = LoggerFactory.getLogger(classOf[PositionsExtractor].getName)

  private val onePosSizePercent = 1.0 / positionsNumber * 100.0

  private def getPositionInTw(startMs: Long, endMs: Long, clazz: Int): Seq[OneCellSegmentData] = {
    val firstTwTmp = (startMs - ds.startTimeMs) / ds.twSizeMs
    val lastTwTmp = (endMs - ds.startTimeMs) / ds.twSizeMs
    val firstTw = firstTwTmp
    if (firstTw < 0) throw new IllegalStateException(s"firstTw < 0: $firstTw")
    val lastTw = if (lastTwTmp >= ds.twCount) ds.twCount - 1 else lastTwTmp // segment can be outside the range
    (firstTw to lastTw).map(twIndex => {
      val currentTwStartMs = ds.startTimeMs + twIndex * ds.twSizeMs
      val currentTwEndMs = currentTwStartMs + ds.twSizeMs
      val startWithinTw = if (startMs > currentTwStartMs) startMs else currentTwStartMs
      val endWithinTw = if (endMs < currentTwEndMs) endMs else currentTwEndMs
      val middle = (endWithinTw - startWithinTw) / 2
      if (middle < 0) throw new IllegalStateException(s"middle < 0: $middle")
      val offsetOfThePointMs = startWithinTw + middle
      val duration = endMs - startMs
      val thePoint = offsetOfThePointMs - startMs
      if (thePoint < 0 || thePoint > duration) throw new IllegalStateException(s"Wrong thePoint = $thePoint")
      val percent = 100.0 * thePoint / duration
      val position = (percent / onePosSizePercent).toInt
      OneCellSegmentData(twIndex.toInt, position, clazz)
    })
  }

  private def getMapFromSeq(seq: Seq[OneCellSegmentData]): Map[Int, Long] =
    seq
      .groupBy(_.clazz)
      .map(x => x._1 -> x._2.length.toLong)

  //Returns ordered by twIndex data for every existing twIndex
  def processSegmentForQueues(name: String): Map[Int, PositionsInCell] = {
    val segments = getOrderedByStartSegments(name)
    segments
      .flatMap(s => getPositionInTw(s._2, s._3, s._4))
      .groupBy(_.twIndex)
      .map(x => (x._1, x._2.groupBy(_.position)))
      .map(x => (x._1, x._2.map(y => y._1 -> getMapFromSeq(y._2))))
      .map(x => x._1 -> PositionsInCell(x._2, positionsNumber, classNumber))
  }

  def process(): Map[String, Map[Int, PositionsInCell]] =
    ds.segmentNames
      .map(name => name -> processSegmentForQueues(name))
      .toMap
}
