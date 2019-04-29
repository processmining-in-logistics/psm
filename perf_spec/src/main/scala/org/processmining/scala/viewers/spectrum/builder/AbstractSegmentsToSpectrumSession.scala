package org.processmining.scala.viewers.spectrum.builder

import java.io.File
import java.time.Duration

import org.processmining.scala.log.common.enhancment.segments.common.{AbstractDurationClassifier, NormalSlowVerySlowDurationClassifier, SpectrumFileNames}
import org.processmining.scala.log.utils.common.csv.common.{CsvExportHelper, CsvImportHelper}
import org.slf4j.LoggerFactory


abstract class AbstractSegmentsToSpectrumSession {
  protected val logger = LoggerFactory.getLogger(classOf[AbstractSegmentsToSpectrumSession])

  def SegmentsPath: String

  def SpectrumRoot: String

  def DatasetSizeDays: Int

  def dateHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)

  def startTime: String

  lazy val startTimeMs = dateHelper.extractTimestamp(startTime)

  def twSizeMs: Int

  lazy val durationMs = Duration.ofDays(DatasetSizeDays).toMillis
  lazy val twCount = (durationMs / twSizeMs).toInt

  def classifier: AbstractDurationClassifier = new NormalSlowVerySlowDurationClassifier


  def run() = {
    logger.info(s"Segments path='$SegmentsPath'")
    logger.info(s"Spectrum path='$SpectrumRoot'")

    val dir = new File(SegmentsPath)
    val files = dir
      .listFiles()
      .filter(_.getName.toLowerCase.endsWith(".seg"))
      .map(_.getPath)
      .toList
    SegmentsToSpectrum.start(files, AbstractSegmentsToSpectrumSession.filenameToSegment, startTimeMs, twSizeMs, twCount, SpectrumRoot, classifier)

  }


}

object AbstractSegmentsToSpectrumSession {
  def filenameToSegment(filename: String): String = {

    val csvIndex = filename.toLowerCase.indexOf(".csv.")
    val xesIndex = filename.toLowerCase.indexOf(".xes.")
    val index = if(xesIndex >=0 ) xesIndex else csvIndex
    SpectrumFileNames.fileNameToSegmentName(filename
      .substring(index + 5, filename.indexOf(".seg")))

  }

}