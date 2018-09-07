package org.processmining.scala.viewers.spectrum.view

import java.awt.{Color, Graphics2D}
import java.time._
import java.time.format.DateTimeFormatter
import java.util.Locale

private[viewers] class TimeScalePanelHelper(val zoneId: ZoneId) {

  private val YearsMs = Duration.ofDays(365).toMillis
  private val MonthsMs = Duration.ofDays(30).toMillis
  private val DayMs = Duration.ofDays(1).toMillis
  private val HourMs = Duration.ofHours(1).toMillis
  private val QuaterMs = HourMs / 4
  private val MinuteMs = HourMs / 60
  private val BoxHeightPx = 15

  private[view] val timestampFormatterYears = DateTimeFormatter.ofPattern("MM-YYYY", Locale.US)
  private[view] val timestampFormatterMonths = DateTimeFormatter.ofPattern("dd-MM-yy", Locale.US)
  private[view] val timestampFormatterDays = DateTimeFormatter.ofPattern("dd-MM", Locale.US)
  private[view] val timestampFormatterHours = DateTimeFormatter.ofPattern("dd-MM HH:00", Locale.US)
  private[view] val timestampFormatter15Mins = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
  private[view] val timestampFormatterMinute = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

  def drawByMonths(g2: Graphics2D,
                   view: TimeDiffGraphics): Unit =
    drawMonths(g2, view, MonthsMs, timestampFormatterMonths)

  def drawByYears(g2: Graphics2D,
                   view: TimeDiffGraphics): Unit =
    draw(g2, view, YearsMs, timestampFormatterYears)

  def drawByDays(g2: Graphics2D,
                 view: TimeDiffGraphics): Unit =
    draw(g2, view, DayMs, timestampFormatterDays)

  def drawByHours(g2: Graphics2D,
                  view: TimeDiffGraphics): Unit =
    draw(g2, view, HourMs, timestampFormatterHours)

  def drawBy15min(g2: Graphics2D,
                  view: TimeDiffGraphics): Unit =
    draw(g2, view, QuaterMs, timestampFormatter15Mins)

  def drawByMinute(g2: Graphics2D,
                   view: TimeDiffGraphics): Unit =
    draw(g2, view, MinuteMs, timestampFormatterMinute)


  private def draw(g2: Graphics2D,
                   view: TimeDiffGraphics,
                   step: Long,
                   formatter: DateTimeFormatter
                  ): Unit = {
    def msRounder(interval: Long)(t: Long) = t / interval * interval

    val rounder: Long => Long = msRounder(step)
    (rounder(view.ds.startTimeMs + view.paintInputParameters.startTwIndex * view.ds.twSizeMs)
      until rounder(view.ds.startTimeMs + view.paintInputParameters.lastTwIndexExclusive * view.ds.twSizeMs)
      by step)
      .map(t => (view.getXByAbsTime(t), view.getXByAbsTime(t + step), t))
      .zipWithIndex
      .foreach(e => {
        g2.setColor(if ((e._2 & 1) != 0) Color.WHITE else Color.BLACK)
        g2.fillRect(e._1._1, 0, e._1._2 - e._1._1, BoxHeightPx)
        if ((e._2 % 4) == 0) {
          g2.setColor(Color.BLACK)
          g2.drawLine(e._1._1, BoxHeightPx, e._1._1, BoxHeightPx + 5)
          val label = LocalDateTime
            .ofInstant(Instant.ofEpochMilli(e._1._3), zoneId)
            .format(formatter)
          g2.drawString(label, e._1._1, BoxHeightPx * 2)
        }

      })
  }


  private def drawMonths(g2: Graphics2D,
                         view: TimeDiffGraphics,
                         step: Long,
                         formatter: DateTimeFormatter
                        ): Unit = {
    def msRounder(interval: Long)(t: Long) = t / interval * interval

    def getLocalDate(t: Long): LocalDateTime =
      LocalDateTime.ofInstant(Instant.ofEpochMilli(t), zoneId)

    val rounder: Long => Long = { x =>
      getLocalDate(msRounder(step)(x))
        .withDayOfMonth(1).
        toEpochSecond(ZoneOffset.UTC) * 1000L
    }

    val startDate = getLocalDate(view.ds.startTimeMs + view.paintInputParameters.startTwIndex * view.ds.twSizeMs)
      .withDayOfMonth(1)
    val monthsCount: Int = ((view.paintInputParameters.lastTwIndexExclusive - view.paintInputParameters.startTwIndex)
      * view.ds.twSizeMs / MonthsMs).toInt
    (0 until monthsCount)
      .map(startDate.plusMonths(_))
      .map(t => (view.getXByAbsTime(t.toEpochSecond(ZoneOffset.UTC) * 1000L),
        view.getXByAbsTime(t.plusMonths(1).toEpochSecond(ZoneOffset.UTC) * 1000L),
        t))
      .zipWithIndex
      .foreach(e => {
        g2.setColor(if ((e._2 & 1) != 0) Color.WHITE else Color.BLACK)
        g2.fillRect(e._1._1, 0, e._1._2 - e._1._1, BoxHeightPx)
        if ((e._2 % 4) == 0) {
          g2.setColor(Color.BLACK)
          g2.drawLine(e._1._1, BoxHeightPx, e._1._1, BoxHeightPx + 5)
          val label = LocalDateTime
            .ofInstant(Instant.ofEpochMilli(e._1._3.toEpochSecond(ZoneOffset.UTC) * 1000L), zoneId)
            .format(formatter)
          g2.drawString(label, e._1._1, BoxHeightPx * 2)
        }

      })
  }


}
