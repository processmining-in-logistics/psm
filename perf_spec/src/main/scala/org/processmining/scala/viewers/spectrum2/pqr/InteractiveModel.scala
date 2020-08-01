package org.processmining.scala.viewers.spectrum2.pqr

import java.awt.{BasicStroke, Color, Font}

import org.processmining.scala.viewers.spectrum.view.AppSettings
import org.processmining.scala.viewers.spectrum2.model.SinkClient
import org.slf4j.LoggerFactory

import scala.swing.event.{MouseClicked, MouseMoved}
import scala.swing.{BorderPanel, Dimension, Graphics2D, Point}
import scala.util.Random

case class PlaceTransition(isTransition: Boolean, qpr: Int, name: String, isSorter: Boolean, isIncoming: Boolean, button: Int = -1) extends Serializable {
  override def toString: String = if (qpr == 2) "" else s"${if (isTransition) "t=" else "p="} $name ($dimension)"

  def toStringLogger: String = s"${toString} button=$button isSorter=$isSorter isIncoming=$isIncoming"

  def dimension = qpr match {
    case -1 => "Q"
    case 0 => "P"
    case 1 => "R"
  }
}

object PlaceTransition {
  val Empty = PlaceTransition(false, 2, "", false, false)
}


case class Node(ltrb: List[Int], pt: PlaceTransition)

class Mapping() {
  private val r = Random

  def random(): (List[Int], PlaceTransition) = items(r.nextInt(items.size))

  def get(label: String) = {
    val fullLabel = if (!label.endsWith(SystemLayout.Start) && !label.endsWith(SystemLayout.Complete) && !label.startsWith(SystemLayout.Enqueue) && !label.startsWith(SystemLayout.Dequeue))
      s"$label${SystemLayout.Sep}${SystemLayout.Start}" else label
    items.find(_._2.name == fullLabel)
  }

  private var items = List[(List[Int], PlaceTransition)]()
  private var isFinal = false

  def makeFinal() = isFinal = true


  def add(pp: List[Point], pt: PlaceTransition): Unit =
    if (!isFinal) {
      val xx = pp.map(_.x)
      val yy = pp.map(_.y)
      // the LTRB order
      items = (List(xx.min, yy.min, xx.max, yy.max), pt) :: items
    }


  def add(p: Point, diameter: Int, pt: PlaceTransition): Unit =
    if (!isFinal)
      add(List(p, new Point(p.x, p.y + diameter), new Point(p.x + diameter, p.y + diameter), new Point(p.x + diameter, p.y)), pt)

  def get(x: Int, y: Int): Option[PlaceTransition] = {
    val retOpt = items.find(z => x >= z._1(0) && x <= z._1(2) && y >= z._1(1) && y <= z._1(3))
    if (retOpt.isDefined) Some(retOpt.get._2) else None
  }

}

class InteractiveModel(updateScrollingPanel: () => Unit, v: PqrVisualizer, mouseMoveListener: PlaceTransition => Unit) extends BorderPanel {

  val sl = new SystemLayout()
  val sorterStepSector = sl.sorterStepSector
  var mapping = new Mapping()
  var isMappingCreated = false
  private val sinkClient = new SinkClient("localhost", AppSettings(AppSettings.DefaultFileName).pqrServerPort)


  def showPlaceTransition(label: String) = {
    val (list, pt) = if (label.isEmpty) mapping.random() else mapping.get(label).get
    (list.map(scale), pt, scale(v.transitionWidthPix))
  }

  def onShowOnlyP(selected: Boolean): Unit = {
    showOnlyP = selected
    forceRepaint()
  }

  listenTo(mouse.clicks, mouse.moves)
  reactions += {
    case MouseMoved(_, p, _) => {
      val opt = mapping.get(unscale(p.x), unscale(p.y))
      if (opt.isDefined)
        mouseMoveListener(opt.get)
      else
        mouseMoveListener(PlaceTransition.Empty)
    }

    case mouseClicked: MouseClicked => {
      val opt = mapping.get(unscale(mouseClicked.point.x), unscale(mouseClicked.point.y))
      if (opt.isDefined) {
        val button = mouseClicked.peer.getButton
        sinkClient.send(opt.get.copy(button = button))
        //logger.info(opt.get.toString + button.toString)
      }
    }
  }

  private var showQRLabels = true
  private var showOnlyP = false

  def onShowQRLabels(selected: Boolean): Unit = {
    showQRLabels = selected
    forceRepaint()
  }


  def modelW = v.width

  def modelH = v.height

  def onVelocityChanged(value: Int): Unit = ???

  def onAngleChanged(value: Int): Unit = {
    sl.sorterStartRadians = Math.PI/180.0*value
    forceRepaint()
  }

  private val logger = LoggerFactory.getLogger(classOf[InteractiveModel])
  private var scrollPanelSize = new Dimension(100, 100)
  private var (zoom, zoomK) = (InteractiveModel.ZoomDefaultValuePercent, InteractiveModel.ZoomDefaultValuePercent / 100.0)

  background = Color.white

  def scale(xy: Int) = (xy * zoomK).toInt

  def unscale(xy: Int) = (xy / zoomK).toInt

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

  //  def getAngle(positionMs: Long): Double =
  //    -(positionMs.toDouble / v.sorterLengthMs) * 2.0 * Math.PI + v.sorterStartRadians

  def pointOnSorter(c: Circle, angle: Double, widthPx: Int, isInner: Boolean): Point = {
    val radiusPx = c.sorterRadiusPx.toDouble + (if (isInner) -widthPx / 2.0 else widthPx / 2.0)
    val x = (Math.cos(angle) * radiusPx + v.sorterCenter.x).toInt
    val y = (Math.sin(angle) * radiusPx + v.sorterCenter.y).toInt
    new Point(x, y)
  }

  def pointOnSorter(c: Circle, angle: Double): Point = {
    val radiusPx = c.sorterRadiusPx.toDouble
    val x = (Math.cos(angle) * radiusPx + v.sorterCenter.x).toInt
    val y = (Math.sin(angle) * radiusPx + v.sorterCenter.y).toInt
    new Point(x, y)
  }

  def drawRectangleOnSorter(g: Graphics2D, c: Circle, startAngle: Double, backAngle: Double, widthPx: Int, filled: Boolean, text: String = "") = {

    val p1 = pointOnSorter(c, startAngle, widthPx, false)
    val p2 = pointOnSorter(c, startAngle, widthPx, true)
    val p3 = pointOnSorter(c, backAngle, widthPx, true)
    val p4 = pointOnSorter(c, backAngle, widthPx, false)
    val points = Array(p1, p2, p3, p4, p1).map(p => new Point(scale(p.x), scale(p.y)))
    if (filled) g.fillPolygon(points.map(_.x), points.map(_.y), points.length) else g.drawPolygon(points.map(_.x), points.map(_.y), points.length)
    if (text.nonEmpty) {
      g.setColor(Color.BLACK)
      val p = if (p1.x > p2.x) p1 else p2
      if (c.isLeft == 0 || showQRLabels)
        lf.drawString(g, text, scale(p.x), scale(p.y))
      //g.drawString(text, scale(p.x), scale(p.y))
    }
    List(p1, p2, p3, p4)
  }

  def drawArrowOnSorter(g: Graphics2D, c: Circle, pointAngle: Double, backAngle: Double, widthPx: Int) = {
    val p = pointOnSorter(c, pointAngle)
    val p1 = pointOnSorter(c, backAngle, widthPx, true)
    val p2 = pointOnSorter(c, backAngle, widthPx, false)
    g.drawLine(scale(p1.x), scale(p1.y), scale(p.x), scale(p.y))
    g.drawLine(scale(p2.x), scale(p2.y), scale(p.x), scale(p.y))
  }


  //returns ((top, left), diameter)
  def drawOvalOnSorter(g: Graphics2D, c: Circle, angle: Double, diameterPx: Int, filled: Boolean, text: String) = {
    val p1 = pointOnSorter(c, angle)
    val p = new Point(scale(p1.x), scale(p1.y))
    val scaledDiameter = scale(diameterPx * 2)
    if (filled) g.fillOval(p.x - scaledDiameter / 2, p.y - scaledDiameter / 2, scaledDiameter, scaledDiameter)
    else g.drawOval(p.x - scaledDiameter / 2, p.y - scaledDiameter / 2, scaledDiameter, scaledDiameter)
    //    if (text.nonEmpty) {
    //      g.setColor(Color.gray)
    //      g.drawString(text, p.x + scaledDiameter / 2, p.y + scaledDiameter / 2)
    //    }
    (new Point(p1.x - diameterPx, p1.y - diameterPx), diameterPx * 2)
  }

  def pointOnRadialFeeder(c: Circle, angle: Double, positionPx: Int, widthPx: Int, isLeft: Int, scalePercent: Double): Point = {
    val radiusPx = c.sorterRadiusPx + positionPx
    val delta = Math.atan(widthPx / 2.0 / radiusPx)
    val shift = delta * isLeft
    val x = (Math.cos(angle + shift) * radiusPx + v.sorterCenter.x).toInt
    val y = (Math.sin(angle + shift) * radiusPx + v.sorterCenter.y).toInt
    new Point(x, y)
  }


  def drawRadialRectangle(g: Graphics2D,
                          c: Circle,
                          angle: Double,
                          headPositionPx: Int,
                          backPositionPx: Int,
                          widthPx: Int,
                          scalePercent: Double,
                          filled: Boolean,
                          text: String = "",
                          isLeftK: Int,
                          color: Color) = {
    val p1 = pointOnRadialFeeder(c, angle, headPositionPx, widthPx, -1 + isLeftK, scalePercent)
    val p2 = pointOnRadialFeeder(c, angle, headPositionPx, widthPx, 1 + isLeftK, scalePercent)
    val p3 = pointOnRadialFeeder(c, angle, backPositionPx, widthPx, 1 + isLeftK, scalePercent)
    val p4 = pointOnRadialFeeder(c, angle, backPositionPx, widthPx, -1 + isLeftK, scalePercent)
    val points = Array(p1, p2, p3, p4, p1).map(p => new Point(scale(p.x), scale(p.y)))
    g.setColor(Color.white)
    g.fillPolygon(points.map(_.x), points.map(_.y), points.length)
    g.setColor(color)
    if (filled) g.fillPolygon(points.map(_.x), points.map(_.y), points.length) else g.drawPolygon(points.map(_.x), points.map(_.y), points.length)
    g.setColor(Color.black)
    if (isLeftK == 0 || showQRLabels)
      lf.drawString(g, text, scale(p1.x), scale(p1.y))
    List(p1, p2, p3, p4)
  }

  //  def drawString(g: Graphics2D, text: String, x: Int, y: Int): Unit = {
  //    val parts = text.split("\\" + SystemLayout.Sep)
  //    val p1 = parts(0)
  //    val p2 = parts(1)
  //    g.setFont(labelFont)
  //    g.drawString(p1, x, y)
  //    g.setFont(scriptFont)
  //    g.drawString(p2, x + 7 * p1.length, y - 7)
  //  }

  def drawRadialCircle(g: Graphics2D,
                       c: Circle,
                       angle: Double,
                       headPositionPx: Int,
                       radiusPx: Int,
                       scalePercent: Double,
                       filled: Boolean,
                       text: String = "",
                       isLeftK: Int,
                       color: Color) = {
    val p = pointOnRadialFeeder(c, angle, headPositionPx, 0, 0 + isLeftK, scalePercent)
    g.setColor(Color.white)
    g.fillOval(scale(p.x - radiusPx), scale(p.y - radiusPx), scale(radiusPx * 2), scale(radiusPx * 2))
    g.setColor(color)
    if (filled) g.fillOval(scale(p.x - radiusPx), scale(p.y - radiusPx), scale(radiusPx * 2), scale(radiusPx * 2)) else g.drawOval(scale(p.x - radiusPx), scale(p.y - radiusPx), scale(radiusPx * 2), scale(radiusPx * 2))
    g.setColor(Color.black)
    //drawString(g, text, scale(p.x), scale(p.y))
    g.drawString(text, scale(p.x), scale(p.y))
    (new Point(p.x - radiusPx, p.y - radiusPx), 2 * radiusPx)
  }


  private val dashedChannel = Array(5.0f, 5.0f)
  private val dashedChannelStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashedChannel, 0.0f)

  def drawChannel(g: Graphics2D, c1: Circle, c2: Circle, pos: Int) = {
    def drawChannel(start: Double): Unit = {
      val p1 = pointOnSorter(c1, start)
      val p2 = pointOnSorter(c2, start)
      g.drawLine(scale(p1.x), scale(p1.y), scale(p2.x), scale(p2.y))
    }

    g.setColor(Color.black)
    val start = 2 * Math.PI - pos * sorterStepSector + sl.sorterStartRadians
    val oldStroke = g.getStroke
    g.setStroke(dashedChannelStroke)
    drawChannel(start - sorterStepSector / 16)
    drawChannel(start - sorterStepSector / 2 - sorterStepSector / 16)
    g.setStroke(oldStroke)
  }

  def drawSector(g: Graphics2D, c: Circle, x: Activity, pos: Int, rename: (String, Boolean) => String, renamePl: (String, Boolean) => String, useFilling: Boolean, isNotQ: Boolean, qpr: Int) = {

    g.setColor(c.color)

    def drawTransition(start: Double, isStart: Boolean, isFilled: Boolean): Unit = {
      //PlaceTransition(isTransition: Boolean, qpr: Int, name: String, isSorter: Boolean, isIncoming: Boolean)
      g.setColor(Color.white)
      drawRectangleOnSorter(g, c, start, start - sorterStepSector / 8, v.transitionWidthPix, true, "")
      g.setColor(c.color)
      val name = s"${rename(x.name, isStart)}"
      val ltrb = drawRectangleOnSorter(g, c, start, start - sorterStepSector / 8, v.transitionWidthPix, isFilled,
        name)
      mapping.add(ltrb, PlaceTransition(true, qpr, name, true, false))
    }

    def drawPlace(start: Double, name: String, filled: Boolean=false): Unit = {
      g.setColor(Color.white)
      drawOvalOnSorter(g, c, start, v.transitionWidthPix, true, "")
      g.setColor(c.color)
      val (p, d) = drawOvalOnSorter(g, c, start, v.transitionWidthPix, filled, name)
      mapping.add(p, d, PlaceTransition(false, qpr, name, true, false))
    }


    val start = 2 * Math.PI - pos * sorterStepSector + sl.sorterStartRadians
    drawTransition(start, true, if (isNotQ) pos % 2 == 0 else pos % 2 == 0)
    if (!showOnlyP)
      drawTransition(start - sorterStepSector / 2, false, if (isNotQ) pos % 2 == 0 else pos % 2 != 0)

    if (c.drawPlaces) {

      if (!showOnlyP)
        drawPlace(start - sorterStepSector / 4 - sorterStepSector / 16, s"${rename(x.name, true)}${SystemLayout.PlSep}${rename(x.name, false)}")

      drawPlace(start - sorterStepSector / 2 - sorterStepSector / 4 - sorterStepSector / 16, renamePl(x.name, false), true)
    }

    if (c.drawCircle) {
      g.setColor(c.color)
      (0 until 2).foreach(i => drawArrowOnSorter(g, c,
        start - i * sorterStepSector / 2,
        start - i * sorterStepSector / 2 + sorterStepSector / 24,
        v.transitionWidthPix
      ))


      (0 until 2).foreach(i => drawArrowOnSorter(g, c,
        start - sorterStepSector / 4 + sorterStepSector / 16 - i * sorterStepSector / 2,
        start - sorterStepSector / 4 + sorterStepSector / 16 - i * sorterStepSector / 2 + sorterStepSector / 24,
        v.transitionWidthPix / 2
      ))
    }
  }


  def drawFeeder(g: Graphics2D, currentCircle: Circle, r: RadialFeeder, qpr: Int, rename: (String, Boolean) => String, renamePl: (String, Boolean) => String) = {
    val (angle, isStartFilled) = sl.getAngle(r.connector, r.isIncoming, true)
    val (isLeftK, color, isP, isQ, isR) = qpr match {
      case -1 => (8, SystemLayout.qColor, false, true, false)
      case 0 => (0, SystemLayout.pColor, true, false, false)
      case 1 => (-8, SystemLayout.rColor, false, false, true)
    }
    val sortedActivities = if (r.isIncoming) r.activities.reverse else r.activities

    def drawChannels(headPositionPx: Int, backPositionPx: Int, isLeftK: Int = isLeftK): Unit = {
      if (!isP) {
        val oldStroke = g.getStroke
        g.setStroke(dashedChannelStroke)
        val p1 = pointOnRadialFeeder(currentCircle, angle, (headPositionPx + backPositionPx) / 2, v.transitionWidthPix, 0, 1.0)
        val p2 = pointOnRadialFeeder(currentCircle, angle, (headPositionPx + backPositionPx) / 2, v.transitionWidthPix, isLeftK, 1.0)
        g.setColor(Color.black)
        g.drawLine(scale(p1.x), scale(p1.y), scale(p2.x), scale(p2.y))
        g.setStroke(oldStroke)
      }
    }

    def drawRadialSector(a: Activity, i: Int): Unit = {


      val f1 = if (i % 2 != 0) isStartFilled else !isStartFilled
      val f2 = if (isQ) !f1 else f1
      val radiusPx = v.transitionWidthPix
      val rename1: (String, Boolean) => String = if (isQ && i == 0) SystemLayout.renameQConnector(r.connector.name) else rename
      val headPositionPx = i * v.radialSectionLengthPx + 2 * v.radialItemPx + v.shiftPx
      val backPositionPx = headPositionPx + v.radialItemPx
      val name = rename1(a.name, !r.isIncoming)
      drawChannels(headPositionPx, backPositionPx)
      if (!showOnlyP || (showOnlyP && !r.isIncoming)) {
        val points = drawRadialRectangle(g, currentCircle, angle,
          headPositionPx,
          backPositionPx, v.transitionWidthPix, 1.0, f1, name, isLeftK, color)
        mapping.add(points, PlaceTransition(true, qpr, name, false, r.isIncoming))
      }

      if (!isQ || (isQ && i != sortedActivities.size - 1)) {
        val headPositionPx = i * v.radialSectionLengthPx + 6 * v.radialItemPx + v.shiftPx
        val backPositionPx = headPositionPx + v.radialItemPx
        val name = rename(a.name, r.isIncoming)
        if (!showOnlyP || (showOnlyP && r.isIncoming)) {
          val points = drawRadialRectangle(g, currentCircle, angle,
            headPositionPx, backPositionPx, v.transitionWidthPix, 1.0, f2, name, isLeftK, color)
          mapping.add(points, PlaceTransition(true, qpr, name, false, r.isIncoming))
        }
        val p = pointOnRadialFeeder(currentCircle, angle, if (r.isIncoming) backPositionPx else headPositionPx, v.transitionWidthPix, 0, 1.0)
        val p1 = pointOnRadialFeeder(currentCircle, angle, if (r.isIncoming) backPositionPx + v.radialItemPx / 4 else headPositionPx - v.radialItemPx / 4, v.transitionWidthPix, -1, 1.0)
        val p2 = pointOnRadialFeeder(currentCircle, angle, if (r.isIncoming) backPositionPx + v.radialItemPx / 4 else headPositionPx - v.radialItemPx / 4, v.transitionWidthPix, 1, 1.0)
        g.setColor(SystemLayout.pColor)
        g.drawLine(scale(p.x), scale(p.y), scale(p1.x), scale(p1.y))
        g.drawLine(scale(p.x), scale(p.y), scale(p2.x), scale(p2.y))


        drawChannels(headPositionPx, backPositionPx)
      }

      //      drawPlace(start - sorterStepSector / 2 - sorterStepSector / 4 - sorterStepSector / 16, renamePl(x.name, false))
      if (isP) {

        val name1 = if (i != 0 /*|| r.isIncoming*/ ) renamePl(a.name, false) else if (!r.isIncoming) s"${rename(r.connector.name, false)}${SystemLayout.PlSep}${rename(a.name, true)}"
        else s"${rename(a.name, false)}${SystemLayout.PlSep}${rename(r.connector.name, true)}"
        val (p1, d1) = drawRadialCircle(g, currentCircle, angle, i * v.radialSectionLengthPx + v.shiftPx + 0 * v.radialItemPx + radiusPx, radiusPx, 1.0, true, "", isLeftK, color)
        //if(i != 0 || r.isIncoming)
        mapping.add(p1, d1, PlaceTransition(false, qpr, name1, false, r.isIncoming))

        if (!showOnlyP) {
          val name2 = s"${rename(a.name, true)}${SystemLayout.PlSep}${rename(a.name, false)}"
          val (p2, d2) = drawRadialCircle(g, currentCircle, angle, i * v.radialSectionLengthPx + v.shiftPx + (0 + 4) * v.radialItemPx + radiusPx, radiusPx, 1.0, false, "", isLeftK, color)
          mapping.add(p2, d2, PlaceTransition(false, qpr, name2, false, r.isIncoming))
        }
      }
    }

    if (isP) {
      val p1 = pointOnRadialFeeder(currentCircle, angle, 0, v.transitionWidthPix, 0, 1.0)
      val p2 = pointOnRadialFeeder(currentCircle, angle, r.activities.size * v.radialSectionLengthPx + v.shiftPx - v.radialItemPx, v.transitionWidthPix, 0, 1.0)
      g.setColor(SystemLayout.pColor)
      g.drawLine(scale(p1.x), scale(p1.y), scale(p2.x), scale(p2.y))
      if (r.isIncoming) {
        val p1 = pointOnRadialFeeder(currentCircle, angle, v.transitionWidthPix, v.transitionWidthPix, 0, 1.0)
        val p3 = pointOnRadialFeeder(currentCircle, angle, v.transitionWidthPix + v.radialItemPx / 4, v.transitionWidthPix, -1, 1.0)
        val p4 = pointOnRadialFeeder(currentCircle, angle, v.transitionWidthPix + v.radialItemPx / 4, v.transitionWidthPix, 1, 1.0)
        g.drawLine(scale(p1.x), scale(p1.y), scale(p3.x), scale(p3.y))
        g.drawLine(scale(p1.x), scale(p1.y), scale(p4.x), scale(p4.y))
      }
    }
    if (!isR) {
      val headPositionPx = v.shiftPx / 2
      val backPositionPx = headPositionPx + v.radialItemPx
      if (isQ) {
        drawChannels(headPositionPx, backPositionPx)
        drawChannels(headPositionPx - v.radialItemPx + v.radialItemPx / 4, backPositionPx, -8 * (if (r.isIncoming) -1 else 1))
      }
      val name = if (isQ)
        SystemLayout.renameQConnector(if (!r.isIncoming) r.activities.head.name else r.activities.last.name)(r.connector.name, r.isIncoming) else SystemLayout.renameP(r.connector.name, r.isIncoming)

      if (!showOnlyP) {
        val points = drawRadialRectangle(g, currentCircle, angle,
          headPositionPx,
          backPositionPx, v.transitionWidthPix, 1.0, !isStartFilled,
          name, isLeftK, color)
        mapping.add(points, PlaceTransition(true, qpr, name, false, r.isIncoming))
      }

    }
    sortedActivities.zipWithIndex.foreach(x => drawRadialSector(x._1, x._2))
  }

  val lf = new LabelFormatter

  override def paint(g: Graphics2D): Unit = {
    super.paint(g)
    mapping = new Mapping()

    val pCircle = Circle(v.pSorterRadiusPx, true, true, SystemLayout.pColor, 0)
    val qCircle = Circle(v.qSorterRadiusPx, false, false, SystemLayout.qColor, -1)
    val rCircle = Circle(v.rSorterRadiusPx, false, false, SystemLayout.rColor, 1)

    if (!showOnlyP) {
      sl.sorterActivities.zipWithIndex.foreach(x => drawChannel(g, pCircle, qCircle, x._2))
      sl.sorterActivities.zipWithIndex.foreach(x => drawChannel(g, pCircle, rCircle, x._2))
    }
    if (!showOnlyP) {
      sl.feeders.foreach(x => drawFeeder(g, pCircle, x, -1, SystemLayout.renameQ(SystemLayout.surroundings(x.activities)), SystemLayout.renamePl(SystemLayout.surroundings(x.activities))))
      sl.feeders.foreach(x => drawFeeder(g, pCircle, x, 1, SystemLayout.renameR, SystemLayout.renamePl(SystemLayout.surroundings(x.activities))))
    }
    sl.feeders.foreach(x => drawFeeder(g, pCircle, x, 0, SystemLayout.renameP,
      if (x.isIncoming) SystemLayout.renamePl(SystemLayout.surroundings(x.activities)) else SystemLayout.renamePlOutgoing(SystemLayout.surroundings(x.activities))
    ))
    g.setColor(pCircle.color)
    g.drawOval(scale(v.sorterCenter.x - pCircle.sorterRadiusPx), scale(v.sorterCenter.y - pCircle.sorterRadiusPx), scale(pCircle.sorterDiameterPx), scale(pCircle.sorterDiameterPx))
    sl.sorterActivities.zipWithIndex.foreach(x => drawSector(g, pCircle, x._1, x._2, SystemLayout.renameP, SystemLayout.renamePl(SystemLayout.surroundings(sl.sorterActivities)), false, true, 0))
    if (!showOnlyP) {
      sl.sorterActivities.zipWithIndex.foreach(x => drawSector(g, qCircle, x._1, x._2, SystemLayout.renameQ(SystemLayout.surroundings(sl.sorterActivities)), SystemLayout.renameQ(SystemLayout.surroundings(sl.sorterActivities)), true, false, -1))
      sl.sorterActivities.zipWithIndex.foreach(x => drawSector(g, rCircle, x._1, x._2, SystemLayout.renameR, SystemLayout.renameQ(SystemLayout.surroundings(sl.sorterActivities)), true, true, 1))
    }
    //mapping.makeFinal()
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


class LabelFormatter() {

  val DefaultFontName = "Consolas"
  val labelFont = new Font(DefaultFontName, Font.BOLD, 12)
  val scriptFont = new Font(DefaultFontName, Font.PLAIN, 10)

  def drawString(g: Graphics2D, text: String, x: Int, y: Int): Unit = {

    val parts = text.split("\\" + SystemLayout.Sep)
    val p1 = parts(0)
    val p2 = parts(1)
    g.setFont(labelFont)
    g.drawString(p1, x, y)
    g.setFont(scriptFont)
    g.drawString(p2, x + 7 * p1.length, y - 7)
  }


}
