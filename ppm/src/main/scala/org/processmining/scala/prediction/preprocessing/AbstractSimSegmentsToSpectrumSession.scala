package org.processmining.scala.prediction.preprocessing

import java.io.File
import java.time.Duration
import org.processmining.scala.log.common.enhancment.segments.common.{AbstractDurationClassifier, NormalSlowVerySlowDurationClassifier, SpectrumFileNames}
import org.processmining.scala.log.utils.csv.common.{CsvExportHelper, CsvImportHelper}
import org.slf4j.LoggerFactory


abstract class AbstractSimSegmentsToSpectrumSession {
  protected val logger = LoggerFactory.getLogger(classOf[AbstractSimSegmentsToSpectrumSession])

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

  private def filenameToSegment(filename: String): String =
    SpectrumFileNames.fileNameToSegmentName(filename
      .substring(filename.indexOf(".csv.") + 5, filename.indexOf(".seg")))


  def run() = {
    logger.info(s"Segments path='$SegmentsPath'")
    logger.info(s"Spectrum path='$SpectrumRoot'")

    val dir = new File(SegmentsPath)
    val files = dir
      .listFiles()
      .filter(_.getName.toLowerCase.endsWith(".seg"))
      .map(_.getPath)
      .toList
    SegmentsToSpectrum.start(files, filenameToSegment, startTimeMs, twSizeMs, twCount, SpectrumRoot, classifier)

  }


}

