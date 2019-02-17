package org.processmining.scala.log.utils.csv.common

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import java.util.Locale

class CsvImportHelper(
                       timestampFormatterPattern: String,
                       zoneIdString: String
                     ) extends Serializable {

  @transient lazy val timestampFormatter = {
    DateTimeFormatter.ofPattern(timestampFormatterPattern, Locale.US)
  }
  @transient lazy val zoneId = {
    ZoneId.of(zoneIdString)
  }

  def extractTimestamp(str: String): Long = {
    val ldtTimestamp = LocalDateTime.parse(
      str,
      timestampFormatter)
    ldtTimestamp
      .atZone(zoneId)
      .toInstant()
      .toEpochMilli()
  }
}
