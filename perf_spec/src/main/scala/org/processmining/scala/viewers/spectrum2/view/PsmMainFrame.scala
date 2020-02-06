package org.processmining.scala.viewers.spectrum2.view

import java.awt.Cursor
import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.Locale
import java.util.concurrent.Callable

import javax.swing.{DefaultComboBoxModel, JFileChooser, SwingUtilities}
import javax.swing.filechooser.FileNameExtensionFilter
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.processmining.scala.log.utils.common.errorhandling.EH
import org.processmining.scala.viewers.spectrum2.model.{AbstractDataSource, CallableWithLevel}
import org.slf4j.LoggerFactory

import scala.swing.BorderPanel.Position
import scala.swing._
import scala.swing.event.{ButtonClicked, MouseClicked, SelectionChanged, ValueChanged}

case class EventLogProperties(dir: String, filenames: Vector[String])

object EventLogProperties {
  val Empty = new EventLogProperties("", Vector(""))
}

abstract class EventLogPropertiesFactory {
  def getEventLog(eventLogFileName: String): EventLogProperties
}

abstract class BuilderFactory {
  def getBuilder(eventLogProperties: EventLogProperties, onlyObservedPs: Boolean): CallableWithLevel[AbstractDataSource]
}


class PsmMainFrame(eventLogPropertiesFactory: EventLogPropertiesFactory, builderFactory: BuilderFactory, onlyObservedPs: Boolean) extends BorderPanel {
  val logger = LoggerFactory.getLogger(classOf[PsmMainFrame])
  var builder = new CallableWithLevel[AbstractDataSource] {

    override def call(level: Int): AbstractDataSource = AbstractDataSource.Empty

    override def levels: Vector[String] = Vector(PsmMainFrame.ObservedPsText)
  }


  val psm = new PerformanceSpectrumPanel(builder,
    psmMouseMoveListener,
    _ => performanceSpectrumScrollingPanel.peer.revalidate())
  private val zoneId = ZoneId.of("Europe/London")
  val dateLabel = new Label("01.01.1970 00:00:00.0") {
    horizontalAlignment = swing.Alignment.Left
    font = new Font("Tahoma", 0, 18)
  }
  val dayOfWeekLabel = new Label("Thu") {
    horizontalAlignment = swing.Alignment.Center
    font = new Font("Tahoma", 0, 16)
  }

  private def psmMouseMoveListener(utcUnixTimeMs: Long) = {
    dateLabel.text =
      LocalDateTime.ofInstant(Instant.ofEpochMilli(utcUnixTimeMs), zoneId).format(PsmMainFrame.timestampFormatter)
    val zoneIdText = zoneId.getDisplayName(TextStyle.FULL, Locale.US)
    dayOfWeekLabel.text =
      s"${LocalDateTime.ofInstant(Instant.ofEpochMilli(utcUnixTimeMs), zoneId).format(PsmMainFrame.weekOfDayFormatter)} ($zoneIdText)"

  }

  val performanceSpectrumScrollingPanel: ScrollPane = new ScrollPane(psm) {
    horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
    listenTo(this)
    reactions += {
      case event.UIElementResized(_) =>
        psm.onComponentResize(size)
    }
  }

  val scrollAndVerticalZoomPanel = new BorderPanel {

    layout(performanceSpectrumScrollingPanel) = Position.Center

    val verticalZoomSlider: Slider = new Slider {
      min = PerformanceSpectrumPanel.VerticalZoomMin
      max = PerformanceSpectrumPanel.VerticalZoomMax
      value = PerformanceSpectrumPanel.VerticalZoomDefaultValue
      tooltip = "Vertical zooming"
      orientation = Orientation.Vertical
      majorTickSpacing = 50
      minorTickSpacing = 10
      paintTicks = true
      paintLabels = true
      reactions += {
        case ValueChanged(`verticalZoomSlider`) => psm.onVerticalZoomChanged(value)
      }
    }
    layout(verticalZoomSlider) = Position.East

  }

  layout(scrollAndVerticalZoomPanel) = Position.Center


  val controlPanel = new BorderPanel {
    val horizontalScrollPanel = new BorderPanel {
      val timeLine = new TimeLine
      layout(timeLine) = Position.Center
      val horizontalScrollSlider = new Slider {
        min = PerformanceSpectrumPanel.HorizontalScrollMin
        max = PerformanceSpectrumPanel.HorizontalScrollMax
        value = PerformanceSpectrumPanel.HorizontalScrollDefaultValue
        tooltip = "Horizontal position"
        majorTickSpacing = 2000
        minorTickSpacing = 200
        paintLabels = true
        paintTicks = true
        reactions += {
          case ValueChanged(_) => psm.onHorizontalPositionChanged(value)
        }
      }
      layout(horizontalScrollSlider) = Position.South
    }
    layout(horizontalScrollPanel) = Position.North
    val middleAndBottomOfControlPanel = new BorderPanel {
      val middlePanel = new BorderPanel {
        val dateAndDayOfWeekPanel = new BorderPanel {

          layout(dateLabel) = Position.North

          layout(dayOfWeekLabel) = Position.South

        }
        layout(dateAndDayOfWeekPanel) = Position.West
        val zoomPanel = new BorderPanel {

          val horizontalZoomSlider: Slider = new Slider {
            min = PerformanceSpectrumPanel.HorizontalZoomMin
            max = PerformanceSpectrumPanel.HorizontalZoomMax
            value = PerformanceSpectrumPanel.HorizontalZoomDefaultValue
            tooltip = "Horizontal zooming"
            majorTickSpacing = 20000
            minorTickSpacing = 2000
            paintTicks = true
            paintLabels = true
            reactions += {
              case ValueChanged(`horizontalZoomSlider`) => psm.onHorizontalZoomChanged(value)
            }
          }
          layout(horizontalZoomSlider) = Position.South
        }
        layout(zoomPanel) = Position.Center
      }
      layout(middlePanel) = Position.North
      val checkBoxShowReources = new CheckBox("Regions") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onShowReources(selected)
        }
      }

      val checkBoxShowRightBorders = new CheckBox("R.borders") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onShowRightBorders(selected)
        }
      }

      val checkBoxShowMergingSegments = new CheckBox("Inc.feeders") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onShowMerge(selected)
        }
      }

      val checkBoxColorByOrigin = new CheckBox("Color by src") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onColorByOrigin(selected)
        }
      }


      val checkBoxShowCertainLoad = new CheckBox("Certain load") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onCertainLoad(selected)
        }
      }



      val checkBoxShowUncertainLoad = new CheckBox("Uncertain load") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onUncertainLoad(selected)
        }
      }

      val checkBoxShowLoad = new CheckBox("Load") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onLoad(selected)
        }
      }

      val checkBoxShowOverlaidLoad = new CheckBox("Overlaid load") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onOverlaidLoad(selected)
        }
      }

      val checkBoxShowErrorLoad = new CheckBox("Err.load") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onErrorLoad(selected)
        }
      }

      val checkBoxUseStroke = new CheckBox("Dashed") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onUseStroke(selected)
        }
      }

      val checkBoxShowGrid = new CheckBox("Grid") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onShowGrid(selected)
        }
      }

      val textAreaBinSize = new TextArea("15000")
      val buttonApplyBinSize = new Button("Apply") {
        listenTo(mouse.clicks)
        reactions += {
          case evt@MouseClicked(_, _, _, _, _) => {
            if (evt.peer.getButton == 1) {
              psm.onBinSize(textAreaBinSize.text.toLong)
            }
          }
        }
      }


      val checkBoxProtectedSpaceBefore = new CheckBox("Protected space before") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onProtectedSpaceBefore(selected)
        }
      }

      val checkBoxProtectedSpaceAfter = new CheckBox("Protected space after") {
        selected = false
        listenTo(this)
        reactions += {
          case ButtonClicked(_) => psm.onProtectedSpaceAfter(selected)
        }
      }


      val levelBox = new ComboBox(builder.levels) {
        selection.index = builder.levelCount - 1
      }
      val openButton = new Button("Open...")
      val overlaidSegmentsBox = new ComboBox(Array("Overlaid on top", "Overlaid beneath", "Only overlaid", "No overlaid", "None"))

      reactions += {
        case SelectionChanged(`levelBox`) => {
          peer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))
          psm.onLevelChanged(levelBox.selection.index + 1)
          peer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
        }

        case SelectionChanged(`overlaidSegmentsBox`) => {
          psm.onOverlaidModeChanged(overlaidSegmentsBox.selection.index)
        }

        case evt@MouseClicked(`openButton`, pt, _, _, _) => {
          if (evt.peer.getButton == 1) {
            val logFilename = getLogFileNameFromDialog

            peer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))
            builder = if (logFilename.nonEmpty) {
              builderFactory.getBuilder(eventLogPropertiesFactory.getEventLog(logFilename), onlyObservedPs)
            } else builder
            psm.setNewBuilder(builder)
            levelBox.peer.setModel(new DefaultComboBoxModel)
            levelBox.peer.removeAllItems()
            builder.levels.foreach(x => levelBox.peer.addItem(x))
            levelBox.selection.index = builder.levelCount - 1
            levelBox.repaint()
            peer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))


            //            val dirDlg = new JFileChooser()
            //            dirDlg.setFileSelectionMode(JFileChooser.FILES_ONLY)
            //            val filterXes = new FileNameExtensionFilter("XES Event Log Files", "xes", "gz", "zip", "xml")
            //            //          val filterCsv = new FileNameExtensionFilter("Folder with CSV Event Log Files", CSV_DIR)
            //            //          val filterSeg = new FileNameExtensionFilter("Folder with Segment Files", SEG_DIR)
            //            //          val filterPsm = new FileNameExtensionFilter("PSM Session Files", "psm")
            //            dirDlg.setFileFilter(filterXes)
            //            //          dirDlg.setFileFilter(filterCsv)
            //            //          dirDlg.setFileFilter(filterSeg)
            //            //          dirDlg.setFileFilter(filterPsm)
            //
            //            if (dirDlg.showOpenDialog(peer) == JFileChooser.APPROVE_OPTION) {
            //              val path = dirDlg.getSelectedFile.getPath
            //              val filename = dirDlg.getSelectedFile.getPath
            //              //val proc = new T3LogsToSegments2(LogFilename, LogPath /*, ".\\sim_ein\\aggregation\\scanner_rename.ini"*/, true)
            //            }
          }
        }
      }
      listenTo(levelBox.selection, openButton.mouse.clicks, overlaidSegmentsBox.selection)

      val bottomPanel = new FlowPanel(FlowPanel.Alignment.Trailing)(
        levelBox, checkBoxShowReources, checkBoxShowRightBorders, checkBoxColorByOrigin, checkBoxShowMergingSegments, overlaidSegmentsBox,
        checkBoxShowLoad,checkBoxShowCertainLoad, checkBoxShowUncertainLoad, checkBoxShowOverlaidLoad, checkBoxShowErrorLoad, checkBoxShowGrid, checkBoxUseStroke,
        textAreaBinSize, buttonApplyBinSize,
        checkBoxProtectedSpaceBefore, checkBoxProtectedSpaceAfter, openButton)
      layout(bottomPanel) = Position.Center
    }
    layout(middleAndBottomOfControlPanel) = Position.Center
  }
  layout(controlPanel) = Position.South


  def getLogFileNameFromDialog() = {
    val dirDlg = new JFileChooser()
    dirDlg.setFileSelectionMode(JFileChooser.FILES_ONLY)
    val filterXes = new FileNameExtensionFilter("XES Event Log Files", "xes", "gz", "zip", "xml")
    //          val filterCsv = new FileNameExtensionFilter("Folder with CSV Event Log Files", CSV_DIR)
    //          val filterSeg = new FileNameExtensionFilter("Folder with Segment Files", SEG_DIR)
    //          val filterPsm = new FileNameExtensionFilter("PSM Session Files", "psm")
    dirDlg.setFileFilter(filterXes)
    //          dirDlg.setFileFilter(filterCsv)
    //          dirDlg.setFileFilter(filterSeg)
    //          dirDlg.setFileFilter(filterPsm)

    if (dirDlg.showOpenDialog(peer) == JFileChooser.APPROVE_OPTION) {
      dirDlg.getSelectedFile.getPath
      //val proc = new T3LogsToSegments2(LogFilename, LogPath /*, ".\\sim_ein\\aggregation\\scanner_rename.ini"*/, true)
    }
    else ""

  }

}

object PsmMainFrame {
  val timestampFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.S", Locale.US)
  val weekOfDayFormatter = DateTimeFormatter.ofPattern("E", Locale.forLanguageTag("en"))
  val ObservedPsText = "Observed PS"
  val AppTitle = "Performance Spectrum Miner-R"
}

//object PSM2 extends SimpleSwingApplication {
//  private val logger = LoggerFactory.getLogger(PSM2.getClass)
//  lazy val ui = new PsmMainFrame
//
//  override def top: Frame = new MainFrame {
//    PropertyConfigurator.configure("./log4j.properties")
//    JvmParams.reportToLog(logger, s"${getClass} started")
//    title = "Performance Spectrum Miner 2"
//    contents = ui
//  }
//}