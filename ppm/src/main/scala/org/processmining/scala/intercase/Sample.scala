package org.processmining.scala.intercase

import java.io.{File, PrintWriter}

case class Sample(lineNumber: Int, id: String, elapsedTime: Double, endTime: Long, lastEvent: String, L123: List[Int], cityDistance: List[Int], snapshotDistance: List[Int], y: Outcome)

object Sample {
  //private val logger = LoggerFactory.getLogger(Sample.getClass)
  val FilenameSuffixY1 = ".y1.csv"
  val FilenameSuffixY2 = ".y2.csv"


  def getNonNormalizedSamplesAndMax(samples: List[Sample]): (List[List[Double]], List[Double]) ={
    val nonNormalizedSamples = samples
      .map(s => List(s.y.remainingTime.toDouble, s.y.segmentDuration.toDouble, s.elapsedTime) :::
        s.L123.map(_.toDouble)
        ::: s.cityDistance.map(_.toDouble)
        ::: s.snapshotDistance.map(_.toDouble))
    val max =
      nonNormalizedSamples
        .foldLeft(nonNormalizedSamples.head.indices.map(_ => Double.MinValue))(
          (z, s) => z.zip(s).map(x => Math.max(x._1, x._2)))
        .toList
    (nonNormalizedSamples, max)
  }

  def normalize(nonNormalizedFilename: Option[String], samples: List[Sample]): (List[List[Double]], List[Double]) = {
    val (nonNormalizedSamples, max) = getNonNormalizedSamplesAndMax(samples)
    if(nonNormalizedFilename.isDefined) export(nonNormalizedFilename.get, nonNormalizedSamples, max)
    (nonNormalizedSamples
      .map(s => s.zip(max)
        .map(x => if (x._2 != 0.0) x._1 / x._2 else 0.0)), max)
  }

  def normalize(nonNormalizedFilename: Option[String], samples: List[Sample], max: List[Double]): (List[List[Double]], List[Double]) = {
    val (nonNormalizedSamples, _) = getNonNormalizedSamplesAndMax(samples)
    if(nonNormalizedFilename.isDefined) export(nonNormalizedFilename.get, nonNormalizedSamples, max)
    (nonNormalizedSamples
      .map(s => s.zip(max)
        .map(x => if (x._2 != 0.0) x._1 / x._2 else 0.0)), max)
  }

  def export(filename: String, samples: List[List[Double]], max: List[Double]) = {
    val pwSamples = new PrintWriter(new File(filename))
    samples.foreach(x => pwSamples.println(x.map(y => s"$y").mkString(","))) //f"$y%.06f"
    pwSamples.close()

    val pwMax = new PrintWriter(new File(s"$filename.max.csv"))
    pwMax.println(max.mkString(","))
    pwMax.close()

    val pwY1 = new PrintWriter(new File(s"$filename$FilenameSuffixY1"))
    pwY1.println(max(0))
    pwY1.close()

    val pwY2 = new PrintWriter(new File(s"$filename$FilenameSuffixY2"))
    pwY2.println(max(1))
    pwY2.close()
  }
}