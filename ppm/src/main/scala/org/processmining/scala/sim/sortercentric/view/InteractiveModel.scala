package org.processmining.scala.sim.sortercentric.view

import java.awt.Color

import org.processmining.scala.sim.sortercentric.experiments.{ExperimentTemplate, Link, Shortcut}
import org.processmining.scala.sim.sortercentric.items.{AbstractConveyor, TsuState}
import org.slf4j.LoggerFactory

import scala.swing.{BorderPanel, Dimension, Graphics2D, Point}

class InteractiveModel(updateScrollingPanel: Unit => Unit, v: MhsVisualizer, env: ExperimentTemplate) extends BorderPanel {

  def onShowDst(selected: Boolean): Unit = {
    showDst = selected
    forceRepaint()
  }

  private var showCaseId = false
  private var showDst = false

  def onShowCaseId(selected: Boolean): Unit = {
    showCaseId = selected
    forceRepaint()
  }


  def modelW = v.width

  def modelH = v.height

  def onVelocityChanged(value: Int): Unit = ???

  def onPositionChanged(value: Int): Unit = ???

  private val logger = LoggerFactory.getLogger(classOf[InteractiveModel])
  private var scrollPanelSize = new Dimension(100, 100)
  private var (zoom, zoomK) = (InteractiveModel.ZoomDefaultValuePercent, InteractiveModel.ZoomDefaultValuePercent / 100.0)

  background = Color.white

  def scale(xy: Int) = (xy * zoomK).toInt

  def onComponentResize(d: Dimension) = {
    scrollPanelSize = d
    logger.info(s"scrollPanelSize=$scrollPanelSize")
    forceRepaint()
  }

  updatePrefferedSize

  def updatePrefferedSize() = preferredSize = new Dimension(scale(modelW), scale(modelH))


  def onZoomChanged(newZoom: Int): Unit = {
    zoom = newZoom
    zoomK = newZoom / 100.0
    logger.info(s"zoom=$zoom zoomK=$zoomK")
    updatePrefferedSize()
    updateScrollingPanel()
    forceRepaint()
  }

  def getAngle(positionMs: Long): Double =
    -(positionMs.toDouble / v.sorterLengthMs) * 2.0 * Math.PI + v.sorterStartRadians

  def pointOnSorter(positionMs: Long, widthPx: Int, isInner: Boolean): Point = {
    val angle = getAngle(positionMs)
    val radiusPx = v.sorterRadiusPx.toDouble + (if (isInner) -widthPx / 2.0 else widthPx / 2.0)
    val x = (Math.cos(angle) * radiusPx + v.sorterCenter.x + v.sorterRadiusPx).toInt
    val y = (Math.sin(angle) * radiusPx + v.sorterCenter.y + v.sorterRadiusPx).toInt
    new Point(x, y)
  }

  def drawRectangleOnSorter(g: Graphics2D, headPositionMs: Long, backPositionMs: Long, widthPx: Int, filled: Boolean, text: String = "") = {

    val p1 = pointOnSorter(headPositionMs, widthPx, false)
    val p2 = pointOnSorter(headPositionMs, widthPx, true)
    val p3 = pointOnSorter(backPositionMs, widthPx, true)
    val p4 = pointOnSorter(backPositionMs, widthPx, false)
    val points = Array(p1, p2, p3, p4, p1).map(p => new Point(scale(p.x), scale(p.y)))
    if (filled) g.fillPolygon(points.map(_.x), points.map(_.y), points.length) else g.drawPolygon(points.map(_.x), points.map(_.y), points.length)
    if (text.nonEmpty)
      g.drawString(text, scale(p1.x), scale(p1.y))
  }

  def pointOnRadialFeeder(positionOnSorterMs: Long, positionMs: Long, widthPx: Int, isLeft: Boolean, scalePercent: Double): Point = {
    val angle = getAngle(positionOnSorterMs)
    val radiusPx = v.sorterRadiusPx + v.msToPixels(positionMs)
    val delta = Math.atan(widthPx / 2.0 / radiusPx)
    val shift = if (isLeft) -delta else delta
    val x = (Math.cos(angle + shift) * radiusPx + v.sorterCenter.x + v.sorterRadiusPx).toInt
    val y = (Math.sin(angle + shift) * radiusPx + v.sorterCenter.y + v.sorterRadiusPx).toInt
    new Point(x, y)
  }


  def drawRadialRectangle(g: Graphics2D,
                          positionOnSorterMs: Long,
                          headPositionMs: Long,
                          backPositionMs: Long,
                          widthPx: Int,
                          scalePercent: Double,
                          filled: Boolean,
                          text: String = "") = {
    val p1 = pointOnRadialFeeder(positionOnSorterMs, headPositionMs, widthPx, true, scalePercent)
    val p2 = pointOnRadialFeeder(positionOnSorterMs, headPositionMs, widthPx, false, scalePercent)
    val p3 = pointOnRadialFeeder(positionOnSorterMs, backPositionMs, widthPx, false, scalePercent)
    val p4 = pointOnRadialFeeder(positionOnSorterMs, backPositionMs, widthPx, true, scalePercent)
    val points = Array(p1, p2, p3, p4, p1).map(p => new Point(scale(p.x), scale(p.y)))
    if (filled) g.fillPolygon(points.map(_.x), points.map(_.y), points.length) else g.drawPolygon(points.map(_.x), points.map(_.y), points.length)
    g.drawString(text, scale(p1.x), scale(p1.y))
  }

  def tsuTitle(x: TsuState) = {
    val caseId = if(showCaseId) x.tsu.id else ""
    val dst = if(showDst) x.tsu.dst.mkString("(",";",")") else ""
    s"$caseId$dst"

  }

  def drawRadialLink(g: Graphics2D, t: (Link, List[Long])) = {
    val totalMs = t._1.orderedBelts.map(_.config.lengthMs).sum
    t._1.orderedBelts
      .zip(t._2)
      .foreach(x => {
        val startPos = x._2
        val belt = x._1
        g.setColor(if (x._1.state.wasMovedDuringLastStep) Color.black else Color.red)
        if(x._1.state.isBlocked()) g.setColor(Color.MAGENTA)

        if(t._1.isIncoming)
          drawRadialRectangle(g, t._1.positionMs, totalMs - startPos, totalMs - startPos - belt.config.lengthMs, v.bagWidthPix, 100.0, false, x._1.id)
        else
          drawRadialRectangle(g, t._1.positionMs, startPos, startPos + belt.config.lengthMs, v.bagWidthPix, 100.0, false, x._1.id)



        belt.state.tsu.foreach(x => {
          g.setColor(env.getColorByDst(x.tsu.dst))

          if (t._1.isIncoming)
            drawRadialRectangle(g,
              t._1.positionMs,
              totalMs - startPos - x.headPositionMs,
              totalMs - startPos - x.backPositionMs(belt.config),
              v.bagWidthPix, 100, true, tsuTitle(x))
          else drawRadialRectangle(g,
            t._1.positionMs,
            startPos + x.headPositionMs,
            startPos + x.backPositionMs(belt.config),
            v.bagWidthPix, 100, true, tsuTitle(x))

        })
      })
  }

  //  val strokeThik = new BasicStroke(5.0f)
  //  val strokeThin = new BasicStroke(1.0f)
  def drawShortcut(g: Graphics2D, shortcut: Shortcut): Unit = {
    val s = pointOnSorter(shortcut.startPositionMs, v.bagWidthPix, true)
    val e = pointOnSorter(shortcut.endPositionMs, v.bagWidthPix, true)
    val xs = s.x.toDouble
    val ys = s.y.toDouble
    val xe = e.x.toDouble
    val ye = e.y.toDouble
    val k = (ye - ys) / (xe - xs) // vertical lines are not supported
    val totalLengthMs = shortcut.totalLenghtMs()
    val tanA = k
    val A = Math.atan(tanA) + Math.PI / 2
    val dX = scale((v.bagWidthPix * Math.cos(A) / 2.0).toInt)
    val dY = scale((v.bagWidthPix * Math.sin(A) / 2.0).toInt)

    def drawRectange(headPosMs: Long, backPosMs: Long, dotsOnBorders: Boolean, filled: Boolean, text: String = ""): Unit = {
      def getPoint(posMs: Long) = {
        val posRel = posMs.toDouble / totalLengthMs
        new Point(scale((xs + (xe - xs) * posRel).toInt), scale((ys + (ye - ys) * posRel).toInt))
      }

      val headPos = getPoint(headPosMs)
      val backPos = getPoint(backPosMs)
      if (text.nonEmpty) {
        g.drawString(text, headPos.x - 2, headPos.y - 2)
      }
      val p1 = new Point(headPos.x + dX, headPos.y + dY)
      val p2 = new Point(backPos.x + dX, backPos.y + dY)
      val p3 = new Point(backPos.x - dX, backPos.y - dY)
      val p4 = new Point(headPos.x - dX, headPos.y - dY)
      val points = Array(p1, p2, p3, p4, p1)
      if (filled) g.fillPolygon(points.map(_.x), points.map(_.y), points.length) else g.drawPolygon(points.map(_.x), points.map(_.y), points.length)
    }

    shortcut.orderedBelts.foldLeft(0L)((z: Long, x: AbstractConveyor) => {
      if (x.state.wasMovedDuringLastStep) g.setColor(Color.black) else g.setColor(Color.red)
      if(x.state.isBlocked()) g.setColor(Color.MAGENTA)
      drawRectange(z, z + x.config.lengthMs, false, false, x.id)
      x.state.tsu.foreach(t => {
        g.setColor(env.getColorByDst(t.tsu.dst))
        drawRectange(z + t.headPositionMs, z + t.backPositionMs(x.config), false, true, tsuTitle(t))
      })
      z + x.config.lengthMs
    })
  }

  def drawShortcuts(g: Graphics2D): Unit = {
    env.shortcuts().foreach(x => {
      drawShortcut(g, x)
    })
  }

  override def paint(g: Graphics2D): Unit = {
    super.paint(g)
//    g.setColor(Color.red)
    g.setColor(Color.black)
    val shiftPx = v.bagWidthPix / 2
    g.drawOval(scale(v.sorterCenter.x + shiftPx), scale(v.sorterCenter.y + shiftPx), scale(v.sorterDiameterPx - v.bagWidthPix), scale(v.sorterDiameterPx - v.bagWidthPix))
    g.drawOval(scale(v.sorterCenter.x - shiftPx), scale(v.sorterCenter.y - shiftPx), scale(v.sorterDiameterPx + v.bagWidthPix), scale(v.sorterDiameterPx + v.bagWidthPix))
    g.setColor(Color.black)
    v.posOfEachSorterConveyor.zip(env.sorter)
      .foreach(x => {
        if (x._2.state.wasMovedDuringLastStep) g.setColor(Color.black) else g.setColor(Color.red)
        if(x._2.state.isBlocked()) g.setColor(Color.MAGENTA)
        drawRectangleOnSorter(g, x._1, x._1, v.bagWidthPix, false, x._2.id)})
    env.links
      .zip(v.posWithinLink)
      .foreach(drawRadialLink(g, _))
    val tsu = env.sorter
      .zip(v.posOfEachSorterConveyor)
      .flatMap(x => x._1.state.tsu.map(t => t.copy(headPositionMs = t.headPositionMs + x._2)))
    tsu.map(x => {
      g.setColor(env.getColorByDst(x.tsu.dst))
      drawRectangleOnSorter(g, x.headPositionMs, x.backPositionMs(env.sorter.head.config), v.bagWidthPix, true, tsuTitle(x))
    }
    )
    drawShortcuts(g)
  }

  def forceRepaint() = {
    peer.invalidate()
    peer.repaint()
  }
}

object InteractiveModel {
  val PositionMin = 0
  val PositionMax = 10000
  val PositionDefaultValue = 0

  val ZoomMinPercent = 10
  val ZoomMaxPercent = 250
  val ZoomDefaultValuePercent = 100

  val VelocityMin = 0
  val VelocityMax = 100000
  val VelocityDefaultValue = 100

}