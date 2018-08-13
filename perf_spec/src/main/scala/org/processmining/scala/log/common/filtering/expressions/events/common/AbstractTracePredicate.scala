package org.processmining.scala.log.common.filtering.expressions.events.common

import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace

/** Provides the interface to implementations of trace predicates
  * for regex-based and variables-based implementations
  *
  */
trait AbstractTracePredicate extends Serializable {

  /**
    * Evaluate a trace against a build expression tree
    *
    * @param trace trace to be evaluated
    * @return evaluation value
    */
  def evaluate(trace: UnifiedTrace): Boolean

}
