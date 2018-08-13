package org.processmining.scala.log.common.unified.trace

// TODO: make private
class UnifiedTraceIdImpl(override val id: String) extends UnifiedTraceId{

  def canEqual(other: Any): Boolean = other.isInstanceOf[UnifiedTraceIdImpl]

  override def equals(other: Any): Boolean = other match {
    case that: UnifiedTraceIdImpl =>
      (that canEqual this) &&
        id == that.id
    case _ => false
  }

  override def hashCode(): Int = id.hashCode

  override def toString(): String = id
}
