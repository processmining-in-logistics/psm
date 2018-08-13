package org.processmining.scala.viewers.spectrum.view

import org.processmining.scala.log.common.utils.common.EventAggregator

class ManyToOneEventAggregator(isLeft: Boolean) extends EventAggregator{
  /** Aggregates names of activities */
  override def aggregate(name: String) = name

  /** Aggregates names of segments */
  override def aggregateSegmentKey(originalKey: String) = {
    val parts = originalKey.split(":")
    if(parts.length != 2) throw new IllegalArgumentException(s"$originalKey must have exactly one ':' symbol")
    if(isLeft) s"any:${parts(1)}" else s"${parts(0)}:any"

  }
}
