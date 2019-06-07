package org.processmining.scala.viewers.spectrum.view

import java.awt.datatransfer.StringSelection
import java.awt.{BasicStroke, Color, Font, Graphics2D, Rectangle, Toolkit}

import org.apache.commons.lang3.tuple.Pair
import org.processmining.scala.viewers.spectrum.features.{FeaturesExtractorForOneSegmentPatternsClassifiers, FeaturesPointer, PositionsExtractor, PositionsInCell}
import org.slf4j.LoggerFactory
import org.processmining.scala.viewers.spectrum.model.AbstractDataSource
import org.processmining.scala.viewers.spectrum.patterns.{OneSegmentPatternsDetector, OneSegmentPerformancePattern}
import org.processmining.scala.viewers.spectrum.view.TimeDiffController.palettes


private[viewers] trait Zooming {
  def changeVerticalZoom(delta: Int)

  def changeHorizontalZoom(delta: Int)

}

private[viewers] case class PaintParams(g: Graphics2D,
                                        xx: Array[Pair[Integer, Integer]],
                                        yy: Array[Pair[Integer, Integer]],
                                        names: Array[(String, Int)]
                                       )

private[viewers] class TimeDiffController(val ds: AbstractDataSource, val appSettings: AppSettings) {

  val fontSize = appSettings.fontSize
  val font = new Font(appSettings.fontName, Font.PLAIN, fontSize)

  val view: TimeDiffGraphics = new TimeDiffGraphics(this)
  private val logger = LoggerFactory.getLogger(classOf[TimeDiffController].getName)
  val MinPower = 5
  val percentileForMaxIntervalsBetweenStartsOrEndsPercent = 50
  val PosNumber = 5
  val ClassesNumber = 4

  //val positions = new PositionsExtractor(ds, PosNumber, ClassesNumber).process()


  //val oneSegmentPatternsDetector0 = new OneSegmentPatternsDetector(ds, (0, 1), MinPower, percentileForMaxIntervalsBetweenStartsOrEndsPercent)

  //  val patterns0 = oneSegmentPatternsDetector0.processQueues()
  //  val patterns1 = new OneSegmentPatternsDetector(ds, (1, 2), MinPower, percentileForMaxIntervalsBetweenStartsOrEndsPercent).processQueues()
  //  val patterns2 = new OneSegmentPatternsDetector(ds, (2, 3), MinPower, percentileForMaxIntervalsBetweenStartsOrEndsPercent).processQueues()
  //  val oneSegmentPatternsDetector3 = new OneSegmentPatternsDetector(ds, (3, 4), MinPower, percentileForMaxIntervalsBetweenStartsOrEndsPercent)
  //  val patterns3 = oneSegmentPatternsDetector3.processQueues()
  //  val patternsIntersections = new OneSegmentPatternsDetector(ds, (0, 0), MinPower, percentileForMaxIntervalsBetweenStartsOrEndsPercent).processIntersections2()
  //
  //
  //  def exportFeatures(): Unit = {
  //    val segmentForLabeling = "TransferIn_1:Link3B"
  //    val labels3 = oneSegmentPatternsDetector3.convert(patterns3(segmentForLabeling))
  //    val labels0 = oneSegmentPatternsDetector0.convert(patterns0(segmentForLabeling))
  //    val segmentsForFeatures = Set("Link1S_1:F11.TO_MC_1_0", "Link1S_1:E11.TO_SCAN_1_0")
  //    val segmentsForIncomingFlow = Set("A4_1:Link1_0")
  //    val fPtr1 = FeaturesPointer(segmentsForIncomingFlow, -5, 5)
  //    val fPtr2 = FeaturesPointer(segmentsForFeatures, -5, 2)
  //    val tmp3 = new FeaturesExtractorForOneSegmentPatternsClassifiers(positions,
  //      labels3,
  //      fPtr1,
  //      fPtr2
  //    )
  //
  //    tmp3.export("features3.txt", tmp3.processLabels(), _ => 1)
  //
  //    val tmp0 = new FeaturesExtractorForOneSegmentPatternsClassifiers(positions,
  //      labels0,
  //      fPtr1,
  //      fPtr2
  //    )
  //    tmp0.export("features0.txt", tmp0.processLabels(), _ => 0)
  //
  //  }


  //private var selectedIds: Set[String] = intersections.flatMap(_._2).map(_._1).toSet
  private var selectedIds: Set[String] = Set()

  def setZooming(zooming: Zooming): Unit = {
    view.setZooming(zooming)
  }


  def paint(g: Graphics2D): Unit = {

    val xx = (view.paintInputParameters.startTwIndex until view.paintInputParameters.lastTwIndexExclusive)
      .map(view.getXIntervalInclusiveByTwIndex(_))
      .toArray
    val names = view.paintInputParameters.names.zipWithIndex

    val yy = names.map(x => view.getYIntervalInclusiveByIndex(x._2))
    val pp = PaintParams(g, xx, yy, names)
    if (view.viewerState.showTraces) {
      if (isSelectedTracesDefined) drawTraces(pp, 1)
      drawTraces(pp, 2)
    }
    if (view.viewerState.showBins > 0) {
      if (isSelectedBinsDefined) drawSelectedSegments(pp) else drawNormalSegments(pp)
    }
    //    drawPatternsForClass(pp, patterns0, getDefaultClazzColor(0, 50))
    //    drawPatternsForClass(pp, patterns1, getDefaultClazzColor(1, 50))
    //    drawPatternsForClass(pp, patterns2, getDefaultClazzColor(2, 50))
    //    drawPatternsForClass(pp, patterns3, getDefaultClazzColor(3, 50))
    //    drawPatternsForClass(pp, patternsIntersections, new Color(0, 255, 0, 50))

    //drawPositions(pp, positions, Color.black)

    view.drawHorizontalGrid(g);
    if (view.viewerState.showGrid) {
      view.drawVerticalGrid(g);
    }
    appSettings.customThickVerticalGridDates.foreach(view.drawCustomGrid(g, _, false))
    appSettings.customThinVerticalGridDates.foreach(view.drawCustomGrid(g, _, true))
    drawNames(pp)
  }

  private var selectedBins: Option[Set[(String, Int)]] = None

  private var selectedClazzOfTraces: Set[Int] = Set()

  def addId(ids: String): Unit = {
    selectedIds = selectedIds ++ ids.split("\\n|\\r\\n|;").filter(_.nonEmpty)
    view.forceRepaint()
  }

  def clearSelectionMode(): Unit = {
    selectedBins = None
    selectedIds = Set()
    view.forceRepaint()
    //exportFeatures()
  }

  def showTracesByClazz(clazz: Int): Unit =
    selectedClazzOfTraces =
      if (clazz == TimeDiffGraphics.AllClasses) Set()
      else if (selectedClazzOfTraces.contains(clazz)) selectedClazzOfTraces - clazz
      else selectedClazzOfTraces + clazz


  def isSelectedBinsDefined(): Boolean = selectedBins.isDefined

  def isSelectedTracesDefined(): Boolean = selectedIds.nonEmpty


  private def drawSegmentPart(g: Graphics2D, x: Int, y: Int, w: Int, h: Int, drawRect: Boolean) =
    if (view.viewerState.show3DBars) g.fill3DRect(x, y, w, h, true)
    else {
      g.fillRect(x, y, w, h)
      //      if (drawRect) {
      //        g.setColor(Color.red)
      //        g.drawRect(x, y, w, h)
      //      }
    }


  def drawPattern(pp: PaintParams, name: (String, Int), pattern: OneSegmentPerformancePattern, color: Color): Unit = {
    //    val xx1 = pp.xx(pattern.leftTop.asInstanceOf[Int] - view.paintInputParameters.startTwIndex)
    //    val xx2 = pp.xx(pattern.rightBottom.asInstanceOf[Int] - view.paintInputParameters.startTwIndex)

    val leftTop = view.getXByAbsTime(pattern.leftTop)
    val leftBottom = view.getXByAbsTime(pattern.leftBottom)
    val rightTop = view.getXByAbsTime(pattern.rightTop)
    val rightBottom = view.getXByAbsTime(pattern.rightBottom)


    val yy = pp.yy(name._2)
    pp.g.setColor(color)

    pp.g.fillPolygon(
      Array(leftTop, rightTop, rightBottom, leftBottom, leftTop),
      Array(yy.getLeft, yy.getLeft, yy.getRight, yy.getRight, yy.getLeft), 5)

    //pp.g.setStroke(dashed)
    pp.g.setColor(new Color(10, 128, 10))

    pp.g.drawPolygon(
      Array(leftTop, rightTop, rightBottom, leftBottom, leftTop),
      Array(yy.getLeft, yy.getLeft, yy.getRight, yy.getRight, yy.getLeft), 5)

    //    pp.g.fillRect(xx1.getLeft, yy.getLeft, xx2.getLeft - xx1.getLeft, yy.getRight - yy.getLeft)
    //    pp.g.setColor(new Color(255, 0, 125))
    //    pp.g.drawRect(xx1.getLeft, yy.getLeft, xx2.getLeft - xx1.getLeft, yy.getRight - yy.getLeft)
  }


  def getAggregationValue(e: (Int, Long, Long, Long)): Long =
    view.viewerState.showBins match {
      case 1 => e._2
      case 2 => e._3
      case 3 => e._4
      case 4 => e._2 + e._3 + e._4
      case _ => throw new IllegalArgumentException(s"Wrong showBins ${view.viewerState.showBins}")
    }


  def drawNormalSegment(pp: PaintParams, twIndex: Int, name: (String, Int),
                        segmentsOriginal: List[(Int, Long, Long, Long)]): Unit = {
    val segments = if (view.viewerState.reverseColors) segmentsOriginal.reverse else segmentsOriginal
    val xx = pp.xx(twIndex - view.paintInputParameters.startTwIndex)
    val yy = pp.yy(name._2)
    val max = ds.maxSegmentCountForAggregation(name._1, view.viewerState.showBins)
    val chartXx = (xx.getLeft, Math.max(xx.getRight - 1, xx.getLeft))
    val chartYy = (yy.getLeft, Math.max(yy.getRight - 1, yy.getLeft))
    val height = chartYy._2 - chartYy._1
    val alpha = if (view.viewerState.showTraces) TimeDiffController.Transparant else TimeDiffController.NotTransparent
    segments.foldLeft(chartYy._2)((z, s) => {
      val clazz = s._1
      val count = getAggregationValue(s)
      val top = z - ((count.toDouble / max) * height).toInt
      pp.g.setColor(if (isSelectedBinsDefined) TimeDiffController.getGrayscaleClazzColor(clazz, alpha) else getDefaultClazzColor(clazz, alpha))

      drawSegmentPart(pp.g, chartXx._1, top, chartXx._2 - chartXx._1, z - top, view.viewerState.showTraces)
      if (isSelectedBinsDefined) {
        if (selectedBins.get.contains((name._1, twIndex))) {
          pp.g.setColor(Color.MAGENTA)
          drawSegmentPart(pp.g, chartXx._1, top, (chartXx._2 - chartXx._1) / 4, z - top, false)
        }

        //        val selectedIds = getSelectedIds()
        //        val optionalMap1 = ds.segmentIds(twIndex).get(name._1)
        //        if (optionalMap1.isDefined) {
        //          val id2class = optionalMap1.get
        //          val selectedCount = id2class
        //            .filter(_._2 == clazz)
        //            .filter(x => selectedIds.contains(x._1))
        //            .size
        //          val newTop = z - ((selectedCount.toDouble / max) * height).toInt
        //          pp.g.setColor( TimeDiffController.getDefaultClazzColor(clazz))
        //          drawSegmentPart(pp.g, chartXx._1, newTop, chartXx._2 - chartXx._1, z - newTop)
        //          if(view.selectedSegmentsArea.contains(twIndex, name._2)){
        //            pp.g.setColor(Color.MAGENTA)
        //            drawSegmentPart(pp.g, chartXx._1, newTop, (chartXx._2 - chartXx._1)/4, z - newTop)
        //          }
        //        }
      }
      top
    })
  }

  def drawNames(pp: PaintParams): Unit = {
    if (view.viewerState.showNamesOfSegments) {


      //pp.g.setFont(pp.g.getFont().deriveFont(fontSize).deriveFont(Font.BOLD))
      pp.g.setFont(font)
      pp.names.foreach(name => {
        val yy = pp.yy(name._2)
        val height = fontSize + 2

        //val thString = if (view.viewerState.showBins) s" /${ds.maxSegmentsCount(name._1)}" else ""
        val thString = s" /${ds.maxSegmentsCount(name._1)}"
        val label = s"${name._1}$thString"
        val width = (label.length * 6.1).asInstanceOf[Int] + (if (label.length < 30) 32 else 0)
        val yTop = yy.getLeft + 1
        val yBottom = yTop + height
        //      val yBottom = yy.getRight - Math.max(0, ((yy.getRight-yy.getLeft - height)/2 -1))
        //      val yTop = yBottom - height
        pp.g.setColor(Color.white)
        pp.g.fillRect(1, yTop, width, height)
        pp.g.setColor(getDefaultFontColor())
        pp.g.drawString(label, 2, yBottom - 1)
      }
      )
    }
  }

  def drawNormalSegments(pp: PaintParams): Unit =
    (view.paintInputParameters.startTwIndex until view.paintInputParameters.lastTwIndexExclusive)
      .foreach { twIndex => {
        val twData = ds.segmentsCount(twIndex)
        pp.names.foreach(name => {
          val entry = twData.get(name._1)
          if (entry.isDefined) drawNormalSegment(pp, twIndex, name, entry.get)
        }
        )
      }
      }

  def drawPositionsForSegment(pp: PaintParams, name: (String, Int), twIndex: Int, pic: PositionsInCell, color: Color) = {
    if (twIndex >= view.paintInputParameters.startTwIndex && twIndex < view.paintInputParameters.lastTwIndexExclusive) {
      val xx = pp.xx(twIndex - view.paintInputParameters.startTwIndex)
      val yy = pp.yy(name._2)
      val countByPos = pic.data
        .map(x => (x._1, x._2.map(_._2).sum))
        .toSeq
        .sortBy(_._1)
        .mkString(";")
      pp.g.drawString(countByPos, xx.getLeft, yy.getRight - 1)
    }
  }

  def drawPositions(pp: PaintParams, positions: Map[String, Map[Int, PositionsInCell]], color: Color) = {
    pp.g.setColor(color)
    pp.names
      .filter(x => positions.contains(x._1))
      .foreach(name => {
        positions(name._1)
          //          .filter(x => TODO: Fix
          //            x.leftTop >= view.getXIntervalInclusiveByTwIndex(view.paintInputParameters.startTwIndex).getLeft
          //              && x.rightBottom < view.getXIntervalInclusiveByTwIndex(view.paintInputParameters.lastTwIndexExclusive).getRight)
          .map(x => drawPositionsForSegment(pp, name, x._1, x._2, color))
      }
      )
  }

  def drawPatternsForClass(pp: PaintParams, patterns: Map[String, Seq[OneSegmentPerformancePattern]], color: Color) =
    pp.names
      .filter(x => patterns.contains(x._1))
      .foreach(name => {
        patterns(name._1)
          //          .filter(x => TODO: Fix
          //            x.leftTop >= view.getXIntervalInclusiveByTwIndex(view.paintInputParameters.startTwIndex).getLeft
          //              && x.rightBottom < view.getXIntervalInclusiveByTwIndex(view.paintInputParameters.lastTwIndexExclusive).getRight)
          .map(drawPattern(pp, name, _, color))
      }
      )

  def setSelectedIds(selectedSegmentsArea: Rectangle, clazz: Int): Unit = {

    val names = (selectedSegmentsArea.y until selectedSegmentsArea.y + selectedSegmentsArea.height)
      .map(view.getNameByIndex)
      .filter(_.nonEmpty)

    selectedIds = selectedIds ++ (selectedSegmentsArea.x until selectedSegmentsArea.x + selectedSegmentsArea.width)
      .flatMap(twIndex => {
        val nameIdClazz = ds.segmentIds(twIndex)
        names.map(nameIdClazz.get)
          .filter(_.isDefined)
          .map(_.get)
          .map(_.filter(clazz == TimeDiffGraphics.AllClasses || _._2 == clazz))
          .flatMap(_.keySet)
      }
      ).toSet
    copyToClipboard(selectedIds)

    val selectedBinsMap = (selectedSegmentsArea.x until selectedSegmentsArea.x + selectedSegmentsArea.width)
      .flatMap(twIndex =>
        (selectedSegmentsArea.y until selectedSegmentsArea.y + selectedSegmentsArea.height)
          .map(y => (view.getNameByIndex(y), twIndex))
          .filter(_._1.nonEmpty)
      ).toSet
    selectedBins = if (!selectedBins.isDefined) Some(selectedBinsMap) else Some(selectedBins.get ++ selectedBinsMap)
  }

  private def copyToClipboard(selectedIds: Set[String]): Unit = {
    val idsAsString = s"""n=${selectedIds.size}|${selectedIds.mkString("|")}"""
    val selection = new StringSelection(idsAsString)
    Toolkit.getDefaultToolkit.getSystemClipboard.setContents(selection, selection)
    logger.info(s"Selected traces: '$idsAsString'")
  }

  def drawSelectedSegments(pp: PaintParams): Unit = {
    (view.paintInputParameters.startTwIndex until view.paintInputParameters.lastTwIndexExclusive)
      .foreach { twIndex => {
        val twData = ds.segmentsCount(twIndex)
        pp.names.foreach(name => {
          val entry = twData.get(name._1)
          if (entry.isDefined) {
            drawNormalSegment(pp, twIndex, name, entry.get)
          }

        })
      }
      }
  }

  private val dash1 = Array(4.0f, 4.0f)
  private val dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f)

  def drawTrace(pp: PaintParams, twIndex: Int, name: (String, Int), clazz: Int, segment: (String, Long, Long), color: Color): Unit = {
    if (selectedClazzOfTraces.isEmpty || selectedClazzOfTraces.contains(clazz)) {
      val x1 = view.getXByAbsTime(segment._2)
      val x2 = view.getXByAbsTime(segment._2 + segment._3)
      val yInterval = view.getYIntervalInclusiveByIndex(name._2)
      pp.g.setColor(color)
      pp.g.drawLine(x1, yInterval.getLeft, x2, yInterval.getRight)

    }
  }


  def drawTraces(pp: PaintParams, twIndex: Int, name: (String, Int), segments: Map[Int, List[(String, Long, Long)]], step: Int): Unit = {

    val initialStroke = pp.g.getStroke
    var prevStroke = initialStroke

    segments.foreach(entry => {
      entry._2.foreach(segment => {
        if (isSelectedTracesDefined() && !selectedIds.contains(segment._1)) {
          if (step == 1) {
            if (dashed != prevStroke) {
              prevStroke = dashed
              pp.g.setStroke(dashed)
            }

            drawTrace(pp, twIndex, name, entry._1, segment, getBackgroundTracesColor())
          }
        }
        else {
          if (step == 2 && !view.viewerState.hideSelected) {
            if (initialStroke != prevStroke) {
              prevStroke = initialStroke
              pp.g.setStroke(initialStroke)
            }
            drawTrace(pp, twIndex, name, entry._1, segment, getDefaultClazzColor(entry._1, TimeDiffController.NotTransparent))
          }
        }
      }
      )
    })
    pp.g.setStroke(initialStroke)
  }

  def drawTraces(pp: PaintParams, step: Int): Unit = {
    (view.paintInputParameters.startTwIndex until view.paintInputParameters.lastTwIndexExclusive)
      .foreach { twIndex => {
        val segments = ds.segments(twIndex)
        pp.names.foreach(name => {
          val optionalEntry = segments.get(name._1)
          if (optionalEntry.isDefined) {
            val entry = optionalEntry.get
            drawTraces(pp, twIndex, name, entry, step)
          }
        })
      }
      }

  }

  def getDefaultClazzColor(clazz: Int, a: Int): Color =
    palettes(appSettings.paletteId).getClazzColor(clazz, a)


  def getDefaultFontColor(): Color =
    palettes(appSettings.paletteId).getDefaultFontColor()

  def getDefaultGridColor(): Color =
    palettes(appSettings.paletteId).getDefaultGridColor()

  def getBackgroundTracesColor(): Color = palettes(appSettings.paletteId).getBackgroundTracesColor()

}

object TimeDiffController {
  val Transparant = 150
  val NotTransparent = 255

  val palettes: Map[Int, Palette] =
    (new DefaultPalette() :: new OriginalPalette() :: new Bw5Palette() :: new BwQ4Palette() :: Nil)
      .map(x => (x.getId -> x)).toMap

  private val Zebra = Array(10, 210, 20, 200, 30, 190, 40, 180, 50, 170, 60, 160, 70, 150, 80, 140, 90, 130, 100, 120, 110)

  private def colorPart(clazz: Int): Int =
    if (clazz < Zebra.length) Zebra(clazz) else 0

  def segmentNameLt(order: Array[String]): (String, String) => Boolean =
    segmentNameLt(order.zipWithIndex.toMap)

  def dummy(): String=> Boolean = _ => true

  //TODO: make smarter implementation
  private def segmentNameLt(order: Map[String, Int])(x: String, y: String): Boolean = {
    val optIndexX = order.get(x)
    val optIndexY = order.get(y)
    val newX = if (optIndexX.isDefined) "%05d".format(optIndexX.get) + x else x
    val newY = if (optIndexY.isDefined) "%05d".format(optIndexY.get) + y else y
    newX < newY
  }

  private def getClazzColorMonochrome(rgbPos: Int)(clazz: Int, a: Int): Color =
    rgbPos match {
      case 0 => new Color(colorPart(clazz), colorPart(clazz), colorPart(clazz), a)
      case 1 => new Color(colorPart(clazz), 0, 0, a)
      case 2 => new Color(0, colorPart(clazz), 0, a)
      case 3 => new Color(0, 0, colorPart(clazz), a)
      case _ => new Color(colorPart(clazz), colorPart(clazz), 0, a)
    }


  val getGrayscaleClazzColor: (Int, Int) => Color = getClazzColorMonochrome(0)

  val getGreenishClazzColor: (Int, Int) => Color = getClazzColorMonochrome(2)

  val getRedClazzColor: (Int, Int) => Color = getClazzColorMonochrome(1)

}
