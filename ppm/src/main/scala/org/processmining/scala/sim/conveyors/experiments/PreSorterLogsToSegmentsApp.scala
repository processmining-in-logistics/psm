package org.processmining.scala.sim.conveyors.experiments

import java.io.File

import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.utils.common.csv.common.{CsvExportHelper, CsvImportHelper}
import org.processmining.scala.log.utils.common.errorhandling.JvmParams
import org.processmining.scala.sim.conveyors.impl.AbstractEngine
import org.processmining.scala.viewers.spectrum.builder.Logs2Segments
import org.slf4j.LoggerFactory

import scala.collection.immutable.SortedMap

object PreSorterLogsToSegmentsApp {
  private val logger = LoggerFactory.getLogger(PreSorterLogsToSegmentsApp.getClass)
  private val importCsvHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)

  private def factory(caseIdIndex: Int, completeTimestampIndex: Int, activityIndex: Int, field0Index: Int)(row: Array[String]): (String, UnifiedEvent) =
    (
      row(caseIdIndex),
      UnifiedEvent(
        importCsvHelper.extractTimestamp(row(completeTimestampIndex)),
        row(activityIndex),
        SortedMap(AbstractEngine.Field0 -> row(field0Index)),
        None))

  private def factoryOfFactory(header: Array[String]): Array[String] => (String, UnifiedEvent) = {
    factory(header.indexOf("CaseID"),
      header.indexOf("Timestamp"),
      header.indexOf("Activity"),
      header.indexOf(AbstractEngine.Field0))
  }


  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, "PreSorterLogsToSegmentsApp started")
    if (args.isEmpty) {
      logger.info(s"Use the following arguments: INPUT_OUTPUT_DIR")
    } else {
      logger.info(s"Cli args:${args.mkString(",")}")
      try {
        val eventLogPath = args(0)
        logger.info(s"Event log path='$eventLogPath'")
        val dir = new File(eventLogPath)
        val files = dir.listFiles().filter(_.getName.toLowerCase.endsWith(".csv")).map(_.getPath)
        Logs2Segments.start(files.toList, factoryOfFactory, false, x => x, eventLogPath)
      } catch {
        case e: Throwable => logger.error(e.toString)
      }
    }
    logger.info(s"App is completed.")
  }


}

