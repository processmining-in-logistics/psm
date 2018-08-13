package org.processmining.scala.log.common.unified.trace

import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace
import org.processmining.scala.log.common.utils.common.EventAggregator

object UnifiedEventRenaming {

  private def rename(aggregator: EventAggregator)(trace: UnifiedTrace): UnifiedTrace =
   (trace._1, trace._2.map(e => e.copy(aggregator.aggregate(e.activity) )))

  def apply(aggregator: EventAggregator): UnifiedTrace => UnifiedTrace =
    rename(aggregator)

}
