package org.processmining.scala.viewers.spectrum.model

private [viewers] class EmptyDatasource extends AbstractDataSource{
  override def twCount: Int = 0

  override def twSizeMs: Long = 0

  override def startTimeMs: Long = 0

  override def segmentNames: Array[String] = Array()

  override def classesCount: Int = 0

  override def maxSegmentsCount(name: String): Long = 0

  override def goingToRequest(startTwIndex: Int, endTwIndex: Int, includingIds: Boolean, includingSegments: Boolean): Unit = ()

  override def segmentsCount(twIndex: Int): Map[String, List[(Int, Long)]] = ???

  override def segmentIds(twIndex: Int): Map[String, Map[String, Int]] = ???

  override def segments(twIndex: Int): Map[String, Map[Int, List[(String, Long, Long)]]] = ???

  override def legend = ""
}
