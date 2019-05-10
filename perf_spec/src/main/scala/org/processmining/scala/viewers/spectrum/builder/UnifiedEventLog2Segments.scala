package org.processmining.scala.viewers.spectrum.builder

import java.io.PrintWriter

import org.processmining.scala.log.common.enhancment.segments.common.SegmentUtils
import org.processmining.scala.log.common.unified.event.{CommonAttributeSchemas, UnifiedEvent}
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.processmining.scala.log.common.unified.trace.UnifiedTraceId
import org.processmining.scala.log.utils.common.csv.common.CsvExportHelper
import org.slf4j.LoggerFactory


class UnifiedEventLog2Segments(log: UnifiedEventLog,
                               segmentsFileName: String => String,
                               logPreProcessing: UnifiedEventLog => UnifiedEventLog) extends Runnable {

  private val logger = LoggerFactory.getLogger(classOf[UnifiedEventLog2Segments])
  private val csvExportHelper = new CsvExportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone, ",")

  private def writeSegments(segmentName: String, orderedSegments: List[(UnifiedTraceId, UnifiedEvent)]) = {
    val filename = segmentsFileName(segmentName)
    val w = new PrintWriter(filename)
    val header = "CaseID,Timestamp,Activity,Duration,Class"
    w.println(header)

    orderedSegments
      .map(x =>
        s"${x._1.id},${csvExportHelper.timestamp2String(x._2.timestamp)},${x._2.activity},${x._2.getAs[Long](CommonAttributeSchemas.AttrNameDuration)},-1")
      .foreach(w.println)
    w.close()
    logger.info(s"'$filename' exported.")
  }

  override def run(): Unit =
    logPreProcessing(log)
      .traces
      .map(e => ("x", SegmentUtils.convertToSegments(SegmentUtils.DefaultSeparator, e))) //TODO: keep attribute Field0 value
      .map(e => (e._2._1, e._2._2.map(_.copy("Field0", e._1))))
      .flatMap(x => x._2.map((x._1, _)))
      .groupBy(_._2.activity)
      .map(x => (x._1, x._2.toList.sortBy(_._2.timestamp)))
      .foreach(x => writeSegments(x._1, x._2))

  //  private def exportEventLog(log: UnifiedEventLog, filename: String) = {
  //    CsvWriter.logToCsvLocalFilesystem(log, filename, csvExportHelper.timestamp2String)
  //
  //  }

}
