package org.processmining.scala.viewers.spectrum2.view

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.geom.AffineTransform
import java.awt.{BasicStroke, Color, Font, Graphics2D, Rectangle, Toolkit}
import java.io.File
import java.util.regex.Pattern

import javax.imageio.ImageIO
import javax.swing.Timer
import org.processmining.scala.log.utils.common.errorhandling.EH
import org.processmining.scala.viewers.spectrum.view.{AppSettings, BwQ4Palette, DefaultPalette}
import org.processmining.scala.viewers.spectrum2.model.{SegmentEvent, _}
import org.processmining.scala.viewers.spectrum2.pqr.{PlaceTransition, SystemLayout}
import org.slf4j.LoggerFactory

import scala.collection.mutable.{HashMap, MultiMap, Set}
import scala.swing.event.{MouseClicked, MouseMoved, MousePressed, MouseReleased}
import scala.swing.{BorderPanel, Dimension, Graphics2D, Point}

case class SortingOrderEntry(variantName: String, segmentNames: Vector[SegmentName], slaveSegments: Map[SegmentName, SegmentName])

case class Point2(x: Int, y: Int)


class PerformanceSpectrumPanel(private var builder: CallableWithLevel[AbstractDataSource],
                               mouseMoveListener: Long => Unit,
                               updateScrollingPanel: Unit => Unit,
                               updateHorizontalScrollAndZoom: (Int, Int) => Unit) extends BorderPanel with ActionListener {


  def onBinSize(toLong: Long) = {
    binSizeMs = toLong
    forceRepaint()
  }


  def onShowGrid(selected: Boolean): Unit = {
    showGrid = selected
    forceRepaint()
  }

  def onUseStroke(selected: Boolean): Unit = {
    useStroke = selected
    forceRepaint()
  }


  def onOverlaidLoad(selected: Boolean): Unit = {
    showOverlaidLoad = selected
    forceRepaint()
  }

  def onErrorLoad(selected: Boolean): Unit = {
    showErrorLoad = selected
    forceRepaint()
  }

  def onUncertainLoad(selected: Boolean): Unit = {
    showUncertainLoad = selected
    forceRepaint()
  }

  def onCertainLoad(selected: Boolean): Unit = {
    showCertainLoad = selected
    forceRepaint()
  }

  def onLoad(selected: Boolean): Unit = {
    showLoad = selected
    forceRepaint()
  }


  private var ds = builder.call()
  private var ls = new LoadSource(ds)
  private var currentLevel = builder.levelCount
  private var currentOverlaidMode = PerformanceSpectrumPanel.OverlaidOnTop
  var useStroke = false
  var foregroundOverliadSegments: List[(Int, Int, Int, Int)] = List()
  private var isSegmentRmsePrinted = false

  private var binSizeMs = 15000L
  private val dashOverlaid = Array(4.0f, 4.0f)
  private val dashBorders = Array(2.0f, 2.0f)

  private val dashOverlaidLoad = Array(4.0f, 4.0f)
  private val dashCertainLoad = Array(5.0f, 5.0f)
  private val dashUncertainLoad = Array(3.0f, 2.0f)

  private val dashedOverlaid = new BasicStroke(5.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashOverlaid, 0.0f)
  private val dashedBorders = new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashBorders, 0.0f)
  private val thikStroke50 = new BasicStroke(5f)
  private val thikStroke35 = new BasicStroke(3.5f)
  private val thikStroke30 = new BasicStroke(3f)
  private val thikStroke20 = new BasicStroke(2f)
  private val thinStroke10 = new BasicStroke(0.25f)
  private val unobservedSegmentStroke = new BasicStroke(0.1f)

  private val dashedOverlaidLoad = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashOverlaidLoad, 0.0f)
  private val dashedCertainLoad = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashCertainLoad, 0.0f)
  private val dashedUncertainLoad = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashUncertainLoad, 0.0f)
  private var debugTimer: Timer = new Timer(500, this)

  val pqrSinkClient = new SinkClient("localhost", AppSettings(AppSettings.DefaultFileName).simServerPort)


  def setNewBuilder(newBuilder: CallableWithLevel[AbstractDataSource]) = {
    builder = newBuilder
    ds = builder.call()
    ls = new LoadSource(ds)
    currentLevel = builder.levelCount
    isSegmentRmsePrinted = false
    forceRepaint()
    ds.setupCallbacks(onSegmentOccurrence, () => onReset(), x => onTimer(x))
    //    debugTimer = new Timer(500, this)
    //    debugTimer.start()

  }

  def onReset(): Unit = {
    onVerticalZoomChanged(verticalZoom)
  }

  //  private var isAutoScrollMode = false
  //
  //  private var justStarted = false
  //

  private def onStartStop(isStarted: Boolean): Unit = {
    //    if (isStarted) {
    //      justStarted = true
    //    } else {
    //      justStarted = false
    //      ds.restoreEndTimeMs()
    //      forceRepaint()
    //    }
    //
    //
    //    isAutoScrollMode = isStarted
  }

  private var ongoingSegments: Map[(String, SegmentName), (SegmentName, Segment)] = Map()
  private var nextActivity: Map[String, String] = Map()
  //private val DefaultDeltaMs = 1000L


  def updateOngoingSegments(data: Map[SegmentName, Vector[Segment]]) = {
    var toErase: Vector[(SegmentName, Segment)] = Vector()
    val toDraw = data.flatMap(e => {
      nextActivity += (e._1.a -> e._1.b)
      val nextActivityOpt = nextActivity.get(e._1.b)
      e._2.map(s => {
        val observedSegment = (e._1, s)
        val key = (observedSegment._2.caseId, observedSegment._1)
        val unobservedSegmentOpt = ongoingSegments.get(key)
        if (unobservedSegmentOpt.isDefined) {
          ongoingSegments -= key
          toErase = toErase :+ (unobservedSegmentOpt.get._1 -> unobservedSegmentOpt.get._2)
        }
        if (nextActivityOpt.isDefined) {

          val newUnobservedSegmentName = SegmentName(observedSegment._1.b, nextActivityOpt.get)
          if (!ds.hasChoice.contains(newUnobservedSegmentName)) {
            val (_, minDurationMs) = ds.classify(newUnobservedSegmentName, 0)
            if (minDurationMs > 0) {
              val newUnobservedSegment = (newUnobservedSegmentName,
                observedSegment._2.copy(start = observedSegment._2.end, end = SegmentEventImpl(observedSegment._2.end.minMs + minDurationMs)))
              ongoingSegments += (observedSegment._2.caseId, newUnobservedSegment._1) -> newUnobservedSegment
              Some(newUnobservedSegment)
            } else None
          } else None


        } else None
      })
    }).filter(_.isDefined).map(_.get).groupBy(_._1).map(x => x._1 -> x._2.map(_._2).toVector)

    if (showOngoing) {
      paintSegmentMap(toErase.groupBy(_._1).map(x => x._1 -> x._2.map(_._2)), true, false)
      paintSegmentMap(toDraw, false, true)
    }
  }

  private def paintSegmentMap(g: Graphics2D, data: Map[SegmentName, Vector[Segment]], erase: Boolean, unobserved: Boolean) = {
      val flatSortingOrder = getFlatSortingOrder
      paintSegments(g, flatSortingOrder, true, data, erase, unobserved)

  }

  private def paintSegmentMap(data: Map[SegmentName, Vector[Segment]], erase: Boolean, unobserved: Boolean): Unit =
    paintSegmentMap(peer.getGraphics.asInstanceOf[Graphics2D], data, erase, unobserved)


  //Map[(String, SegmentName), (SegmentName, Segment)]
  private def paintOngoingMap(g: Graphics2D) = {
    val data = ongoingSegments.values.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).toVector)
    paintSegmentMap(g, data, false, true)
  }


  private def onTimer(currentTimeMs: Long) = {
  if(showOngoing) {
    ongoingSegments = ongoingSegments.mapValues(x => {
      val (newVal, chaanged) = if (x._2.end.minMs >= currentTimeMs) (x._2.end, false) else {
        (SegmentEventImpl(currentTimeMs), true)
      }
      val ret = x._1 -> x._2.copy(end = newVal)
      if (chaanged) {
        val toErase: Map[SegmentName, Vector[Segment]] = Map(x._1 -> Vector(x._2))
        val toDraw: Map[SegmentName, Vector[Segment]] = Map(x._1 -> Vector(ret._2))
        paintSegmentMap(toErase, true, true)
        paintSegmentMap(toDraw, false, true)
      }
      ret
    }
    )
  }
  }

  private def onSegmentOccurrence(data: Map[SegmentName, Vector[Segment]]): Unit = {

    try {
      updateOngoingSegments(data)

      val maxTimeMs = data.flatMap(_._2).map(_.end.minMs).max
      if (maxTimeMs >= ds.endTimeMs) {
        val screenWidthMs = pixelsToMs(PerformanceSpectrumPanel.HundredPercentPixels) - pixelsToMs(0)
        val fakeEndTs = maxTimeMs + screenWidthMs
        ds.setFakeEndTimeMs(fakeEndTs)
        val newHorizontalScroll = ((maxTimeMs - ds.startTimeMs).toDouble * PerformanceSpectrumPanel.HorizontalScrollMax / ds.durationMs).toInt
        val newZoom = OneHundredPercent.toDouble / ((fakeEndTs - ds.startTimeMs).toDouble / ds.durationMs - newHorizontalScroll.toDouble / PerformanceSpectrumPanel.HorizontalScrollMax)
        updateHorizontalScrollAndZoom(newHorizontalScroll, newZoom.toInt)
        forceRepaint()
      } else {
        paintSegmentMap(data, false, false)
      }
    } catch {
      case e: Throwable => logger.error(EH.formatError(e.toString, e))
    }
  }

  //  private var debugNextTs = 1000L
  //  private var debugDuration = 5000L
  //  private var debugState = 0


  def actionPerformed(e: ActionEvent): Unit = {
    //    case class SegmentEventDebug(override val minMs: Long, override val maxMs: Long) extends SegmentEvent
    //    if (debugState == 0) {
    //      ds.startStop(true)
    //      debugState = 1
    //    } else if (debugState == 100) {
    //      ds.startStop(false)
    //      debugState = 0
    //    } else {
    //      debugState += 1
    //      val caseId = "ID1"
    //      val ts1 = debugNextTs
    //      val ts2 = debugNextTs + debugDuration
    //      //      ds.segmentOccurrence(
    //      //        SegmentName("IN_3_3:x", "IN_4_3:x"),
    //      //        Segment(caseId, SegmentEventDebug(ts1, ts1), SegmentEventDebug(ts2, ts2), 0))
    //      debugSinkClient.send(SegmentName("IN_3_3:x", "IN_4_3:x"), Segment(caseId, SegmentEventDebug(ts1, ts1), SegmentEventDebug(ts2, ts2), 0))
    //      debugNextTs += debugDuration
    //    }
  }

  def onLevelChanged(level: Int): Unit = {
    if (currentLevel != level) {
      ds = builder.call(level)
      ds.setupCallbacks(onSegmentOccurrence, () => onReset(), x => onTimer(x))
      currentLevel = level
      //      sortingOrder = if (level == 1) {
      //        if (observedPsSortingOrderFactory.isDefined) observedPsSortingOrderFactory.get(ds) else PerformanceSpectrumPanel.defaultSortingOrder(ds)
      //      }
      //      else {
      //        if (unobservedPsSortingOrderFactory.isDefined) unobservedPsSortingOrderFactory.get(ds) else PerformanceSpectrumPanel.defaultSortingOrder(ds)
      //      }
      updatePrefferedSize()
      forceRepaint()
    }
  }


  private val logger =
    LoggerFactory.getLogger(classOf[PerformanceSpectrumPanel])
  private var horizontalZoomPercent =
    PerformanceSpectrumPanel.HorizontalZoomDefaultValue
  var verticalZoom =
    PerformanceSpectrumPanel.VerticalZoomDefaultValue
  private var horizontalScroll =
    PerformanceSpectrumPanel.HorizontalScrollDefaultValue
  private var scrollPanelSize =
    new Dimension(100, 100)
  private val OneHundredPercent =
    100

  private var showRegions =
    false
  private var showRightBorders =
    false
  private var showMerges =
    false
  private var useOrigins =
    false
  private var showProtectedSpaceBefore =
    false
  private var showProtectedSpaceAfter =
    false
  private var showCertainLoad =
    false
  private var showLoad =
    false
  private var showUncertainLoad =
    false
  private var showOverlaidLoad =
    false
  private var showErrorLoad =
    false

  private var showGrid =
    false

  val segmentFont = new Font(AppSettings.DefaultFontName, Font.PLAIN, AppSettings.DefaultFontSize)
  val variantFont = new Font(AppSettings.DefaultFontName, Font.PLAIN, AppSettings.DefaultFontSize - 4)

  import java.awt.TexturePaint

  var img1 = ImageIO.read(new File("./img/pattern2.jpg"));
  val tp1 = new TexturePaint(img1, new Rectangle(0, 0, 100, 100))
  var img2 = ImageIO.read(new File("./img/pattern3.jpg"));
  val tp2 = new TexturePaint(img2, new Rectangle(0, 0, 100, 100))

  listenTo(mouse.clicks, mouse.moves)
  background = Color.white

  var pointPressed = new Point()

  def pointToindex(p: Point): Int = {
    p.y / getSegmentHeight
  }

  def processSorting(p1: Point, p2: Point) {
    val fso = getFlatSortingOrder()
    val i1 = pointToindex(p1)
    val i2 = pointToindex(p2)
    if (i1 < fso.size && i2 < fso.size && i1 != i2) {
      ds.changeOrder(i1, i2)
      forceRepaint()
    }

  }

  def processDelete(p: Point) {
    val fso = getFlatSortingOrder()
    val i = pointToindex(p)
    if (i < fso.size) {
      ds.remove(i)
      forceRepaint()
    }

  }

  def processAddDimensions(p: Point) {
    val fso = getFlatSortingOrder()
    val i = pointToindex(p)
    if (i < fso.size) {
      //val labels = fso(i)._1._1.a.split(SystemLayout.QSep)
      val sn = fso(i)._1._1
      val qpr = PerformanceSpectrumPanel.getLabelTypeQpr(sn)
      if(qpr == 0){
        val rs1 = s"${SystemLayout.Start}${SystemLayout.Sep}${sn.a}"
        val rc1 = s"${SystemLayout.Complete}${SystemLayout.Sep}${sn.a}"
        val rs2 = s"${SystemLayout.Start}${SystemLayout.Sep}${sn.b}"
        val rc2 = s"${SystemLayout.Complete}${SystemLayout.Sep}${sn.b}"
        val enq = s"${SystemLayout.Enqueue}${SystemLayout.Sep}${sn.a}${SystemLayout.QSep}${sn.b}"
        //ds.pqrCommand(PlaceTransition(true,1, rc1, false, false, 1))
        ds.pqrCommand(PlaceTransition(true,1, rs1, false, false, 1))
        ds.pqrCommand(PlaceTransition(true,-1, enq, false, false, 1))
        //ds.pqrCommand(PlaceTransition(true,1, rc2, false, false, 1))
        ds.pqrCommand(PlaceTransition(true,1, rs2, false, false, 1))





      }


    }

  }




  def processFind(p: Point) {
    val fso = getFlatSortingOrder()
    val i = pointToindex(p)
    if (i < fso.size) {
      val label = fso(i)._1._1.a
      pqrSinkClient.send(PlaceTransition(true, 0, label, false, false))
    }

  }

  reactions += {
    case mousePressed: MousePressed => {
      if (mousePressed.peer.getButton == 1)
        pointPressed = mousePressed.point
    }


    case mouseReleased: MouseReleased => {
      if (mouseReleased.peer.getButton == 1) {
        processSorting(pointPressed, mouseReleased.point)
        pointPressed = new Point()
      } else {
        processDelete(mouseReleased.point)
      }
    }

    case mouseClicked: MouseClicked => {
      if (mouseClicked.peer.getButton == 1 && mouseClicked.clicks == 1) {
        processFind(mouseClicked.point)
      } else if (mouseClicked.peer.getButton == 1 && mouseClicked.clicks == 2) {
        //processFind(mouseClicked.point)
        processAddDimensions(mouseClicked.point)
      }
    }

    case MouseMoved(_, point, _) => {
      val utcUnixTimeMs = pixelsToMs(point.x)
      mouseMoveListener(utcUnixTimeMs)
      highlightRegion(point)

      //      if(pointPressed != new Point) {
      //        val g: Graphics2D = peer.getGraphics.asInstanceOf[Graphics2D]
      //        g.setColor(Color.orange)
      //        g.fillOval(point.x, point.y, 2, 2)
      //      }

    }
  }

  updatePrefferedSize

  val p2id = new HashMap[Point2, Set[String]] with MultiMap[Point2, String]
  var selectedId = scala.collection.mutable.Set[String]()

  val highlightingAccuracy = 5

  def highlightRegion(p: Point): Unit = {
    val p2 = Point2(p.x / highlightingAccuracy, p.y / highlightingAccuracy)
    //logger.debug(s"${p2.x};${p2.y}")
    val idSet = p2id.get(p2)
    if (idSet.isDefined) {
      selectedId = idSet.get
      forceRepaint()
    }
  }

  def updatePrefferedSize() = preferredSize =
    new Dimension(100, getSegmentHeight * ds.sortingOrder.map(_.segmentNames.size).sum + ds.sortingOrder.size * getSegmentGap)


  def scaleY(x: Double): Double = x * verticalZoom

  def getSegmentHeight() = (PerformanceSpectrumPanel.SegmentHeight * verticalZoom.toDouble).toInt

  def getSegmentGap() = (PerformanceSpectrumPanel.SegmentGap * verticalZoom.toDouble).toInt


  def drawSegmentText(g: Graphics2D, soe: SortingOrderEntry, segments: Vector[Segment], y1: Int, h: Int, segmentName: SegmentName, isMaster: Boolean) = {
    val y2 = y1 + h
    val middleY = y2 - 5
    if (isMaster) {
      g.setColor(Color.white)

      g.fillRect(30, middleY - 20, 150, 30)
      g.fillRect(0, y1 + 2, 20, y2 - y1 - 4) //vertical

      g.setColor(PerformanceSpectrumPanel.getLabelColor(segmentName))
      g.setFont(segmentFont)
      g.drawString(segmentName.toString, 35, middleY)

      val affineTransform = new AffineTransform
      affineTransform.rotate(Math.toRadians(270), 0, 0)
      val rotatedFont = segmentFont.deriveFont(affineTransform)
      g.setFont(rotatedFont)
      g.setColor(Color.black)
      g.drawString(soe.variantName, 15, y2 - 10)
    } else {
      g.setColor(Color.white)
      g.fillRect(20, y1 + 5, 125, 20)
      g.setColor(Color.black)
      g.setFont(segmentFont)
      g.drawString(s"${segmentName.a}${SegmentName.DefaultSeparator}", 25, y1 + 20)
    }
  }

  def drawSegmentTextSimple(g: Graphics2D, y1: Int, h: Int, segmentName: SegmentName, color: Color) = {
    val y2 = y1 + h
    val middleY = y2 - 5
    g.setColor(Color.white)
    g.fillRect(30, middleY - 20, 150, 30)
    g.fillRect(0, y1 + 2, 20, y2 - y1 - 4) //vertical
    g.setColor(color)
    g.setFont(segmentFont)
    g.drawString(segmentName.toString, 35, middleY)
  }


  var foregroundRegions: List[(Array[Int], Array[Int])] = List()

  def drawForeground(g: Graphics2D): Unit = {
    val initialStroke = g.getStroke

    g.setStroke(dashedBorders)
    foregroundRegions.foreach(e => {
      val (xx, yy) = e
      g.setColor(new Color(255, 255, 0, 150))
      g.fillPolygon(xx, yy, xx.size)
      g.setColor(Color.blue)
      g.setStroke(dashedBorders)
      g.drawLine(xx(1), yy(0), xx(2), yy(2))
      g.setColor(Color.red)
      g.drawLine(xx(0), yy(0), xx(3), yy(2))
    })

    foregroundOverliadSegments.foreach(e => {
      val (x1, y1, x2, y2) = e
      g.setColor(Color.black)
      g.setStroke(dashedOverlaid)
      g.drawLine(x1, y1, x2, y2)
    })
    g.setStroke(initialStroke)

  }

  def load(g: Graphics2D, y1: Int, h: Int, bins: Array[Int], binSizeMs: Long, max: Int, fill: Boolean = false) = {
    val y2 = y1 + h
    var prev = -1
    bins.zipWithIndex.foreach(x => {
      val x1 = msToPixels(ds.startTimeMs + x._2 * binSizeMs)
      val x2 = msToPixels(ds.startTimeMs + (x._2 + 1) * binSizeMs)
      val loadPx = y2 - ((x._1.toDouble / max) * h).toInt
      if (prev >= 0) {

        if (fill) {
          val xx = Array(x1, x2, x2, x1, x1)
          val yy = Array(loadPx, loadPx, y2, y2, loadPx)
          g.setColor(new Color(255, 0, 0))
          g.fillPolygon(xx, yy, xx.length)
        } else {
          g.setColor(new Color(0, 0, 0))
          g.drawLine(x1, prev, x1, loadPx)
          g.drawLine(x1, loadPx, x2, loadPx)
        }


      }
      prev = loadPx
    })

  }

  val palette = new BwQ4Palette

  def drawSegment(g: Graphics2D, soe: SortingOrderEntry, segments: Vector[Segment], y1: Int, h: Int, segmentName: SegmentName, isMaster: Boolean, doDrawing: Boolean,
                  erase: Boolean,
                  isDashed: Boolean) = {

    var isMaeRmsePrinted = false

    val y2 = y1 + h
    val shiftPx = PerformanceSpectrumPanel.MinDistanceMs

    segments
      //.filter(_.caseId == "44834241")
      .foreach(s => {
        val x1 = msToPixels(s.start.minMs)
        val x1Max = msToPixels(s.start.maxMs)
        val x2 = msToPixels(s.end.minMs)
        val x2Max = msToPixels(s.end.maxMs)


        val x1ProtectedLeft = msToPixels(s.start.minMs - shiftPx)
        val x2ProtectedLeft = msToPixels(s.end.minMs - shiftPx)

        val x1MaxProtectedRight = msToPixels(s.start.maxMs + shiftPx)
        val x2MaxProtectedRight = msToPixels(s.end.maxMs + shiftPx)

        val dx1 = x1Max - x1
        val dx2 = x2Max - x2
        val dx1PRight = x1MaxProtectedRight - x1
        val dx2PRight = x2MaxProtectedRight - x2

        val xx = Array(x1, x1 + (if (isMaster) dx1 else (dx1 / 2)), x2 + dx2, x2, x1)
        val xxPLeft = Array(x1ProtectedLeft, x1 + (if (isMaster) dx1 else (dx1 / 2)), x2 + dx2, x2ProtectedLeft, x1ProtectedLeft)
        val xxPRight = Array(x1, x1 + (if (isMaster) dx1PRight else (dx1PRight / 2)), x2 + dx2PRight, x2, x1)
        val yy = Array(y1, y1, y2, y2, y1)

        //val origin = ds.caseIdToOriginActivityAndIndex(s.caseId)
        //TODO: thesis
        val origin = ("", 0)


        if (doDrawing) {
          if (showProtectedSpaceBefore) {
            g.setColor(new Color(255, 0, 0, 50))
            //g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .4f))
            g.setPaint(tp1)
            g.fillPolygon(xxPLeft, yy, xx.size)
          }
          if (showProtectedSpaceAfter) {
            g.setColor(new Color(0, 255, 0, 50))
            g.setPaint(tp2)
            g.fillPolygon(xxPRight, yy, xx.size)

          }

          val isSelected = selectedId.contains(s.caseId)

          if (showRegions) {

            if (useOrigins) {
              val color = PerformanceSpectrumPanel.getColorByIndex(origin._2, if (isMaster) 75 else 90)
              g.setColor(color)
            } else if (isMaster) g.setColor(new Color(0, 0, 199, 81)) else g.setColor(new Color(255, 199, 0, 90))

            if (isSelected) {
              foregroundRegions = (xx, yy) :: foregroundRegions
            }
            g.fillPolygon(xx, yy, xx.size)
            //        if (isMaster) g.setColor(Color.blue) else g.setColor(Color.orange)
            //        g.drawPolygon(xx, yy, xx.size)

            p2id.addBinding(Point2(xx(0) / highlightingAccuracy, yy(0) / highlightingAccuracy), s.caseId)
          }
          if (currentOverlaidMode != PerformanceSpectrumPanel.OverlaidNothing) {
            if (ds.isClassificationSupported()) {

              //!!!!!

              val (clazz, _) = ds.classify(segmentName, s.end.minMs - s.start.minMs)
              val color = if (erase) Color.white else if (isDashed) if (clazz > 1) Color.magenta else Color.lightGray
              else palette.getClazzColor(clazz, 255)
              g.setColor(color)

              //g.setColor(palette.getClazzColor(clazz, 255))


              //              if (clazz == 0) g.setColor(Color.green)
              //              else if (clazz == 1) g.setColor(Color.orange)
              //              else g.setColor(Color.red)

            } else
              g.setColor(if (useOrigins && !showRegions) PerformanceSpectrumPanel.getColorByIndex(origin._2, 100) else if (isMaster) Color.yellow else new Color(0, 128, 0))
            //if (isDashed) g.setStroke(unobservedSegmentStroke) else g.setStroke(thinStroke10)
            g.setStroke(thinStroke10)
            g.drawLine(x1, y1, x2, y2)
            //            if (showRightBorders) {
            //              g.setColor(Color.blue)
            //              g.drawLine(xx(1), y1, xx(2), y2)
            //            }

            //            if (s.start.isObserved) g.fillOval(x1, y1, 4, 5)
            //            if (s.end.isObserved) g.fillOval(x2, y2 - 5, 4, 5)
          }

          if (s.start.isObserved) g.drawLine(x1, y1 + 1, x1 + (if (isMaster) dx1 else (dx1 / 2)), y1 + 1)
          if (s.end.isObserved) g.drawLine(x2, y2 - 1, x2 + dx2, y2 - 1)
        }

        if (isMaster) g.setColor(Color.lightGray) else g.setColor(Color.lightGray)
        g.drawLine(0, y1, 2000, y1)
        g.drawLine(0, y2, 2000, y2)
        if (isMaster) {
          g.setColor(Color.lightGray)
          (ds.startTimeMs / 1000 * 1000 until ds.endTimeMs by 2000).foreach(t => {
            val x1 = msToPixels(t)
            val x2 = msToPixels(t + 1000)
            g.drawLine(x1, y2, x2, y2)
          }
          )
        }

        //      g.setColor(Color.black)
        //      g.drawString(s.caseId, x1, y1)


        val k = 2;




        if (doDrawing) {
          if (showCertainLoad || showUncertainLoad || showOverlaidLoad || showErrorLoad || showLoad) {
            val initStroke = g.getStroke
            val (tmpSegmentsMin, tmpSegmentsMax) = ls.segmentLoad(binSizeMs)(segmentName)
            val tmpOverlaid = ls.overlaidSegmentLoad(segmentName, binSizeMs)
            val sumLoad = tmpSegmentsMin.bins.zip(tmpSegmentsMax.bins).map(x => x._1 + x._2 / k)
            val sumLoadMax = sumLoad.max
            val max = if (tmpOverlaid.max > 0) tmpOverlaid.max else Math.max(sumLoadMax, tmpOverlaid.max)

            if (showErrorLoad) {
              if (!isSegmentRmsePrinted) {
                isSegmentRmsePrinted = true
                val utils = new Utils(ds)
                utils.getNormalizedAverageTimestampInterval().foreach(x => logger.info(s"Timestamp average duration: ${x._1} = ${x._2}%"))
                utils.getNormalizedErrors().foreach(x => logger.info(s"Timestamp error: (MAE, RMSE) of ${x._1} = ${x._2._1}% ${x._2._2}%"))
              }
              if (tmpOverlaid != null) {
                if (useStroke)
                  g.setStroke(dashedOverlaidLoad)
                else
                  g.setStroke(thikStroke20)
                //g.setColor(new Color(255, 0, 0))

                val diff = sumLoad
                  .zip(tmpOverlaid.bins)
                  .map(x => Math.abs(x._2 - x._1))
                //.map(x => x._2 - x._1)
                load(g, y1, h, diff, binSizeMs, max, true)

                if (!isMaeRmsePrinted) {
                  val (mae, rmse) = Utils.computeMaeRmse(sumLoad.map(_.toLong), tmpOverlaid.bins.map(_.toLong), max)
                  logger.info(s"(MAE, RMSE) of '${segmentName}' = ($mae;  $rmse)")
                  isMaeRmsePrinted = true
                }
              }
            }


            if (showLoad) {
              //            if (useStroke)
              //              g.setStroke(dashedCertainLoad)
              //            else
              //              g.setStroke(thikStroke35)
              g.setStroke(thikStroke20)
              g.setColor(new Color(0, 0, 0))
              val sumLoad = tmpSegmentsMin.bins.zip(tmpSegmentsMax.bins).map(x => x._1 + x._2 / k)
              load(g, y1, h, sumLoad, binSizeMs, max)
              //g.setStroke(thikStroke35)
            }

            if (showCertainLoad) {
              if (useStroke)
                g.setStroke(dashedCertainLoad)
              else
                g.setStroke(thikStroke35)
              g.setColor(new Color(255, 0, 0, 50))
              load(g, y1, h, tmpSegmentsMin.bins, binSizeMs, max)
            }
            if (showUncertainLoad) {
              if (useStroke)
                g.setStroke(dashedUncertainLoad)
              else
                g.setStroke(thikStroke30)
              g.setColor(new Color(0, 0, 255, 50))
              load(g, y1, h, tmpSegmentsMax.bins.map(_ / k), binSizeMs, max / k)
            }
            if (showOverlaidLoad) {
              if (tmpOverlaid != null) {
                if (useStroke)
                  g.setStroke(dashedOverlaidLoad)
                else
                  g.setStroke(thikStroke20)
                g.setColor(new Color(0, 255, 0))
                load(g, y1, h, tmpOverlaid.bins, binSizeMs, max)
              }
            }


            g.setStroke(initStroke)
          }
        }

      }
      )

  }


  def drawOverlaidSegments(g: Graphics2D, h: Int, srcSegment: (SegmentName, List[Int]), ySMap: Map[SegmentName, List[Int]]): Unit = {

    val overlaidSegments = ds.overlaidSegments(srcSegment._1)
    overlaidSegments.foreach(x => {
      val dstSegmentsOpt = ySMap.get(x.dstSegment)
      if (dstSegmentsOpt.isDefined) {
        val dstSegments = dstSegmentsOpt.get
        val x1 = msToPixels(x.timestamp1)
        val x2 = msToPixels(x.timestamp2)
        srcSegment._2.foreach(y1 => {
          dstSegments.foreach(y2 => {
            val adjustedY1 = if (x.overlaidSegment.a != srcSegment._1.a) y1 + h else y1
            val adjustedY2 = if (x.overlaidSegment.b != x.dstSegment.a) y2 + h else y2
            val isSelected = selectedId.contains(x.caseId)
            g.setColor(x.color)
            g.drawLine(x1, adjustedY1, x2, adjustedY2)
            p2id.addBinding(Point2(x1 / highlightingAccuracy, adjustedY1 / highlightingAccuracy), x.caseId)
            if (isSelected) {
              foregroundOverliadSegments = (x1, adjustedY1, x2, adjustedY2) :: foregroundOverliadSegments
            }
          })
        })
      }

      //      if (showOverlaidLoad) {
      //        val tmp = ls.overlaidSegmentLoad(srcSegment._1, binSizeMs)
      //
      //        g.setColor(Color.red)
      //        load(g, adjustedY1, h, tmp, binSizeMs)
      //
      //      }


    })
  }



  def paintNamesAndLines(g: Graphics2D, flatSortingOrder: Seq[((SegmentName, Int, SortingOrderEntry), Int)]): Unit = {
    val h = getSegmentHeight
    flatSortingOrder.foreach(x => {
      val variantIndex = x._1._2
      val segmentIndex = x._2
      val y = segmentIndex * h + variantIndex * getSegmentGap
      drawSegmentTextSimple(g, y, h, x._1._1, PerformanceSpectrumPanel.getLabelColor(x._1._1))
      g.setColor(Color.lightGray)
      g.drawLine(0, y + h, 2500, y + h)
    })
  }

  def paintSegments(g: Graphics2D,
                    flatSortingOrder: Seq[((SegmentName, Int, SortingOrderEntry), Int)],
                    doDrawing: Boolean,
                    dsSegments: Map[SegmentName, Vector[Segment]],
                    erase: Boolean,
                    isDashed: Boolean

                   ) = {
    val h = getSegmentHeight
    var yS: List[(SegmentName, Int)] = List()
    flatSortingOrder.foreach(x => {
      val segmentsOpt = dsSegments.get(x._1._1)
      if (segmentsOpt.isDefined) {

        val segments = segmentsOpt.get
        val variantIndex = x._1._2
        val segmentIndex = x._2
        val y = segmentIndex * h + variantIndex * getSegmentGap

        val slave = x._1._3.slaveSegments.get(x._1._1)
        //        if (showMerges) {
        //          if (slave.isDefined) {
        //            val slaveSegments = dsSegments(slave.get)
        //            val shift = (getSegmentHeight() * 0.2).toInt
        //            drawSegment(g, x._1._3, slaveSegments, y + shift, h - shift, slave.get, false, doDrawing)
        //
        //          }
        //        }
        drawSegment(g, x._1._3, segments, y, h, x._1._1, true, doDrawing, erase, isDashed)
        yS = (x._1._1, y) :: yS
        drawSegmentText(g, x._1._3, segments, y, h, x._1._1, true)
        //        if (showMerges) {
        //          if (slave.isDefined) {
        //            //TODO: remove dublicates
        //            val slaveSegments = dsSegments(slave.get)
        //            val shift = (getSegmentHeight() * 0.2).toInt
        //            drawSegmentText(g, x._1._3, slaveSegments, y + shift, h - shift, slave.get, false)
        //          }
        //        }
      } //else logger.warn(s"Segment '${x._1._1}' not found")
    })
    yS
  }


  def getFlatSortingOrder(): Seq[((SegmentName, Int, SortingOrderEntry), Int)] =
    ds.sortingOrder
      .zipWithIndex
      .flatMap(x => x._1.segmentNames.map(s => (s, x._2, x._1))).zipWithIndex


  override def paint(g: Graphics2D): Unit

  = {
    super.paint(g)
    ls = new LoadSource(ds)
    foregroundOverliadSegments = List()
    foregroundRegions = List()
    p2id.clear()
    if (showGrid)
      drawGrid(g)


    val flatSortingOrder = getFlatSortingOrder

    paintNamesAndLines(g, flatSortingOrder)

    val doDrawing = currentOverlaidMode == PerformanceSpectrumPanel.OverlaidOnTop || currentOverlaidMode == PerformanceSpectrumPanel.OverlaidNone || currentOverlaidMode == PerformanceSpectrumPanel.OverlaidNothing
    val yS = paintSegments(g, flatSortingOrder, doDrawing, ds.segments, false, false)
    if (currentOverlaidMode != PerformanceSpectrumPanel.OverlaidNone && currentOverlaidMode != PerformanceSpectrumPanel.OverlaidNothing) {
      val ySMap = yS
        .groupBy(_._1)
        .map(x => (x._1, x._2.map(_._2)))
      ySMap.foreach(s => {
        drawOverlaidSegments(g, getSegmentHeight, s, ySMap)
      })
    }

    if (currentOverlaidMode == PerformanceSpectrumPanel.OverlaidBeneath)
      paintSegments(g, flatSortingOrder, true, ds.segments, false, false)


    drawForeground(g)
    if(showOngoing)
      paintOngoingMap(g)
    //logger.debug(selectedId.mkString("Selected ID", "; ", ""))
  }

  def drawGrid(g: Graphics2D): Unit = {
    if (binSizeMs != 0 && ds.endTimeMs > 0) {
      val h = getSegmentHeight * ds.segmentNames.size
      g.setColor(Color.lightGray)
      (ds.startTimeMs until ds.endTimeMs by binSizeMs).foreach(t => {
        g.drawLine(msToPixels(t), 0, msToPixels(t), h)
      })
    }

  }

  def onHorizontalPositionChanged(pos: Int): Unit = {
    horizontalScroll = pos
    logger.info(s"horizontalScroll=$horizontalScroll")
    forceRepaint()
  }

  def onHorizontalZoomChanged(zoom: Int): Unit = {
    horizontalZoomPercent = zoom
    logger.info(s"horizontalZoom=$horizontalZoomPercent")
    forceRepaint()
  }

  def onVerticalZoomChanged(zoom: Int): Unit = {
    verticalZoom = zoom
    logger.info(s"verticalZoom=$verticalZoom")
    updatePrefferedSize()
    updateScrollingPanel()
    forceRepaint()

  }

  def onComponentResize(d: Dimension) = {
    scrollPanelSize = d
    logger.info(s"scrollPanelSize=$scrollPanelSize")
    forceRepaint()
  }

  def pixelsToMs(x: Int): Long = {
    val offsetMs: Double = ds.startTimeMs + horizontalScroll.toDouble / PerformanceSpectrumPanel.HorizontalScrollMax * ds.durationMs
    val tailMs: Double = ((x.toDouble / PerformanceSpectrumPanel.HundredPercentPixels) / (horizontalZoomPercent / OneHundredPercent) * ds.durationMs).toLong
    (offsetMs + tailMs).toLong
  }

  def msToPixels(t: Long): Int = {
    val relTimeMs = (t - ds.startTimeMs).toDouble
    val fractionTime = relTimeMs / ds.durationMs
    val ret = (fractionTime - horizontalScroll.toDouble / PerformanceSpectrumPanel.HorizontalScrollMax) * PerformanceSpectrumPanel.HundredPercentPixels * horizontalZoomPercent / OneHundredPercent
    //logger.info(s"t=$t ret=$ret")
    ret.toInt
  }

  def forceRepaint() = {
    peer.invalidate()
    peer.repaint()
  }

  def onShowReources(selected: Boolean): Unit = {
    showRegions = selected
    forceRepaint()
  }

  def onShowRightBorders(selected: Boolean): Unit = {
    showRightBorders = selected
    forceRepaint()
  }

  var showOngoing = false

  def onShowOngoing(selected: Boolean): Unit = {
    showOngoing = selected
    forceRepaint()
  }

  def onColorByOrigin(selected: Boolean): Unit = {
    useOrigins = selected
    forceRepaint()
  }

  def onProtectedSpaceBefore(selected: Boolean): Unit = {
    showProtectedSpaceBefore = selected
    forceRepaint()
  }

  def onProtectedSpaceAfter(selected: Boolean): Unit = {
    showProtectedSpaceAfter = selected
    forceRepaint()
  }

  def onOverlaidModeChanged(mode: Int) = {
    currentOverlaidMode = mode
    forceRepaint()
  }

}


object PerformanceSpectrumPanel {
  def defaultSortingOrder(ds: AbstractDataSource) = Vector(SortingOrderEntry("", ds.segmentNames, Map()))

  val HorizontalScrollMin = -10000
  val HorizontalScrollMax = 10000
  val HorizontalScrollDefaultValue = 0
  val VerticalZoomMin = 0
  val VerticalZoomMax = 250
  val VerticalZoomDefaultValue = 10

  val HorizontalZoomMin = 0
  val HorizontalZoomMax = 100000
  val HorizontalZoomDefaultValue = 100

  val MinDistanceMs = 1000

  val SegmentHeight = 5 // * VerticalZoomDefaultValue = pix
  val SegmentGap = 1
  val HundredPercentPixels = Toolkit.getDefaultToolkit().getScreenSize().width - 50

  def getColorByIndex(i: Int, a: Int): Color =
    i match {
      case 0 => new Color(128, 128, 0, a)
      case 1 => new Color(0, 255, 0, a)
      case 2 => new Color(0, 255, 255, a)
      case 3 => new Color(0, 0, 255, a)
      case 4 => new Color(255, 0, 255, a)
      case 5 => new Color(0, 128, 128, a)
      case 6 => new Color(128, 0, 0, a)
      case 7 => new Color(255, 215, 0, a)
      case 8 => new Color(100, 149, 237, a)
      case 9 => new Color(148, 0, 211, a)
      case _ => {
        val s = i - 9
        new Color(Math.min(255, s * 10), Math.min(255, s * 5), Math.max(0, 255 - s * 10), a)
      }

    }

  val OverlaidOnTop = 0
  val OverlaidBeneath = 1
  val OverlaidOnly = 2
  val OverlaidNone = 3
  val OverlaidNothing = 4

  val regQPattern = Pattern.compile(s".+${SystemLayout.Complete}.+${SystemLayout.Start}.*")
  val regRPattern = Pattern.compile(s".+${SystemLayout.Start}.+${SystemLayout.Complete}.*")

  def getLabelTypeQpr(segmentName: SegmentName): Int = {
    val label = segmentName.toString
    if (regRPattern.matcher(label).matches())
      1
    else if (regQPattern.matcher(label).matches()) {
      if (segmentName.a.split("\\" + SystemLayout.Sep)(0) == segmentName.b.split("\\" + SystemLayout.Sep)(0))
        1
      else
        -1
    }
    else
      0
  }

  def getLabelColor(segmentName: SegmentName): Color = {
    getLabelTypeQpr(segmentName) match {
      case -1 => SystemLayout.qColor
      case 0 => SystemLayout.pColor
      case 1 => SystemLayout.rColor
    }
  }


}