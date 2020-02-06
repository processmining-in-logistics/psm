package org.processmining.scala.viewers.spectrum2.experiments

import java.io.File

import javax.swing.UIManager
import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.utils.common.errorhandling.JvmParams
import org.processmining.scala.viewers.spectrum2.model.{AbstractDataSource, CallableWithLevel, UnifiedEventLogBasedBuilder}
import org.processmining.scala.viewers.spectrum2.view.{BuilderFactory, EventLogProperties, EventLogPropertiesFactory, PsmMainFrame}
import org.slf4j.LoggerFactory

import scala.swing.{Frame, MainFrame, SimpleSwingApplication}

class DebugEventLogPropertiesFactory extends EventLogPropertiesFactory {
  override def getEventLog(eventLogFileName: String): EventLogProperties =
  //EventLogProperties("c:/gaps", Vector("c:/gaps/20171003.csv"))
  //EventLogProperties("c:/sim_gaps", Vector("c:/sim_gaps/!incomplete_log.csv"))  // PN2020
    EventLogProperties("", Vector("!incomplete_log.csv")) // PN2020


}

class SingleFileEventLogPropertiesFactory() extends EventLogPropertiesFactory {
  override def getEventLog(eventLogFileName: String): EventLogProperties = {
    val file = new File(eventLogFileName)
    val parent = file.getParent
    val dir = if (parent == null) "" else parent.toString
    EventLogProperties(dir, Vector(eventLogFileName))
  }
}

class BuilderFactoryImpl(skipPreProcessing: Boolean, exportLog: Boolean) extends BuilderFactory {
  override def getBuilder(eventLogWithProperties: EventLogProperties, onlyObservedPs: Boolean): CallableWithLevel[AbstractDataSource] = {
    val proc = new TLogsToSegments(skipPreProcessing, eventLogWithProperties.filenames(0), eventLogWithProperties.dir, exportLog)
    new UnifiedEventLogBasedBuilder(proc, onlyObservedPs, eventLogWithProperties.dir)
  }
}

object PSM2 extends SimpleSwingApplication {
  private val logger = LoggerFactory.getLogger(PSM2.getClass)
  val isGeneralPurposePsm = false
  lazy val ui = if (isGeneralPurposePsm) new PsmMainFrame(new SingleFileEventLogPropertiesFactory, new BuilderFactoryImpl(true, false), true)
  else new PsmMainFrame(new DebugEventLogPropertiesFactory, new BuilderFactoryImpl(false, false), false)

  override def top: Frame = new MainFrame {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, s"${getClass} started")
    for (info <- UIManager.getInstalledLookAndFeels) {
      if ("Nimbus" == info.getName) {
        UIManager.setLookAndFeel(info.getClassName)
      }
    }

    System.setProperty("sun.java2d.opengl", "True")
    title = PsmMainFrame.AppTitle
    contents = ui
  }
}