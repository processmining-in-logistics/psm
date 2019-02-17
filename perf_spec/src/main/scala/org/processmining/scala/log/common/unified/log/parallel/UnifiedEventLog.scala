package org.processmining.scala.log.common.unified.log.parallel

import org.processmining.scala.log.common.filtering.expressions.events.regex.EventExpression
import org.processmining.scala.log.common.filtering.expressions.events.common.AbstractTracePredicate
import org.processmining.scala.log.common.filtering.expressions.traces.{AbstractTraceExpression, SubtraceExpression}
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace
import org.processmining.scala.log.common.unified.trace.{UnifiedTraceId, UnifiedTraceIdImpl}

import scala.collection.parallel.ParSeq

trait UnifiedEventLog extends Serializable {

  def traces(): ParSeq[UnifiedTrace]

  def count():Int = traces().size

  def events(): ParSeq[(UnifiedTraceId, UnifiedEvent)]

  def withDebugInfo(debug: String => Unit = println(_)): UnifiedEventLog

  def withoutDebugInfo(): UnifiedEventLog

  //type LogEvent = Traceable

  def map(ex: AbstractTraceExpression): UnifiedEventLog

  def map(f: UnifiedTrace => UnifiedTrace): UnifiedEventLog

  def flatMap(ex: SubtraceExpression): UnifiedEventLog

  def flatMap(f: UnifiedTrace => List[UnifiedTrace]): UnifiedEventLog

  def filter(ex: AbstractTracePredicate): UnifiedEventLog

  /** filters events to keep in only events that have the names provided */
  def filterByAttributeNames(attrNames: Set[String]): UnifiedEventLog

  def filterByAttributeNames(attrNames: String*): UnifiedEventLog = filterByAttributeNames(attrNames.toSet)


  def filterByTraceIds(ids: String*): UnifiedEventLog

  def fullOuterJoin(that: UnifiedEventLog): UnifiedEventLog

  def project(ex: EventExpression*): UnifiedEventLog

  /** remove events that match provided patterns */
  def remove(ex: EventExpression*): UnifiedEventLog

  def projectAttributes(attrNames: Set[String]): UnifiedEventLog

  def find(id: String): Option[UnifiedTrace]

  /** returns min and max timestamps or (0, 0) for empty log */
  def minMaxTimestamp(): (Long, Long)
}

object UnifiedEventLog {

  def create(events: ParSeq[(String, UnifiedEvent)]): UnifiedEventLog =
    fromEvents(events.map(x => (new UnifiedTraceIdImpl(x._1), x._2)))

  def createEmpty(): UnifiedEventLog = new UnifiedEventLogImpl(ParSeq())

  def fromEvents(events: ParSeq[(UnifiedTraceId, UnifiedEvent)]): UnifiedEventLog =
    UnifiedEventLog.fromTraces(
      UnifiedEventLogImpl.groupAndSort(
        events
          .map(x => (x._1, x._2))
      )
    )


  def fromTraces(traces: ParSeq[UnifiedTrace]): UnifiedEventLog = new UnifiedEventLogImpl(traces)

  def fromTraces(traces: Seq[UnifiedTrace]): UnifiedEventLog = new UnifiedEventLogImpl(traces.par)
}
