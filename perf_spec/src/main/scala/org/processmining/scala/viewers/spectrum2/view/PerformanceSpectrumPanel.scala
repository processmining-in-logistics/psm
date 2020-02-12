package org.processmining.scala.viewers.spectrum2.view

import java.awt.geom.AffineTransform
import java.awt.{BasicStroke, Color, Font, Rectangle, Toolkit}
import java.io.File

import javax.imageio.ImageIO
import org.processmining.scala.viewers.spectrum.view.AppSettings
import org.processmining.scala.viewers.spectrum2.model._
import org.slf4j.LoggerFactory

import scala.collection.mutable.{HashMap, MultiMap, Set}
import scala.swing.event.MouseMoved
import scala.swing.{BorderPanel, Dimension, Graphics2D, Point}

case class SortingOrderEntry(variantName: String, segmentNames: Vector[SegmentName], slaveSegments: Map[SegmentName, SegmentName])

case class Point2(x: Int, y: Int)

class PerformanceSpectrumPanel(private var builder: CallableWithLevel[AbstractDataSource],
                               mouseMoveListener: Long => Unit,
                               updateScrollingPanel: Unit => Unit) extends BorderPanel {


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

  private val dashedOverlaidLoad = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashOverlaidLoad, 0.0f)
  private val dashedCertainLoad = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashCertainLoad, 0.0f)
  private val dashedUncertainLoad = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashUncertainLoad, 0.0f)


  def setNewBuilder(newBuilder: CallableWithLevel[AbstractDataSource]) = {
    builder = newBuilder
    ds = builder.call()
    ls = new LoadSource(ds)
    currentLevel = builder.levelCount
    isSegmentRmsePrinted = false
    forceRepaint()
  }

  def onLevelChanged(level: Int): Unit = {
    if (currentLevel != level) {
      ds = builder.call(level)
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


  private val logger = LoggerFactory.getLogger(classOf[PerformanceSpectrumPanel])
  private var horizontalZoomPercent = PerformanceSpectrumPanel.HorizontalZoomDefaultValue
  private var verticalZoom = PerformanceSpectrumPanel.VerticalZoomDefaultValue
  private var horizontalScroll = PerformanceSpectrumPanel.HorizontalScrollDefaultValue
  private var scrollPanelSize = new Dimension(100, 100)
  private val OneHundredPercent = 100

  private var showRegions = false
  private var showRightBorders = false
  private var showMerges = false
  private var useOrigins = false
  private var showProtectedSpaceBefore = false
  private var showProtectedSpaceAfter = false
  private var showCertainLoad = false
  private var showLoad = false
  private var showUncertainLoad = false
  private var showOverlaidLoad = false
  private var showErrorLoad = false

  private var showGrid = false

  val segmentFont = new Font(AppSettings.DefaultFontName, Font.PLAIN, AppSettings.DefaultFontSize)
  val variantFont = new Font(AppSettings.DefaultFontName, Font.PLAIN, AppSettings.DefaultFontSize - 4)

  import java.awt.TexturePaint

  var img1 = ImageIO.read(new File("./img/pattern2.jpg"));
  val tp1 = new TexturePaint(img1, new Rectangle(0, 0, 100, 100))
  var img2 = ImageIO.read(new File("./img/pattern3.jpg"));
  val tp2 = new TexturePaint(img2, new Rectangle(0, 0, 100, 100))

  listenTo(mouse.clicks, mouse.moves)
  background = Color.white

  reactions += {
    case MouseMoved(_, point, _) => {
      val utcUnixTimeMs = pixelsToMs(point.x)
      mouseMoveListener(utcUnixTimeMs)
      highlightRegion(point)
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

      g.setColor(Color.black)
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



  def drawSegment(g: Graphics2D, soe: SortingOrderEntry, segments: Vector[Segment], y1: Int, h: Int, segmentName: SegmentName, isMaster: Boolean, doDrawing: Boolean) = {

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

        val origin = ds.caseIdToOriginActivityAndIndex(s.caseId)

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
            g.setColor(if (useOrigins && !showRegions) PerformanceSpectrumPanel.getColorByIndex(origin._2, 100) else if (isMaster) Color.red else new Color(0, 128, 0))
            g.setStroke(thinStroke10)
            g.drawLine(x1, y1, x2, y2)
            if (showRightBorders) {
              g.setColor(Color.blue)
              g.drawLine(xx(1), y1, xx(2), y2)
            }

            if (s.start.isObserved) g.fillOval(x1, y1, 4, 5)
            if (s.end.isObserved) g.fillOval(x2, y2 - 5, 4, 5)
          }

                  if (s.start.isObserved) g.drawLine(x1, y1 + 1, x1 + (if (isMaster) dx1 else (dx1 / 2)), y1 + 1)
                  if (s.end.isObserved) g.drawLine(x2, y2 - 1, x2 + dx2, y2 - 1)
        }

        if (isMaster) g.setColor(Color.lightGray) else g.setColor(Color.lightGray)
        g.drawLine(0, y1, 2000, y1)
        g.drawLine(0, y2, 2000, y2)
                if (isMaster){
                  g.setColor(Color.lightGray)
                  (ds.startTimeMs/1000*1000 until ds.endTimeMs by 2000).foreach(t =>
                    {
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
              if(!isSegmentRmsePrinted) {
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

                if(!isMaeRmsePrinted) {
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

  def paintSegments(g: Graphics2D, flatSortingOrder: Seq[((SegmentName, Int, SortingOrderEntry), Int)], doDrawing: Boolean) = {
    val h = getSegmentHeight
    var yS: List[(SegmentName, Int)] = List()
    flatSortingOrder.foreach(x => {
      val segmentsOpt = ds.segments.get(x._1._1)
      if (segmentsOpt.isDefined) {

        val segments = segmentsOpt.get
        val variantIndex = x._1._2
        val segmentIndex = x._2
        val y = segmentIndex * h + variantIndex * getSegmentGap

        val slave = x._1._3.slaveSegments.get(x._1._1)
        if (showMerges) {
          if (slave.isDefined) {
            val slaveSegments = ds.segments(slave.get)
            val shift = (getSegmentHeight() * 0.2).toInt
            drawSegment(g, x._1._3, slaveSegments, y + shift, h - shift, slave.get, false, doDrawing)

          }
        }
        drawSegment(g, x._1._3, segments, y, h, x._1._1, true, doDrawing)
        yS = (x._1._1, y) :: yS
        drawSegmentText(g, x._1._3, segments, y, h, x._1._1, true)
        if (showMerges) {
          if (slave.isDefined) {
            //TODO: remove dublicates
            val slaveSegments = ds.segments(slave.get)
            val shift = (getSegmentHeight() * 0.2).toInt
            drawSegmentText(g, x._1._3, slaveSegments, y + shift, h - shift, slave.get, false)
          }
        }
      } else logger.warn(s"Segment '${x._1._1}' not found")
    })
    yS
  }

  override def paint(g: Graphics2D): Unit = {
    super.paint(g)
    ls = new LoadSource(ds)
    foregroundOverliadSegments = List()
    foregroundRegions = List()
    p2id.clear()
    if (showGrid)
      drawGrid(g)


    val flatSortingOrder: Seq[((SegmentName, Int, SortingOrderEntry), Int)] = ds.sortingOrder
      .zipWithIndex
      .flatMap(x => x._1.segmentNames.map(s => (s, x._2, x._1))).zipWithIndex
    val doDrawing = currentOverlaidMode == PerformanceSpectrumPanel.OverlaidOnTop || currentOverlaidMode == PerformanceSpectrumPanel.OverlaidNone || currentOverlaidMode == PerformanceSpectrumPanel.OverlaidNothing
    val yS = paintSegments(g, flatSortingOrder, doDrawing)
    if (currentOverlaidMode != PerformanceSpectrumPanel.OverlaidNone && currentOverlaidMode != PerformanceSpectrumPanel.OverlaidNothing) {
      val ySMap = yS
        .groupBy(_._1)
        .map(x => (x._1, x._2.map(_._2)))
      ySMap.foreach(s => {
        drawOverlaidSegments(g, getSegmentHeight, s, ySMap)
      })
    }

    if (currentOverlaidMode == PerformanceSpectrumPanel.OverlaidBeneath)
      paintSegments(g, flatSortingOrder, true)


    drawForeground(g)
    logger.debug(selectedId.mkString("Selected ID", "; ", ""))
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

  def onShowMerge(selected: Boolean): Unit = {
    showMerges = selected
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

}