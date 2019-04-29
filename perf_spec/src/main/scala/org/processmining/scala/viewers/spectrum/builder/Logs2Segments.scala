package org.processmining.scala.viewers.spectrum.builder

import java.io.File
import java.time.ZoneId
import java.util.concurrent.{Executors, TimeUnit}

import org.ini4j.{Ini, IniPreferences}
import org.processmining.scala.log.common.csv.parallel.CsvReader
import org.processmining.scala.log.common.enhancment.segments.common.SpectrumFileNames
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.processmining.scala.log.utils.common.csv.common.{CsvExportHelper, CsvImportHelper}
import org.slf4j.LoggerFactory

class Logs2Segments(logFilename: String,
                    segmentsFileName: String => String,
                    factoryOfFactory: Array[String] => Array[String] => (String, UnifiedEvent),
                    logPreProcessing: UnifiedEventLog => UnifiedEventLog,
                    exportAggregatedLog: Boolean) extends Runnable {

  private val logger = LoggerFactory.getLogger(classOf[Logs2Segments])

  override def run(): Unit = {
    logger.info(s"Task for '$logFilename' started...")
    val csvReader = new CsvReader()
    val (header, lines) = csvReader.read(logFilename)
    val factory = factoryOfFactory(header)
    val log = UnifiedEventLog.create(csvReader.parse(lines, factory))
    new UnifiedEventLog2Segments(log, segmentsFileName, logPreProcessing).run()
    logger.info(s"Task for '$logFilename' is done.")
  }
}

object Logs2Segments {
  val logger = LoggerFactory.getLogger(Logs2Segments.getClass)
  val systemZone: ZoneId = ZoneId.systemDefault

  def logFileNameToSegmentFileName(segmentsPath: String, logFullPath: String)(segment: String): String = {
    val logFilename = new File(logFullPath).getName
    s"$segmentsPath/$logFilename.${SpectrumFileNames.segmentNameToFileName(segment)}.seg"
  }

  // private

  private def getFactory(filename: String): (Array[String] => Array[String] => (String, UnifiedEvent), Long, Long) = {


    val iniPrefs = new IniPreferences(new Ini(new File(filename)))
    val generalNode = iniPrefs.node("GENERAL")
    val dateFormat = generalNode.get("dateFormat", CsvExportHelper.FullTimestampPattern)
    val zoneIdString = generalNode.get("zoneId", systemZone.getId)
    val importCsvHelper = new CsvImportHelper(dateFormat, zoneIdString)
    val caseIdColumn = generalNode.get("caseIdColumn", "CaseID")
    val activityColumn = generalNode.get("activityColumn", "Activity")
    val timestampColumn = generalNode.get("timestampColumn", "Timestamp")
    val startTime = generalNode.get("startTime", "")
    val endTime = generalNode.get("endTime", "")

    def factory(caseIdIndex: Int, completeTimestampIndex: Int, activityIndex: Int)(row: Array[String]): (String, UnifiedEvent) =
      (
        row(caseIdIndex),
        UnifiedEvent(
          importCsvHelper.extractTimestamp(row(completeTimestampIndex)),
          row(activityIndex)))

    def factoryOfFactory(header: Array[String]): Array[String] => (String, UnifiedEvent) = {
      factory(header.indexOf(caseIdColumn),
        header.indexOf(timestampColumn),
        header.indexOf(activityColumn))
    }

    (factoryOfFactory, importCsvHelper.extractTimestamp(startTime), importCsvHelper.extractTimestamp(endTime))
  }

  // Returns startTimeMs, twCount
  def startNoPreprocessing(iniFilename: String, segmentsPath: String, parallelProcessing: Boolean): (Long, Long) = {
    val dirName = new File(iniFilename).getParent
    val dir = new File(dirName)
    val files = dir.listFiles().filter(_.getName.toLowerCase.endsWith(".csv")).map(_.getPath).toList
    val (factory, startTimeMs, endTimeMs) = getFactory(iniFilename)
    start(files, factory, false, x => x, segmentsPath, parallelProcessing)
    (startTimeMs, endTimeMs)
  }

  def startNoPreprocessing(log: UnifiedEventLog, segmentsPath: String, logName: String) =
    new UnifiedEventLog2Segments(log, logFileNameToSegmentFileName(segmentsPath, s"c:/dummy/$logName"), x => x)
      .run()


  def start(filenames: List[String],
            factoryOfFactory: Array[String] => Array[String] => (String, UnifiedEvent),
            exportAggregatedLog: Boolean,
            logPreProcessing: UnifiedEventLog => UnifiedEventLog,
            segmentsPath: String,
            parallelProcessing: Boolean = false
           ): Unit = {
    if (filenames.isEmpty) throw new IllegalArgumentException(s"filenames is empty")
    new File(segmentsPath).mkdirs()
    val poolSize = if (parallelProcessing) Math.min(Runtime.getRuntime.availableProcessors(), filenames.length) else 1
    logger.info(s"poolSize=$poolSize")
    val executorService = Executors.newFixedThreadPool(poolSize)
    val futures = filenames.map(filename => {
      val l2s = new Logs2Segments(filename, logFileNameToSegmentFileName(segmentsPath, filename), factoryOfFactory, logPreProcessing, exportAggregatedLog)
      executorService.submit(l2s)
    })
    logger.info("All tasks are submitted.")
    futures.foreach(_.get())
    logger.info("All tasks are done.")
    executorService.shutdown()
    while (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {}
    logger.info("Thread pool is terminated.")
  }
}
