package org.processmining.scala.log.common.csv.common

import java.io.PrintWriter

object CsvWriter {

  def eventsToCsvLocalFilesystem[E](events: Iterable[E],
                                    csvHeader: String,
                                    toCsv: E => String,
                                    filename: String): Unit = {
    val w = new PrintWriter(filename)
    try {
      if (!events.isEmpty) {
        w.println(csvHeader)
        events
          .seq
          .foreach { e => w.println(toCsv(e)) }
      }
    } finally {
      w.close()
    }
  }


}
