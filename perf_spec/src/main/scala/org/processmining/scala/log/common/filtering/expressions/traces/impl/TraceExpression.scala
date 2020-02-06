package org.processmining.scala.log.common.filtering.expressions.traces.impl

import java.util.regex.Pattern

import org.processmining.scala.log.common.filtering.expressions.traces.{AbstractTraceExpression, SubtraceExpression, TracePredicate}
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace
import org.processmining.scala.log.common.unified.trace.UnifiedTraceId

/**
  * Defines the interface for building a tree of expressions
  * for trace transformation or evaluation
  */
trait TraceExpression extends AbstractTraceExpression {


  /**
    * Extracts all subtraces of a given trace that match a given pattern
    * When more than 1 subtrace found, they are merged into a single one
    *
    * @param pattern pattern to match subtraces
    * @return found subtrace or empty trace
    */
  def subtrace(pattern: Pattern): TraceExpression

  @deprecated
  def keepLast(removePattern: Pattern, lastPattern: Pattern, attrName: String): TraceExpression

  def keepLast(removePattern: Pattern, lastPattern: Pattern, factory: (UnifiedEvent, Int) => UnifiedEvent): TraceExpression

  /**
    * Extracts all subtraces of a given trace that match a given pattern
    * When more than 1 subtrace found, they are not merged but transformed into new separate traces
    * They get trace IDs as follows: oldID_zeroBasedInedexOfTheSubtrace
    *
    * @param pattern pattern to match subtraces
    * @return a new expression
    */
  def extractSubtraces(pattern: Pattern): SubtraceExpression

  /**
    * Matches the ENTIRE trace to a given pattern (uses Java Matcher.matches)
    *
    * @param pattern
    * @return true if matches
    */
  def matches(pattern: Pattern): TracePredicate

  /**
    * Aggregate events of traces according to a provided function
    *
    * @param aggregator UDF aggregation function
    * @return a new expression
    */
  def aggregate(aggregator: UnifiedTrace => UnifiedTrace): TraceExpression

  def aggregateSubtraces(pair: (Pattern, String)): TraceExpression

  /**
    * De-aggregates previously aggregated events (if any)
    *
    * @return a new expression
    */
  def deaggregate(): TraceExpression

  /**
    * Tries to find at least one matching subtrace (uses Java Matcher.find)
    *
    * @param pattern
    * @return true if found
    */
  def contains(pattern: Pattern): TracePredicate


  /**
    * Removes events that are not in [from, end] time interval
    *
    * @param from inclusive
    * @param to   inclusive
    * @return trace expression
    */
  def trimToTimeframe(from: Long, to: Long): TraceExpression


  /**
    * Modify event(s) if required, based on a whole trace and event's index
    *
    * @param t UDF for modifications
    * @return trace expression
    */
  def modifyEvents(t: (UnifiedTrace, UnifiedEvent, Int) => UnifiedEvent): TraceExpression

  /**
    * Evaluates length of traces
    *
    * @param from  inclusive
    * @param until exclusive
    * @return
    */
  def length(from: Int, until: Int): TracePredicate

  def duration(from: Long, until: Long): TracePredicate

  /**
    * Adds artificial Start/End to a given trace
    * If a trace is empty, no events are added
    *
    * @param artificialStartFactory start event factory
    * @param artificialEndFactory   end event factory
    * @return trace
    */
  def withArtificialStartsStops(
                                 artificialStartFactory: (UnifiedTraceId, UnifiedEvent) => UnifiedEvent,
                                 artificialEndFactory: (UnifiedTraceId, UnifiedEvent) => UnifiedEvent): TraceExpression

  /** Keep in provided number of events in a trace */
  def take(num: Int): TraceExpression

  /** Keep in a trace if all events are contained in the provided time interval (inclusive) */
  def keepContainedInTimeframe(from: Long, to: Long): TracePredicate

  /** Keep in a trace if some events are earlier and later than boundaries of the provided interval (inclusive) */
  def keepIntersectingTimeframe(from: Long, to: Long): TracePredicate

  /** Keep in a trace if at least one event is contained in the provided time interval (inclusive) */
  def keepWithSomeEventsWithinTimeframe(from: Long, to: Long): TracePredicate

  /** Keep in a trace if it started inside the provided time interval (inclusive) */
  def keepStartedInTimeframe(from: Long, to: Long): TracePredicate

  /** Keep in a trace if it completed inside the provided time interval (inclusive) */
  def keepCompletedInTimeframe(from: Long, to: Long): TracePredicate

  /** Shortcut for trimToTimeframe */
  def |>>| : (Long, Long) => TraceExpression

  /** Shortcut for keepIntersectingTimeframe */
  def >/>>/> : (Long, Long) => TracePredicate

  /** Shortcut for keepContainedInTimeframe */
  def />>/ : (Long, Long) => TracePredicate

  /** Shortcut for keepStartedInTimeframe */
  def />>/> : (Long, Long) => TracePredicate

  /** Shortcut for keepCompletedInTimeframe */
  def >/>>/ : (Long, Long) => TracePredicate

  /** Shortcut for keepWithSomeEventsWithinTimeframe */
  def ~|>|~ : (Long, Long) => TracePredicate

}

/** Factory for TraceExpression */
object TraceExpression {
  /**
    * Factory for TraceExpression[E]
    *
    * @return a TraceExpression object that can be used and re-used for buidling expressions
    */
  def create(): TraceExpression = new TraceExpressionImpl(x => x)

  /**
    * Factory for TraceExpression[RegexpableEvent]
    *
    * @return a TraceExpression object that can be used and re-used for buidling expressions
    */
  def apply(): TraceExpression = TraceExpression.create()

}

