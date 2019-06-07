package org.processmining.scala.intercase

import java.util.Locale
import java.util.concurrent.Callable
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.slf4j.LoggerFactory

/** *
  * Trace prefix
  *
  * @param id        case ID
  * @param startTime case start time
  * @param endTime   timestamp of the last event in this prefix
  * @param prefix    prefix (names of event activities)
  */
case class Prefix(id: String, startTime: Long, endTime: Long, prefix: List[String])

// The case outcome
case class Outcome(remainingTime: Long, segmentDuration: Long)

/** *
  * Implementation of the the data-driven inter-case feature encoding, see
  * A.Senderovich, C.D.Francescomarino, and F.M.Maggi,
  * “From knowledge-driven to data-driven inter-case feature encoding in predictive
  * process monitoring,” Information Systems, 2019. [Online]. Available:
  * https://doi.org/10.1016/j.is.2019.01.007
  *
  * Based on the Python implementation, provided by the authors of this paper.
  *
  * @param log          event log
  * @param thresholds   threshold for city and snapshot distances
  * @param outcome      outcome function
  * @param isCompleted  predicate for filtering in complete traces
  * @param prefixFilter predicate for filtering in prefixes of interest
  */
class DdeInterCaseFeatureEncoder(log: UnifiedEventLog,
                                 thresholds: List[Long],
                                 outcome: (Prefix, List[UnifiedEvent]) => Option[Outcome],
                                 isCompleted: (Prefix, List[UnifiedEvent]) => Boolean,
                                 prefixFilter: Prefix => Boolean
                                ) extends Callable[List[Sample]] {

  private val logger = LoggerFactory.getLogger(classOf[DdeInterCaseFeatureEncoder])

  private val traces = log.traces().seq.sortBy(_._1.id)

  private val traceDictionary = traces.map(x => x._1.id -> x._2).toMap

  private def returnLongestRunningNew(running_now: IndexedSeq[Prefix], p: Prefix, kOriginal: Int): List[Prefix] = {
    val k = Integer.min(p.prefix.size, kOriginal)

    def extractLastK(x: List[String]): Option[List[String]] =
      if (x.size >= k) Some(x.reverse.zipWithIndex.filter(_._2 < k).map(_._1)) else None

    val suffix = extractLastK(p.prefix).get
    val prefixesWithTheSameSuffix = running_now
      .map(x => (x, extractLastK(x.prefix)))
      .filter(x => x._2.isDefined && x._2.get == suffix)
      .map(_._1)

    prefixesWithTheSameSuffix
      .groupBy(_.id)
      .map(_._2.maxBy(_.endTime))
      .toList
  }

  private def snapshotDistance(x: Prefix, p: Prefix): Long = p.endTime - x.endTime

  private def cityDistance(x: Prefix, p: Prefix): Long = (x.startTime - p.startTime).abs + (x.endTime - p.endTime).abs

  // Calculates snapshot or city distance between two prefixes
  private def returnTemporalDistance(runningNow: IndexedSeq[Prefix], p: Prefix, threshold: Long, metric: (Prefix, Prefix) => Long) =
    runningNow
      .map(x => (x, metric(x, p)))
      .filter(_._2 < threshold)
      .map(_._1)
      .groupBy(_.id)
      .map(_._2.maxBy(_.endTime))

  /** *
    * Entry-point to the implementation
    * The method repeats the steps of the original implementation
    *
    * @return samples for training and test sets (in the form of the original Python-based implementation)
    */
  override def call(): List[Sample] = {
    Locale.setDefault(Locale.US)

    //building all prefixes of traces
    val prefixesOfEvents = traces
      .flatMap(_._2
        ./:(List[List[UnifiedEvent]]())((z, e) => if (z.isEmpty) List(e) :: z else (e :: z.head) :: z)
        .map(_.reverse)
        .zipWithIndex
        .reverse)

    val prefixes = prefixesOfEvents.map(p => p._1.map(_.activity)).toArray
    logger.info(s"Prefixes are built")
    // building other indices
    val ids = traces.flatMap(t => t._2.map(_ => t._1.id)).toArray
    val intervals = traces.flatMap(t => t._2.map(e => (t._2.head.timestamp, e.timestamp))).toArray
    // building a data frame of running case prefixes
    val dfRunning = prefixes // not completed prefixes
      .indices
      .map(i => Prefix(ids(i), intervals(i)._1, intervals(i)._2, prefixes(i)))
      .filter(x => !isCompleted(x, traceDictionary(x.id)))

    logger.info(s"Running cases are found")
    // extracting samples for training and test sets
    val prefixesOfInterest = dfRunning.filter(prefixFilter(_))
    logger.info(s"Prefixes of interest are found")
    val ret = prefixesOfInterest.zipWithIndex // to preserve line numbers
      .map { x =>
      val lineNumber = x._2
      val p = x._1
      if (lineNumber % 500 == 0) logger.info(s"Calculating $lineNumber out of ${prefixesOfInterest.size}")
      val outcomeOptional = outcome(p, traceDictionary(p.id)) // computing the outcome ('y' value)
      if (outcomeOptional.isDefined) { // the outcome is successfully computed
        val outcome = outcomeOptional.get
        val elapsedTime = ((p.endTime - p.startTime) / 1000L) / 60.0
        val run_now = dfRunning.filter(y => y.id != p.id && y.endTime > p.startTime && y.endTime < p.endTime) //simultaneously running prefixes
        val L123 = List(0, 1, 3).map(k => returnLongestRunningNew(run_now, p, k).size) // Level 1,2,3 in the paper
        val city = thresholds.map(t => returnTemporalDistance(run_now, p, t, cityDistance).size) //city distances
        val snap = thresholds.map(t => returnTemporalDistance(run_now, p, t, snapshotDistance).size) //snapshot distances
        Some(Sample(lineNumber, p.id, elapsedTime, p.endTime, p.prefix.last, L123, city, snap, outcome)) //sample
      }
      else None // the outcome was not computed for the prefix
    }
      .filter(_.isDefined) // only non-None samples are needed
      .map(_.get) // getting rid of Optional values
      .toList
    ret
  }
}

