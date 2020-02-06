package org.processmining.scala.sim.conveyors.experiments

import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.common.enhancment.segments.common.{AbstractDurationClassifier, DummyDurationClassifier, NormalSlowVerySlowDurationClassifier}
import org.processmining.scala.log.utils.common.errorhandling.{EH, JvmParams}
import org.processmining.scala.viewers.spectrum.builder.AbstractSegmentsToSpectrumSession
import org.slf4j.LoggerFactory


object SimSegmentsToSpectrumApp extends AbstractSegmentsToSpectrumSession {

  val SegmentsPath = "g:/sim_logs/Scan1"
  val SpectrumRoot = "G:/SIM_PS/Scan1_NormalSlowVerySlowDurationClassifier"
  val DatasetSizeDays = 7
  val startTime = "01-09-2018 00:00:00.000"
  val twSizeMs = 20 * 1000

  override def classifier: AbstractDurationClassifier = new NormalSlowVerySlowDurationClassifier {}

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, s"${SimSegmentsToSpectrumApp.getClass} started")
    try {
      run()
    } catch {
      case e: Throwable => logger.error(EH.formatError(e.toString, e))
    }
    logger.info(s"App is completed.")
  }

}

class SimSegmentsToSpectrumCli(override val SegmentsPath: String, override val SpectrumRoot: String, override val DatasetSizeDays: Int, override val startTime: String, override val twSizeMs: Int) extends AbstractSegmentsToSpectrumSession {
  override def classifier: AbstractDurationClassifier = new DummyDurationClassifier {}

}

object SimSegmentsToSpectrumCli {
  private val logger = LoggerFactory.getLogger(PreSorterLogsToSegmentsApp.getClass)

  //  val startTime = "01-09-2018 00:00:00.000"
  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, s"${SimSegmentsToSpectrumCli.getClass} started")
    if (args.isEmpty) {
      logger.info(s"Use the following arguments: SEGMENTS_DIR DAYS START_TIME BIN_DURATION_MS OUT_DIR")
    } else {
      logger.info(s"Cli args:${args.mkString(",")}")
      try {
        val SegmentsPath = args(0)
        val SpectrumRoot = args(4)
        val DatasetSizeDays = args(1).toInt
        val startTime = args(2)
        val twSizeMs = args(3).toInt
        new SimSegmentsToSpectrumCli(SegmentsPath, SpectrumRoot, DatasetSizeDays, startTime, twSizeMs).run()
      } catch {
        case e: Throwable => logger.error(EH.formatError(e.toString, e))
      }
    }
    logger.info(s"App is completed.")
  }

}
