package org.processmining.scala.log.common.unified.trace

/**
  * Represents traceID
  * All implementations must implement equals/hashCode
  */
trait UnifiedTraceId extends Serializable {

  /** trace ID */
  def id: String
}
