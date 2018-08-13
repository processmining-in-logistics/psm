package org.processmining.scala.log.common.utils.common

import java.time.temporal.ChronoUnit
import java.time.{DayOfWeek, Instant, ZoneId, ZonedDateTime}

object DateRounding {

  def toZonedDateTime(zoneId: ZoneId, timestamp: Long): ZonedDateTime =
    ZonedDateTime
      .ofInstant(Instant.ofEpochMilli(timestamp), zoneId)

  private def truncateToImpl(zoneId: ZoneId, chronoUnit: ChronoUnit, timestamp: Long): ZonedDateTime =
    toZonedDateTime(zoneId, timestamp)
      .truncatedTo(chronoUnit)

  def truncatedTo(zoneId: ZoneId, chronoUnit: ChronoUnit, timestamp: Long): Long =
    truncateToImpl(zoneId, chronoUnit, timestamp).toInstant.toEpochMilli

  def truncatedToWeeks(zoneId: ZoneId, timestamp: Long): Long = {
    var truncated = truncateToImpl(zoneId, ChronoUnit.DAYS, timestamp)
    while(truncated.getDayOfWeek != DayOfWeek.MONDAY){
      truncated = truncated.minusDays(1)
    }
    truncated.toInstant.toEpochMilli
  }
}
