package org.processmining.scala.sim.sortercentric.view

import javax.swing.UIManager
import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.utils.common.errorhandling.JvmParams
import org.processmining.scala.sim.sortercentric.experiments.Sorter
import org.slf4j.LoggerFactory

import scala.swing.{Frame, MainFrame, SimpleSwingApplication}

object SimApp extends SimpleSwingApplication {
  private val logger = LoggerFactory.getLogger(SimApp.getClass)
  lazy val ui = new SimPanel(new Sorter)

  override def top: Frame = new MainFrame {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, s"${getClass} started")
    for (info <- UIManager.getInstalledLookAndFeels) {
      if ("Nimbus" == info.getName) {
        UIManager.setLookAndFeel(info.getClassName)
      }
    }

    System.setProperty("sun.java2d.opengl", "True")
    title = "MHS Simulator"
    contents = ui
  }
}
