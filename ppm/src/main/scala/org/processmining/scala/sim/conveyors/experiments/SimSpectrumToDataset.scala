package org.processmining.scala.sim.conveyors.experiments

import java.io.{File, PrintWriter}
import java.time.Duration

import org.apache.log4j.PropertyConfigurator
import org.ini4j.{Ini, IniPreferences}
import org.processmining.scala.log.utils.common.errorhandling.{EH, JvmParams}
import org.processmining.scala.prediction.preprocessing.{AbstractSpectrumToDatasetSession, SpectrumToDataset}
import org.slf4j.LoggerFactory

class SimSpectrumToDataset(override val spectrumRoot: String,
                           override val datasetDir: String,
                           override val experimentName: String,
                           override val dayStartOffsetHours: Int,
                           override val dayDurationHours: Int,
                           override val howFarInFutureBins: Int,
                           override val historicalDataDurationBins: Int,
                           override val binsPerLabel: Int,
                           override val labelSegment: String,
                           override val stateSegments: Seq[String],
                           override val firstDayDateTime: String,
                           override val totalDaysFromFirstDayInPerformanceSpectrum: Int,
                           val daysNumberInTrainingValidationDataset: Int
                          ) extends AbstractSpectrumToDatasetSession {

  override def daysForTraining: Seq[Int] = (0 until daysNumberInTrainingValidationDataset)

  override def daysForTest: Seq[Int] = (daysNumberInTrainingValidationDataset until totalDaysFromFirstDayInPerformanceSpectrum)

  def dateToDayNumber(s: String): Int =
    ((importCsvHelper.extractTimestamp(s) - importCsvHelper.extractTimestamp(firstDayDateTime)) / Duration.ofDays(1).toMillis).toInt


  //  val spectrumRoot = "G:/SIM_PS/Scan1"
  //  val datasetDir = "G:/psm_ml2/data"
  //  val experimentName = "Scan2"
  //
  //  val dayStartOffsetHours = 10
  //  val dayDurationHours = 12
  //  val howFarInFutureBins = 6
  //  val historicalDataDurationBins = 4
  //  val binsPerLabel = 2
  //
  //  val labelSegment = "E1.TO_SCAN_1_0:E2.SCAN_1"

  val incomingFlowOffsetBins = -historicalDataDurationBins

  val incomingFlowDurationBins = historicalDataDurationBins + howFarInFutureBins

  //  val stateSegments = //Seq() //labelSegment +:
  //    (Set(
  //      "A3_0:Link1_0", "A2_0:A4_0", "A1_0:A4_0") - labelSegment).toSeq

  val incomingFlowSegments = Seq()

  override protected def processIncomingFlow(incomingFlowFeatures: Seq[Seq[Double]], isEvaluationDataset: Boolean) = {
    val size = incomingFlowSegments.size
    val baseline2 =
      incomingFlowFeatures.map(row => {
        val zipped = row.zipWithIndex
        (
          zipped.filter(x => x._2 >= (howFarInFutureBins - 3) * size && x._2 < (howFarInFutureBins - 2) * size).map(_._1).sum +
            zipped.filter(x => x._2 >= (howFarInFutureBins - 2) * size && x._2 < (howFarInFutureBins - 1) * size).map(_._1).sum
          ) / 2
      }
      )
    logger.info(s"Exporting baseline2...")
    val pw = new PrintWriter(s"${if (isEvaluationDataset) testDatasetDir else trainingDatasetDir}/baseline2.csv")
    baseline2.foreach(x => pw.println(SpectrumToDataset.rowToString(Seq(x))))
    pw.close()
  }

}


object SimSpectrumToDataset {
  private val logger = LoggerFactory.getLogger(SimSpectrumToDataset.getClass)

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, s"${SimSpectrumToDataset.getClass} started")
    if (args.isEmpty) {
      logger.info(s"Use the following arguments: INI_FILENAME")
    } else {
      logger.info(s"Cli args:${args.mkString(",")}")
      try {
        val iniFileName = args(0)
        val iniPrefs = new IniPreferences(new Ini(new File(iniFileName)))
        val generalNode = iniPrefs.node("GENERAL")
        val spectrumRoot = generalNode.get("spectrumRoot", "")
        val datasetDir = generalNode.get("datasetDir", "")
        val experimentName = generalNode.get("experimentName", "")
        val labelSegment = generalNode.get("targetSegments", "")
        val stateSegments = generalNode.get("historicSegments", "").split("\\s").toSeq
        val firstDayDateTime = generalNode.get("firstDayDateTime", "")


        val dayStartOffsetHours = generalNode.getInt("dayStartOffsetHours", 0)
        val dayDurationHours = generalNode.getInt("dayDurationHours", 0)

        val howFarInFutureBins = generalNode.getInt("howFarInFutureBins", 0)
        val historicalDataDurationBins = generalNode.getInt("historicalDataDurationBins", 0)
        val binsPerLabel = generalNode.getInt("binsPerLabel", 0)

        val totalDaysFromFirstDayInPerformanceSpectrum = generalNode.getInt("totalDaysFromFirstDayInPerformanceSpectrum", 0)
        val daysNumberInTrainingValidationDataset = generalNode.getInt("daysNumberInTrainingValidationDataset", 0)

        new SimSpectrumToDataset(spectrumRoot,
          datasetDir,
          experimentName,
          dayStartOffsetHours,
          dayDurationHours,
          howFarInFutureBins,
          historicalDataDurationBins,
          binsPerLabel,
          labelSegment,
          stateSegments,
          firstDayDateTime,
          totalDaysFromFirstDayInPerformanceSpectrum,
          daysNumberInTrainingValidationDataset).run()
      } catch {
        case e: Throwable =>
          logger.error(EH.formatError(e.toString, e))
      }
    }
    logger.info(s"App is completed.")
  }
}
