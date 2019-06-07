package org.processmining.scala.log.common.unified.log.parallel

import java.util.regex.Pattern

import org.processmining.scala.log.common.filtering.expressions.events.regex.EventExpression
import org.processmining.scala.log.common.filtering.expressions.events.common.AbstractTracePredicate
import org.processmining.scala.log.common.filtering.expressions.traces.{AbstractTraceExpression, SubtraceExpression, TracePredicate}
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace
import org.processmining.scala.log.common.unified.trace.UnifiedTraceId

import scala.collection.parallel.ParSeq

//TODO: move to ParMap
private[parallel] class UnifiedEventLogImpl(override val traces: ParSeq[UnifiedTrace],
                                            val debug: String => Unit = { _ => }
                                           ) extends UnifiedEventLog {


  override def withDebugInfo(debug: String => Unit = println(_)) = new UnifiedEventLogImpl(traces, debug)

  override def withoutDebugInfo() = new UnifiedEventLogImpl(traces)

  override def map(ex: AbstractTraceExpression): UnifiedEventLog =
    map(ex.transform(_))

  override def map(f: UnifiedTrace => UnifiedTrace): UnifiedEventLog =
    UnifiedEventLog.fromTraces(
      traces
        .map(f)
        .filter(_._2.nonEmpty)
    )

  override def flatMap(f: UnifiedTrace => List[UnifiedTrace]): UnifiedEventLog =
    UnifiedEventLog.fromTraces(
      traces
        .flatMap(f)
        .filter(_._2.nonEmpty)
    )

  override def flatMap(ex: SubtraceExpression): UnifiedEventLog =
    flatMap(ex.transform(_))


  override def filter(ex: AbstractTracePredicate): UnifiedEventLog =
    UnifiedEventLog
      .fromTraces(
        traces
          .filter(ex.evaluate))


  override def take(n: Int): UnifiedEventLog =
    UnifiedEventLog
      .fromTraces(
        traces
          .take(n))


  override def filterByAttributeNames(attrNames: Set[String]): UnifiedEventLog =
    UnifiedEventLog.fromTraces(
      traces
        .map(x => (x._1, x._2.filter(_.hasAttributes(attrNames))))
        .filter(_._2.nonEmpty)
    )

  //  override def filter(schema: StructType): UnifiedEventLog = {
  //    schema.simpleString
  //
  //    UnifiedEventLog
  //      .fromTraces(traces
  //        .map(t => (t._1, t._2.filter(_.attrs.schema == schema)))
  //        .filter(_._2.nonEmpty)
  //      )
  //
  //  }


  //  override def filterByAttributeNames(attrNames: Set[String]): UnifiedEventLog =
  //    UnifiedEventLog
  //      .fromTraces(
  //        traces
  //          .mapValues(_.filter(_.hasAttributes(attrNames)))
  //          .filter(_._2.nonEmpty)
  //
  //      )


  override def minMaxTimestamp(): (Long, Long) = {
    val timestamps = traces
      .flatMap(x => x._2.map(_.timestamp))
      .toList
      .sorted
    (timestamps.head, timestamps.last)
  }


  override def fullOuterJoin(that: UnifiedEventLog): UnifiedEventLog =
    UnifiedEventLog.fromTraces(
      traces
        .union(that.traces)
        .groupBy(_._1)
        .map(x => (x._1, x._2.flatMap(_._2).distinct.toList.sortBy(_.timestamp)))
        .toSeq
    )

  override def events(): ParSeq[(UnifiedTraceId, UnifiedEvent)] =
    traces.flatMap(t => t._2.map((t._1, _)))

  private def projectImpl(delete: Boolean, ex: EventExpression*): UnifiedEventLog = {
    val patterns = ex
      .map(_.translate)
      .map(x => {
        debug(x)
        Pattern.compile(x)
      }
      )
    UnifiedEventLog.fromEvents(events.filter(x => {
      val ex = patterns.exists(_.matcher(x._2.regexpableForm()).matches())
      if (delete) !ex else ex
    }))
  }

  override def project(ex: EventExpression*): UnifiedEventLog = projectImpl(false, ex :_*)

  override def remove(ex: EventExpression*): UnifiedEventLog = projectImpl(true, ex :_*)

  override def filterByTraceIds(ids: String*): UnifiedEventLog =
    UnifiedEventLog.fromTraces(traces.filter(x => ids.contains(x._1.id)))

  override def find(id: String): Option[UnifiedTrace] =
    traces.find(_._1.id == id)

  override def projectAttributes(attrNames: Set[String]) =
    UnifiedEventLog.fromTraces(traces.map(x => (x._1, x._2.map(_.project(attrNames)))))
}

private[parallel] object UnifiedEventLogImpl {
  def groupAndSort(events: ParSeq[(UnifiedTraceId, UnifiedEvent)]): ParSeq[UnifiedTrace] =
    events
      .groupBy(_._1)
      .map {
        x => (x._1, x._2.map(_._2).toList.sortBy(_.timestamp))
      }.toSeq
}