package org.processmining.scala.viewers.spectrum.builder

import java.io.File
import java.util.concurrent.Callable

import org.deckfour.xes.model.XLog
import org.processmining.scala.log.common.enhancment.segments.common._
import org.processmining.scala.log.common.xes.parallel.XesReader
import org.slf4j.LoggerFactory

class PreProcessor(filename: String,
                   originalXLog: XLog,
                   sep: String,
                   activityClassifier: Array[String],
                   spectrumDirName: String,
                   twSize: Long,
                   //aggregationFunctionCode: Int,
                   durationClassifierCode: Int,
                   handler: Runnable) extends Callable[String] {
  private val logger = LoggerFactory.getLogger(classOf[PreProcessor].getName)

  private def getNonCsvLog() = {
    val xLog = if (filename.nonEmpty) {
      val logs = XesReader.readXes(new File(filename))
      if (logs.isEmpty) throw new RuntimeException(s"XES file '$filename' is empty") else logs.head
    } else originalXLog
    XesReader.read(Seq(xLog), None, None, XesReader.DefaultTraceAttrNamePrefix, sep, activityClassifier: _*).head
  }

  override def call(): String = {
    val spectrumDir = new File(spectrumDirName)
    spectrumDir.mkdirs()
    val (startTimeMs: Long, endTimeMs: Long) =
      if (filename.toLowerCase.endsWith(".csvdir"))
        Logs2Segments.startNoPreprocessing(filename, spectrumDirName, false)
      else {
        val unifiedEventLog = getNonCsvLog
        Logs2Segments.startNoPreprocessing(unifiedEventLog, spectrumDirName, if (filename.nonEmpty) filename else "log.xes")
        unifiedEventLog.minMaxTimestamp()
      }
    val twCount = ((endTimeMs - startTimeMs) / twSize).toInt
    handler.run()
    val files = spectrumDir
      .listFiles()
      .filter(_.getName.toLowerCase.endsWith(".seg"))
      .map(_.getPath)
      .toList
    SegmentsToSpectrum.start(files, AbstractSegmentsToSpectrumSession.filenameToSegment, startTimeMs, twSize, twCount, spectrumDirName, PreProcessor.durationClassifier(durationClassifierCode))
    spectrumDirName
  }
}


object PreProcessor {
  val InventoryAggregationCode = 0
  val StartAggregationCode = 1
  val EndAggregationCode = 2

  val Q4DurationClassifierCode = 0
  val FasterNormal23VerySlowDurationClassifierCode = 1


  @deprecated
  def aggregationFunction(code: Int): AbstractAggregationFunction =
    code match {
      case InventoryAggregationCode => InventoryAggregation
      case StartAggregationCode => StartAggregation
      case EndAggregationCode => EndAggregation
      case _ => throw new RuntimeException(s"Wrong aggregation code $code")
    }

  def durationClassifier(code: Int): AbstractDurationClassifier =
    code match {
      case Q4DurationClassifierCode => new Q4DurationClassifier
      case FasterNormal23VerySlowDurationClassifierCode => new FasterNormal23VerySlowDurationClassifier
      case _ => throw new RuntimeException(s"Wrong classifier code $code")
    }
}