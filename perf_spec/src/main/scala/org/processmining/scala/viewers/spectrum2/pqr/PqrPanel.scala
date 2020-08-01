package org.processmining.scala.viewers.spectrum2.pqr

import java.time.format.DateTimeFormatter
import java.util.Locale

import javax.swing.SwingUtilities
import org.processmining.scala.log.utils.common.errorhandling.EH
import org.processmining.scala.viewers.spectrum.view.AppSettings
import org.processmining.scala.viewers.spectrum2.view.PqrPacketsSinkServer
import org.slf4j.LoggerFactory

import scala.swing.BorderPanel.Position
import scala.swing.event.{ButtonClicked, Key, MouseClicked, ValueChanged}
import scala.swing.{BorderPanel, Button, CheckBox, Dimension, FlowPanel, Font, Label, Orientation, ScrollPane, Slider, TextArea, event}

class PqrPanel() extends BorderPanel {
  val logger = LoggerFactory.getLogger(classOf[PqrPanel])
  val interactiveModelPanel = new InteractiveModel(scrollingPanel.peer.revalidate, PqrVisualizer(), psmMouseMoveListener)
  var isSimStarted = false
  var simTimer = ScalaTimer(0, false)(() => {})
  val timerIntervalMs = 0
  val pqrServer = new PqrPacketsSinkServer(AppSettings(AppSettings.DefaultFileName).simServerPort, x => SwingUtilities.invokeLater(() => onPTFind(x.name)))
  val threadPsmListener = new Thread(pqrServer) {
    start()
  }

  def close() = {}

  val labelMaxSize = 50
  val dateLabel = new Label(" " * labelMaxSize) {
    horizontalAlignment = swing.Alignment.Left
    font = new Font("Consolas", 0, 15)
  }
  val dayOfWeekLabel = new Label("") {
    horizontalAlignment = swing.Alignment.Center
    font = new Font("Consolas", 0, 16)
  }


  val scrollingPanel: ScrollPane = new ScrollPane(interactiveModelPanel) {
    horizontalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
    listenTo(this)
    reactions += {
      case event.UIElementResized(_) =>
        interactiveModelPanel.onComponentResize(size)
    }
  }

  val scrollAndZoomPanel = new BorderPanel {
    layout(scrollingPanel) = Position.Center
    val zoomSlider: Slider = new Slider {
      min = InteractiveModel.ZoomMinPercent
      max = InteractiveModel.ZoomMaxPercent
      value = InteractiveModel.ZoomDefaultValuePercent
      tooltip = "Zooming"
      orientation = Orientation.Vertical
      majorTickSpacing = 50
      minorTickSpacing = 10
      paintTicks = true
      paintLabels = true
      reactions += {
        case ValueChanged(`zoomSlider`) => interactiveModelPanel.onZoomChanged(value)
      }
    }
    layout(zoomSlider) = Position.East

  }

  layout(scrollAndZoomPanel) = Position.Center

  def onPTFind(label: String) = {
    val (list, pt, shift) = interactiveModelPanel.showPlaceTransition(label)
    val x = list(0)
    val y = list(1)
    //            logger.info(scrollingPanel.verticalScrollBar.value.toString)
    //            logger.info(scrollingPanel.horizontalScrollBar.value.toString)
    logger.info(pt.toStringLogger)
    scrollingPanel.horizontalScrollBar.value = Math.max(0, x - shift)
    scrollingPanel.verticalScrollBar.value = Math.max(0, y - shift)
  }


  val controlPanel = new BorderPanel {
    val positionPanel = new BorderPanel {

      val angleSlider = new Slider {
        min = 0
        max = 359
        value = 180
        tooltip = "Angle"
        majorTickSpacing = 60
        minorTickSpacing = 15
        paintLabels = true
        paintTicks = true
        enabled = true
        reactions += {
          case ValueChanged(_) => interactiveModelPanel.onAngleChanged(value)
        }
      }
      layout(angleSlider) = Position.South
    }
    layout(positionPanel) = Position.North


    val middleAndBottomOfControlPanel = new BorderPanel {
      val middlePanel = new BorderPanel {
        val dateAndDayOfWeekPanel = new BorderPanel {
          layout(dateLabel) = Position.North
          layout(dayOfWeekLabel) = Position.South
        }
        layout(dateAndDayOfWeekPanel) = Position.West

        val velocityAndPlayerPanel = new BorderPanel {

//          val playButton = new Button(">>"){
//            enabled = false
//          }
          //val rewindToStartButton = new Button("|<")


//          reactions += {
//
//            //case KeyPressed(`playButton`, key, mod, loc)=>{
//
//            case evt@MouseClicked(`playButton`, pt, _, _, _) => {
//              if (evt.peer.getButton == 1)
//                onPTFind("")
//            }
          //}

          //listenTo(playButton.mouse.clicks)

          val playerPanel = new FlowPanel()
          layout(playerPanel) = Position.West

//          val velocityPanel = new BorderPanel {
//
//            val velocitySlider: Slider = new Slider {
//              min = InteractiveModel.VelocityMin
//              max = InteractiveModel.VelocityMax
//              value = InteractiveModel.VelocityDefaultValue
//              tooltip = "Velocity"
//              majorTickSpacing = 20000
//              minorTickSpacing = 2000
//              paintTicks = true
//              paintLabels = true
//              enabled = false
//              reactions += {
//                case ValueChanged(`velocitySlider`) => interactiveModelPanel.onVelocityChanged(value)
//              }
//            }
//            layout(velocitySlider) = Position.South
//          }
//          layout(velocityPanel) = Position.Center
        }
        layout(velocityAndPlayerPanel) = Position.Center


      }
      layout(middlePanel) = Position.North

      val cmdPanel = new BorderPanel {
        preferredSize = new Dimension(320, 25)
        val textAreaPTLabel = new TextArea("")
        val buttonFindPT = new Button("Find transition")
        layout(textAreaPTLabel) = Position.Center
        layout(buttonFindPT) = Position.East

        reactions += {
          case evt@MouseClicked(`buttonFindPT`, pt, _, _, _) => {
            if (evt.peer.getButton == 1) {
              try {
                onPTFind(textAreaPTLabel.text)

              }
              catch {
                case e: Exception => EH.apply().warnAndMessageBox(s"Wrong place/transition name '${textAreaPTLabel.text}'", e)
              }
            }
          }
        }
        listenTo(buttonFindPT.mouse.clicks)

      }
      //val buttonEventLogSnapshot = new Button("Snapshot")
      val checkBoxShowQRLabels = new CheckBox("Show Q-,R-labels") {
        mnemonic = Key.Q
        selected = true
      }
      val checkBoxOnlyP = new CheckBox("Only P-proclet *.start") {
        mnemonic = Key.P
      }

      val bottomPanel = new FlowPanel(cmdPanel, checkBoxShowQRLabels, checkBoxOnlyP) {
        listenTo(checkBoxShowQRLabels, checkBoxOnlyP)
        reactions += {
          case evt@ButtonClicked(`checkBoxShowQRLabels`) => interactiveModelPanel.onShowQRLabels(checkBoxShowQRLabels.selected)
          case evt@ButtonClicked(`checkBoxOnlyP`) => interactiveModelPanel.onShowOnlyP(checkBoxOnlyP.selected)


        }
      }


      layout(bottomPanel) = Position.Center
    }
    layout(middleAndBottomOfControlPanel) = Position.Center
  }
  layout(controlPanel) = Position.South

  def executeStep() = {
    //engine.executeStep()
    interactiveModelPanel.forceRepaint()
    //psmMouseMoveListener(engine.instance.state.timeMs)
  }


  private def psmMouseMoveListener(pt: PlaceTransition) = {
    dateLabel.text = " " + pt.toString + " " * (labelMaxSize - pt.toString.length)
    //dayOfWeekLabel.text = s" Dimension '${pt.dimension}'"
  }

}


object PqrPanel {
  val timestampFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.S", Locale.US)
  val weekOfDayFormatter = DateTimeFormatter.ofPattern("E", Locale.forLanguageTag("en"))

}
