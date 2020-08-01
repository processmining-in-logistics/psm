package org.processmining.scala.sim.conveyors.impl

import java.io.{File, PrintWriter}
import java.time.Instant
import java.util.concurrent.Callable

import org.processmining.scala.log.utils.common.csv.common.CsvExportHelper
import org.processmining.scala.sim.conveyors.api.{AbstractEquipment, Tsu}
import org.slf4j.LoggerFactory

import scala.util.Random


abstract class AbstractEngine(eventLogFileName: String,
                              stepMs: Long,
                              val startTime: Long,
                              val seed: Int,
                              numberOfIterations: Int) extends Runnable{


  protected val logger = LoggerFactory.getLogger(classOf[AbstractEngine])

  protected val EventLogHeader = s"CaseID,Timestamp,Activity,${AbstractEngine.Field0}"

  private val rnd = new Random(seed)

  protected def uniformDelay(interval: (Int, Int)) = interval._1 + rnd.nextInt(interval._2 - interval._1)

  protected val divertMergeFunctions: DivertMergeFunctions = new DivertMergeFunctions(rnd)

  protected val state: scala.collection.mutable.Map[String, AbstractEquipment] = scala.collection.mutable.Map()
  private val sorters: scala.collection.mutable.Map[String, Sorter] = scala.collection.mutable.Map()

  private val printWriter = new PrintWriter(new File(eventLogFileName))

  protected val dateHelper = new CsvExportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone, ",")

  def addItem(item: AbstractEquipment): Unit = state += (item.name -> item)

  def addSorter(sorter: Sorter): Unit = sorters += (sorter.name -> sorter)

  protected def headToHeadMinDistanceMs: Long

  protected def tsuFactory(a: AbstractEquipment): Tsu

  protected def addConveyor(name: String, durationMs: Long, sensors: Seq[(String, Double)]): Unit =
    addConveyor(name, durationMs, 1.0, sensors)

  protected def addConveyor(name: String, address: String, durationMs: Long, speed: Double, sensors: Seq[(String, Double)]): Unit =
    addItem(ConveyorItem(new ConveyorItemConfig(durationMs, speed, sensors, headToHeadMinDistanceMs, eventLogger), name, address))

  protected def addSingleTsuBlock(name: String, function: Tsu => Long): Unit =
    addItem(SingleTsuBlock(SingleTsuBlockConfig(function, eventLogger), name))


  protected def addConveyor(name: String, durationMs: Long, speed: Double, sensors: Seq[(String, Double)]): Unit =
    addConveyor(name, name, durationMs, speed, sensors)

  protected def addConveyor(name: String, durationMs: Long, sensorsStepPercents: Int): Unit =
    addConveyor(name, durationMs, 1.0, sensorsStepPercents)

  protected def addConveyor(name: String, address: String, durationMs: Long, sensorsStepPercents: Int): Unit =
    addConveyor(name, address, durationMs, 1.0, sensorsStepPercents)



  protected def addConveyor(name: String, address:String, durationMs: Long, speed: Double, sensorsStepPercents: Int): Unit =
    addConveyor(name, address, durationMs, speed,
      (0 until 100 by sensorsStepPercents)
        .zipWithIndex
        .map(x => (s"${name}_${x._2}", x._1.toDouble)))

  protected def addConveyor(name: String, durationMs: Long, speed: Double, sensorsStepPercents: Int): Unit =
    addConveyor(name, name, durationMs, speed, sensorsStepPercents)


  protected def addSorter(name: String, traysNumber: Int, trayLengthMs: Long, sensorsNumber: Int, gap: Int): Unit = {
    val z: Option[Tsu] = None
    addSorter(Sorter(
      SorterConfig(
        traysNumber, trayLengthMs,
        (0 until sensorsNumber)
          .map(x => (s"$name`$x", x * (traysNumber * trayLengthMs / sensorsNumber))),
        eventLogger, gap),
      (0 until traysNumber).map(_ => z).toVector, 0, 0, name, name, 0))
  }

  override def run(): Unit = {
    init()
    onStart()
    printWriter.println(EventLogHeader)
    (0 until numberOfIterations).foreach(step => executeOneStep(stepMs, step * stepMs))
    printWriter.close()
    onStop()
  }

  protected def onStart() = {}

  protected def onStop() = {}

  protected def executeOneStep(stepMs: Long, simTimeMs: Long)

  protected def init(): Unit


  def addNewTsuIfPossible(aName: String): Unit = {
    var a = state(aName)
    a = if (a.canLoad()) a.put(tsuFactory(a)) else a
    state(aName) = a
  }


  protected def connect(aName: String, bName: String): Unit = connect(aName, bName, x => x)

  protected def connect(aName: String, bName: String, bagFunc: Tsu => Tsu): Unit = {
    val a = state(aName)
    val b = state(bName)
    val (a1, b1) =
      if (a.shouldOffload()) {
        if (b.canLoad()) {
          val (originalBag, newA) = a.move(stepMs)
          val bag = if (originalBag.isDefined) Some(bagFunc(originalBag.get)) else originalBag
          (newA, b.put(bag.get))
        } else (a.adjustTime(stepMs), b)
      } else (a.move(stepMs)._2, b)
    state(aName) = a1
    state(bName) = b1
  }

  protected def consumeAndMove(bName: String): Unit = state(bName) = state(bName).move(stepMs)._2

  protected def fromSorter(sName: String, aName: String, posMs: Long, canOffload: (Tsu, Sorter, AbstractEquipment) => (Boolean, Tsu)): Unit = {
    var s = sorters(sName)
    var a = state(aName)
    if (a.canLoad() && s.peek(posMs).isDefined) {
      val (canLoadFlag, updatedTsu) = canOffload(s.peek(posMs).get, s, a)
      s = s.updateTsu(updatedTsu)
      if (canLoadFlag) {
        val (bag, newS) = s.offload(posMs)
        a = a.put(bag)
        s = newS
      }
    }
    sorters(sName) = s
    state(aName) = a
  }

  protected def moveSorter(sName: String): Unit = {
    val s = sorters(sName)
    sorters(sName) = s.move(stepMs)
  }

  protected def connectWithSorter(aName: String, sName: String, posMs: Long): Unit = {
    val s = sorters(sName)
    val a = state(aName)
    if (a.shouldOffload()) {
      if (s.canLoad(posMs)) {
        val (bag, newA) = a.move(stepMs)
        val newS = s.load(posMs, bag.get)
        sorters(sName) = newS
        state(aName) = newA
      } else {
        state(aName) = a.adjustTime(stepMs)
      }
    } else {
      val (_, newA) = a.move(stepMs)
      state(aName) = newA
    }
  }


  protected def divert(aName: String, b1Name: String, b2Name: String, divertFunc: (Tsu, AbstractEquipment, AbstractEquipment) => Int): Unit = {
    var a = state(aName)
    val b1 = state(b1Name)
    val b2 = state(b2Name)
    val (newB1, newB2) = if (a.shouldOffload()) {
      val dest = divertFunc(a.peek(), b1, b2)
      if (dest != DivertMergeFunctions.Stop) {
        val (bag, newA) = a.move(stepMs)
        a = newA
        dest match {
          case DivertMergeFunctions.First =>
            (b1.put(bag.get), b2)
          case DivertMergeFunctions.Second =>
            (b1, b2.put(bag.get))
        }
      } else {
        a = a.adjustTime(stepMs)
        (b1, b2)
      }
    } else {
      a = a.move(stepMs)._2
      (b1, b2)
    }
    state(aName) = a
    state(b1Name) = newB1
    state(b2Name) = newB2
  }

  protected def merge(a1Name: String, a2Name: String, bName: String, mergeFunc: () => Int): Unit = {
    var a1 = state(a1Name)
    var a2 = state(a2Name)
    var b = state(bName)
    val srcState = (a1.shouldOffload(), a2.shouldOffload(), b.canLoad())
    srcState match {
      case (false, false, _) => {
        a1 = a1.move(stepMs)._2
        a2 = a2.move(stepMs)._2
      }
      case (_, _, false) => {
        a1 = if (srcState._1) a1.adjustTime(stepMs) else a1.move(stepMs)._2
        a2 = if (srcState._2) a2.adjustTime(stepMs) else a2.move(stepMs)._2
      }

      case (true, false, true) => {
        a2 = a2.move(stepMs)._2
        val (bag, newA1) = a1.move(stepMs)
        a1 = newA1
        b = b.put(bag.get)
      }

      case (false, true, true) => {
        a1 = a1.move(stepMs)._2
        val (bag, newA2) = a2.move(stepMs)
        a2 = newA2
        b = b.put(bag.get)
      }

      case (true, true, true) =>
        if (mergeFunc() == DivertMergeFunctions.First) {
          a2 = a2.adjustTime(stepMs)
          val (bag, newA1) = a1.move(stepMs)
          a1 = newA1
          b = b.put(bag.get)
        } else {
          a1 = a1.adjustTime(stepMs)
          val (bag, newA2) = a2.move(stepMs)
          a2 = newA2
          b = b.put(bag.get)
        }
    }
    state(a1Name) = a1
    state(a2Name) = a2
    state(bName) = b
  }


  protected def logEvent(pw: PrintWriter, caseId: String, timeMs: Long, activity: String, field0: String) =
    pw.println(s"$caseId,${dateHelper.timestamp2String(startTime + timeMs)},$activity,$field0")

  protected def eventLogger(caseId: String, timeMs: Long, activity: String): Unit = {
    logEvent(printWriter, caseId, timeMs, activity, "0")

  }


}

object AbstractEngine{
  val Field0 = "Field0"
}