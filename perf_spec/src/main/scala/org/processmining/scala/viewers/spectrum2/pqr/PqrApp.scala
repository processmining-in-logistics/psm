package org.processmining.scala.viewers.spectrum2.pqr
import javax.swing.UIManager
import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.utils.common.errorhandling.JvmParams
import org.slf4j.LoggerFactory

import scala.swing.{Frame, MainFrame, SimpleSwingApplication}

object PqrApp extends SimpleSwingApplication {
  private val logger = LoggerFactory.getLogger(PqrApp.getClass)
  lazy val ui = new PqrPanel()

  override def top: Frame = new MainFrame {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, s"${getClass} started")
    for (info <- UIManager.getInstalledLookAndFeels) {
      if ("Nimbus" == info.getName) {
        UIManager.setLookAndFeel(info.getClassName)
      }
    }

    System.setProperty("sun.java2d.opengl", "True")
    title = "PQR-System"
    contents = ui
  }
}
