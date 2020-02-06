package org.processmining.scala.log.utils.common.csv.common

import java.util.regex.Pattern

object CsvReaderHelper {

  val EmptySep = ("", "")
  private val NameCharacter = "[-a-zA-Z0-9_]"
  private val SepCharacter = "[,;|]"
  private val WrapCharacter = "[\"']"

  private def detect(header: String, regex: String): String = {
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(header)
    if (matcher.find()) header.substring(matcher.start(1), matcher.end(1)) else ""
  }

  def detectSep(header: String): (String, String) = {
    val sepWithWrap = detect(header, s"${CsvReaderHelper.NameCharacter}(${CsvReaderHelper.WrapCharacter}${CsvReaderHelper.SepCharacter}${CsvReaderHelper.WrapCharacter})${CsvReaderHelper.NameCharacter}")
    val sepWithoutWrap = detect(header, s"${CsvReaderHelper.NameCharacter}(${CsvReaderHelper.SepCharacter})${CsvReaderHelper.NameCharacter}")
    val sep = if (!sepWithWrap.isEmpty) sepWithWrap else if (!sepWithoutWrap.isEmpty) sepWithoutWrap
    else throw new IllegalArgumentException(s"Cannot detect a separator from '${CsvReaderHelper.SepCharacter}' in '$header', please provide it manually")
    val wrap = if (sep.length > 1) sep.substring(0, 1) else ""
    (sep.replace("|", "\\|"), wrap)
  }
}
