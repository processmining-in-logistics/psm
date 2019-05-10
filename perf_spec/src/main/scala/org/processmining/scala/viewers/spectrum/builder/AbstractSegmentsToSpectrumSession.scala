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
      .filter(_.getName.toLowerCase.endsWith(s".${AbstractSegmentsToSpectrumSession.SegmentsExtension}"))
      .map(_.getPath)
      .toList
    SegmentsToSpectrum.start(files, AbstractSegmentsToSpectrumSession.filenameToSegment, startTimeMs, twSizeMs, twCount, SpectrumRoot, classifier)

  }
}

object AbstractSegmentsToSpectrumSession {
  val SegmentsExtension = "seg"

  def filenameToSegment(filename: String): String = {
    val csvPattern = ".csv."
    val csvIndex = filename.toLowerCase.indexOf(csvPattern)
    val xesGzPattern = ".xes.gz."
    val xesGzIndex = filename.toLowerCase.indexOf(xesGzPattern)
    val xesPattern = ".xes."
    val xesIndex = filename.toLowerCase.indexOf(xesPattern)
    val (index, length) = if (xesGzIndex >= 0) (xesGzIndex, xesGzPattern.length)
    else if (xesIndex >= 0) (xesIndex, xesPattern.length) else (csvIndex, csvPattern.length)
    if (index < 0) {
      throw new IllegalArgumentException(s"Cannot find parts containing '$xesPattern', '$xesGzPattern' or '$csvPattern' in '$filename'")
    }
    SpectrumFileNames.fileNameToSegmentName(filename
      .substring(index + length, filename.indexOf(s".$SegmentsExtension")))
  }

}