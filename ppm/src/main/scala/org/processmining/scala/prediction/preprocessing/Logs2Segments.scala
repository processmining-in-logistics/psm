package org.processmining.scala.prediction.preprocessing

import java.io.PrintWriter
import java.util.concurrent.{Executors, TimeUnit}
import org.processmining.scala.log.common.csv.parallel.{CsvReader, CsvWriter}
import org.processmining.scala.log.common.enhancment.segments.common.{SegmentUtils, SpectrumFileNames}
import org.processmining.scala.log.common.unified.event.{CommonAttributeSchemas, UnifiedEvent}
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.processmining.scala.log.common.unified.trace.UnifiedTraceId
import org.processmining.scala.log.utils.csv.common.CsvExportHelper
import org.processmining.scala.sim.conveyors.impl.AbstractEngine
import org.slf4j.LoggerFactory

class Logs2Segments(logFilename: String,
                    segmentsFileName: String => String,
                    factoryOfFactory: Array[String] => Array[String] => (String, UnifiedEvent),
                    logPreProcessing: UnifiedEventLog => UnifiedEventLog,
                    //eventAggregator: EventAggregator,
                    exportAggregatedLog: Boolean) extends Runnable {

  protected val logger = LoggerFactory.getLogger(classOf[Logs2Segments])
//  val t = TraceExpression()
//  val exAggregation = t aggregate UnifiedEventAggregator(eventAggregator)
//  val exToBeDeleted = EventEx("PreSortersToBeDeleted")
  val csvExportHelper = new CsvExportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone, ",")

  def writeSegments(segmentName: String, orderedSegments: List[(UnifiedTraceId, UnifiedEvent)]) = {
    val filename = segmentsFileName(segmentName)
    val w = new PrintWriter(filename)
    val header = "CaseID,Timestamp,Activity,Duration,Field0"
    w.println(header)

    orderedSegments
      .map(x =>
        s"${x._1.id},${csvExportHelper.timestamp2String(x._2.timestamp)},${x._2.activity},${x._2.getAs[Long](CommonAttributeSchemas.AttrNameDuration)},${x._2.getAs[String](AbstractEngine.Field0)}")
      .foreach(w.println)
    w.close()
    logger.info(s"'$filename' exported.")
  }

  def exportEventLog(log: UnifiedEventLog, filename: String) ={
    CsvWriter.logToCsvLocalFilesystem(log, filename, csvExportHelper.timestamp2String)

  }

  override def run(): Unit = {
    logger.info(s"Task for '$logFilename' started...")
    val csvReader = new CsvReader()
    val (header, lines) = csvReader.read(logFilename)
    val factory = factoryOfFactory(header)
    val log = UnifiedEventLog.create(csvReader.parse(lines, factory))

    val preProcessedLog = logPreProcessing(log)
//      .map(exAggregation)
//      .remove(exToBeDeleted)

    if(exportAggregatedLog)
      exportEventLog(preProcessedLog, s"$logFilename.preprocessed.csv")
    logger.info(s"$logFilename.preprocessed.csv exported")

    preProcessedLog
      .traces
      .map(e => ("x" /*e._2.head.getAs[String](AbstractEngine.Field0)*/, SegmentUtils.convertToSegments(SegmentUtils.DefaultSeparator, e))) //TODO: keep attribute Field0 value
      .map(e => (e._2._1, e._2._2.map(_.copy(AbstractEngine.Field0, e._1))))
      .flatMap(x => x._2.map((x._1, _)))
      .groupBy(_._2.activity)
      .map(x => (x._1, x._2.toList.sortBy(_._2.timestamp)))
      .foreach(x => writeSegments(x._1, x._2))
    logger.info(s"Task for '$logFilename' done.")

  }
}

object Logs2Segments {

  val logger = LoggerFactory.getLogger(Logs2Segments.getClass)



  def logFileNameToSegmentFileName(filename: String)(segment: String): String =
    s"$filename.${SpectrumFileNames.segmentNameToFileName(segment)}.seg"

  def start(filenames: List[String],
            factoryOfFactory: Array[String] => Array[String] => (String, UnifiedEvent),
            exportAggregatedLog: Boolean,
            logPreProcessing: UnifiedEventLog => UnifiedEventLog
            /*eventAggregator: EventAggregator = new EventAggregatorImpl()*/) = {
    //val poolSize = Math.min(Runtime.getRuntime.availableProcessors() * 2, filenames.length)
    val poolSize = 1
    logger.info(s"poolSize=$poolSize")
    val executorService = Executors.newFixedThreadPool(poolSize)
    val futures = filenames.map(filename => {
      val l2s = new Logs2Segments(filename, logFileNameToSegmentFileName(filename), factoryOfFactory, logPreProcessing, exportAggregatedLog)
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



