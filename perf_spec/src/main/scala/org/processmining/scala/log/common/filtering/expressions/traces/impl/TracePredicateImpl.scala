package org.processmining.scala.log.common.filtering.expressions.traces.impl

import org.processmining.scala.log.common.filtering.expressions.events.common.AbstractTracePredicate
import org.processmining.scala.log.common.filtering.expressions.traces.{AbstractTraceExpression, TracePredicate}
import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace

private [traces] class TracePredicateImpl(private val func: (UnifiedTrace) => Boolean,
                                          private val debug: String => Unit = { x => ()/*println(x)*/}) extends TracePredicate {

  private def orImpl(p1: AbstractTracePredicate, p2: AbstractTracePredicate, trace: UnifiedTrace): Boolean = {
    val x = p1.evaluate(trace) || p2.evaluate(trace)
    debug(s"orImpl($trace)")
    x
  }

  private def andImpl(p1: AbstractTracePredicate, p2: AbstractTracePredicate, trace: UnifiedTrace): Boolean = {
    val x = p1.evaluate(trace) && p2.evaluate(trace)
    debug(s"andImpl($trace)")
    x
  }

  private def notImpl(p1: AbstractTracePredicate, trace: UnifiedTrace): Boolean = {
    val x = !p1.evaluate(trace)
    debug(s"notImpl($trace)")
    x
  }

  private def ifElseImpl(p: AbstractTracePredicate, e1: AbstractTraceExpression, e2: AbstractTraceExpression, trace: UnifiedTrace): UnifiedTrace = {
    if(p.evaluate(trace)) e1.transform(trace) else e2.transform(trace)
  }

  override def or(tracePred: AbstractTracePredicate): TracePredicate = new TracePredicateImpl(orImpl(this, tracePred, _: UnifiedTrace))

  override def and(tracePred: AbstractTracePredicate): TracePredicate = new TracePredicateImpl(andImpl(this, tracePred, _: UnifiedTrace))

  override def unary_!(): TracePredicate = new TracePredicateImpl(notImpl(this, _: UnifiedTrace))

  override def evaluate(trace: UnifiedTrace): Boolean = func(trace)

  override def ifElse(thenExpression: AbstractTraceExpression, elseExpression: AbstractTraceExpression): AbstractTraceExpression =
    new TraceExpressionImpl(ifElseImpl(this, thenExpression, elseExpression, _: UnifiedTrace))



}

