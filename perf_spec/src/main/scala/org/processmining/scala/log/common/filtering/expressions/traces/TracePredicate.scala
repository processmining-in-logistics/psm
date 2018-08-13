package org.processmining.scala.log.common.filtering.expressions.traces

import org.processmining.scala.log.common.filtering.expressions.events.common.AbstractTracePredicate


/**
  * Defines boolean logical operators on filtering expressions
  */
trait TracePredicate extends AbstractTracePredicate {

  /** OR operator */
  def or(tracePred: AbstractTracePredicate): TracePredicate

  /** AND operator */
  def and(tracePred: AbstractTracePredicate): TracePredicate

  /** NOT operator */
  def unary_!(): TracePredicate

  /**
    * Depending on the predicate evaluation return one of the provided trace expressions
    *
    * @param thenExpression
    * @param elseExpression
    * @return traceExpression
    */
  def ifElse(thenExpression: AbstractTraceExpression, elseExpression: AbstractTraceExpression): AbstractTraceExpression


}
