package org.processmining.scala.log.common.enhancment.segments.common

import org.processmining.scala.log.common.types.{Clazz, Segment, SegmentWithClazz, Timestamp}

object ClazzBasedSegmentProcessorHelper2 {

  //for internal processing
  case class SegementWithEndTimestamp(segment: Segment, timestamp: Long) extends Serializable with Timestamp

  case class TimestampClazzImpl(timestamp: Long, clazz: Int) extends Timestamp with Clazz


  /**
    * Maps events to segments of one case
    *
    * @param segments           for a case
    * @param optionalAttributes for a case
    * @return classified segments
    */
  def addAttribute[T](
                       segments: Iterable[Segment],
                       optionalAttributes: Option[Iterable[T]],
                       id: T => String,
                       timestamp: T => Long,
                       clazz: T => Int
                     ): List[SegmentWithClazz] = {
    val attributesAsTimestamp =
      if (optionalAttributes.isDefined)
        optionalAttributes.get.toList.map(x => TimestampClazzImpl(timestamp(x), clazz(x)).asInstanceOf[Timestamp])
      else List()

    val segmentsAsTimestamp = segments
      .toList
      .map { x => SegementWithEndTimestamp(x, x.timestamp + x.duration).asInstanceOf[Timestamp] }
    (segmentsAsTimestamp ::: attributesAsTimestamp)
      .sortBy(_.timestamp)
      ./:((List[SegmentWithClazz](), 0)) { (z, o) =>
        o match {
          case a: TimestampClazzImpl => (z._1, a.clazz)
          case s: SegementWithEndTimestamp => (SegmentWithClazz(s.segment.id, s.segment.key, s.segment.timestamp, s.segment.duration, z._2) :: z._1, z._2)
        }
      }._1
  }

}


