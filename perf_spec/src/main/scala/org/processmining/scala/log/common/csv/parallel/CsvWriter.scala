package org.processmining.scala.log.common.csv.parallel

import java.io.PrintWriter
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import scala.collection.parallel.ParSeq


object CsvWriter {

  @deprecated
  def eventsToCsvLocalFilesystem[E](events: ParSeq[E],
                                    csvHeader: String,
                                    toCsv: E => String,
                                    filename: String): Unit = {
    val w = new PrintWriter(filename)
    try {
      if (!events.isEmpty) {
        w.println(csvHeader)
        events
          .map(toCsv)
          .seq // because PrintWriter implementation is not thread safe
          .foreach(w.println(_))
      }
    } finally {
      w.close()
    }
  }


  //  @deprecated
  //  def logToCsvLocalFilesystem(unifiedEventLog: UnifiedEventLog,
  //                              filename: String,
  //                              timestampConverter: Long => String,
  //                              attrs: String*
  //                             ): Unit = {
  //    val w = new PrintWriter(filename)
  //    try {
  //      val attrsHeader = if (attrs.isEmpty) "" else attrs.mkString(";", """";"""", "")
  //      w.println(""""id";"timestamp";"activity"""" + attrsHeader)
  //      if (attrs.nonEmpty)
  //        unifiedEventLog.
  //          events
  //          .filter(_._2.hasAttributes(attrs: _*))
  //          .map(t => s""""${t._1.id}";${t._2.toCsv(timestampConverter, attrs: _*)}""")
  //      else //faster
  //        unifiedEventLog.
  //          events
  //          .map(t => s""""${t._1.id}";${t._2.toCsvWithoutAttributes(timestampConverter)}""")
  //          .seq // because PrintWriter implementation is not thread safe
  //          .foreach(w.println(_))
  //    } finally {
  //      w.close()
  //    }
  //  }

  def logToCsvLocalFilesystem(unifiedEventLog: UnifiedEventLog,
                              filename: String,
                              timestampConverter: Long => String,
                              unsortedAttrs: String*
                             ): Unit = {
    val w = new PrintWriter(filename)
    logToCsvLocalFilesystem(w, unifiedEventLog, filename, timestampConverter, true, true, unsortedAttrs: _*)
  }


  def logToCsvLocalFilesystem(w: PrintWriter,
                              unifiedEventLog: UnifiedEventLog,
                              filename: String,
                              timestampConverter: Long => String,
                              addHeader: Boolean,
                              shouldClose: Boolean,
                              unsortedAttrs: String*
                             ): Unit = {

    try {
      val attrs = unsortedAttrs.sorted
      //val attrs = schema.fields.map(_.name)
      val attrsHeader = if (attrs.isEmpty) "" else attrs.mkString(""";"""", """";"""", """"""")
      if(addHeader)
        w.println(""""id";"timestamp";"activity"""" + attrsHeader)
      unifiedEventLog
        //.filter(schema)
        .events
        .map(t => s""""${t._1.id}";${t._2.toCsv(timestampConverter, attrs: _*)}""")
        .seq // because PrintWriter implementation is not thread safe
        .foreach(w.println(_))
    } finally {
      if(shouldClose)
        w.close()
    }
  }

}
