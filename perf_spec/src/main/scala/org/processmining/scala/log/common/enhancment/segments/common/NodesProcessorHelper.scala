package org.processmining.scala.log.common.enhancment.segments.common

import org.processmining.scala.log.common.types.{Segment, SegmentWithClazz}

object NodesProcessorHelper2 {
  def addAttribute[T](segments: List[Segment], optionalAttributes: Option[Iterable[T]],
                      id: T => String,
                      timestamp: T => Long,
                      clazz: T => Int): List[SegmentWithClazz] = {
    val attributeMap: Map[Long, Int] =
      if (optionalAttributes.isDefined) optionalAttributes.get.map { x => timestamp(x) -> clazz(x) }.toMap else Map()

    segments
      .map { x =>
        val optionalClazz = attributeMap.get(x.timestamp)
        SegmentWithClazz(x.id, x.key, x.timestamp, x.duration, if (optionalClazz.isDefined) optionalClazz.get else 0)
      }
  }

}
