package org.processmining.scala.log.common.filtering.expressions.traces.impl

import org.processmining.scala.log.common.filtering.expressions.traces.SubtraceExpression
import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace

private[traces] class SubtraceExpressionImpl(
                                              private val func: (UnifiedTrace => List[UnifiedTrace]),
                                              private val debug: String => Unit = { _ => }) extends SubtraceExpression{

  override def transform(trace: UnifiedTrace): List[UnifiedTrace] = func(trace)

  //override def subtrace(pattern: Pattern) = ???
}
