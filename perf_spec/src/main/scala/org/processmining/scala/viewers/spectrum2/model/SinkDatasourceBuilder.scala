package org.processmining.scala.viewers.spectrum2.model

import javax.swing.SwingUtilities
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.processmining.scala.log.common.enhancment.segments.common.{AbstractDurationClassifier, KryoFactory, Normal12VerySlowDurationClassifier}
import org.processmining.scala.viewers.spectrum.view.AppSettings
import org.processmining.scala.viewers.spectrum2.pqr.{PlaceTransition, SystemLayout}
import org.processmining.scala.viewers.spectrum2.view.{PqrPacketsSinkServer, SortingOrderEntry}
import org.slf4j.LoggerFactory

object SinkDatasourceBuilder {

  private val logger = LoggerFactory.getLogger(classOf[SinkClient].getName)
  val kryo = KryoFactory()
  val classifier: AbstractDurationClassifier = new Normal12VerySlowDurationClassifier
  val DefualtEndTimeMs = 3 * 60000L

  def apply(allSegmentNames: Vector[SegmentName], initSortingOrder: Vector[SegmentName]) = {

    new AbstractDataSource {

      private var currentStartTimeMs = 0L
      private var currentEndTimeMs = DefualtEndTimeMs
      private var fakeEndTimeMs = currentEndTimeMs
      private var fakeEndTimeMsMode = false
      val udpServer = new SimPacketsSinkServer(AppSettings(AppSettings.DefaultFileName).psmServerPort, callbackSim)
      val pqrServer = new PqrPacketsSinkServer(AppSettings(AppSettings.DefaultFileName).pqrServerPort, callbackPqr)
      val threadSim = new Thread(udpServer) {
        start()
      }

      val threadPqr = new Thread(pqrServer) {
        start()
      }


      private def callbackSim(segmentName: SegmentName, segment: Segment): Unit = {
        SwingUtilities.invokeLater(() => {
          segmentOccurrence(segmentName, segment)
        }
        )
      }

      def sortingOrderEntryFactory(segmentName: SegmentName, soe: SortingOrderEntry) =
        SortingOrderEntry("", soe.segmentNames :+ segmentName, Map())

      def removeSortingOrderEntry(segmentName: SegmentName, soe: SortingOrderEntry) =
        SortingOrderEntry("", soe.segmentNames.filter(_ != segmentName), Map())

      override def pqrCommand(pt: PlaceTransition): Unit = {

        def update(segmentName: SegmentName): Unit = {
          if (pt.button == 1) //add
            {
              if (!so.head.segmentNames.contains(segmentName)) {
                so = Vector(sortingOrderEntryFactory(segmentName, so.head))
                onReset()
              }
            } else //remove
            {
              if (so.head.segmentNames.contains(segmentName)) {
                so = Vector(removeSortingOrderEntry(segmentName, so.head))
                onReset()
              }
            }
        }

        logger.debug(pt.toStringLogger)
        if (!pt.isTransition) {
          val parts = pt.name.split(SystemLayout.PlSep)
          val segmentName = SegmentName(parts(0).replace(s"${SystemLayout.Sep}${SystemLayout.Complete}", ""), parts(1).replace(s"${SystemLayout.Sep}${SystemLayout.Start}", ""))
          update(segmentName)

        } else {
          val parts = pt.name.split("\\" + SystemLayout.Sep)

          val segmentName =
            if ( /*parts(0) == SystemLayout.Start || */ parts(0) == SystemLayout.Complete)
              SegmentName(s"${parts(1)}${SystemLayout.Sep}${SystemLayout.Complete}${SystemLayout.QSep}${parts(1)}${SystemLayout.Sep}${SystemLayout.Start}")
              else if (parts(0) == SystemLayout.Enqueue || parts(0) == SystemLayout.Dequeue) {
              val act = parts(1).split(SystemLayout.QSep)
              SegmentName(s"${act(0)}${SystemLayout.Sep}${SystemLayout.Complete}${SystemLayout.QSep}${act(1)}${SystemLayout.Sep}${SystemLayout.Start}")
            } else if (parts(0) == SystemLayout.Start && pt.qpr == 1) SegmentName(s"${parts(1)}${SystemLayout.Sep}${SystemLayout.Start}${SystemLayout.QSep}${parts(1)}${SystemLayout.Sep}${SystemLayout.Complete}")
            else SegmentName("", "")

          if (segmentName.b.nonEmpty)
            update(segmentName)
        }
      }

      private def callbackPqr(pt: PlaceTransition): Unit = {
        SwingUtilities.invokeLater(() => {
          pqrCommand(pt)
        }
        )
      }

      private var so: Vector[SortingOrderEntry] = AbstractDataSource.buildSortingOrderFromSegmentNames(initSortingOrder)

      override def changeOrder(from: Int, to: Int): Unit = {
        val v = so.head.segmentNames
        val s = v.size
        val newV =
          if (from > to) (v.dropRight(s - to) :+ v(from)) ++ v.zipWithIndex.filter(_._2 != from).map(_._1).drop(to)
          else (
            v.zipWithIndex.filter(x => x._2 < from).map(_._1) ++
              v.zipWithIndex.filter(x => x._2 > from && x._2 <= to).map(_._1) :+ v(from)) ++ v.zipWithIndex.filter(x => x._2 > to).map(_._1)
        so = Vector(SortingOrderEntry("", newV, Map()))
      }

      override def remove(i: Int): Unit = {
        val v = so.head.segmentNames
        val newV = v.zipWithIndex.filter(x => x._2 < i).map(_._1) ++ v.zipWithIndex.filter(x => x._2 > i).map(_._1)
        so = Vector(SortingOrderEntry("", newV, Map()))
      }

      override def sortingOrder(): Vector[SortingOrderEntry] = so

      override def reset(): Unit = {
        data = Map()
        stat = Map()
        currentStartTimeMs = 0L
        fakeEndTimeMsMode = true
        currentEndTimeMs = DefualtEndTimeMs
      }


      override def setFakeEndTimeMs(timeMs: Long): Unit = {
        fakeEndTimeMs = timeMs
        fakeEndTimeMsMode = true
      }

      override def restoreEndTimeMs(): Unit =
        fakeEndTimeMsMode = false


      private var data = Map[SegmentName, Vector[Segment]]()

      private var stat = Map[SegmentName, DescriptiveStatistics]()

      override def startTimeMs: Long = currentStartTimeMs

      override def endTimeMs: Long = if (fakeEndTimeMsMode) fakeEndTimeMs else currentEndTimeMs

      override val segmentNames: Vector[SegmentName] = allSegmentNames

      override def segments: Map[SegmentName, Vector[Segment]] = data

      override def caseIdToOriginActivityAndIndex: Map[String, (String, Int)] = Map() // TODO: not impl.

      private def segmentOccurrence(name: SegmentName, segment: Segment): Unit = {

        if (name.a == "Update") {
          onTimer(segment.start.minMs)
        }else if (segment.start.minMs == -1) {
          reset()
          onReset()
        }
        else {
          val entry = data.get(name)
          data += name -> (if (entry.isDefined) entry.get :+ segment else Vector(segment))
          currentEndTimeMs = Math.max(currentEndTimeMs, segment.end.maxMs)
          currentStartTimeMs = Math.min(currentStartTimeMs, segment.end.minMs)
          val optStat = stat.get(name)
          val duration = segment.end.minMs - segment.start.minMs
          if (optStat.isDefined) {
            optStat.get.addValue(duration)
          } else {
            val ds = new DescriptiveStatistics()
            ds.addValue(duration)
            stat += name -> ds
          }
          val item = name -> Vector(segment)
          onSegmentOccurrence(Map() + item)
          //logger.info(s"segmentName='$name'")
        }
      }

      override def classify(name: SegmentName, durationMs: Long): (Int, Int) = {
        val optStat = stat.get(name)
        if (optStat.isDefined) {
          val median = optStat.get.getPercentile(50)
          (classifier.classify(durationMs, 0, median, 0, "", 0, "", 0), optStat.get.getMin.toInt)

          //          if (durationMs <= 1.1 * x) 0
          //          else if (durationMs <= 1.6 * x) 1
          //          else 2
          //        } else 0
        } else (0, 0)
      }

      override val isClassificationSupported = true
    }

  }
}
