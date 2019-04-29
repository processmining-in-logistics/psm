package org.processmining.scala.viewers.spectrum.view

import java.awt.event.{ActionEvent, ActionListener}
import java.util.concurrent.{Executors, Future, TimeUnit}
import javax.swing.WindowConstants.{DISPOSE_ON_CLOSE, DO_NOTHING_ON_CLOSE}
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.{JFileChooser, Timer}
import org.processmining.scala.log.utils.common.errorhandling.EH
import org.processmining.scala.viewers.spectrum.features.SpectrumToDatasetDefaultImpl
import org.processmining.scala.viewers.spectrum.model.AbstractDataSource
import scala.swing.BorderPanel.Position
import scala.swing._
import scala.swing.event.ButtonClicked

class DatasetExportDialog(ds: AbstractDataSource) extends Dialog with ActionListener {
  title = "Export as training/test sets"
  modal = true
  resizable = false
  minimumSize = new Dimension(600, 0)
  centerOnScreen()
  val defaultBorder = new EmptyBorder(5, 5, 5, 5)
  var path = ""

  val buttonOk = new Button("Start") {
    enabled = false
    reactions += {
      case ButtonClicked(_) => {
        process()
      }
    }
  }

  val labelProgress = new Label("not started")

  val borderPanel0 = new BorderPanel {
    border = defaultBorder
    val borderPanelFileSelection = new BorderPanel {
      border = defaultBorder
      val editFilenamePanel = new BorderPanel {
        border = new EmptyBorder(0, 0, 0, 5)
        val editFilename = new EditorPane {
          enabled = false
        }
        layout(editFilename) = Position.Center
      }

      val buttonOpen = new Button {
        text = "Open..."
        reactions += {
          case ButtonClicked(_) => {
            val dlg = new JFileChooser()
            dlg.setFileSelectionMode(JFileChooser.FILES_ONLY)
            val filterPsm = new FileNameExtensionFilter("PSM dataset config", "psmdataset")
            dlg.setFileFilter(filterPsm)
            if (dlg.showOpenDialog(peer) == JFileChooser.APPROVE_OPTION) {
              path = dlg.getSelectedFile.getPath
              editFilenamePanel.editFilename.text = path
              buttonOk.enabled = path.nonEmpty
            }
          }
        }
      }
      layout(editFilenamePanel) = Position.Center
      layout(buttonOpen) = Position.East
    }
    val borderPanel1 = new BorderPanel {
      border = defaultBorder
      val borderPanelProgress = new BorderPanel {
        border = defaultBorder
        val labelProgressTitle = new Label("Progress: ")

        layout(labelProgressTitle) = Position.West
        layout(labelProgress) = Position.Center
      }
      val borderPanel2 = new BorderPanel {
        border = defaultBorder
        //        val buttonCancel = new Button("Cancel") {
        //        }
        layout(buttonOk) = Position.East
        //layout(buttonCancel) = Position.West
      }
      layout(borderPanelProgress) = Position.North
      layout(borderPanel2) = Position.Center
    }
    layout(borderPanelFileSelection) = Position.North
    layout(borderPanel1) = Position.Center
  }
  contents = borderPanel0

  val executorService = Executors.newSingleThreadExecutor
  val TimeoutMs = 100
  val timer = new Timer(TimeoutMs, this)
  var task: Future[_] = _
  val MaxDots = 16
  var currentDots = 0

  def process(): Unit = {
    peer.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)
    buttonOk.enabled = false
    val impl = SpectrumToDatasetDefaultImpl.getSpectrumToDatasetDefaultImpl(path)
    task = executorService.submit(impl)
    executorService.shutdown()
    timer.start()
  }

  override def actionPerformed(e: ActionEvent): Unit = {
    try {
      if (executorService.awaitTermination(10, TimeUnit.MILLISECONDS)) {
        labelProgress.text = "Done"
        peer.setDefaultCloseOperation(DISPOSE_ON_CLOSE)
      }
      else {
        currentDots = if (currentDots >= MaxDots) 0 else currentDots + 1
        labelProgress.text = (0 to currentDots).map(_ => ".").reduce(_ + _)
        timer.start()
      }
    }
    catch {
      case ex: Exception =>
        EH.apply.errorAndMessageBox("Error", ex)
    }
  }
}
