package org.processmining.scala.sim.sortercentric.view

import java.awt.Point

import org.processmining.scala.sim.sortercentric.experiments.ExperimentTemplate
import org.processmining.scala.sim.sortercentric.items.AbstractConveyor

case class MhsVisualizerSettings(msPixelRate: Double) {

}

object MhsVisualizerSettings {
  def apply() = new MhsVisualizerSettings(0.008)
}


case class MhsVisualizer(env: ExperimentTemplate, config: MhsVisualizerSettings) {

  def msToPixels(ms: Long) = (ms * config.msPixelRate).toInt

  val sorterLengthMs = env.sorter.map(_.config.lengthMs).sum

  val sorterDiameterMs = (sorterLengthMs / Math.PI).toLong

  val sorterDiameterPx = msToPixels(sorterDiameterMs)

  val sorterRadiusPx = sorterDiameterPx / 2

  val width = 1750

  val height = 1200

  val sorterStartRadians = math.Pi

  val bagWidthPix = 6

  val sorterLabelWidthMs = 2000

  val sorterLabelWidthPx = msToPixels(sorterLabelWidthMs)

  val sorterCenter = new Point(1000, 190)

  val posOfEachSorterConveyor: List[Long] = env.sorter.foldLeft((0L, List[Long]()))((z: (Long, List[Long]), x: AbstractConveyor) =>
    (z._1 + x.config.lengthMs, z._1 :: z._2))._2.reverse

  val posWithinLink: List[List[Long]] =
    env.links.map(l => {
      l.orderedBelts.foldLeft((0L, List[Long]()))((z: (Long, List[Long]), x: AbstractConveyor) =>
        (z._1 + x.config.lengthMs, z._1 :: z._2))._2.reverse

    })




  //  val posWithinLink =
//    env.links.map(l => {
//      val totalMs = l.orderedBelts.map(_.config.lengthMs).sum
//      l.orderedBelts.foldLeft((totalMs + l.orderedBelts.head.config.lengthMs, List[Long]()))((z: (Long, List[Long]), x: AbstractConveyor) =>
//        (x.config.lengthMs, (z._1-x.config.lengthMs) :: z._2))._2.reverse
//
//    })

}
