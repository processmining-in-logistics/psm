package org.processmining.scala.log.common.enhancment.segments.common

trait AbstractAggregationFunction extends Serializable {
  def getTws(timestamp: Long, duration: Long, timestamp1Ms: Long, timestamp2Ms: Long, twSize: Long): List[Long]
}

object InventoryAggregation extends AbstractAggregationFunction {
  override def getTws(timestamp: Long, duration: Long, timestamp1Ms: Long, timestamp2Ms: Long, twSize: Long): List[Long] = {
    val start = timestamp.max(timestamp1Ms)
    val end = (timestamp + duration).min(timestamp2Ms)
    val startIndex = (start - timestamp1Ms) / twSize
    val endIndex = ((end - timestamp1Ms).toDouble / twSize).ceil.toLong
    if(startIndex != endIndex)  (startIndex until endIndex).toList else List(startIndex)
  }
}

object StartAggregation extends AbstractAggregationFunction {
  override def getTws(timestamp: Long, duration: Long, timestamp1Ms: Long, timestamp2Ms: Long, twSize: Long): List[Long] = {
    if (timestamp >= timestamp1Ms && timestamp < timestamp2Ms) {
      val startIndex = (timestamp - timestamp1Ms) / twSize
      List(startIndex)
    } else {
      List()
    }
  }
}

object EndAggregation extends AbstractAggregationFunction {
  override def getTws(timestamp: Long, duration: Long, timestamp1Ms: Long, timestamp2Ms: Long, twSize: Long): List[Long] = {
    val end = (timestamp + duration).min(timestamp2Ms)
    if (end >= timestamp1Ms && end <= timestamp2Ms) {
      val endIndex = ((end - timestamp1Ms).toDouble / twSize).floor.toLong // TODO: check index!
      List(endIndex)
    } else List()
  }
}
