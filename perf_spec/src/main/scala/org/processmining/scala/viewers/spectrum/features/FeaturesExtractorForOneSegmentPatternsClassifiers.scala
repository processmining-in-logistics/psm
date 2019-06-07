package org.processmining.scala.viewers.spectrum.features

import java.io.PrintWriter

import org.processmining.scala.viewers.spectrum.patterns.OneSegmentPerformancePattern
import org.slf4j.LoggerFactory

case class LabelForOneSegmentPerformancePattern(twIndexLeft: Int, twIndexRight: Int, twIndexMiddle: Int, clazz: Int)

case class FeaturesPointer(segmentsForFeatureExtraction: Set[String], offsetBeginning: Int, duration: Int)


class FeaturesExtractorForOneSegmentPatternsClassifiers(features: Map[String, Map[Int, PositionsInCell]],
                                                        patternsForLabeling: Seq[LabelForOneSegmentPerformancePattern],
                                                        incomingFlow: FeaturesPointer,
                                                        state: FeaturesPointer
                                                       ) {

  private val logger = LoggerFactory.getLogger(classOf[FeaturesExtractorForOneSegmentPatternsClassifiers].getName)

  def process(ptr: FeaturesPointer, labelTwIndex: Int): Option[Map[String, Map[Int, Seq[Int]]]] = {
    val ret = ptr.segmentsForFeatureExtraction
      .toSeq
      .sorted
      .map(name => {
        val featuresForSegment = features(name)
        val twIndexStart = labelTwIndex + ptr.offsetBeginning
        val twIndexEnd = twIndexStart + ptr.duration
        logger.debug(s"labelTwIndex=$labelTwIndex segment = '$name' [$twIndexStart; $twIndexEnd)")
        (name,
          if (twIndexStart >= 0) {
            (twIndexStart until twIndexEnd)
              .map(tw => {
                val fOptional = featuresForSegment.get(tw)
                //TODO: have positions and classes here
                val f = if (fOptional.isDefined) fOptional.get.extractFeatures() else (0 to 20).map(_ => 0)
                logger.debug(s"tw=$tw ${f.mkString(";")}")
                tw -> f
              })
              .toMap
          }
          else {
            logger.debug(s"Too close to the left")
            Map[Int, Seq[Int]]()
          })
      }).toMap

    if (ret.exists(_._2.isEmpty)) None else Some(ret)
  }

  def processLabels(): Seq[(Int, Map[String, Map[Int, Seq[Int]]], Map[String, Map[Int, Seq[Int]]])] = {
    patternsForLabeling.map(x => {
      val incomingFlowFeatures = process(incomingFlow, x.twIndexMiddle)
      val stateFeatures = process(state, x.twIndexMiddle)
      (incomingFlowFeatures, stateFeatures, x.clazz)

    })
      .filter(x => x._1.isDefined && x._2.isDefined)
      .map(x => (x._3, x._1.get, x._2.get))
  }


  private def toSeq(x: Map[String, Map[Int, Seq[Int]]]): scala.Seq[Int] =
    x.toSeq
      .sortBy(_._1)
      .flatMap(_._2.toSeq.sortBy(_._1).flatMap(_._2))


  def export(filename: String,
             ds: Seq[(Int, Map[String, Map[Int, Seq[Int]]], Map[String, Map[Int, Seq[Int]]])],
             classConvertor: Int => Int) = {
    val pw = new PrintWriter(filename)
    ds.foreach(x => pw.println(s"${classConvertor(x._1)} ${toSeq(x._2).zipWithIndex.map(x => s"${x._2 + 1}:${x._1}.0").mkString(" ")}"))
    pw.close()

  }


}
