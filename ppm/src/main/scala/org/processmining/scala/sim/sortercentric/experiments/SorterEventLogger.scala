package org.processmining.scala.sim.sortercentric.experiments

import java.io.PrintWriter

import org.processmining.scala.log.utils.common.csv.common.CsvExportHelper
import org.processmining.scala.sim.sortercentric.items.Engine
import org.processmining.scala.viewers.spectrum2.model.{Segment, SegmentEvent, SegmentName, Separator, SinkClient}

private case class Event(caseId: String, timestampMs: Long, activity: String, flag: Int)


class SorterEventLogger {
  private val pwCompleteLog = new PrintWriter("!standard_log.csv")
  private val pwIncompleteLog = new PrintWriter("!incomplete_log.csv")
  private val pwOverlaid2 = new PrintWriter("overlaid_segments_2.csv")
  private val pwOverlaid3 = new PrintWriter("overlaid_segments_3.csv")
  private val pwOverlaid4 = new PrintWriter("overlaid_segments_4.csv")
  private val csvExportHelper = new CsvExportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone, ",")
  private val csvExportHelperOverlaid = new CsvExportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone, ",")
  private val nonStandardActivities = Set(s"IN_[1-7]_3${Separator.S}[xy]", s"z${Separator.S}x", s"y${Separator.S}S[1-4]_0")
  private var s1Cache = Map[String, Long]()
  private var prevEvents = Map[String, Event]()
//  private var prevEventsOfStandardLog = Map[String, (String, Long)]()
//  private val sinkClient = new SinkClient("localhost", 60000)

  def rename(a: String) = {
    //    val opt = mapping.get(a)
    //    if (opt.nonEmpty) opt.get else a
    a
  }


  def openExtendendLogger() = {
    val header = s""""id";"timestamp";"activity""""
    pwCompleteLog.println(header)
    pwIncompleteLog.println(header)
  }


  def logOverlaidSegment(id: String, timestampMs: Long, activity: String, flag: Int) = {
    val e2 = Event(id, timestampMs, activity, flag)
    val optPrevEvent1 = prevEvents.get(id)
    if (optPrevEvent1.isDefined) {
      val e1 = optPrevEvent1.get
      val renameActivity1 = rename(e1.activity)
      val renameActivity2 = rename(e2.activity)

      val line = s""""$id","$renameActivity1","$renameActivity2","${csvExportHelperOverlaid.timestamp2String(e1.timestampMs)}","${csvExportHelperOverlaid.timestamp2String(e2.timestampMs)}","$renameActivity1","$renameActivity2","$renameActivity1","$renameActivity2","000000aa""""
      pwOverlaid2.println(line)
      pwOverlaid3.println(line)
      pwOverlaid4.println(line)
    }
    prevEvents = prevEvents + (id -> e2)
  }

//  private def createSegment(id: String, t1: Long, t2: Long)=
//    Segment(id,
//      new SegmentEvent {
//        override val minMs: Long = t1
//        override val maxMs: Long = t1
//      },
//      new SegmentEvent {
//        override val minMs: Long = t2
//        override val maxMs: Long = t2
//      }, 0)
//
//  private var isFirstPacket = true

  def extendendLogger(id: String, timestampMs: Long, activity: String, flag: Int) = {
    val s1Flag = activity == s"y${Separator.S}S1_0"
    val timestampString = csvExportHelper.timestamp2String(timestampMs)
    if (flag == Engine.StandardEvent ||
      ((flag == Engine.ExtendedMergeDivertEvent || flag == Engine.ExtendedMoveEvent || flag == Engine.StandardMergeEvent)
        && nonStandardActivities.exists(activity.matches))) {
      pwCompleteLog.println(s""""$id";"$timestampString";"${rename(activity)}"""")
//      val prevOpt = prevEventsOfStandardLog.get(id)
//      if (prevOpt.isDefined) {
//        if(isFirstPacket) {
//          sinkClient.send(SegmentName("X", "Y"), createSegment(id, -1, -1))
//          isFirstPacket = false
//        }
//        sinkClient.send(SegmentName(prevOpt.get._1, activity), createSegment(id, prevOpt.get._2, timestampMs))
//      }
//      prevEventsOfStandardLog = prevEventsOfStandardLog + (id -> (activity, timestampMs))
      logOverlaidSegment(id, timestampMs, activity, flag)
    }

    if (id == "3") {
      println()
    }

    if (flag == Engine.StandardEvent || s1Flag ||
      ((flag == Engine.StandardDivertEvent || flag == Engine.StandardMergeEvent || flag == Engine.ExtendedMoveEvent)
        && nonStandardActivities.exists(activity.matches))) {

      val optTimestampForS1 = s1Cache.get(id)
      val recordEvent =
        if (!s1Flag) true else if (optTimestampForS1.isEmpty || timestampMs - optTimestampForS1.get > 150000) true else false

      if (recordEvent)
        pwIncompleteLog.println(s""""$id";"$timestampString";"${rename(activity)}"""")
      if (s1Flag)
        s1Cache = s1Cache + (id -> timestampMs)
    }
  }

  def flushExtendendLogger(): Unit = {
    pwCompleteLog.flush()
    pwIncompleteLog.flush()
    pwOverlaid2.flush()
    pwOverlaid3.flush()
    pwOverlaid4.flush()
  }

  def closeExtendendLogger(): Unit = {
    pwCompleteLog.close()
    pwIncompleteLog.close()
    pwOverlaid2.close()
    pwOverlaid3.close()
    pwOverlaid4.close()
  }
}
