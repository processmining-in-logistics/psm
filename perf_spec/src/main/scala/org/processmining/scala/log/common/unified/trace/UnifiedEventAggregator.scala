package org.processmining.scala.log.common.unified.trace

import org.processmining.scala.log.common.unified.event.{CommonAttributeSchemas, UnifiedEvent}
import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace
import org.processmining.scala.log.common.utils.common.EventAggregator

import scala.collection.immutable.SortedMap

/** Implemets aggregation/deaggregation */
object UnifiedEventAggregator {

  private def createAggregatedEvent(activity: String, events: List[UnifiedEvent]): UnifiedEvent =
    UnifiedEvent.apply(
      events.head.timestamp,
      activity,
      SortedMap(CommonAttributeSchemas.AttrNameDuration -> (events.last.timestamp - events.head.timestamp), CommonAttributeSchemas.AttrNameSize -> events.size),
      Some(events)
    )

  private def aggregate(aggregator: EventAggregator, trace: UnifiedTrace): UnifiedTrace = {
    val eventsPairedWithAggregatedActivities = trace
      ._2
      .map(e => (e, aggregator.aggregate(e.activity)))

    val initialValueForAggregatedTrace = (List[UnifiedEvent](), None: Option[(String, List[UnifiedEvent])])

    val aggregatedTrace = eventsPairedWithAggregatedActivities
      .foldLeft(initialValueForAggregatedTrace)(
        (z: (List[UnifiedEvent], Option[(String, List[UnifiedEvent])]),
         e: (UnifiedEvent, String)
        )
        =>
          if (!z._2.isDefined) (z._1, Some((e._2, List(e._1)))) //a new sequence of events is empty: creating a new sequence of events is empty
          else if (z._2.get._1 == e._2) (z._1, Some((z._2.get._1, z._2.get._2 ::: List[UnifiedEvent](e._1)))) // adding to the existing sequence
          else (z._1 ::: List(createAggregatedEvent(z._2.get._1, z._2.get._2)), // new activity: completing the current sequence
            Some((e._2, List(e._1)))) //and adding a new sequence
      )

    val finalTrace =
      if (aggregatedTrace._2.isDefined)
        aggregatedTrace._1 ::: List(createAggregatedEvent(aggregatedTrace._2.get._1, aggregatedTrace._2.get._2))
      else aggregatedTrace._1

    (trace._1, finalTrace)
  }

  /** De-aggregates events of traces */
  def deaggregate(trace: UnifiedTrace): UnifiedTrace =
    (trace._1, trace
      ._2
      .flatMap(e => if(e.aggregatedEvents.isDefined) e.aggregatedEvents.get else List(e))
    )

  /** Factory for aggregation functions */
  def apply(ea: EventAggregator): UnifiedTrace => UnifiedTrace =
    UnifiedEventAggregator.aggregate(ea, _ : UnifiedTrace)
}
