package org.processmining.scala.log.common.filtering.expressions.traces

import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace

trait AbstractTraceExpression extends Serializable {

  /**
    * Transforms a given trace according to the built expression tree
    *
    * @param trace a trace to transform
    * @return a transformed trace
    */
  def transform(trace: UnifiedTrace): UnifiedTrace
}
