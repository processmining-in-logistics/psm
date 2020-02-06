package org.processmining.scala.viewers.spectrum.model

import scala.util.Random


private [viewers] class FakeDataSource(override val twCount: Int,
                     override val twSizeMs: Long,
                     override val startTimeMs: Long,
                     override val classesCount: Int) extends AbstractDataSource {

  private val idMaxValue = 100

  override val segmentNames: Array[String] =
    (0 until 100).flatMap { i =>
      (0 until 10).map { j =>
        s"A$i:A$j"
      }
    }.toArray

  override def maxSegmentsCount(name: String): (Long, Long, Long, Long) = (50, 50, 50, 50)

  private def generateCounts(twIndex: Int, name: String): List[(Int, Long, Long, Long)] =
    (0 until classesCount)
      .map(clazz => (clazz, (new Random(twIndex * 219 + clazz * 16 + name.hashCode * 3).nextDouble() * (maxSegmentsCount(name)._1 / classesCount)).toLong, 0L, 0L))
      .toList


  override def segmentsCount(twIndex: Int): Map[String, List[(Int, Long, Long, Long)]] =
    segmentNames
      .map(name => name -> generateCounts(twIndex, name))
      .toMap


  override def segmentIds(twIndex: Int): Map[String, Map[String, Int]] = {
    val r = new Random(twIndex * 19)
    segmentNames
      .map {
        name =>
          name ->
            (0 until r.nextInt(20))
              .map(i => r.nextInt(idMaxValue).toString -> r.nextInt(classesCount))
              .toMap

      }.toMap
  }

  override def segments(twIndex: Int): Map[String, Map[Int, List[(String, Long, Long)]]] = {
    val r = new Random(twIndex * 19)
    segmentNames
      .map {
        name =>
          name ->
            (0 until r.nextInt(classesCount))
              .map(clazz =>
                clazz -> (0 until r.nextInt(5))
                  .map { i =>
                    (r.nextInt(idMaxValue).toString,
                      startTimeMs + twIndex * twSizeMs + r.nextInt(twSizeMs.toInt),
                      r.nextInt(twSizeMs.toInt * 10).toLong)
                  }.toList
              ).toMap

      }.toMap
  }

  override def legend = ""

  override val classifierName: String = ""

  override def forgetSegmentsCount(twIndex: Int): Unit = {}
}
