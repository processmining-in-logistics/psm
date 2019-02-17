package org.processmining.scala.prediction.preprocessing

import java.io.{ByteArrayOutputStream, File, PrintWriter}
import java.util.concurrent.{Callable, Executors, TimeUnit}

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import org.apache.commons.math3.stat.descriptive.{DescriptiveStatistics, SummaryStatistics}
import org.processmining.scala.log.common.csv.parallel.CsvReader
import org.processmining.scala.log.common.enhancment.segments.common._
import org.processmining.scala.log.common.enhancment.segments.parallel.SegmentProcessor
import org.processmining.scala.log.utils.csv.common.{CsvExportHelper, CsvImportHelper}
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.FileOutputStream

case class SegmentDescriptiveStatisticsEntry(
                                              n: Long,
                                              median: Double,
                                              mean: Double,
                                              stdev: Double,
                                              kurtosis: Double,
                                              skewness: Double,
                                              min: Double,
                                              max: Double,
                                              q25: Double,
                                              q75: Double) extends Serializable


class SegmentsToSpectrum(
                          segmentFiles: List[String],
                          segmentName: String,
                          startTimeMs: Long,
                          twSizeMs: Long,
                          twCount: Int,
                          spectrumRootDir: String,
                          durationClassifier: AbstractDurationClassifier
                        ) extends Callable[(String, Array[Int])] {

  protected val logger = LoggerFactory.getLogger(classOf[SegmentsToSpectrum])
  private val fileNames = SpectrumFileNames(spectrumRootDir)
  private val kryo = KryoFactory()


  private def readSegmentFile(filename: String): List[SegmentImpl] = {
    val csvReader = new CsvReader()
    val importCsvHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)
    val (_, lines) = csvReader.read(filename)

    def factory(a: Array[String]) = SegmentImpl(a(0),
      importCsvHelper.extractTimestamp(a(1)),
      a(3).toLong,
      a(4),
      -1)

    lines
      .map(factory)
      .toList
  }

  private def getDs(segments: List[SegmentImpl]): SegmentDescriptiveStatisticsEntry = {
    val ds = new DescriptiveStatistics()
    segments.foreach(x => ds.addValue(x.duration))
    SegmentDescriptiveStatisticsEntry(ds.getN, ds.getPercentile(50), ds.getMean, ds.getStandardDeviation, ds.getKurtosis, ds.getSkewness, ds.getMin, ds.getMax, ds.getPercentile(25), ds.getPercentile(75))
  }

  private def getTwInterval(timeMs: Long, duration: Long): (Int, Int) =
    (((timeMs - startTimeMs) / twSizeMs).toInt,
      ((timeMs + duration - startTimeMs) / twSizeMs).toInt)


  private def updateSpectrum(spectrum: Map[Int, (List[SegmentImpl], List[SegmentImpl], List[SegmentImpl])],
                             twIndex: Int,
                             update: ((List[SegmentImpl], List[SegmentImpl], List[SegmentImpl])) => (List[SegmentImpl], List[SegmentImpl], List[SegmentImpl])
                            ) = {
    val optional = spectrum.get(twIndex)
    val triple = update(if (optional.isDefined) optional.get else (List(), List(), List()))
    spectrum + (twIndex -> triple)
  }

  override def call(): (String, Array[Int]) = {
    logger.info(s"Task for '$segmentName' started...")
    new File(fileNames.getDataDir(segmentName)).mkdir()
    new File(fileNames.getStartedDir(segmentName)).mkdir()
    val segments = segmentFiles
      .flatMap(readSegmentFile)
      .sortBy(_.timeMs) // files can be sorted to avoid this sorting
    val dse = getDs(segments)
    val classifiedSegments = segments.map(x => x.copy(clazz = durationClassifier.classify(x.duration, dse.q25, dse.median, dse.q75)))
    // triple: aggregation for start, intersect, stop
    val initialPs: Map[Int, (List[SegmentImpl], List[SegmentImpl], List[SegmentImpl])] = Map()
    val sparsePs = classifiedSegments.foldLeft(initialPs)((ps1, s) => {
      val (tw1, tw2): (Int, Int) = getTwInterval(s.timeMs, s.duration)
      val ps2 = updateSpectrum(ps1, tw1, t => t.copy(_1 = s :: t._1))
      val ps3 = updateSpectrum(ps2, tw2, t => t.copy(_3 = s :: t._3))
      val twIntersects = ((tw1 + 1) until tw2).map(x => x)
      twIntersects.foldLeft(ps3)((ps, twIndexIntersection) =>
        updateSpectrum(ps, twIndexIntersection, t => t.copy(_2 = s :: t._2))
      )
    }
    ).filter(x => x._1 >= 0 && x._1 < twCount)
    logger.info(s"Task for '$segmentName': exporting...")
    exportData(sparsePs)
    exportStarted(sparsePs)
    // start intersect stop sum
    val ss = Array(new SummaryStatistics(), new SummaryStatistics(), new SummaryStatistics(), new SummaryStatistics())
    sparsePs.foreach(x => {
      ss(0).addValue(x._2._1.length)
      ss(1).addValue(x._2._2.length)
      ss(2).addValue(x._2._3.length)
      ss(3).addValue(x._2._1.length + x._2._2.length + x._2._3.length)
    })
    logger.info(s"Task for '$segmentName' completed.")
    (segmentName, ss.map(_.getMax.toInt))
  }

  private def aggregate(segments: List[SegmentImpl]): Map[Int, Int] =
    segments
      .groupBy(_.clazz)
      .map(x => (x._1, x._2.length))

  private def getCount(v: Option[Int]): Int = if (v.isDefined) v.get else 0


  private def exportData(ps: Map[Int, (List[SegmentImpl], List[SegmentImpl], List[SegmentImpl])]): Unit = {

    val dataOut = new BufferedOutputStream(new FileOutputStream(fileNames.getDataFileName(segmentName)))
    var index: Map[Int, Int] = Map()
    var offset = 0
    ps.foreach(x => {
      val tw = x._1
      val startMap = aggregate(x._2._1)
      val intersectMap = aggregate(x._2._2)
      val stopMap = aggregate(x._2._3)
      val list = (0 until durationClassifier.classCount)
        .map(clazz => (clazz, getCount(startMap.get(clazz)), getCount(intersectMap.get(clazz)), getCount(stopMap.get(clazz))))
        .filter(x => x._2 != 0 || x._3 != 0 || x._4 != 0)
        .map(x => BinsImpl(x._1, x._2, x._3, x._4))

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

  private def exportStarted(ps: Map[Int, (List[SegmentImpl], List[SegmentImpl], List[SegmentImpl])]): Unit = {
    val dataOut = new BufferedOutputStream(new FileOutputStream(fileNames.getStartedFileName(segmentName)))
    var index: Map[Int, Int] = Map()
    var sumData = 0
    var offset = 0
    ps
      .map(x => (x._1, x._2._1))
      .foreach(x => {
        val tw = x._1
        val started = x._2
        val byteOutputStream = new ByteArrayOutputStream()
        val output = new Output(byteOutputStream)
        kryo.writeObject(output, ListOfSegments(started))

        output.close()
        val ba = byteOutputStream.toByteArray

        byteOutputStream.close

        index = index + (tw -> offset)
        offset = offset + ba.length
        if(ba.length == 0){
          logger.info(started.toString())
        }
        dataOut.write(ba)
        sumData += started.length
      })
    if(sumData == 0){
      logger.debug("")
    }
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
}

object SegmentsToSpectrum {
  private val logger = LoggerFactory.getLogger(SegmentsToSpectrum.getClass)


  def mergeTmp(dirs: Seq[String], header: String, filename: String, removeTmp: Boolean) = {
    dirs.par.foreach(dir => {
      val dirFile = new File(dir)
      if (dirFile.exists() && dirFile.isDirectory) {
        val tmpFiles = dirFile
          .listFiles()
          .filter(_.getPath.endsWith(".tmp"))
        if (tmpFiles.nonEmpty) {
          val pw = new PrintWriter(s"$dirFile/$filename")
          pw.print(header)
          pw.print('\n')
          val allLines = tmpFiles.flatMap(tmpFile => {
            val source = scala.io.Source.fromFile(tmpFile)
            val lines = source.getLines().toList
            source.close()
            if (removeTmp) tmpFile.delete()
            lines
              .map(x => {
                val x1 = if (x.endsWith("\r\n")) x.substring(0, x.length - 2) else x
                if (x1.endsWith("\n")) x1.substring(0, x1.length - 1) else x1
              })
          }).mkString("\n")
          pw.print(allLines)
          pw.close()
        }
      }
    })
  }

  def start(filenames: List[String],
            filenameToSegment: String => String,
            startTimeMs: Long,
            twSizeMs: Long,
            twCount: Int,
            spectrumRootDir: String,
            durationClassifier: AbstractDurationClassifier) = {


    new File(SpectrumFileNames.getAggregatedDir(spectrumRootDir)).mkdirs()
    new File(SpectrumFileNames.getStartedDir(spectrumRootDir)).mkdirs()

    val groupsTmp = filenames.map(x => (filenameToSegment(x), x))
      .groupBy(_._1)

    //val groups = Map("Link1S_0:E1.TO_SCAN_1_0" -> groupsTmp("Link1S_0:E1.TO_SCAN_1_0"))
    val groups = groupsTmp

    val poolSize = Math.min(Runtime.getRuntime.availableProcessors() * 2, groups.size)
    logger.info(s"poolSize=$poolSize")
    val executorService = Executors.newFixedThreadPool(poolSize)
    val futures = groups.map(x => {
      val task = new SegmentsToSpectrum(x._2.map(_._2), x._1, startTimeMs, twSizeMs, twCount, spectrumRootDir, durationClassifier)
      executorService.submit(task)
    })
    logger.info("All tasks are submitted.")
    futures
      .zipWithIndex
      .foreach(f => {
        val (segment, maxPerBinStartIntersectEndSum) = f._1.get()
        //val maxPerBinEver = maxPerBin.max
        val pw = new PrintWriter(s"$spectrumRootDir/${f._2}.tmp")
        pw.println(s""""$segment";"${maxPerBinStartIntersectEndSum(0)}";"${maxPerBinStartIntersectEndSum(1)}";"${maxPerBinStartIntersectEndSum(2)}";"${maxPerBinStartIntersectEndSum(3)}"""")
        pw.close()
      })
    logger.info("All tasks are done.")
    executorService.shutdown()
    while (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {}
    logger.info("Thread pool is terminated.")
    logger.info("Merging tmp files...")
//    val dataDirs = (0 until twCount).map(x => s"$spectrumRootDir/data/$x")
//    val segDirs = (0 until twCount).map(x => s"$spectrumRootDir/segments/$x")
    val statDir = List(spectrumRootDir)
//    mergeTmp(dataDirs, """"index";"key";"clazz";"count";"countIntersect";"countStop"""", SegmentProcessor.ProcessedDataFileName, true)
//    mergeTmp(segDirs, """"id";"key";"timestamp";"duration";"clazz"""", SegmentProcessor.SegmentsFileName, true)
    mergeTmp(statDir, """"key";"max";"maxIntersect";"maxStop";"maxSum"""", SegmentProcessor.StatDataFileName, true)
    val session = PreprocessingSession(startTimeMs, startTimeMs + twCount * twSizeMs, twSizeMs, durationClassifier.classCount, "", "")
      .copy(legend = durationClassifier.legend)
    PreprocessingSession.toDisk(session, s"$spectrumRootDir/${SegmentProcessor.SessionFileName}", true)
  }
}
