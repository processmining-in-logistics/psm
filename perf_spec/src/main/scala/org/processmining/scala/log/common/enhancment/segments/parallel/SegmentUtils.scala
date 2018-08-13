package org.processmining.scala.log.common.enhancment.segments.parallel

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.processmining.scala.log.common.enhancment.segments.common.SegmentDescriptiveStatistics
import org.processmining.scala.log.common.types.Segment
import scala.collection.parallel.{ParMap, ParSeq}

object SegmentUtils {
  def descriptiveStatistics(segments: ParSeq[Segment]): ParMap[String, SegmentDescriptiveStatistics] =
    segments
      .groupBy(_.key)
      .mapValues(
        _.foldLeft(new DescriptiveStatistics())((z: DescriptiveStatistics, s: Segment) => {
          z.addValue(s.duration)
          z
        }
        )
      )
      .map(x => x._1 -> SegmentDescriptiveStatistics(x._1, x._2.getPercentile(25.0), x._2.getPercentile(50.0), x._2.getPercentile(75.0), x._2.getMean, x._2.getStandardDeviation))
}
