package org.processmining.scala.viewers.spectrum2.view

import org.processmining.scala.viewers.spectrum2.model.{AbstractDataSource, FakeDataSource, SegmentEvent}

class Utils(ds: AbstractDataSource, activityToMinSegmentDuration: Map[String, Long] = FakeDataSource.activityToMinSegmentDuration) {

  private case class TimestampInterval(caseId: String, interval: SegmentEvent)

  private def getTimestampIntervalsPerActivity(): Map[String, Vector[TimestampInterval]] = {
    val segments = ds.segments

    val firstActivity2interval = segments.map(x =>
      x._1.a -> x._2.map(s => TimestampInterval(s.caseId, s.start)).filter(!_.interval.isObserved))

    val secondActivity2interval = segments.map(x =>
      x._1.b -> x._2.map(s => TimestampInterval(s.caseId, s.end)).filter(!_.interval.isObserved))

    firstActivity2interval.filter(_._2.nonEmpty) ++ secondActivity2interval.filter(_._2.nonEmpty)
  }

  // for logs with the ground truth
  def getNormalizedErrors(): Map[String, (Double, Double)] = {
    val segments = ds.segmentNames.map(n => n -> ds.overlaidSegments(n).map(x => (x.caseId, x.timestamp1, x.timestamp2)))

    val firstActivity2interval = segments.map(x =>
      x._1.a -> x._2.map(s => (s._1, s._2))).toMap

    val secondActivity2interval = segments.map(x =>
      x._1.b -> x._2.map(s => (s._1, s._3))).toMap

    val segment2timestamp = firstActivity2interval ++ secondActivity2interval

    val timestampIntervalsPerActivity = getTimestampIntervalsPerActivity

    val estimatedAndTrueValuePerActivity = timestampIntervalsPerActivity.map(x => x._1 -> {
      x._2.map(i => {
        val overlaidSegments = segment2timestamp(x._1)
        val exactValueOpt =
          if (i.interval.minMs - i.interval.maxMs <= 0) //workaround
            overlaidSegments.find(o => o._1 == i.caseId && o._2 >= i.interval.minMs && o._2 <= i.interval.maxMs)
          else
            overlaidSegments.find(o => o._1 == i.caseId && o._2 <= i.interval.minMs && o._2 >= i.interval.maxMs)
        val trueValue = if (exactValueOpt.isDefined) {
          exactValueOpt.get._2
        } else -1
        (i.interval.minMs, trueValue)

      }).filter(_._2 >= 0)
    })

    estimatedAndTrueValuePerActivity.map(x => x._1 -> Utils.computeMaeRmse2(
      x._2.map(_._1).toArray,
      x._2.map(_._2).toArray,
      if (activityToMinSegmentDuration.contains(x._1)) activityToMinSegmentDuration(x._1) else -10000000
    ))

  }


  // for logs without the ground truth
  def getNormalizedAverageTimestampInterval(): Map[String, Double] = {

    val nonNormalized = getTimestampIntervalsPerActivity.map(x => x._1 -> {
      val tmp = x._2
        .map(_.interval.absDuration.toDouble)
      if (tmp.nonEmpty) tmp.sum / tmp.size else -1
    })
    nonNormalized.map(x => x._1 -> {
      if (activityToMinSegmentDuration.contains(x._1)) (x._2 / activityToMinSegmentDuration(x._1)) * 100 else -2
    })
  }

}

object Utils {

  def computeMaeRmse(x1: Array[Long], x2: Array[Long], max: Long): (Double, Double) = {
    val d1 = x1.take(x1.length - 12) // to cut several very last segments that cannot be repaired by this implementation
    val d2 = x2.take(x2.length - 12) // to cut several very last segments that cannot be repaired by this implementation
    val diff = d1.zip(d2).map(x => Math.abs(x._1 - x._2))
    val mae = diff.sum.toDouble / diff.size
    val rmse = Math.sqrt(diff.map(x => x.toDouble * x).sum / diff.length)
    (mae / max * 100, rmse / max * 100)
  }

  def computeMaeRmse2(x1: Array[Long], x2: Array[Long], max: Long): (Double, Double) = {
    val d1 = x1.take(x1.length) // to cut several very last segments that cannot be repaired by this implementation
    val d2 = x2.take(x2.length) // to cut several very last segments that cannot be repaired by this implementation
    val diff = d1.zip(d2).map(x => Math.abs(x._1 - x._2))
    val mae = diff.sum.toDouble / diff.size
    val rmse = Math.sqrt(diff.map(x => x.toDouble * x).sum / diff.length)
    (mae / max * 100, rmse / max * 100)
  }
}
