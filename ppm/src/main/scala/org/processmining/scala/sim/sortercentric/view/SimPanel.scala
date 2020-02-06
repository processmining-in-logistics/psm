package org.processmining.scala.sim.sortercentric.view

import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.{DateTimeFormatter, TextStyle}
import java.util.Locale

import org.processmining.scala.log.utils.common.errorhandling.EH
import org.processmining.scala.sim.sortercentric.experiments.{ExperimentTemplate, SomeIncomingProcess}
import org.slf4j.LoggerFactory

import scala.swing.BorderPanel.Position
import scala.swing.event.{ButtonClicked, Key, KeyPressed, MouseClicked, ValueChanged}
import scala.swing.{BorderPanel, Button, CheckBox, Dimension, FlowPanel, Font, Label, Orientation, ScrollPane, Slider, TextArea, event}

class SimPanel(experimentEnv: ExperimentTemplate) extends BorderPanel {
  val logger = LoggerFactory.getLogger(classOf[SimPanel])
  val interactiveModelPanel = new InteractiveModel(_ => scrollingPanel.peer.revalidate(), MhsVisualizer(experimentEnv, MhsVisualizerSettings()), experimentEnv)
  private val zoneId = ZoneId.of("Europe/London")
  val engine = experimentEnv.initAndGetEngine()
  var isSimStarted = false
  var simTimer = ScalaTimer(0, false)(() => {})
  val timerIntervalMs = 0

  def close() = experimentEnv.unititialize()

  val dateLabel = new Label("01.01.1970 00:00:00.0") {
    horizontalAlignment = swing.Alignment.Left
    font = new Font("Tahoma", 0, 18)
  }
  val dayOfWeekLabel = new Label("Thu") {
    horizontalAlignment = swing.Alignment.Center
    font = new Font("Tahoma", 0, 16)
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


  val controlPanel = new BorderPanel {
    val positionPanel = new BorderPanel {

      val positionSlider = new Slider {
        min = InteractiveModel.PositionMin
        max = InteractiveModel.PositionMax
        value = InteractiveModel.PositionDefaultValue
        tooltip = "Execution position"
        majorTickSpacing = 2000
        minorTickSpacing = 200
        paintLabels = true
        paintTicks = true
        reactions += {
          case ValueChanged(_) => interactiveModelPanel.onPositionChanged(value)
        }
      }
      layout(positionSlider) = Position.South
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

          val playButton = new Button(">>")
          val rewindToStartButton = new Button("|<")

          reactions += {

            //case KeyPressed(`playButton`, key, mod, loc)=>{

            case evt@MouseClicked(`playButton`, pt, _, _, _) => {
              if (evt.peer.getButton == 1) {
                if (isSimStarted) {
                  simTimer.stop()
                  isSimStarted = false
                  playButton.text = ">>"
                  engine.flushEventLogFile()
                } else {
                  playButton.text = "||"
                  isSimStarted = true
                  simTimer = ScalaTimer(timerIntervalMs/*experimentEnv.systemConfig.stepMs.toInt*/)(executeStep)
                }
              }
            }
          }

          listenTo(playButton.mouse.clicks, rewindToStartButton)

          val playerPanel = new FlowPanel(playButton, rewindToStartButton)
          layout(playerPanel) = Position.West

          val velocityPanel = new BorderPanel {

            val velocitySlider: Slider = new Slider {
              min = InteractiveModel.VelocityMin
              max = InteractiveModel.VelocityMax
              value = InteractiveModel.VelocityDefaultValue
              tooltip = "Velocity"
              majorTickSpacing = 20000
              minorTickSpacing = 2000
              paintTicks = true
              paintLabels = true
              reactions += {
                case ValueChanged(`velocitySlider`) => interactiveModelPanel.onVelocityChanged(value)
              }
            }
            layout(velocitySlider) = Position.South
          }
          layout(velocityPanel) = Position.Center
        }
        layout(velocityAndPlayerPanel) = Position.Center


      }
      layout(middlePanel) = Position.North

      val cmdPanel = new BorderPanel{
        preferredSize = new Dimension(320, 25)
        val textAreaCmd = new TextArea("block x 10000")
        val buttonSendCmd = new Button("Send")
        layout(textAreaCmd) = Position.Center
        layout(buttonSendCmd) = Position.East

        reactions += {
          case evt@MouseClicked(`buttonSendCmd`, pt, _, _, _) => {
            if (evt.peer.getButton == 1) {
              try {
                experimentEnv.runCmd(textAreaCmd.text)
              }
              catch {
                case e: Exception => EH.apply().warnAndMessageBox(s"Wrong cmd '${textAreaCmd.text}'", e)
              }
            }
          }
        }
        listenTo(buttonSendCmd.mouse.clicks)

      }
      val buttonEventLogSnapshot = new Button("Snapshot")
      val checkBoxShowCaseId = new CheckBox("Show CaseID"){
        mnemonic = Key.I
      }
      val checkBoxShowDst = new CheckBox("Show Dst"){
        mnemonic = Key.D
      }

      val bottomPanel = new FlowPanel(cmdPanel, checkBoxShowCaseId, checkBoxShowDst, buttonEventLogSnapshot){
        listenTo(buttonEventLogSnapshot.mouse.clicks, checkBoxShowCaseId, checkBoxShowDst)
        reactions += {
          case evt@ButtonClicked(`checkBoxShowCaseId`) => interactiveModelPanel.onShowCaseId(checkBoxShowCaseId.selected)
          case evt@ButtonClicked(`checkBoxShowDst`) => interactiveModelPanel.onShowDst(checkBoxShowDst.selected)

          case evt@MouseClicked(`buttonEventLogSnapshot`, pt, _, _, _) => {
            if (evt.peer.getButton == 1) {
              try {
                }
              catch {
                case e: Exception => EH.apply().warnAndMessageBox(s"", e)
              }
            }
          }
        }
      }


      layout(bottomPanel) = Position.Center
    }
    layout(middleAndBottomOfControlPanel) = Position.Center
  }
  layout(controlPanel) = Position.South

  def executeStep()= {
    engine.executeStep()
    interactiveModelPanel.forceRepaint()
    psmMouseMoveListener(engine.instance.state.timeMs)
  }

  private def psmMouseMoveListener(utcUnixTimeMs: Long) = {
    dateLabel.text =
      LocalDateTime.ofInstant(Instant.ofEpochMilli(utcUnixTimeMs), zoneId).format(SimPanel.timestampFormatter)
    val zoneIdText = zoneId.getDisplayName(TextStyle.FULL, Locale.US)
    dayOfWeekLabel.text =
      s"${LocalDateTime.ofInstant(Instant.ofEpochMilli(utcUnixTimeMs), zoneId).format(SimPanel.weekOfDayFormatter)} ($zoneIdText)"

  }

}


object SimPanel {
  val timestampFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.S", Locale.US)
  val weekOfDayFormatter = DateTimeFormatter.ofPattern("E", Locale.forLanguageTag("en"))

}
