package org.processmining.scala.intercase

import java.io.{File, PrintWriter}

import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.utils.csv.common.{CsvExportHelper, CsvImportHelper}
import org.slf4j.LoggerFactory

/*** Helper to create sessions for experiments (i.e. classes with main() function)
  * to run experiments based on the data-driven inter-case feature encoding, see
  * A.Senderovich, C.D.Francescomarino, and F.M.Maggi, “From knowledge-driven
  * to data-driven inter-case feature encoding in predictive
  * process monitoring,” Information Systems, 2019. [Online]. Available:
  * https://doi.org/10.1016/j.is.2019.01.007
  */
class BaseDdeInterCaseFeatureEncodingSession {
  val logger = LoggerFactory.getLogger(classOf[BaseDdeInterCaseFeatureEncodingSession])
  val dateHelper = new CsvImportHelper(CsvExportHelper.ShortTimestampPattern, CsvExportHelper.AmsterdamTimeZone) //to provide time intervals in code


  /***
    * Implementation of the outcome function: remaining case time
    * @param p prefix
    * @param events the whole trace for the prefix
    * @return remaining case time if possible to compute or None
    */
  def defaultOutcome(p: Prefix, events: List[UnifiedEvent]): Option[Outcome] =
    Some(Outcome((events.last.timestamp - p.endTime) / 1000L, 0))

  def defaultIsCompleted(p: Prefix, events: List[UnifiedEvent]): Boolean =
    events.size == p.prefix.size

  def defaultPrefixFilter(p: Prefix): Boolean = true

  def exportIntermediateDataset(filename: String, samples: List[Sample], headerNoEndTimeYes: Boolean = false) = {
    val pw = new PrintWriter(new File(filename))
    if(!headerNoEndTimeYes) pw.println(BaseDdeInterCaseFeatureEncodingSession.Header)
    samples.foreach{s =>
      val line = f"""${s.lineNumber},${s.id},${s.elapsedTime}%.1f,${s.lastEvent},${s.L123.mkString(",")},${s.cityDistance.mkString(",")},${s.snapshotDistance.mkString(",")},${s.y}"""
      pw.println(if(headerNoEndTimeYes) s"${s.endTime}," + line else line)}
    pw.close
  }

  def sortForSplittingIntoTrainingAndTestSets(samples: List[Sample], splittingDate: Long): (List[Sample], Int) = {
    val sorted = samples.sortBy(_.endTime)
    val sampleOpt = sorted.zipWithIndex.find(_._1.endTime >= splittingDate)
    if (!sampleOpt.isDefined) throw new IllegalArgumentException(s"Wrong splittingDate=$splittingDate")
    (sorted, sampleOpt.get._2)
  }

  def exportTrainingTestSet(dir: String, unsortedSamples: List[Sample], splittingDateString: String) = {
    val splittingDate = dateHelper.extractTimestamp(splittingDateString)
    exportIntermediateDataset(s"$dir/intermediate_dataset.csv", unsortedSamples)
    val (sortedSamples, index) = sortForSplittingIntoTrainingAndTestSets(unsortedSamples, splittingDate)
    logger.info(s"Split index = $index for dataset of size ${sortedSamples.size}")
    exportIntermediateDataset(s"$dir/intermediate_dataset_sorted_by_end_time.csv", sortedSamples)
    val (normalized, max) = Sample.normalize(Some(s"$dir/non_normalized.csv"), sortedSamples)
    Sample.export(s"$dir/normalized.csv", normalized, max)
    val (trainingIntermediate, testIntermediate) = sortedSamples.span(_.endTime <= splittingDate)
    exportIntermediateDataset(s"$dir/intermediate_training.csv", trainingIntermediate)
    exportIntermediateDataset(s"$dir/intermediate_test.csv", testIntermediate, true)
    val (training, test) = normalized.zipWithIndex.span(_._2 < index)
    if (training.size != trainingIntermediate.size) throw new IllegalStateException(s"training.size != trainingIntermediate.size: ${training.size} != ${trainingIntermediate.size}")
    Sample.export(s"$dir/training.csv", training.map(_._1), max)
    Sample.export(s"$dir/test.csv", test.map(_._1), max)
  }
}

object BaseDdeInterCaseFeatureEncodingSession{
  val Header = ",ID,Elapsed,LastEvent,L1,L2,L3,City1,City2,City3,City4,City5,Snap1,Snap2,Snap3,Snap4,Snap5,Y"

  def exportIntermediateDataset(filename: String, samples: List[Sample], headerNoEndTimeYes: Boolean = false) = {
    val pw = new PrintWriter(new File(filename))
    if(!headerNoEndTimeYes) pw.println(Header)
    samples.foreach{s =>
      val line = f"""${s.lineNumber},${s.id},${s.elapsedTime}%.1f,${s.lastEvent},${s.L123.mkString(",")},${s.cityDistance.mkString(",")},${s.snapshotDistance.mkString(",")},${s.y}"""
      pw.println(if(headerNoEndTimeYes) s"${s.endTime}," + line else line)}
    pw.close
  }

}