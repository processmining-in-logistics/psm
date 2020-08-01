package org.processmining.scala.viewers.spectrum.builder

import java.io.File
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.time.ZoneId
import java.util.concurrent.Callable
import org.deckfour.xes.model.XLog
import org.ini4j.{Ini, IniPreferences}
import org.processmining.scala.log.common.enhancment.segments.common._
import org.processmining.scala.log.common.xes.parallel.XesReader
import org.processmining.scala.log.utils.common.csv.common.{CsvExportHelper, CsvImportHelper}
import org.processmining.scala.viewers.spectrum.view.FramePanel
import org.slf4j.LoggerFactory

class PreProcessor(filename: String,
                   originalXLog: XLog,
                   sep: String,
                   activityClassifier: Array[String],
                   spectrumDirName: String,
                   twSize: Long,
                   durationClassifier: AbstractDurationClassifier,
                   handler: Runnable) extends Callable[String] {
  private val logger = LoggerFactory.getLogger(classOf[PreProcessor].getName)

  private def getNonCsvLog() = {
    val xLog = if (filename.nonEmpty) {
      val logs = XesReader.readXes(new File(filename))
      if (logs.isEmpty) throw new RuntimeException(s"XES file '$filename' is empty") else logs.head
    } else originalXLog
    XesReader.read(Seq(xLog), None, None, XesReader.DefaultTraceAttrNamePrefix, sep, activityClassifier: _*).head
  }

  private def getSegCsv(filename: String, defaultClassifier: AbstractDurationClassifier) = {
    val iniPrefs = new IniPreferences(new Ini(new File(filename)))
    val generalNode = iniPrefs.node("GENERAL")
    val dateFormat = generalNode.get("dateFormat", CsvExportHelper.FullTimestampPattern)
    val zoneIdString = generalNode.get("zoneId", ZoneId.systemDefault.getId)
    val importCsvHelper = new CsvImportHelper(dateFormat, zoneIdString)
    val startTime = importCsvHelper.extractTimestamp(generalNode.get("startTime", ""))
    val endTime = importCsvHelper.extractTimestamp(generalNode.get("endTime", ""))
    val name = generalNode.get("name", "No name")
    val legend = generalNode.get("legend", "")
    val classCount = generalNode.getInt("classCount", 0)
    (startTime, endTime, if(classCount ==0) defaultClassifier else SegmentDirClassifier(legend, classCount, name))
  }

  override def call(): String = {
    val spectrumDir = new File(spectrumDirName)
    //logger.info(s"Custom classifier name = '$customClassifierName'")
    spectrumDir.mkdirs()
    val (startTimeMs: Long, endTimeMs: Long, classifier: AbstractDurationClassifier) =
      if (filename.toLowerCase.endsWith("." + FramePanel.CSV_DIR)) {
        val ret = Logs2Segments.startNoPreprocessing(filename, spectrumDirName, false)
        (ret._1, ret._2, durationClassifier)
      }
      else if (filename.toLowerCase.endsWith("." + FramePanel.SEG_DIR)) {
        val ret = getSegCsv(filename, durationClassifier)
        new File(new File(filename).getParent)
          .listFiles()
          .filter(_.getName.toLowerCase.endsWith(".seg"))
          .foreach(x => {
            val name = x.getName
            Files.copy(Paths.get(x.toString), Paths.get(s"$spectrumDirName/$name"), StandardCopyOption.REPLACE_EXISTING)
          })
        ret
      }
      else {
        val unifiedEventLog = getNonCsvLog
        Logs2Segments.startNoPreprocessing(unifiedEventLog, spectrumDirName, if (filename.nonEmpty) filename else "log.xes")
        val ret = unifiedEventLog.minMaxTimestamp()
        (ret._1, ret._2, durationClassifier)
      }
    val twCount = ((endTimeMs - startTimeMs) / twSize).toInt + 1
    handler.run()
    val files = spectrumDir
      .listFiles()
      .filter(_.getName.toLowerCase.endsWith(s".${AbstractSegmentsToSpectrumSession.SegmentsExtension}"))
      .map(_.getPath)
      .toList
    SegmentsToSpectrum.start(files,
      AbstractSegmentsToSpectrumSession.filenameToSegment,
      startTimeMs,
      twSize,
      twCount,
      spectrumDirName,
      classifier)
    spectrumDirName
  }
}


object PreProcessor {
  val InventoryAggregationCode = 0
  val StartAggregationCode = 1
  val EndAggregationCode = 2

  val Q4DurationClassifierCode = 0
  val FasterNormal23VerySlowDurationClassifierCode = 1
  val CustomClassifier = -1

  @deprecated
  def aggregationFunction(code: Int): AbstractAggregationFunction =
    code match {
      case InventoryAggregationCode => InventoryAggregation
      case StartAggregationCode => StartAggregation
      case EndAggregationCode => EndAggregation
      case _ => throw new RuntimeException(s"Wrong aggregation code $code")
    }

  def createClassifier(code: Int, className: String, path: String): AbstractDurationClassifier =
    code match {
      case Q4DurationClassifierCode => new Q4DurationClassifier
      case FasterNormal23VerySlowDurationClassifierCode => new FasterNormal23VerySlowDurationClassifier
      case CustomClassifier => AbstractDurationClassifier.apply(className, path)
      case _ => throw new RuntimeException(s"Wrong classifier code $code")
    }
}