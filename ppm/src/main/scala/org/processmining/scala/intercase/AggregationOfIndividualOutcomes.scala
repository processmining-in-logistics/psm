package org.processmining.scala.intercase

import java.io.{File, PrintWriter}
import java.time.Duration
import java.util.concurrent.Callable

import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.common.csv.parallel.CsvReader
import org.processmining.scala.log.utils.common.csv.common.{CsvExportHelper, CsvImportHelper}
import org.processmining.scala.log.utils.common.errorhandling.{EH, JvmParams}
import org.slf4j.LoggerFactory

import scala.io.Source

/**
  * Provides aggregation for individual outcomes (time until the given activity)
  *
  * @param originalTimestamps     end of prefix
  * @param predictedRemainingTime normalized duration
  * @param maxRemainingTime       max duration (used during normalization)
  * @param startDate              start datetime of the binning
  * @param binSizeMs              bin duration in ms
  * @param targetBinsCount        number of target bins
  * @param binCount               bin number in computed aggregation
  * @param predictionHorizonBins  prediction horizon
  */
class AggregationOfIndividualOutcomes(originalTimestamps: List[Long],
                                      predictedRemainingTime: List[Double],
                                      maxRemainingTime: Double,
                                      startDate: Long,
                                      binSizeMs: Long,
                                      targetBinsCount: Int,
                                      binCount: Int,
                                      predictionHorizonBins: Int,
                                      timeZoneShift: Int = 60
                                     ) extends Callable[List[Double]] {


  override def call(): List[Double] = {
    if (originalTimestamps.size != predictedRemainingTime.size) throw new IllegalStateException(s"originalTimestamps.size != predictedOutcome.size: ${originalTimestamps.size} != ${predictedRemainingTime.size}")
    val binning = originalTimestamps
      .zip(predictedRemainingTime.map(_ * maxRemainingTime))
      //.filter(x => x._2 >= binSizeMs * (predictionHorizonBins - 1)) // we do not count 'faster' cases that happen before the predictions horizon
      .map(x => (((x._1 + x._2) - startDate) / binSizeMs).toInt - predictionHorizonBins)
      .map(x => (x + timeZoneShift, 1))
      .groupBy(_._1)
      .map(x => (x._1, x._2.size))

    val arr = (0 until binCount).map { i =>
      val count = binning.get(i)
      if (count.isDefined) count.get else 0
    }.toArray

    val ret = (0 until arr.length - (targetBinsCount - 1))
      .map(i => (i until i + targetBinsCount)
        .map(arr(_)).sum.toDouble / targetBinsCount)
      .toList
    ret ::: (0 until targetBinsCount - 1).map(_ => 0.0).toList
  }
}

class AggregationOfIndividualOutcomesPending(originalTimestamps: List[Long],
                                             predictedRemainingTime: List[Double],
                                             maxRemainingTime: Double,
                                             predictedDuration: List[Double],
                                             maxDuration: Double,
                                             startDate: Long,
                                             binSizeMs: Long,
                                             targetBinsCount: Int,
                                             binCount: Int,
                                             predictionHorizonBins: Int,
                                             timeZoneShift: Int = 60
                                            ) extends Callable[List[Double]] {


  override def call(): List[Double] = {
    if (originalTimestamps.size != predictedRemainingTime.size) throw new IllegalStateException(s"originalTimestamps.size != predictedOutcome.size: ${originalTimestamps.size} != ${predictedRemainingTime.size}")
    val v1 = originalTimestamps
      .zip(predictedRemainingTime.map(_ * maxRemainingTime))
      .zip(predictedDuration.map(_ * maxDuration))
      .map(x => (x._1._1, x._1._2, x._2))
    //.filter(x => x._2 >= binSizeMs * (predictionHorizonBins - 1)) // we do not count 'faster' cases that happen before the predictions horizon

    val binning = v1
      .map(x => (
        (((x._1 + x._2) - startDate) / binSizeMs).toInt - predictionHorizonBins,
        (((x._1 + x._2 + x._3) - startDate) / binSizeMs).toInt - predictionHorizonBins
      ))
      .flatMap(x => (x._1 + 1 until x._2).map(i => i + timeZoneShift))
      .map(x => (x, 1))
      .groupBy(_._1)
      .map(x => (x._1, x._2.size))

    val arr = (0 until binCount).map { i =>
      val count = binning.get(i)
      if (count.isDefined) count.get else 0
    }.toArray

    val ret = (0 until arr.length - (targetBinsCount - 1))
      .map(i => (i until i + targetBinsCount)
        .map(arr(_)).sum.toDouble / targetBinsCount)
      .toList
    ret ::: (0 until targetBinsCount - 1).map(_ => 0.0).toList
  }
}


class AggregationOfIndividualOutcomesExternal(originalTimestamps: List[Long],
                                              predictedRemainingTime: (List[Double], Double),
                                              predictedDuration: Option[(List[Double], Double)],
                                              startDate: Long,
                                              binSizeMs: Long,
                                              targetBinsCount: Int,
                                              predictionHorizonBins: Int,
                                              days: Int,
                                              startOffsetHours: Int,
                                              durationHours: Int
                                             ) extends Callable[List[Double]] {
  val binCount = (Duration.ofDays(days).toMillis / binSizeMs).toInt

  override def call(): List[Double] = {
    val binning = (if (!predictedDuration.isDefined)
      new AggregationOfIndividualOutcomes(originalTimestamps, predictedRemainingTime._1, predictedRemainingTime._2, startDate, binSizeMs, targetBinsCount, binCount, predictionHorizonBins)
    else new AggregationOfIndividualOutcomesPending(originalTimestamps, predictedRemainingTime._1, predictedRemainingTime._2, predictedDuration.get._1, predictedDuration.get._2, startDate, binSizeMs, targetBinsCount, binCount, predictionHorizonBins))
      .call()
    val intervals = (0 until days)
      .map { day =>
        val dayStart = Duration.ofDays(day).toMillis
        (dayStart + Duration.ofHours(startOffsetHours).toMillis, dayStart + Duration.ofHours(startOffsetHours + durationHours).toMillis)
      }
    binning
      .zipWithIndex
      .map(x => (x._1, x._2 * binSizeMs))
      .filter(x => intervals.exists { interval =>
        val binStartMs = x._2
        binStartMs >= interval._1 && (binStartMs + binSizeMs) <= interval._2
      }).map(_._1)
  }


}


object AggregationOfIndividualOutcomesExternal {
  val logger = LoggerFactory.getLogger(AggregationOfIndividualOutcomesExternal.getClass)
  val Root = "G:\\PI1_19\\dde"
  val PsMax = (46.0, 62.0) // start, pending
  val usePending = false
  val Dir = s"$Root"

  def loadTimestamps(filename: String): List[Long] = {
    def factory(timestampIndex: Int, row: Array[String]): Long = row(timestampIndex).toLong

    val csvReader = new CsvReader()
    val lines = csvReader.readNoHeaderSeq(filename)
    csvReader.parseSeq(lines, factory(0, _: Array[String])).toList
  }

  private def loadPredictions(filename: String, column: Int): List[Double] = {
    def factory(predictionIndex: Int, row: Array[String]): Double = row(predictionIndex).toDouble

    val csvReader = new CsvReader(",", ",")
    val lines = csvReader.readNoHeaderSeq(filename)
    csvReader.parseSeq(lines, factory(column, _: Array[String])).toList
  }

  def loadMaxY(filename: String): Long = Source.fromFile(filename).getLines().mkString.toDouble.toLong

  val dateHelper = new CsvImportHelper(CsvExportHelper.ShortTimestampPattern, CsvExportHelper.AmsterdamTimeZone) //to provide time intervals in code


  def exportBins(bins: List[Double], filename: String) = {
    val pw = new PrintWriter(new File(s"$filename"))
    pw.print(bins.map(_ / (if (usePending) PsMax._2 else PsMax._1)).mkString("\r\n"))
    pw.close()
  }

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, "AggregationOfIndividualOutcomes started")
    try {
      val timestamps = loadTimestamps(s"$Dir/intermediate_test.csv")
      val predictionFilename = s"$Dir/17_fnn/outputs.csv"
      val predictions = loadPredictions(predictionFilename, 0)
      val remainingTime = if (usePending) Some((loadPredictions(predictionFilename, 1), loadMaxY(s"$Dir/test.csv" + Sample.FilenameSuffixY2).toDouble)) else None
      val bins = new AggregationOfIndividualOutcomesExternal(
        timestamps,
        (predictions, loadMaxY(s"$Dir/test.csv" + Sample.FilenameSuffixY1)),
        remainingTime,
        dateHelper.extractTimestamp("23-03-2018 00:00:00"),
        60 * 1000L,
        2,
        4,
        9,
        7,
        13
      ).call()
      exportBins(bins, s"""$Dir/baseline7_${if (usePending) "pending" else "start"}.csv""")
      println(s"max=${bins.max}")
    } catch {
      case e: Throwable => EH().error(e)
    }
    logger.info(s"App is completed.")
  }
}

