package org.processmining.scala.viewers.spectrum.view

private[viewers] case class ViewerSettings(
                         segmentHeightPx: Int,
                         outerBorderPx: Int
                         )


private[viewers] object ViewerSettings{
  def apply(): ViewerSettings =
    ViewerSettings(50, 6)

}