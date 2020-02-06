package org.processmining.scala.log.common.filtering.expressions.events.variables

import org.processmining.scala.log.common.filtering.expressions.events.common.AbstractTracePredicate
import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace

/** Bridges  AbstractTracePredicate to AbstractVarPredicate. To be refactored. */
trait AbstractVarPredicate extends AbstractTracePredicate {

  /** Evaluates the predicate */
  def contains(trace: UnifiedTrace): Boolean

  override def evaluate(trace: UnifiedTrace): Boolean = contains(trace)


}
