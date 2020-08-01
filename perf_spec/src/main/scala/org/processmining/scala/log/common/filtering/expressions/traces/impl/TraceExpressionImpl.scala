package org.processmining.scala.log.common.filtering.expressions.traces.impl

import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import org.processmining.scala.log.common.filtering.expressions.traces.{SubtraceExpression, TracePredicate}
import org.processmining.scala.log.common.unified.event.{CommonAttributeSchemas, UnifiedEvent, UnifiedEventHelper}
import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace
import org.processmining.scala.log.common.unified.trace.{UnifiedEventAggregator, UnifiedTraceId, UnifiedTraceIdImpl}

import scala.collection.immutable.SortedMap

private[traces] class TraceExpressionImpl(
                                           private val func: (UnifiedTrace => UnifiedTrace),
                                           private val debug: String => Unit = { x => () /*println(x)*/}) extends TraceExpression {

  override def transform(trace: UnifiedTrace): UnifiedTrace = func(trace)

  private def subtraceImpl(pattern: Pattern, traceToBeSent: UnifiedTrace): UnifiedTrace = {
    val trace = transform(traceToBeSent)
    (trace._1, subtrace2Log(pattern, trace._2).flatten)
  }

  private def keepLastImpl(removePattern: Pattern, lastPattern: Pattern, factory: (UnifiedEvent, Int) => UnifiedEvent, traceToBeSent: UnifiedTrace): UnifiedTrace = {
    val trace = transform(traceToBeSent)
    val events = trace._2.zipWithIndex
    val lastEvent = events.reverse.find(e => lastPattern.matcher(e._1.regexpableForm()).matches())
    if (lastEvent.isDefined) {

      val removedEventsCount = trace._2
        .filter(e => removePattern.matcher(e.regexpableForm()).matches())
        .size
      val eventsWithoutLast = events.filter(_._2 != lastEvent.get._2)
      val newEvents = (factory(lastEvent.get._1, removedEventsCount) ::
        eventsWithoutLast
          .map(_._1)
          .filter(e => !removePattern.matcher(e.regexpableForm()).matches()))
        .sortBy(_.timestamp)
      (trace._1, newEvents)
    } else trace

    //(trace._1, subtrace2Log(pattern, trace._2).flatten)
  }

  override def keepLast(removePattern: Pattern, lastPattern: Pattern, attrName: String): TraceExpression = {
    debug(s"keepLast removePattern: '$removePattern' lastPattern = '$lastPattern'")

    def helper(e: UnifiedEvent, counter: Int): UnifiedEvent =
      e.copy(attrName, counter)

    new TraceExpressionImpl(keepLastImpl(removePattern, lastPattern, helper, _: UnifiedTrace))
  }

  override def keepLast(removePattern: Pattern, lastPattern: Pattern, factory: (UnifiedEvent, Int) => UnifiedEvent): TraceExpression = {
    debug(s"keepLast removePattern: '$removePattern' lastPattern = '$lastPattern'")
    new TraceExpressionImpl(keepLastImpl(removePattern, lastPattern, factory, _: UnifiedTrace))
  }


  private def extractSubtracesImpl(pattern: Pattern, traceToBeSent: UnifiedTrace): List[UnifiedTrace] = {
    val trace = transform(traceToBeSent)
    subtrace2Log(pattern, trace._2)
      .zipWithIndex
      .map { p => (new UnifiedTraceIdImpl(trace._1 + "_" + p._2), p._1) }
  }

  private def subtrace2Log(pattern: Pattern, trace: List[UnifiedEvent]): List[List[UnifiedEvent]] = {
    val regexpableForm = trace.map {
      _.regexpableForm()
    }
    val regexForm = regexpableForm.mkString("")
    debug(s"T: $regexForm")
    val matcher = pattern.matcher(regexForm)
    var subtraces = List[List[UnifiedEvent]]()
    while (matcher.find() /* && matcher.groupCount() > 0 */ ) {
      val sequence = matcher.group(0)
      debug(s"S: $sequence")
      val beforePattern = regexForm.substring(0, matcher.start())
      val beforePatternSequenceLength: Int = TraceExpressionImpl.getEventsCount(beforePattern)
      val sequenceEventsCount = TraceExpressionImpl.getEventsCount(sequence)
      subtraces = trace
        .zipWithIndex
        .filter { t => t._2 >= beforePatternSequenceLength && t._2 < beforePatternSequenceLength + sequenceEventsCount }
        .map {
          _._1
        } :: subtraces
    }
    subtraces
  }


  private def matchWorkaround(pattern: Pattern, regexForm: String): Boolean = {
    val matcher = pattern.matcher(regexForm)
    if (matcher.matches()) matcher.replaceFirst("").isEmpty else false
  }

  private def matchesImpl(
                           pattern: Pattern,
                           useMatches: Boolean,
                           traceToBeSent: UnifiedTrace): UnifiedTrace = {
    val trace = transform(traceToBeSent)
    val regexForm = trace
      ._2
      .map(_.regexpableForm())
      .mkString("")
    val matches = if (useMatches) matchWorkaround(pattern, regexForm) else pattern.matcher(regexForm).find()
    debug(s"${if (matches) "M" else "N"}: $regexForm")
    (trace._1, if (matches) trace._2 else List())
  }


  override def subtrace(eventExpression: Pattern): TraceExpression = {
    debug(s"subtrace pattern: '$eventExpression'")
    new TraceExpressionImpl(subtraceImpl(eventExpression, _: UnifiedTrace))
  }

  override def aggregateSubtraces(pair: (Pattern, String)): TraceExpression = {
    val (eventExpression, name) = pair
    debug(s"aggregate pattern: '$eventExpression' name: '$name'")
    new TraceExpressionImpl(aggregateSubtracesImpl(pair, _: UnifiedTrace))
  }

  private def aggregateSubtracesImpl(pair: (Pattern, String), traceToBeSent: UnifiedTrace): UnifiedTrace = {
    val trace = transform(traceToBeSent)
    val subtraces = subtrace2Log(pair._1, trace._2)
    val events2remove = subtraces.flatten
    val eventsWithoutSubtraces = trace._2.filter(x => !events2remove.contains(x))
    val aggregatedSubtraces = subtraces.map(events => UnifiedEvent(
      events.head.timestamp,
      pair._2,
      SortedMap(CommonAttributeSchemas.AttrNameDuration -> (events.last.timestamp - events.head.timestamp),
        CommonAttributeSchemas.AttrNameSize -> events.size),
      Some(events)))
    val newTraces = (eventsWithoutSubtraces ::: aggregatedSubtraces).sortBy(_.timestamp)
    (trace._1, newTraces)
  }


  def aggregateImpl(aggregator: UnifiedTrace => UnifiedTrace, traceToBeSent: UnifiedTrace): UnifiedTrace = {
    val trace = transform(traceToBeSent)
    aggregator(trace)
  }

  override def aggregate(aggregator: UnifiedTrace => UnifiedTrace): TraceExpression = {
    debug(s"aggregator")
    new TraceExpressionImpl(aggregateImpl(aggregator, _: UnifiedTrace))
  }


  def deaggregateImpl(traceToBeSent: UnifiedTrace): UnifiedTrace = {
    val trace = transform(traceToBeSent)
    UnifiedEventAggregator.deaggregate(trace)
  }

  override def deaggregate(): TraceExpression = {
    debug(s"deaggregator")
    new TraceExpressionImpl(deaggregateImpl(_: UnifiedTrace))
  }

  override def extractSubtraces(eventExpression: Pattern): SubtraceExpression = {
    debug(s"subtrace extractSubtraces: '$eventExpression'")
    new SubtraceExpressionImpl(extractSubtracesImpl(eventExpression, _: UnifiedTrace))
  }

  override def contains(eventExpression: Pattern): TracePredicate = {
    debug(s"contains pattern: '$eventExpression'")
    new TracePredicateImpl(trace => matchesImpl(eventExpression, false, trace)._2.nonEmpty)
  }

  override def matches(eventExpression: Pattern): TracePredicate = {
    debug(s"matches pattern: '$eventExpression'")
    new TracePredicateImpl(trace => matchesImpl(eventExpression, true, trace)._2.nonEmpty)
  }

  private def trimToTimeframeImpl(from: Long, to: Long, traceToBeSent: UnifiedTrace): UnifiedTrace = {
    val trace = transform(traceToBeSent)
    (trace._1, trace._2.filter { e => e.timestamp >= from && e.timestamp <= to })
  }

  override def trimToTimeframe(from: Long, to: Long): TraceExpression =
    new TraceExpressionImpl(trimToTimeframeImpl(from, to, _: UnifiedTrace))


  def modifyEventsImpl(t: (UnifiedTrace, UnifiedEvent, Int) => UnifiedEvent, traceToBeSent: UnifiedTrace): UnifiedTrace = {
    val trace = transform(traceToBeSent)
    (trace._1, trace._2.zipWithIndex.map(x => t(trace, x._1, x._2)))
  }

  override def modifyEvents(t: (UnifiedTrace, UnifiedEvent, Int) => UnifiedEvent): TraceExpression =
    new TraceExpressionImpl(modifyEventsImpl(t, _: UnifiedTrace))


  def keepContainedInTimeframeImpl(from: Long, to: Long, traceToBeSent: UnifiedTrace): Boolean = {
    val trace = transform(traceToBeSent)
    trace._2.head.timestamp >= from && trace._2.last.timestamp <= to
  }

  override def keepContainedInTimeframe(from: Long, to: Long): TracePredicate =
    new TracePredicateImpl(trace => keepContainedInTimeframeImpl(from, to, trace))


  private def keepIntersectingTimeframeImpl(from: Long, to: Long, traceToBeSent: UnifiedTrace): Boolean = {
    val trace = transform(traceToBeSent)
    ((trace._2.head.timestamp <= from && trace._2.last.timestamp >= from) || (trace._2.head.timestamp <= to && trace._2.last.timestamp >= to))
  }

  override def keepIntersectingTimeframe(from: Long, to: Long): TracePredicate =
    new TracePredicateImpl(trace => keepIntersectingTimeframeImpl(from, to, trace))

  private def keepWithSomeEventsWithinTimeframeImpl(from: Long, to: Long, traceToBeSent: UnifiedTrace): Boolean = {
    val trace = transform(traceToBeSent)
    trace._2.exists(e => e.timestamp >= from && e.timestamp <= to)
  }

  override def keepWithSomeEventsWithinTimeframe(from: Long, to: Long): TracePredicate =
    new TracePredicateImpl(trace => keepWithSomeEventsWithinTimeframeImpl(from, to, trace))

  private def keepStartedInTimeframeImpl(from: Long, to: Long, traceToBeSent: UnifiedTrace): Boolean = {
    val trace = transform(traceToBeSent)
    trace._2.head.timestamp >= from && trace._2.head.timestamp <= to
  }

  override def keepStartedInTimeframe(from: Long, to: Long): TracePredicate =
    new TracePredicateImpl(trace => keepStartedInTimeframeImpl(from, to, trace))


  private def keepCompletedInTimeframeImpl(from: Long, to: Long, traceToBeSent: UnifiedTrace): Boolean = {
    val trace = transform(traceToBeSent)
    trace._2.last.timestamp >= from && trace._2.last.timestamp <= to
  }

  override def keepCompletedInTimeframe(from: Long, to: Long): TracePredicate =
    new TracePredicateImpl(trace => keepCompletedInTimeframeImpl(from, to, trace))


  override def |>>| : (Long, Long) => TraceExpression = trimToTimeframe

  override def >/>>/> : (Long, Long) => TracePredicate = keepIntersectingTimeframe

  override def />>/ : (Long, Long) => TracePredicate = keepContainedInTimeframe

  override def />>/> : (Long, Long) => TracePredicate = keepStartedInTimeframe

  override def >/>>/ : (Long, Long) => TracePredicate = keepCompletedInTimeframe

  override def ~|>|~ : (Long, Long) => TracePredicate = keepWithSomeEventsWithinTimeframe

  private def lengthImpl(traceEx: TraceExpressionImpl, from: Int, until: Int, traceToBeSent: UnifiedTrace): Boolean = {
    val trace = transform(traceToBeSent)
    trace._2.size >= from && trace._2.size < until
  }

  override def length(from: Int, until: Int): TracePredicate =
    new TracePredicateImpl(lengthImpl(this, from, until, _: UnifiedTrace))


  private def durationImpl(traceEx: TraceExpressionImpl, from: Long, until: Long, traceToBeSent: UnifiedTrace): Boolean = {
    val trace = transform(traceToBeSent)
    val d = trace._2.last.timestamp - trace._2.head.timestamp
    d >= from && d < until
  }

  override def duration(from: Long, until: Long): TracePredicate =
    new TracePredicateImpl(durationImpl(this, from, until, _: UnifiedTrace))


  private def withArtificialStartsStopsImpl(traceEx: TraceExpressionImpl,
                                            artificialStartFactory: (UnifiedTraceId, UnifiedEvent) => UnifiedEvent,
                                            artificialEndFactory: (UnifiedTraceId, UnifiedEvent) => UnifiedEvent,
                                            traceToBeSent: UnifiedTrace): UnifiedTrace = {

    val trace = transform(traceToBeSent)
    val artificialStart =
      if (trace._2.nonEmpty) List[UnifiedEvent](artificialStartFactory(trace._1, trace._2.head)) else List[UnifiedEvent]()
    val artificialEnd =
      if (trace._2.nonEmpty) List[UnifiedEvent](artificialEndFactory(trace._1, trace._2.last)) else List[UnifiedEvent]()
    (trace._1, (artificialStart ::: trace._2) ::: artificialEnd)
  }

  private def takeImpl(traceEx: TraceExpressionImpl, num: Int, traceToBeSent: UnifiedTrace): UnifiedTrace = {
    val trace = transform(traceToBeSent)
    (trace._1, trace._2.take(num))
  }

  override def withArtificialStartsStops(
                                          artificialStartFactory: (UnifiedTraceId, UnifiedEvent) => UnifiedEvent,
                                          artificialEndFactory: (UnifiedTraceId, UnifiedEvent) => UnifiedEvent): TraceExpression =
    new TraceExpressionImpl(withArtificialStartsStopsImpl(this, artificialStartFactory, artificialEndFactory, _: UnifiedTrace))

  override def take(num: Int) = new TraceExpressionImpl(takeImpl(this, num, _: UnifiedTrace))
}

private[filtering] object TraceExpressionImpl {

  def getEventsCount(events: String): Int = {
    val lefts = StringUtils.countMatches(events, UnifiedEventHelper.LeftEventSep)
    val rights = StringUtils.countMatches(events, UnifiedEventHelper.RightEventSep)
    if (lefts != rights) {
      throw new IllegalArgumentException(s"Subtrace $events contains different numbers of start/end sequences")
    }
    lefts
  }

}



