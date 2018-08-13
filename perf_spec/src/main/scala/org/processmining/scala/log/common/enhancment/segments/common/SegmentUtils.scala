package org.processmining.scala.log.common.enhancment.segments.common

import org.processmining.scala.log.common.types.Segment
import org.processmining.scala.log.common.unified.event.{CommonAttributeSchemas, UnifiedEvent}
import org.processmining.scala.log.common.unified.trace.UnifiedTraceId

import scala.collection.immutable.SortedMap

object SegmentUtils {

  val DefaultSeparator = ":"

  def convertToSegments(sep: String, tracePair: (UnifiedTraceId, List[UnifiedEvent])): (UnifiedTraceId, List[UnifiedEvent]) =
    tracePair match {
      case (unifiedTrace, unifiedEvents) =>
        (unifiedTrace, unifiedEvents
          // converting  into list of segments (E, E)
          ./:((List[(UnifiedEvent, UnifiedEvent)](), None: Option[UnifiedEvent]))((l: (List[(UnifiedEvent, UnifiedEvent)], Option[UnifiedEvent]), e: UnifiedEvent) =>
          if (l._2.isEmpty) (l._1, Some(e)) else ((l._2.get, e) :: l._1, Some(e)))
          ._1
          .map(s => Segment(unifiedTrace.id,
            s._1.activity + sep + s._2.activity,
            s._1.timestamp,
            s._2.timestamp - s._1.timestamp))
          .map(s => UnifiedEvent(
            s.timestamp,
            s.key,
            SortedMap(CommonAttributeSchemas.AttrNameDuration -> s.duration),
            None)
          )
          .reverse)
    }



}
