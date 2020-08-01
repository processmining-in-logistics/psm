package org.processmining.scala.viewers.spectrum2.experiments

import java.io.File

import org.processmining.scala.log.common.csv.parallel.{CsvReader, CsvWriter}
import org.processmining.scala.log.common.filtering.expressions.events.regex.impl.EventEx
import org.processmining.scala.log.common.filtering.expressions.traces.impl.TraceExpression
import org.processmining.scala.log.common.filtering.expressions.traces._
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.processmining.scala.log.common.unified.trace.{UnifiedTraceId, UnifiedTraceIdImpl}
import org.processmining.scala.log.common.utils.common.UnifiedEventLogSubtraceUtils
import org.processmining.scala.log.common.xes.parallel.XesReader
import org.processmining.scala.log.utils.common.csv.common.{CsvExportHelper, CsvImportHelper}
import org.processmining.scala.viewers.spectrum2.model._
import org.processmining.scala.viewers.spectrum2.view.PerformanceSpectrumPanel
import org.slf4j.LoggerFactory


case class EventWithOrder(id: UnifiedTraceId, e: UnifiedEvent, initOrderInTrace: Int, orderInLog: Int) {
  override def toString: String = s"id='$id' @${EventWithOrder.exportCsvHelper.timestamp2String(e.timestamp)} logOrder=$orderInLog " +
    s"MaxTs=${if (e.hasAttribute(AbstractDataSource.MaxTsName)) e.getAs[Long](AbstractDataSource.MaxTsName) else ""}"
}

object EventWithOrder {
  val empty = EventWithOrder(new UnifiedTraceIdImpl(""), UnifiedEvent.EmptyEvent, -1, -1)
  val exportCsvHelper = new CsvExportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone, "")
}

class TLogsToSegments(skipPreProcessing: Boolean, logFilename: String, eventLogPath: String, exportAggregatedLog: Boolean = false) extends CallableWithLevel[UnifiedEventLog] {
  private val logger = LoggerFactory.getLogger(classOf[TLogsToSegments])
  private val importCsvHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)
  private val exportCsvHelper = new CsvExportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone, "")
  private val t = TraceExpression()

  val beforeIN_1_0 = EventEx(A.IN_1_0)
  val beforeIN_2_0 = EventEx(A.IN_2_0)
  val beforeIN_3_0 = EventEx(A.IN_3_0)
  val beforeIN_4_0 = EventEx(A.IN_4_0)
  val beforeIN_56_0 = EventEx("IN_[56]_0")
  val beforeIN_7_0 = EventEx(A.IN_7_0)
  val nonLoopStarts = Vector(beforeIN_1_0, beforeIN_2_0, beforeIN_3_0, beforeIN_4_0, beforeIN_56_0, beforeIN_7_0)
  val startCheckIn = EventEx(s"IN_[1-4]_3${Separator.S}x")
  val startTransfer1 = EventEx(A.IN_7_3_TO_x)
  val startTransfer2 = EventEx(s"IN_[56]_3${Separator.S}x")
  val mc = EventEx("MC[12].*")
  val scanners = EventEx(s"y${Separator.S}S[1-7]_0")
  val loop = EventEx(A.z_TO_x)
  val allActivities = nonLoopStarts ++ Vector(startCheckIn, startTransfer1, startTransfer2, mc, scanners, loop)
  val rightStarts = getLog(logFilename)

  private def factory(caseIdIndex: Int, completeTimestampIndex: Int, activityIndex: Int)(row: Array[String]): (String, UnifiedEvent) =
    (
      row(caseIdIndex),
      UnifiedEvent(
        importCsvHelper.extractTimestamp(row(completeTimestampIndex)),
        row(activityIndex)))

  def factoryOfFactory(header: Array[String]): Array[String] => (String, UnifiedEvent) = {
    factory(header.indexOf("id"),
      header.indexOf("timestamp"),
      header.indexOf("activity"))
  }


  def getLog(logFilename: String) = {

    val lowerCaseFileName = logFilename.toLowerCase
    val log = if (lowerCaseFileName.endsWith("xes") || lowerCaseFileName.endsWith("xes.gz")) {
      XesReader.read(new File(logFilename))(0)
    } else {


      val importCsvHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)
      val csvReader = new CsvReader()
      val (header, lines) = csvReader.read(logFilename)
      val factory = factoryOfFactory(header)
      val tmp = UnifiedEventLog
        .create(csvReader.parse(lines, factory))
      //TODO: UNCOMMENT FOR REAL-LIFE files!!!!!!!!!!!!!
      //              .map(t trimToTimeframe(
      //                importCsvHelper.extractTimestamp("03-10-17 11:00:00.000"),
      //                importCsvHelper.extractTimestamp("03-10-17 11:30:00.000")))
      //END
      //    val aggregation = new EventAggregatorImpl(aggregationIniFilename)
      //    val exAggregation = t aggregate UnifiedEventAggregator(aggregation)


      val projectedEvents = tmp
        //.map(exAggregation)
        .project(allActivities: _*)



      val rightStarts = projectedEvents.map(t subtrace (beforeIN_1_0 >> startCheckIn >> (scanners oneOrMore)))
        .fullOuterJoin(projectedEvents.map(t subtrace (beforeIN_2_0 >> startCheckIn >> (scanners oneOrMore))))
        .fullOuterJoin(projectedEvents.map(t subtrace (beforeIN_3_0 >> startCheckIn >> (scanners oneOrMore))))
        .fullOuterJoin(projectedEvents.map(t subtrace (beforeIN_4_0 >> startCheckIn >> (scanners oneOrMore))))
        .fullOuterJoin(projectedEvents.map(t subtrace (beforeIN_7_0 >> startTransfer1 >> (scanners oneOrMore))))
        .fullOuterJoin(projectedEvents.map(t subtrace (beforeIN_56_0 >> startTransfer2 >> (scanners oneOrMore))))
        .fullOuterJoin(projectedEvents.flatMap(t extractSubtraces (loop >> (scanners oneOrMore))))

      //TODO: split into several traces by timestamps and/or LPC!!!
      UnifiedEventLog.fromTraces(
        rightStarts
          .traces()
          .map(t => (t._1, t._2.filter(_.timestamp - t._2.head.timestamp < 15 * 60 * 1000)))
          .filter(_._2.nonEmpty)
      )
    }
    log
  }

  //LEVEL 3
  def sortEvents(traces: List[UnifiedTrace], sortedCaseId: List[UnifiedTraceId]): List[UnifiedTrace] = {

    val events = traces.flatMap(t => t._2.zipWithIndex.map(e => EventWithOrder(t._1, e._1, e._2, -1)))
    val eventsByActivity = events.groupBy(_.e.activity)
    val order = sortedCaseId.zipWithIndex.map(x => x._1.id -> x._2).toMap
    val eventsByActivityWithOrderSortedByLogOrder =
      eventsByActivity
        .map(x => (x._1, x._2.map(e => e.copy(orderInLog = order(e.id.id))).sortBy(_.orderInLog)))



    val tmp = eventsByActivityWithOrderSortedByLogOrder.values.map(events => // events of a single activity ordered by the log order (by the scanner order)
      UnifiedEventLogSubtraceUtils
        .mapSubtrace[EventWithOrder](events, EventWithOrder.empty)
          (!_.e.hasAttribute(AbstractDataSource.MaxTsName),
            !_.e.hasAttribute(AbstractDataSource.MaxTsName),
            (middle: List[EventWithOrder], left: Option[EventWithOrder], right: Option[EventWithOrder]) => {
              if ( /*right.isDefined &&*/ left.isDefined) {
                //val leftmostTs = left.get.e.timestamp + minDistanceMs
                val rightmostTs = if (right.isDefined) right.get.e.timestamp - PerformanceSpectrumPanel.MinDistanceMs else Long.MaxValue

                val fixed = middle.foldLeft((left.get.e.timestamp, List[EventWithOrder]()))((z: (Long, List[EventWithOrder]), ewo: EventWithOrder) => {
                  val correctedTimestamp =
                    if (ewo.e.timestamp < z._1 + PerformanceSpectrumPanel.MinDistanceMs) {
                      logger.debug(s"ORDER LEFT: set ${z._1 + PerformanceSpectrumPanel.MinDistanceMs} instead of ${ewo.e.timestamp}")
                      //                    if (z._1 + minDistanceMs - ewo.e.timestamp > 3 * 60 * 1000L)
                      //                      logger.debug(s"ORDER LEFT: set ${z._1 + minDistanceMs} instead of ${ewo.e.timestamp}")
                      z._1 + PerformanceSpectrumPanel.MinDistanceMs

                    } else {
                      if (ewo.e.timestamp > rightmostTs) {
                        logger.debug(s"ORDER RIGHT: set ${rightmostTs} instead of ${ewo.e.timestamp}")
                        rightmostTs - PerformanceSpectrumPanel.MinDistanceMs
                      } else {
                        ewo.e.timestamp
                      }
                    }
                  val maxTs = ewo.e.attributes(AbstractDataSource.MaxTsName).asInstanceOf[Long]
                  val newEwoCorrectedMax = if (maxTs > rightmostTs) rightmostTs - PerformanceSpectrumPanel.MinDistanceMs else maxTs
                  val newUnifiedEvent = ewo.e.copy(correctedTimestamp).copy(AbstractDataSource.MaxTsName, newEwoCorrectedMax)
                  val newEwo = ewo.copy(e = newUnifiedEvent)
                  (
                    correctedTimestamp,
                    newEwo :: z._2
                  )
                })

                val ret = if (right.isDefined) {
                  fixed._2.zipWithIndex.reverse.map(x => {
                    val ue = x._1.e
                    if (ue.hasAttribute(AbstractDataSource.MaxTsName)) {
                      val newMaxMs = Math.min(rightmostTs - x._2 * PerformanceSpectrumPanel.MinDistanceMs, ue.getAs[Long](AbstractDataSource.MaxTsName))
                      x._1.copy(e = ue.copy(AbstractDataSource.MaxTsName, newMaxMs))
                    } else x._1
                  })
                } else fixed._2.reverse
                ret
              } else middle

            })
    )



    tmp.flatten.map(x => (x.id, x))
      .toList
      .groupBy(x => x._1)
      .map(x => (x._1, x._2.map(_._2).sortBy(_.initOrderInTrace).map(_.e)))
      .toList
    //List[UnifiedTrace]()


  }

  override def call(level: Int): UnifiedEventLog = {


    val finalLog = if (skipPreProcessing) rightStarts
    else {

      val tree = new PreSorter50TreeBuilder(rightStarts)()
      val nodes = SorterTree.getRootAndChildrenAsMap(Some(tree.root))


      val tracesSortedByScanner97 = UnifiedEventLog.fromTraces(rightStarts
        .traces()
        .filter(_._2.nonEmpty)
        .filter(_._2.exists(_.activity == A.y_TO_S1_0))
        .toList
        .sortBy(_._2.find(_.activity == A.y_TO_S1_0).get.timestamp))
      //.zipWithIndex

      val retLog = if (level == 1) tracesSortedByScanner97
      else {
        tracesSortedByScanner97.mapSubtrace(
          _ => true,
          (_: List[UnifiedEvent], left: Option[UnifiedEvent], right: Option[UnifiedEvent]) => {
            val leftNode = nodes.get(left.get.activity)
            val path = SorterTree.pathToRoot(leftNode).zipWithIndex
            val foundRightInThePathOpt = path.find(_._1.activity == right.get.activity)
            if (foundRightInThePathOpt.isDefined) {
              val missingNodesWithHead = path.filter(_._2 < foundRightInThePathOpt.get._2).map(_._1)
              if (missingNodesWithHead.nonEmpty) {
                val missingNodes = missingNodesWithHead.tail
                val missingEventsWoTimestamps = missingNodes.map(node =>
                  UnifiedEvent(-1, node.activity)
                    .copy(AbstractDataSource.MaxTsName, right.get.timestamp)
                    .copy(AbstractDataSource.Ttp, node.ttp)
                )
                val shiftedTtp = path.map(_._1).map(_.ttp)
                val missingEvents = missingEventsWoTimestamps
                  .zip(shiftedTtp)
                  .foldLeft((left.get.timestamp, List[UnifiedEvent]()))((z: (Long, List[UnifiedEvent]), e: (UnifiedEvent, Long)) => {
                    val newEvent = e._1.copy(z._1 + e._2)
                    (newEvent.timestamp, newEvent :: z._2)
                  })._2.reverse
                missingEvents
              } else List()
            } else {
              logger.warn(s"Cannot find path from '${leftNode.get.activity}' to '${right.get.activity}'")
              List()
            }
          }
        )
      }


      if (level > 2) {
        val sorted = UnifiedEventLog
          .fromTraces(sortEvents(retLog.traces().toList, tracesSortedByScanner97.traces().map(t => t._1).toList))
        if (level == 3) sorted else {
          sorted.mapSubtrace(
            !_.hasAttribute(AbstractDataSource.MaxTsName),
            (middle: List[UnifiedEvent], left: Option[UnifiedEvent], right: Option[UnifiedEvent]) => {
              if (right.isDefined) {
                middle.foldRight((right.get.timestamp, List[UnifiedEvent]()))((e: UnifiedEvent, z: (Long, List[UnifiedEvent])) => {
                  val currentMaxTs = e.attributes(AbstractDataSource.MaxTsName).asInstanceOf[Long]
                  val ttp = e.attributes(AbstractDataSource.Ttp).asInstanceOf[Long]
                  val newMaxTs = math.min(currentMaxTs, z._1 - ttp)
                  (newMaxTs, e.copy(AbstractDataSource.MaxTsName, newMaxTs) :: z._2)
                })._2
              } else middle
            }
          )
        }
      }
      else retLog

    }
    if (exportAggregatedLog)
      CsvWriter.logToCsvLocalFilesystem(finalLog, s"$eventLogPath/out/right_starts3.csv", exportCsvHelper.timestamp2String)
    finalLog


  }

  override def levels: Vector[String] = FakeDataSource.Levels
}

