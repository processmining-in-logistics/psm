package org.processmining.scala.log.common.enhancment.segments.common

case class SegmentDescriptiveStatistics(key: String, q2:Double, median: Double, q4: Double, mean: Double, stdev: Double) {
  def toCsv() = s""""$key";"$q2";"$median";"$q4";"$mean";"$stdev""""
}

object SegmentDescriptiveStatistics {
  val csvHeader = """"key";"q2";"median";"q4";"mean";"stdev""""
}
