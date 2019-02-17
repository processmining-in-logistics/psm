package org.processmining.scala.sim.conveyors.experiments

import java.io._
import java.util.Locale

import com.esotericsoftware.kryo.io.Output
import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.common.csv.common.CsvReader
import org.processmining.scala.log.common.enhancment.segments.common._
import org.processmining.scala.log.common.enhancment.segments.parallel.SegmentProcessor
import org.processmining.scala.log.utils.common.errorhandling.JvmParams
import org.processmining.scala.log.utils.csv.common.{CsvExportHelper, CsvImportHelper}
import org.slf4j.LoggerFactory

import scala.io.Source

object OutputToPsFormat {
  private val logger = LoggerFactory.getLogger(OutputToPsFormat.getClass)
  val SegmentYTest = "1.Real data:"
  val SegmentYOutput = "2.Predicted data:"
  val SegmentYBaseline = "3.Baseline:"
  val SegmentYBaseline2 = "4.Baseline:"

  private val dir = "G:\\e1\\Scan1NonLinear"
  private val SpectrumRoot = "G:\\T3_v8\\Scan1NonLinear"

  private val importCsvHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)
  private val yTestFilename = s"$dir/y_test.csv"
  private val outputFilename = s"$dir/outputs.csv"
  private val baselineFilename = s"$dir/baseline.csv"
  private val baseline2Filename = s"$dir/baseline2.csv"
  private val MaxFilename = s"$dir/max.csv"

  private val csvReader = new CsvReader((",", ""))

  private val classCount = 1
  private val twSizeMs: Long = 2*20*1000
  private val startTimeMs = importCsvHelper.extractTimestamp("01-09-2018 00:00:00.000")
  private val fileNames = SpectrumFileNames(SpectrumRoot)
  private val kryo = KryoFactory()

  private lazy val norm: Int = Source.fromFile(MaxFilename).getLines.toList.head.toInt

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

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, "SimSegmentsToSpectrumStarter started")
    logger.info(s"yTest='$yTestFilename'")
    logger.info(s"output='$outputFilename'")
    logger.info(s"baseline='$baselineFilename'")
    logger.info(s"maxFilename='$MaxFilename'")
    logger.info(s"max=$norm")
    logger.info(s"Spectrum path='$SpectrumRoot'")
    try {
      Locale.setDefault(Locale.US)
      new File(SpectrumFileNames.getAggregatedDir(SpectrumRoot)).mkdirs()
      val yTest = readLabels(yTestFilename, classCount)
      val yOutput = readLabels(outputFilename, classCount)
      val yBaseline = readLabels(baselineFilename, classCount)


      if (yTest.length != yOutput.length) throw new IllegalArgumentException(s"yTest.length != yOutput.length: ${yTest.length}!=${yOutput.length}")
      if (yTest.length != yBaseline.length) throw new IllegalArgumentException(s"yTest.length != yBaseline.length: ${yTest.length}!=${yBaseline.length}")





      writeSegment(SegmentYTest, yTest)
      exportStarted(SegmentYTest)


      writeSegment(SegmentYOutput, yOutput)
      exportStarted(SegmentYOutput)

      writeSegment(SegmentYBaseline, yBaseline)
      exportStarted(SegmentYBaseline)

      val header = """"key";"max";"maxIntersect";"maxStop";"maxSum""""
      val pw = new PrintWriter(s"$SpectrumRoot/max.csv")
      pw.println(header)
      pw.println(s""""$SegmentYTest";"$norm";"$norm";"$norm";"$norm"""")
      pw.println(s""""$SegmentYOutput";"$norm";"$norm";"$norm";"$norm"""")
      pw.println(s""""$SegmentYBaseline";"$norm";"$norm";"$norm";"$norm"""")

//      val yBaseline2 = readLabels(baseline2Filename, classCount, 1)
//      writeSegment(SegmentYBaseline2, yBaseline2)
//      exportStarted(SegmentYBaseline2)
//      pw.println(s""""$SegmentYBaseline2";"$norm";"$norm";"$norm";"$norm"""")


      pw.close()
      val session = PreprocessingSession(startTimeMs, startTimeMs + yTest.length * twSizeMs, twSizeMs, classCount, "", "")
        .copy(legend = (new NormalSlowVerySlowDurationClassifier).legend)
      PreprocessingSession.toDisk(session, s"$SpectrumRoot/${SegmentProcessor.SessionFileName}", true)
    } catch {
      case e: Throwable => logger.error(e.toString)
    }
    logger.info(s"App is completed.")

  }
}
