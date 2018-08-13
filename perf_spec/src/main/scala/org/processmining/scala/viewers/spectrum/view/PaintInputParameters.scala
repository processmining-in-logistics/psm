package org.processmining.scala.viewers.spectrum.view

private[viewers] case class PaintInputParameters(
                               startTwIndex: Int,
                               lastTwIndexExclusive: Int,
                               twsFitIntoScreen: Int,
                               names: Array[String] )

private[viewers] object PaintInputParameters{
  val EmptyParameters = PaintInputParameters(0, 1, 0, Array())
}