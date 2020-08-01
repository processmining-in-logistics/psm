package org.processmining.scala.viewers.spectrum2.pqr

import java.awt.{Color, Point}

case class Circle(sorterRadiusPx: Int, drawCircle: Boolean, drawPlaces: Boolean, color: Color, isLeft: Int) {
  val sorterDiameterPx = 2 * sorterRadiusPx
}

case class PqrVisualizer() {

  val sorterCenter = new Point(1800, 1500)
  val radialSectionLengthPx = 250
  val radialItemPx = radialSectionLengthPx / 8
  val width = 3200
  val height = 3500
  val pSorterRadiusPx = 650
  val shiftPx = radialItemPx * 4
  val rSorterRadiusPx = pSorterRadiusPx + shiftPx / 2
  val qSorterRadiusPx = pSorterRadiusPx - shiftPx / 2
  val bagWidthPix = 32
  val transitionWidthPix = 10
  val sorterLabelWidthMs = 2000
  val sorterLabelWidthPx = 20

}
