package org.processmining.scala.prediction.preprocessing

import java.io.PrintWriter
import java.time.Duration
import java.util.concurrent.Callable

import org.processmining.scala.log.common.utils.common.EventAggregatorImpl
import org.processmining.scala.log.utils.csv.common.CsvExportHelper
import org.processmining.scala.viewers.spectrum.model.{AbstractDataSource, FilesystemDataSource}
import org.slf4j.LoggerFactory


final class SpectrumToDataset(spectrumRootDir: String,
                              incomingFlowSegments: Seq[String],
                              startOffsetHours: Int,
                              durationHours: Int,
                              firstDayMs: Long,
                              days: Seq[Int],
                              timeToPredictBins: Int,
                              historicalStateDatasetDurationBins: Int,
                              labelSegmentName: String,
                              datasetFilename: String,
                              stateSegments: Seq[String],
                              incomingFlowOffsetBins: Int,
                              incomingFlowDurationBins: Int,
                              binsPerLabel: Int
                             ) extends Runnable with Callable[(Seq[(Seq[Double], Seq[Double])], Int, Long)] {
  private val logger = LoggerFactory.getLogger(classOf[SpectrumToDataset])
  private val csvExportHelper = new CsvExportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone, ",")
  private val segmentsToLoadInDataSource: Set[String] = (incomingFlowSegments ++ stateSegments).toSet + labelSegmentName
  private val ds = new FilesystemDataSource(spectrumRootDir, new EventAggregatorImpl, (x, y) => x < y, x => segmentsToLoadInDataSource.contains(x))
  private val pw = new PrintWriter(datasetFilename)

  //LABEL{clazz -> aggr} Features{bin -> seg -> clazz -> aggr}
  private def getHeaderFor(bin: String, segment: String, sep: String = SpectrumToDataset.DefaultSeparator): String =
    (0 until ds.classesCount)
      .map(clazz => {
        val prefix = s"${bin}_${SpectrumToDataset.removeColon(segment)}_i${clazz}"
        s"${prefix}_i$sep${prefix}_s$sep${prefix}_e$sep${prefix}_S"
      }).mkString(sep)

  //  private val LabelHeader = getHeaderFor("p", labelSegmentName)
  //  private val FeaturesHeader = (0 to historicalStateDatasetDurationBins)
  //    .map(bin => stateSegments.map(seg => getHeaderFor(bin.toString, seg)).mkString(DefaultSeparator))
  //    .mkString(DefaultSeparator)
  //  private val Header = s"$LabelHeader$DefaultSeparator$FeaturesHeader"
  //
  //
  private val featuresPerBin = ds.classesCount * 4 // start, intersect, stop, sum for every class
  //  val featuresPerBinForSizeOfRow = ds.classesCount * 1 // start, intersect, stop, sum for every class

  private val sizeOfRow =
    (1 + stateSegments.length * (historicalStateDatasetDurationBins + 1)) * ds.classesCount + (incomingFlowDurationBins + 1) * incomingFlowSegments.length

  private def checkInputArgs(): Unit = {
    logger.info(s"Session=${ds.session.toString}")
    logger.info(s"Incoming flow segments: '${incomingFlowSegments.mkString("', '")}'")
    val leftDatetime = firstDayMs + Duration.ofHours(startOffsetHours).toMillis - historicalStateDatasetDurationBins * ds.twSizeMs
    val rightDatetime = firstDayMs + Duration.ofHours(startOffsetHours + durationHours).toMillis + Duration.ofDays(days.last).toMillis + (timeToPredictBins + 1) * ds.twSizeMs
    logger.info(s"labelSegmentName='$labelSegmentName' firstDayMs=${csvExportHelper.timestamp2String(firstDayMs)}")
    logger.info(s"startOffsetHours=$startOffsetHours durationHours=$durationHours daysNumber=${days.size}")
    logger.info(s"days=${days.mkString(";")}")
    logger.info(s"timeToPredictBins=$timeToPredictBins historicalStateDatasetDurationBins=$historicalStateDatasetDurationBins")
    logger.info(s"leftDatetime=${csvExportHelper.timestamp2String(leftDatetime)}")
    logger.info(s"rightDatetime=${csvExportHelper.timestamp2String(rightDatetime)}")
    if (leftDatetime < ds.startTimeMs) throw new IllegalArgumentException(s"leftDatetime < ds.startTimeMs")
    if (rightDatetime >= ds.startTimeMs + ds.twSizeMs * ds.twCount) throw new IllegalArgumentException(s"Wrong rightDatetime")
    if (timeToPredictBins < 2) throw new IllegalArgumentException(s"Wrong timeToPredictBins=$timeToPredictBins")
    if ((firstDayMs - ds.startTimeMs) % ds.twSizeMs > 0) throw new IllegalArgumentException(s"firstDayMs must be aligned with bins")
    if (incomingFlowSegments.contains(labelSegmentName)) throw new IllegalArgumentException(s"incoming flow should not contain label '$labelSegmentName'!")


  }

  private def normalize(nonNormalized: Seq[Double], segmentName: String): Seq[Double] =
    if (nonNormalized.length != AbstractDataSource.AggregationCodes)
      throw new IllegalArgumentException(s"Wrong nonNormalized.length=${nonNormalized.length}")
    else {
      val nonNormalizedZipped = nonNormalized
        .zipWithIndex
        .map(x => (x._1, x._2 + AbstractDataSource.AggregationCodeFirstValue))

      val norm = nonNormalizedZipped
        .map(x => x._1 / ds.maxSegmentCountForAggregation(segmentName, x._2))
      //      logger.debug(s"NonNorm: '$segmentName' ${nonNormalizedZipped.map(x => f"${x._1}%1.3f/${ds.maxSegmentCountForAggregation(segmentName, x._2)}").mkString(", ")}")
      //      logger.debug(s"Norm:    '$segmentName' ${norm.mkString("; ")}")
      norm
    }

  private def extractAndNormalizeFeaturesOfBin(l: List[(Int, Long, Long, Long)], segmentName: String): Seq[Double] = {
    val countsByClass = l
      .map(x => x._1 -> x)
      .toMap
    (0 until ds.classesCount)
      .flatMap(clazz => {
        val opt = countsByClass.get(clazz)
        if (opt.isDefined) {
          val t = opt.get
          normalize(Seq(t._2.toDouble, t._3.toDouble, t._4.toDouble, t._2.toDouble + t._3.toDouble + t._4.toDouble), segmentName)
        } else Seq(0.0, 0.0, 0.0, 0.0)
      })
  }

  private def extractAndNormalizeFeaturesOfBinWithoutNormalization(l: List[(Int, Long, Long, Long)], segmentName: String): Seq[Double] = {
    val countsByClass = l
      .map(x => x._1 -> x)
      .toMap
    (0 until ds.classesCount)
      .flatMap(clazz => {
        val opt = countsByClass.get(clazz)
        if (opt.isDefined) {
          val t = opt.get
          Seq(t._2.toDouble, t._3.toDouble, t._4.toDouble, t._2.toDouble + t._3.toDouble + t._4.toDouble)
        } else Seq(0.0, 0.0, 0.0, 0.0)
      })
  }

  private def extractAndNormalizeFeaturesOfBin(tw: Int, segmentName: String): Seq[Double] = {
    val opt = ds.segmentsCount(tw).get(segmentName)
    if (opt.isDefined) extractAndNormalizeFeaturesOfBin(opt.get, segmentName)
    else (0 until featuresPerBin).map(_ => 0.0)
  }

  private def extractFeaturesOfBinWithoutNormalization(tw: Int, segmentName: String): Seq[Double] = {
    val opt = ds.segmentsCount(tw).get(segmentName)
    if (opt.isDefined) extractAndNormalizeFeaturesOfBinWithoutNormalization(opt.get, segmentName)
    else (0 until featuresPerBin).map(_ => 0.0)
  }


  //tw1, tw2 inclusive
  private def extractFeatures(tw1: Int, tw2: Int, segments: Seq[String]): Seq[Double] =
    (tw1 to tw2)
      .flatMap(tw => (segments).map(extractAndNormalizeFeaturesOfBin(tw, _)))
      .flatten

  //tw1, tw2 inclusive
  private def extractStateFeatures(tw1: Int, tw2: Int): Seq[Double] = extractFeatures(tw1, tw2, stateSegments)


  //tw1, tw2 inclusive
  private def extractIncomingFlow(tw1Now: Int, tw2Label: Int): Seq[Double] =
    (tw1Now to tw2Label)
      .flatMap(tw => incomingFlowSegments.map(extractAndNormalizeFeaturesOfBin(tw, _)))
      .flatten


  //tw1, tw2 inclusive
  private def extractIncomingFlowWithoutNormalization(tw1Now: Int, tw2Label: Int): Seq[Double] =
    (tw1Now to tw2Label)
      .flatMap(tw => incomingFlowSegments.map(extractFeaturesOfBinWithoutNormalization(tw, _)))
      .flatten


  //  private def extractLabel(twLabel: Int): Seq[Double] =
  //    extractAndNormalizeFeaturesOfBin(twLabel, labelSegmentName)

  private def extractLabel(twLabel: Int, binsToBeAggregated: Int): Seq[Double] = {
    val labelFeaturesPerBin = (0 until binsToBeAggregated)
      .map(i => extractAndNormalizeFeaturesOfBin(twLabel + i, labelSegmentName))

    val nFeaturesPerBin = labelFeaturesPerBin.head.size

    val flatLabelFeaturesPerBin = labelFeaturesPerBin.flatten.toArray

    val ret = (0 until nFeaturesPerBin)
      .map {
        featureIndex =>
          (0 until binsToBeAggregated)
            .map(tw => flatLabelFeaturesPerBin(tw * nFeaturesPerBin + featureIndex))
            .sum / binsToBeAggregated
      }
    ret
  }


  private def extractValueForAggregationFunction(s: Seq[Double], offset: Int, i: Int) =
    s.zipWithIndex
      .map(x => (x._1, x._2 + offset))
      .filter(x => (x._2 % i) == 0)
      .map(_._1)

  val StartOffset = 0
  val SumOffset = 4

  private def processOneDay(day: Int): Seq[(Seq[Double], Seq[Double])] = {
    val twCountPerDay = (Duration.ofHours(durationHours).toMillis / ds.twSizeMs).toInt
    val initialTimeMs = firstDayMs + Duration.ofDays(day).toMillis + Duration.ofHours(startOffsetHours).toMillis
    val initialTw = ((initialTimeMs - ds.startTimeMs) / ds.twSizeMs).toInt
    logger.info(s"day=$day twCountPerDay=$twCountPerDay initialTimeMs=${csvExportHelper.timestamp2String(initialTimeMs)} initialTw=$initialTw")
    val dataset = (initialTw until initialTw + twCountPerDay).map(twNow => {
      val twPast = twNow - historicalStateDatasetDurationBins
      val twLabel = twNow + timeToPredictBins
      val startOfIncomingFlowBins = twNow + incomingFlowOffsetBins
      val labelFeatures = extractLabel(twLabel, binsPerLabel)
      val stateFeatures = extractStateFeatures(twPast, twNow)
      val labelAndStateSingleAggregationFunction = extractValueForAggregationFunction(labelFeatures ++ stateFeatures, StartOffset, 4) //sum
      val labelSingleAggregationFunction = extractValueForAggregationFunction(extractFeatures(twPast, twNow, Seq(labelSegmentName)), StartOffset, 4) //sum
      //        extractValueForAggregationFunction(x2, 0, 4) ++
      //        extractValueForAggregationFunction(x2, 2, 4) ++
      //        extractValueForAggregationFunction(x2, 3, 4)
      val incomingFlowFeaturesSingleAggregationFunction = extractValueForAggregationFunction(extractIncomingFlow(startOfIncomingFlowBins, startOfIncomingFlowBins + incomingFlowDurationBins), StartOffset, 4).toArray // start

      val incomingFlowFeaturesSingleAggregationFunctionWithoutNormalization = extractValueForAggregationFunction(extractIncomingFlowWithoutNormalization(startOfIncomingFlowBins, startOfIncomingFlowBins + incomingFlowDurationBins), StartOffset, 4).toArray // start

      val tuples = incomingFlowFeaturesSingleAggregationFunction.length / ds.classesCount
      if (tuples * ds.classesCount != incomingFlowFeaturesSingleAggregationFunction.length) throw new IllegalStateException(s"Wrong size of incomingFlow=${incomingFlowFeaturesSingleAggregationFunction.length}")
      val sumOfClassesForIncFlow = (0 until tuples).map(i => (0 until ds.classesCount).map(j => incomingFlowFeaturesSingleAggregationFunction(i * ds.classesCount + j)).sum)

      val row = labelAndStateSingleAggregationFunction ++ sumOfClassesForIncFlow
//!!!      if (row.length != sizeOfRow) throw new IllegalStateException(s"Error: row.length != sizeOfRow: ${row.length} != $sizeOfRow")
      pw.println(SpectrumToDataset.rowToString(row))
      ds.forgetSegmentsCount(twPast)
      (labelSingleAggregationFunction, incomingFlowFeaturesSingleAggregationFunctionWithoutNormalization.toSeq)
    })
    ds.forgetSegmentCounts()
    dataset
  }

  private def beforeRun(): Unit = {
    checkInputArgs()
    logger.info(s"sizeOfRow=$sizeOfRow")
    ds.initialize()
    //pw.println(Header)
  }

  override def run(): Unit = {
    beforeRun()
    days.foreach(processOneDay)
    pw.close()
  }

  override def call(): (Seq[(Seq[Double], Seq[Double])], Int, Long) = {
    beforeRun()
    val ret = days.flatMap(processOneDay)
    pw.close()
    (ret, ds.classesCount, ds.maxSegmentsCount(labelSegmentName)._4)
  }
}

object SpectrumToDataset {
  private val DefaultSeparator = ","

  def rowToString(row: Seq[Double]) = row.map(v => f"$v%01.5f").mkString(DefaultSeparator)

  def removeColon(s: String): String = s.replace(':', '_')


}
