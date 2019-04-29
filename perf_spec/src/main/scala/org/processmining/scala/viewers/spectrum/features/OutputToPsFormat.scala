package org.processmining.scala.viewers.spectrum.features

import java.io._
import java.util.Locale

import com.esotericsoftware.kryo.io.Output
import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.common.csv.common.CsvReader
import org.processmining.scala.log.common.enhancment.segments.common._
import org.processmining.scala.log.common.enhancment.segments.parallel.SegmentProcessor
import org.processmining.scala.log.utils.common.csv.common.{CsvExportHelper, CsvImportHelper}
import org.processmining.scala.log.utils.common.errorhandling.JvmParams
import org.slf4j.LoggerFactory

import scala.io.Source

class OutputToPsFormat(sourceDir: String, spectrumRootDir: String, twSizeMs: Long, startTimeMs: Long) extends Runnable {

  private val yTestFilename = s"$sourceDir/y_test.csv"
  private val outputFilename = s"$sourceDir/outputs.csv"
  private val baselineFilename = s"$sourceDir/baseline.csv"
  private val maxFilename = s"$sourceDir/max.csv"
  private val fileNames = SpectrumFileNames(spectrumRootDir)
  private val kryo = KryoFactory()
  private val csvReader = new CsvReader((",", ""))
  private val classCount = 1
  private lazy val norm: Int = Source.fromFile(maxFilename).getLines.toList.head.toInt

  private def readLabels(filename: String, nColumns: Int, n: Int): Seq[Seq[BinsImpl]] = {
    def factory(n: Int)(row: Array[String]): Seq[Double] =
      (0 until n).map(row(_).toDouble)

    val (_, lines) = csvReader.read(filename)
    csvReader.parse(lines, factory(nColumns))
      .map(x => x.map(y => if (y < 0) 0.0 else y))
      .map(x => x.zipWithIndex.map(x => BinsImpl(x._2, (x._1 * n).toInt, 0, 0)))
  }

  private def readLabels(filename: String, nColumns: Int): Seq[Seq[BinsImpl]] =
    readLabels(filename, nColumns, norm)


  private def writeSegment(segmentName: String, data: Seq[Seq[BinsImpl]]) = {
    val dir = fileNames.getDataDir(segmentName)
    new File(dir).mkdirs()
    val dataOut = new BufferedOutputStream(new FileOutputStream(fileNames.getDataFileName(segmentName)))
    var index: Map[Int, Int] = Map()
    var offset = 0
    data
      .zipWithIndex
      .foreach(x => {
        val tw = x._2
        val list = x._1
        if (list.nonEmpty) {
          val byteOutputStream = new ByteArrayOutputStream()
          val output = new Output(byteOutputStream)
          kryo.writeObject(output, ListOfBins(list.toList))
          output.close()
          val ba = byteOutputStream.toByteArray
          byteOutputStream.close
          index = index + (tw -> offset)
          offset = offset + ba.length
          dataOut.write(ba)
        }
      })
    dataOut.close()
    val indexOut = new BufferedOutputStream(new FileOutputStream(fileNames.getDataIndexFileName(segmentName)))
    val byteOutputStream = new ByteArrayOutputStream()
    val output = new Output(byteOutputStream)
    kryo.writeObject(output, MapOfInts(index))
    output.close()
    val ba = byteOutputStream.toByteArray
    byteOutputStream.close
    indexOut.write(ba)
    indexOut.close()
  }


  private def exportStarted(segmentName: String): Unit = {
    val dir = fileNames.getStartedDir(segmentName)
    new File(dir).mkdirs()
    val dataOut = new BufferedOutputStream(new FileOutputStream(fileNames.getStartedFileName(segmentName)))
    val index: Map[Int, Int] = Map()
    dataOut.close()
    val indexOut = new BufferedOutputStream(new FileOutputStream(fileNames.getStartedIndexFileName(segmentName)))
    val byteOutputStream = new ByteArrayOutputStream()
    val output = new Output(byteOutputStream)
    kryo.writeObject(output, MapOfInts(index))
    output.close()
    val ba = byteOutputStream.toByteArray
    byteOutputStream.close
    indexOut.write(ba)
    indexOut.close()
  }

  override def run(): Unit = {
    OutputToPsFormat.logger.info(s"yTest='$yTestFilename'")
    OutputToPsFormat.logger.info(s"output='$outputFilename'")
    OutputToPsFormat.logger.info(s"baseline='$baselineFilename'")
    OutputToPsFormat.logger.info(s"maxFilename='$maxFilename'")
    OutputToPsFormat.logger.info(s"max=$norm")
    OutputToPsFormat.logger.info(s"Spectrum path='$spectrumRootDir'")
    new File(SpectrumFileNames.getAggregatedDir(spectrumRootDir)).mkdirs()
    val yTest = readLabels(yTestFilename, classCount)
    val yOutput = readLabels(outputFilename, classCount)
    val yBaseline = readLabels(baselineFilename, classCount)
    if (yTest.length != yOutput.length) throw new IllegalArgumentException(s"yTest.length != yOutput.length: ${yTest.length}!=${yOutput.length}")
    if (yTest.length != yBaseline.length) throw new IllegalArgumentException(s"yTest.length != yBaseline.length: ${yTest.length}!=${yBaseline.length}")
    writeSegment(OutputToPsFormat.SegmentYTest, yTest)
    exportStarted(OutputToPsFormat.SegmentYTest)
    writeSegment(OutputToPsFormat.SegmentYOutput, yOutput)
    exportStarted(OutputToPsFormat.SegmentYOutput)
    writeSegment(OutputToPsFormat.SegmentYBaseline, yBaseline)
    exportStarted(OutputToPsFormat.SegmentYBaseline)
    val header = """"key";"max";"maxIntersect";"maxStop";"maxSum""""
    val pw = new PrintWriter(s"$spectrumRootDir/max.csv")
    pw.println(header)
    pw.println(s""""${OutputToPsFormat.SegmentYTest}";"$norm";"$norm";"$norm";"$norm"""")
    pw.println(s""""${OutputToPsFormat.SegmentYOutput}";"$norm";"$norm";"$norm";"$norm"""")
    pw.println(s""""${OutputToPsFormat.SegmentYBaseline}";"$norm";"$norm";"$norm";"$norm"""")
    pw.close()
    val session = PreprocessingSession(startTimeMs, startTimeMs + yTest.length * twSizeMs, twSizeMs, classCount, "", "")
      .copy(legend = (new NormalSlowVerySlowDurationClassifier).legend)
    PreprocessingSession.toDisk(session, s"$spectrumRootDir/${SegmentProcessor.SessionFileName}", true)
  }
}

object OutputToPsFormat {
  private val logger = LoggerFactory.getLogger(OutputToPsFormat.getClass)
  val SegmentYTest = "1.Real data:"
  val SegmentYOutput = "2.Predicted data:"
  val SegmentYBaseline = "3.Baseline:"
  val SegmentYBaseline2 = "4.Baseline:"

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, "OutputToPsFormat started")
    try {
      Locale.setDefault(Locale.US)
      val sourceDir = args(0)
      val spectrumRootDir = args(1)
      val twSizeMs = args(2).toInt
      val importCsvHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)
      val startTimeMs = importCsvHelper.extractTimestamp(args(3))
      new OutputToPsFormat(sourceDir, spectrumRootDir, twSizeMs, startTimeMs).run()

    } catch {
      case e: Throwable => logger.error(e.toString)
    }
    logger.info(s"App is completed.")
  }
}
