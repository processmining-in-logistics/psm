package org.processmining.scala.viewers.spectrum.view

import java.io.File

import org.slf4j.LoggerFactory;
import org.ini4j.{Ini, IniPreferences}
import org.processmining.scala.log.common.csv.common.CsvImportHelper
import org.processmining.scala.viewers.spectrum.view.AppSettings.logger

case class AppSettings(paletteId: Int, customThickVerticalGridDates: Set[Long], customThinVerticalGridDates: Set[Long]) {
  logger.info(s"paletteId = $paletteId")
  logger.info(s"customThickVerticalGridDates = $customThickVerticalGridDates")
  logger.info(s"customThinVerticalGridDates = $customThinVerticalGridDates")
}

object AppSettings {
  private val logger = LoggerFactory.getLogger(classOf[AppSettings].getName)

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
        val zoneIdString = generalNode.get("zoneIdString", "UTC")
        val csvImportHelper = new CsvImportHelper(dateFormat, zoneIdString)
        val customThickVerticalGridDates= getDates(generalNode.get("customThickVerticalGridDates", ""), csvImportHelper)
        val customThinVerticalGridDates= getDates(generalNode.get("customThinVerticalGridDates", ""), csvImportHelper)

        new AppSettings(generalNode.getInt("paletteId", 3), customThickVerticalGridDates, customThinVerticalGridDates)
      } else {
        logger.info(s"No $filename found")
        AppSettings()
      }
    }
    catch {
      case e: Throwable => logger.warn(e.toString)
        AppSettings()
    }
  }

  def apply() = new AppSettings(3, Set(), Set())
}