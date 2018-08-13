package org.processmining.scala.log.common.filtering.expressions.traces

import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace

/** Defines the interface to work with one to many trace transformation */
trait SubtraceExpression extends Serializable {

  /** Transforms a trace into several */
  def transform(trace: UnifiedTrace): List[UnifiedTrace]

}
