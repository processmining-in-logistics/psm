package org.processmining.scala.viewers.spectrum.view

import java.util.regex.Pattern

private[viewers] case class ViewerState(pos: Int,
                                        xZoom: Int,
                                        yZoom: Int,
                                        whiteList: Array[Pattern],
                                        blackList: Array[Pattern],
                                        minCount: Int,
                                        maxCount: Int,
                                        showTraces: Boolean,
                                        showGrid: Boolean,
                                        show3DBars: Boolean,
                                        reverseColors: Boolean,
                                        showBins: Int,
                                        hideSelected: Boolean,
                                        showNamesOfSegments: Boolean
                                       ) {
  override def toString: String =
    s"pos=$pos xZoom=$xZoom yZoom=$yZoom whiteList='${whiteList.mkString(";")}' blackList='${blackList.mkString(";")}' minCount=$minCount maxCount=$maxCount showTraces=$showTraces showGrid=$showGrid show3DBars=$show3DBars reverseColors=$reverseColors showBins=$showBins showNamesOfSegments=$showNamesOfSegments"

  def copyWithGrid(grid: Boolean) = copy(showGrid = grid)
}


private[viewers] object ViewerState {

  val EmptyState = new ViewerState(0, 0, 0, Array(), Array(), 0, 0, false, false, false, false, 0, false, true)
}