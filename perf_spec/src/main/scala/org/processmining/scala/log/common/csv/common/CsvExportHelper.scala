package org.processmining.scala.log.common.csv.common

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.Locale


class CsvExportHelper(
                       timestampFormatterPattern: String,
                       zoneIdString: String,
                       splitterRegEx: String
                     ) extends Serializable {

  @transient private lazy val timestampFormatter = {
    DateTimeFormatter.ofPattern(timestampFormatterPattern, Locale.US)
  }

  @transient private lazy val zoneId = {
    ZoneId.of(zoneIdString)
  }

  private def timestamp2StringImpl(timestamp: Long): String =
    timestampFormatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId))

  def timestamp2String: Long => String = timestamp2StringImpl

}

object CsvExportHelper{
  val DefaultTimestampPattern = "dd-MM-yy HH:mm:ss.SSS"
  val ShortTimestampPattern = "dd-MM-yyyy HH:mm:ss"
  val AmsterdamTimeZone = "Europe/Amsterdam"
  val UtcTimeZone = "UTC"
  val DefaultSeparator = ";"

  def apply(): CsvExportHelper = new CsvExportHelper(CsvExportHelper.DefaultTimestampPattern,
    CsvExportHelper.UtcTimeZone,
    CsvExportHelper.DefaultSeparator)
}