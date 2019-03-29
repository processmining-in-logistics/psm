package org.processmining.scala.intercase

import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.common.csv.parallel.CsvReader
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.processmining.scala.log.utils.common.errorhandling.{EH, JvmParams}


object ProductionLogExample extends BaseDdeInterCaseFeatureEncodingSession{
  val thresholds = List(86400000L, 432000000L, 864000000L, 1296000000L, 2592000000L)
  val Root = "g:/method_17"
  val Dir = s"$Root/out"

  def loadFromCsv(filename: String): UnifiedEventLog = {
    def factory(caseIdIndex: Int, timestampIndex: Int, activityIndex: Int, row: Array[String]): (String, UnifiedEvent) =
      (
        row(caseIdIndex),
        UnifiedEvent(
          row(timestampIndex).toLong * 1000L,
          row(activityIndex)))

    val csvReader = new CsvReader()
    val (header, lines) = csvReader.read(filename)
    UnifiedEventLog.create(
      csvReader.parse(
        lines,
        factory(header.indexOf("case_id"),
          header.indexOf("start_time"),
          header.indexOf("event"),
          _: Array[String])
      ))
  }


  val outcome: (Prefix, List[UnifiedEvent]) => Option[Outcome] = defaultOutcome
  val isCompleted: (Prefix, List[UnifiedEvent]) => Boolean = defaultIsCompleted
  val prefixFilter: Prefix => Boolean = defaultPrefixFilter

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, "FeatureEncoder started")
    try {
      val log = loadFromCsv(s"$Root/example.csv")
      val samples = new DdeInterCaseFeatureEncoder(log, thresholds, outcome, isCompleted, prefixFilter).call()
      exportTrainingTestSet(Dir, samples, "07-03-2012 00:00:00")
    } catch {
      case e: Throwable => EH().error(e)
    }
    logger.info(s"App is completed.")
  }
}
