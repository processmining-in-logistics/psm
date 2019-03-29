package org.processmining.scala.sim.conveyors.experiments

import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.intercase._
import org.processmining.scala.log.common.csv.parallel.CsvReader
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.processmining.scala.log.utils.common.errorhandling.{EH, JvmParams}
import org.processmining.scala.log.utils.csv.common.{CsvExportHelper, CsvImportHelper}

object SimLogDdeInterCaseFeatureEncodingSession extends BaseDdeInterCaseFeatureEncodingSession with SegmentBasesFilters {
  val thresholds = List(5 * 1000L, 10 * 1000L, 20 * 1000L, 30 * 1000L, 60 * 1000L)
  val Root = "g:/sim_17"
  val Dir = s"$Root/out_new"
  val activityA = "E1.TO_SCAN_1_0"
  val activityB = "E2.SCAN_1"

  def loadFromCsv(filename: String): UnifiedEventLog = {
    val importCsvHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)

    def factory(caseIdIndex: Int, timestampIndex: Int, activityIndex: Int, row: Array[String]): (String, UnifiedEvent) =
      (
        row(caseIdIndex),
        UnifiedEvent(
          importCsvHelper.extractTimestamp(row(timestampIndex)),
          row(activityIndex)))

    val csvReader = new CsvReader()

    val (header, lines) = csvReader.read(filename)
    UnifiedEventLog.create(
      csvReader.parse(
        lines,
        factory(header.indexOf("CaseID"),
          header.indexOf("Timestamp"),
          header.indexOf("Activity"),
          _: Array[String])
      ))
  }

  def prefixFilterImpl(p: Prefix): Boolean = {
    // p.prefix.last == "A1_0" || p.prefix.last == "A2_0" || p.prefix.last == "A3_0" || p.prefix.last == "TransferIn_0"
    if (p.prefix.size == 2) {
      val s = (p.prefix.head, p.prefix.tail.head)
      s == ("A1_0", "A4_0") || s == ("A2_0", "A4_0") || s == ("A3_0", "Link1_0") || s == ("TransferIn_0", "TransferIn_1")
    } else false
  }

  val outcome: (Prefix, List[UnifiedEvent]) => Option[Outcome] = outcomeForSegment(activityA, activityB)

  val isCompleted: (Prefix, List[UnifiedEvent]) => Boolean = (p, _) => findTargetSegment(activityA, activityB, p.prefix).isDefined

  val prefixFilter: Prefix => Boolean = prefixFilterImpl

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, "SimLogDdeInterCaseFeatureEncodingSession started")
    try {
      val log = loadFromCsv(s"$Root/PPM_BHS_Sim_log.csv")
        //.take(1000)
      val samples = new DdeInterCaseFeatureEncoder(log, thresholds, outcome, isCompleted, prefixFilter).call()
      exportTrainingTestSet(Dir, samples, "06-09-2018 00:00:00")
    } catch {
      case e: Throwable => EH().error(e)
    }
    logger.info(s"App is completed.")
  }
}
