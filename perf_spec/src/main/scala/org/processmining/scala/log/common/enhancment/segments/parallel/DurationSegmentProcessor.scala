package org.processmining.scala.log.common.enhancment.segments.parallel

import org.processmining.scala.log.common.enhancment.segments.common.{AbstractDurationClassifier, FasterNormal23VerySlowDurationClassifier}
import org.processmining.scala.log.common.enhancment.segments.common.SegmentDescriptiveStatistics
import org.processmining.scala.log.common.types.{Segment, SegmentWithClazz}

import scala.collection.parallel.{ParMap, ParSeq}

class DurationSegmentProcessor(config: SegmentProcessorConfig, segments: ParSeq[Segment], stat: ParMap[String, SegmentDescriptiveStatistics], val adc: AbstractDurationClassifier)
extends SegmentProcessor(config, 5){

  //TODO: support medianAbsDeviation
  private def classify(duration: Long, q2: Double, median: Double, q4: Double): Int = adc.classify(duration, q2, median, q4, "", 0, "", -1)

  override def getClassifiedSegments(): ParSeq[SegmentWithClazz] =
    segments.map(x => SegmentWithClazz(x.id, x.key, x.timestamp, x.duration, classify(x.duration, stat(x.key).q2, stat(x.key).median, stat(x.key).q4)))
}

object DurationSegmentProcessor {
  def apply(config: SegmentProcessorConfig, adc: AbstractDurationClassifier =  new FasterNormal23VerySlowDurationClassifier())
  : (ParSeq[Segment], ParMap[String, SegmentDescriptiveStatistics], DurationSegmentProcessor) = {
    val segments = SegmentProcessor.getSegments(config.segments)
    val stat = SegmentUtils.descriptiveStatistics(segments)
    (segments, stat, new DurationSegmentProcessor(config, segments, stat, adc))
  }
}
