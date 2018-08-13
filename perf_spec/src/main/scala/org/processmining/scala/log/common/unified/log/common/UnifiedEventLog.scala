package org.processmining.scala.log.common.unified.log.common

import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.trace.UnifiedTraceId

/** Factory for event log objects */
object UnifiedEventLog {

  /** type for traces */
  type UnifiedTrace = (UnifiedTraceId, List[UnifiedEvent])
}
