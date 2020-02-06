package org.processmining.scala.viewers.spectrum2.model

import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.viewers.spectrum2.view.SortingOrderEntry

import scala.swing.Color


case class SegmentName(a: String, b: String) {
  override def toString: String = s"$a${SegmentName.DefaultSeparator}$b"
}

object SegmentName {
  val DefaultSeparator = ":"

  def apply(ab: String): SegmentName = {
    val arr = ab.split(":")
    new SegmentName(arr(0), arr(1))
  }
}

abstract class SegmentEvent {
  val minMs: Long
  val maxMs: Long

  def isObserved: Boolean = minMs == maxMs

  def absDuration: Long = Math.abs(maxMs - minMs)

}

case class SegmentEventImpl(e: UnifiedEvent) extends SegmentEvent {

  override val minMs: Long = e.timestamp

  val maxMs: Long = {
    val maxMsOpt = e.attributes.get(AbstractDataSource.MaxTsName)
    if (maxMsOpt.isDefined) maxMsOpt.get.asInstanceOf[Long] else minMs
  }

  override val isObserved: Boolean = minMs == maxMs
}

object SegmentEventImpl {
  def apply(e: UnifiedEvent): SegmentEventImpl = new SegmentEventImpl(e)
}


case class Segment(
                    caseId: String,
                    start: SegmentEvent,
                    end: SegmentEvent,
                    //phaseResourceUsePercent: Float,
                    caseClass: Int
                  )

case class OverlaidSegment(caseId: String,
                           srcSegment: SegmentName,
                           dstSegment: SegmentName,
                           overlaidSegment: SegmentName,
                           timestamp1: Long,
                           timestamp2: Long,
                           color: Color)


abstract class AbstractDataSource {
  /**
    *
    * @return UNIX time of the earliest event (in ms, UTC time zone)
    */
  def startTimeMs: Long

  /**
    *
    * @return UNIX time of the latest event (in ms, UTC time zone)
    */
  def endTimeMs: Long

  def durationMs: Long = endTimeMs - startTimeMs


  def segmentNames: Vector[SegmentName]

  def segments: Map[SegmentName, Vector[Segment]]

  def caseIdToOriginActivityAndIndex: Map[String, (String, Int)]

  def sortingOrder(): Vector[SortingOrderEntry] = AbstractDataSource.buildSortingOrderFromSegmentNames(segmentNames)

  def overlaidSegments(srcSegmentName: SegmentName): Vector[OverlaidSegment] = Vector()

}

object AbstractDataSource {
  val MaxTsName = "maxTs"
  val ObservationBasedShift = "shiftMs"
  val Ttp = "ttp"

  def buildSortingOrderFromSegmentNames(segmentNames: Vector[SegmentName])= Vector(SortingOrderEntry("", segmentNames, Map()))

  val Empty = new AbstractDataSource {
    override def startTimeMs: Long = 0

    override def endTimeMs: Long = 0

    override def segmentNames: Vector[SegmentName] = Vector()

    override def segments: Map[SegmentName, Vector[Segment]] = Map()

    override def caseIdToOriginActivityAndIndex: Map[String, (String, Int)] = Map()

    override def sortingOrder(): Vector[SortingOrderEntry] = Vector()
  }
}