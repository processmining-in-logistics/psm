package org.processmining.scala.viewers.spectrum.view

import java.time.Duration

object TimeWindowTranslator {

  // s "10d 1h 5m"
  def apply(s: String): Long = {
    s
      .trim
      .split("\\s+")
      .map(_.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")) //https://stackoverflow.com/questions/8270784/how-to-split-a-string-between-letters-and-digits-or-between-digits-and-letters
      .map(toMs)
      .sum
  }

  private def toMs(a: Array[String]): Long = {
    if (a.length == 2) {
      val x = a(1) match {
        case "y" => Duration.ofDays(365).toMillis
        case "mo" => Duration.ofDays(30).toMillis
        case "w" => Duration.ofDays(7).toMillis
        case "d" => Duration.ofDays(1).toMillis
        case "h" => Duration.ofHours(1).toMillis
        case "m" => Duration.ofMinutes(1).toMillis
        case "s" => Duration.ofSeconds(1).toMillis
        case "ms" => 1
      }
      a(0).toLong * x
    } else throw new IllegalArgumentException(s"Cannot parse '${a(0)}${a(1)}'")
  }

}

