package org.processmining.scala.prediction.preprocessing

import java.io.{File, PrintWriter}
import java.nio.file._
import java.time.Duration
import java.util.Locale

import org.processmining.scala.log.utils.csv.common.{CsvExportHelper, CsvImportHelper}
import org.slf4j.LoggerFactory

abstract class AbstractSpectrumToDatasetSession extends Runnable {

  protected val logger = LoggerFactory.getLogger(classOf[SpectrumToDataset])

  protected def experimentName: String

  protected def dayStartOffsetHours: Int

  protected def dayDurationHours: Int

  protected def firstDayDateTime: String

  protected def totalDaysFromFirstDayInPerformanceSpectrum: Int

  protected def howFarInFutureBins: Int

  protected def historicalDataDurationBins: Int

  protected def incomingFlowOffsetBins: Int

  protected def incomingFlowDurationBins: Int

  protected def spectrumRoot: String

  protected lazy val experimentId = s"${experimentName}_off${dayStartOffsetHours}_d${dayDurationHours}_start${firstDayDateTime.replace(':', '_')}_days${daysForTraining.size}_timeToPredict${howFarInFutureBins}_historicalData${historicalDataDurationBins}_binsPerLabel${binsPerLabel}_aggregation_${SpectrumToDataset.aggregationCodeToString(aggregation)}"

  protected def datasetDir: String

  protected def trainingDatasetDir = s"${datasetDir}/$experimentId"

  protected def testDatasetDir = s"${trainingDatasetDir}/test"

  protected def trainingDatasetFilename = s"$trainingDatasetDir/features.csv"

  protected def testDatasetFile = s"${testDatasetDir}/features.csv"

  protected def importCsvHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)

  protected def stateSegments: Seq[String]

  protected def incomingFlowSegments: Seq[String]

  protected def labelSegment: String

  protected def binsPerLabel: Int

  protected def daysForTraining: Seq[Int]

  protected def daysForTest: Seq[Int]

  protected def aggregation: Int


  protected def processIncomingFlow(incomingFlowFeatures: Seq[Seq[Double]], isEvaluationDataset: Boolean): Unit = {}

  override def run(): Unit = {
    Locale.setDefault(Locale.US)
    logger.info(s"Spectrum path='$spectrumRoot'")
    logger.info(s"DatasetFile='$trainingDatasetFilename'")
    logger.info(s"experimentId='$experimentId'")
    logger.info(s"experimentName='$experimentName'")
    logger.info(s"dayStartOffsetHours=$dayStartOffsetHours")
    logger.info(s"dayDurationHours=$dayDurationHours")
    logger.info(s"firstDayDateTime='$firstDayDateTime'")
    logger.info(s"totalDaysFromFirstDayInPerformanceSpectrum=$totalDaysFromFirstDayInPerformanceSpectrum")
    logger.info(s"howFarInFutureBins=$howFarInFutureBins")
    logger.info(s"historicalDataDurationBins=$historicalDataDurationBins")
    logger.info(s"trainingDatasetDir=$trainingDatasetDir")
    logger.info(s"testDatasetDir='$testDatasetDir'")
    logger.info(s"trainingDatasetFilename='$trainingDatasetFilename'")
    logger.info(s"evaluationDatasetFile='$testDatasetFile'")
    logger.info(s"stateSegments='${stateSegments.mkString("', '")}'")
    logger.info(s"stateSegments number='${stateSegments.size}")
    logger.info(s"incomingFlowSegments='${incomingFlowSegments.mkString("', '")}'")
    logger.info(s"incomingFlowSegments number='${incomingFlowSegments.size}")
    logger.info(s"labelSegment='$labelSegment'")
    logger.info(s"binsPerLabel=$binsPerLabel")
    logger.info(s"aggregation=${SpectrumToDataset.aggregationCodeToString(aggregation)}")

    new File(trainingDatasetDir).mkdirs()
    new File(testDatasetDir).mkdirs()
    val (stateLabelsTraining, classCount, maxValueTraining) = new SpectrumToDataset(spectrumRoot,
      incomingFlowSegments,
      dayStartOffsetHours,
      dayDurationHours,
      importCsvHelper.extractTimestamp(firstDayDateTime),
      daysForTraining,
      howFarInFutureBins,
      historicalDataDurationBins,
      labelSegment,
      trainingDatasetFilename,
      stateSegments,
      incomingFlowOffsetBins,
      incomingFlowDurationBins,
      binsPerLabel,
      aggregation
    ).call()

    logger.info(s"Exporting...")
    val baselineDatasetTraining = makeBaseline(stateLabelsTraining.map(_._1), classCount)
    processIncomingFlow(stateLabelsTraining.map(_._2), false)
    val pwTraining = new PrintWriter(s"$trainingDatasetDir/baseline.csv")
    baselineDatasetTraining.foreach(x => pwTraining.println(SpectrumToDataset.rowToString(x)))
    pwTraining.close()
    val pwMaxValueTraining = new PrintWriter(s"$trainingDatasetDir/max.csv")
    pwMaxValueTraining.println(maxValueTraining)
    pwMaxValueTraining.close()

    val (stateLabelsTest, _, maxValueTest) = new SpectrumToDataset(spectrumRoot,
      incomingFlowSegments,
      dayStartOffsetHours,
      dayDurationHours,
      importCsvHelper.extractTimestamp(firstDayDateTime),
      daysForTest,
      howFarInFutureBins,
      historicalDataDurationBins,
      labelSegment,
      testDatasetFile,
      stateSegments,
      incomingFlowOffsetBins,
      incomingFlowDurationBins,
      binsPerLabel,
      aggregation
    ).call()

    logger.info(s"Exporting baseline...")
    val baselineDataset = makeBaseline(stateLabelsTest.map(_._1), classCount)
    processIncomingFlow(stateLabelsTest.map(_._2), true)
    val pwEval = new PrintWriter(s"$testDatasetDir/baseline.csv")
    baselineDataset.foreach(x => pwEval.println(SpectrumToDataset.rowToString(x)))
    pwEval.close()
    val pwMaxValueTest = new PrintWriter(s"$testDatasetDir/max.csv")
    pwMaxValueTest.println(maxValueTest)
    pwMaxValueTest.close()
    logger.info(s"Copying app log...")
    Files.copy(Paths.get("./psm.log"), Paths.get(s"$trainingDatasetDir/exporter_app.log"), StandardCopyOption.REPLACE_EXISTING)
    logger.info(s"Done.")
  }

  protected def makeBaseline(labelsFromState: Seq[Seq[Double]], classCount: Int): Seq[Seq[Double]] = {
    val binCount = historicalDataDurationBins + 1
    val labelsFromStateExpectedSize = binCount * classCount
    val ret = labelsFromState.map(row => {
      if (row.size != labelsFromStateExpectedSize) throw new IllegalArgumentException(s"vector size!= expected ${row.size} != $labelsFromStateExpectedSize")
      val rowArray = row.toArray
      (0 until classCount).map(clazzIndex => {
        (0 until binCount).map(tw => {
          rowArray(tw * classCount + clazzIndex)
        }).sum / binCount
      })
    })
    ret
  }
}
