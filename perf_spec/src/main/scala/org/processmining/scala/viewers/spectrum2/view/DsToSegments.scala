package org.processmining.scala.viewers.spectrum2.view

import java.io.PrintWriter

import org.processmining.scala.log.utils.common.csv.common.CsvExportHelper
import org.processmining.scala.viewers.spectrum2.model.AbstractDataSource


class DsToSegments(ds: AbstractDataSource, outputDir: String, prefix: String) extends Runnable {

  val csvExportHelper = new CsvExportHelper("dd-MM-yyyy HH:mm:ss.SSS", CsvExportHelper.AmsterdamTimeZone, CsvExportHelper.DefaultSeparator)

  override def run(): Unit = {

    val header = """CaseID,Timestamp,Activity,Duration"""
    ds.segments.foreach({ x => {
      val sn = x._1
      val filename = s"$prefix.${sn.a}!${sn.b}.seg"
      val segName = s"${sn.a}:${sn.b}"
      val pw = new PrintWriter(s"$outputDir/$filename")
      pw.println(header)
      x._2
        .sortBy(_.start.minMs)
        .map(s => s"${s.caseId},${csvExportHelper.timestamp2String(s.start.minMs)},$segName,${Math.abs(s.end.minMs - s.start.minMs)}")
        .foreach(pw.println(_))
      pw.close()
    }
    })
  }
}
