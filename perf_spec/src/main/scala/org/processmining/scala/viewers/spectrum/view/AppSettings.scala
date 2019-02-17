package org.processmining.scala.viewers.spectrum.view

import java.io.File
import java.time.ZoneId

import org.ini4j.{Ini, IniPreferences}
import org.processmining.scala.log.utils.common.errorhandling.EH
import org.processmining.scala.log.utils.csv.common.CsvImportHelper
import org.processmining.scala.viewers.spectrum.view.AppSettings.logger
import org.slf4j.LoggerFactory

case class AppSettings(paletteId: Int,
                       customThickVerticalGridDates: Set[Long],
                       customThinVerticalGridDates: Set[Long],
                       zoneId: ZoneId,
                       fontSize: Int,
                       fontName: String) {
  logger.info(s"paletteId = $paletteId")
  logger.info(s"customThickVerticalGridDates = $customThickVerticalGridDates")
  logger.info(s"customThinVerticalGridDates = $customThinVerticalGridDates")
  logger.info(s"zoneId = $zoneId")
  logger.info(s"fontSize = $fontSize")
  logger.info(s"fontName = $fontName")
}

object AppSettings {
  private val logger = LoggerFactory.getLogger(classOf[AppSettings].getName)
  private val systemZone: ZoneId = ZoneId.systemDefault
  private val DefaultFontSize = 20
  private val DefaultFontName = "Gill Sans MT Condensed"

  private def getDates(line: String, csvImportHelper: CsvImportHelper): Set[Long] =
    line.split("&")
      .filter(!_.isEmpty)
      .map(csvImportHelper.extractTimestamp(_))
      .toSet

  def apply(filename: String): AppSettings = {
    try {
      val aggregatorFile = new File(filename)
      if (aggregatorFile.exists() && aggregatorFile.isFile) {
        val iniPrefs = new IniPreferences(new Ini(new File(filename)))
        val generalNode = iniPrefs.node("GENERAL")
        val dateFormat = generalNode.get("dateFormat", "dd-MM-yyyy HH:mm:ss")
        val zoneIdString = generalNode.get("zoneId", systemZone.getId)
        val zoneId = ZoneId.of(zoneIdString)
        val fontSize = generalNode.getInt("fontSize", DefaultFontSize)
        val fontName = generalNode.get("fontName", DefaultFontName)
        val csvImportHelper = new CsvImportHelper(dateFormat, zoneId.getId)
        val customThickVerticalGridDates = getDates(generalNode.get("customThickVerticalGridDates", ""), csvImportHelper)
        val customThinVerticalGridDates = getDates(generalNode.get("customThinVerticalGridDates", ""), csvImportHelper)
        new AppSettings(generalNode.getInt("paletteId", 3), customThickVerticalGridDates, customThinVerticalGridDates,
          zoneId, fontSize, fontName)
      } else {
        logger.info(s"No $filename found")
        AppSettings()
      }
    }
    catch {
      case e: Throwable => EH().warnAndMessageBox(s"Error in '$filename'", e)
        AppSettings()
    }
  }

  def apply() = new AppSettings(3, Set(), Set(), systemZone, DefaultFontSize, DefaultFontName)
}