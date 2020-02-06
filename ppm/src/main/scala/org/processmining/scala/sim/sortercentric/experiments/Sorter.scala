package org.processmining.scala.sim.sortercentric.experiments

import java.awt.Color

import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.utils.common.errorhandling.{EH, JvmParams}
import org.processmining.scala.sim.sortercentric.items.{Conveyor, ConveyorConfig, DivertingUnit, Engine, SensorConfig, SorterCentricSystem, SystemState, Terminator, Tsu, TsuState}
import org.processmining.scala.viewers.spectrum2.model.Separator
import org.slf4j.LoggerFactory

import scala.util.Random


class Sorter extends ExperimentTemplate("simple_sorter_system.csv") {

  val X = "x"
  val Y = "y"
  val Z = "z"
  val QueueMinLength = MaxTsuLengthMs + systemConfig.stepMs
  val SorterBeltLengthMs = 85000
  val rnd = new Random()
  rnd.setSeed(1)
  val sorterEventLogger = new SorterEventLogger()

  val asmIn1 = LinearConveyorAssembly("IN_1_", 63000 - 3 * QueueMinLength, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .addSensor(0, SensorConfig("IN_1_0", systemConfig.stepMs))
  val asmIn2 = LinearConveyorAssembly("IN_2_", 230000 - 3 * QueueMinLength, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .addSensor(0, SensorConfig("IN_2_0", systemConfig.stepMs))
  val asmIn3 = LinearConveyorAssembly("IN_3_", 22300 - 3 * QueueMinLength, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .addSensor(0, SensorConfig("IN_3_0", systemConfig.stepMs))
  val asmIn4 = LinearConveyorAssembly("IN_4_", 27600 - 3 * QueueMinLength, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .addSensor(0, SensorConfig("IN_4_0", systemConfig.stepMs))
  val asmIn5 = LinearConveyorAssembly("IN_5_", 33400 - 3 * QueueMinLength, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .addSensor(0, SensorConfig("IN_5_0", systemConfig.stepMs))
  val asmIn6 = LinearConveyorAssembly("IN_6_", 27200 - 3 * QueueMinLength, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .addSensor(0, SensorConfig("IN_6_0", systemConfig.stepMs))
  val asmIn7 = LinearConveyorAssembly("IN_7_", 21800 - 3 * QueueMinLength, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .addSensor(0, SensorConfig("IN_7_0", systemConfig.stepMs))


  val asmOut1_1 = LinearConveyorAssembly("OUT_1_", 15000, 10000)
    .replace(0, setRoutingTableForBagstoreLinks _)

  val asmOut1_2 = LinearConveyorAssembly("OUT_2_", 15000, 10000)
    .replace(0, setRoutingTableForBagstoreLinks _)

  val asmOut1_3 = LinearConveyorAssembly("OUT_3_", 15000, 10000)
    .replace(0, setRoutingTableForBagstoreLinks _)

  val asmOut2_1 = LinearConveyorAssembly("OUT_4_", 10000, 10000)
    .replace(0, setRoutingTableForFinalSorterLinks _)

  val asmOut2_2 = LinearConveyorAssembly("OUT_5_", 10000, 10000)
    .replace(0, setRoutingTableForFinalSorterLinks _)

  val asmOut2_3 = LinearConveyorAssembly("OUT_6_", 10000, 10000)
    .replace(0, setRoutingTableForFinalSorterLinks _)

  val x = Conveyor(ConveyorConfig(X, SorterBeltLengthMs, true))
  val y = Conveyor(ConveyorConfig(Y, SorterBeltLengthMs, true, Set(asmOut1_1.list.head.id, asmOut2_1.list.head.id)))
  val z = Conveyor(
    ConveyorConfig(Z, SorterBeltLengthMs, true)
      .copy(updateTsuOnExit = tsuState => {
        val tsu1 = tsuState.tsu //.copy(id = tsuState.tsu.id + "@") //!!!!!!!!!!!
        val dst = if (rnd.nextBoolean()) Sorter.DST_FINAL_SORTER else Sorter.DST_BAGSTORE
        val tsu2 = if (tsu1.dst.contains(Sorter.DST_SCREENING_LEVEL_2)) tsu1.copy(dst = Set(dst)) else tsu1
        tsuState.copy(tsu = tsu2)
      })
  )


  def setRoutingTableForScanners(x: ConveyorConfig): ConveyorConfig =
    x.copy(routingTable = Set(Sorter.DST_SCREENING_LEVEL_1))

  def setRoutingTableForFinalSorterLinks(x: ConveyorConfig): ConveyorConfig = x.copy(routingTable = Set(Sorter.DST_FINAL_SORTER))

  def setRoutingTableForBagstoreLinks(x: ConveyorConfig): ConveyorConfig = x.copy(routingTable = Set(Sorter.DST_BAGSTORE))


  def setRoutingActionScanners(x: ConveyorConfig): ConveyorConfig =
    x.copy(routing = _ =>
      if (rnd.nextInt(100) > 80) Set(Sorter.DST_SCREENING_LEVEL_2)
      else if (r.nextBoolean()) Set(Sorter.DST_FINAL_SORTER) else Set(Sorter.DST_BAGSTORE))


  val asmS1 = LinearConveyorAssembly("S1_", 10000, -QueueMinLength, -QueueMinLength, -QueueMinLength, 10000, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .replace(0, setRoutingTableForScanners _)
    .replace(3, setRoutingActionScanners _)

  val asmS2 = LinearConveyorAssembly("S2_", 10000, -QueueMinLength, -QueueMinLength, -QueueMinLength, 10000, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .replace(0, setRoutingTableForScanners _)
    .replace(3, setRoutingActionScanners _)

  val asmS3 = LinearConveyorAssembly("S3_", 10000, -QueueMinLength, -QueueMinLength, -QueueMinLength, 10000, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .replace(0, setRoutingTableForScanners _)
    .replace(3, setRoutingActionScanners _)

  val asmS4 = LinearConveyorAssembly("S4_", 10000, -QueueMinLength, -QueueMinLength, -QueueMinLength, 10000, -QueueMinLength, -QueueMinLength, -QueueMinLength)
    .replace(0, setRoutingTableForScanners _)
    .replace(3, setRoutingActionScanners _)


  override val sorter = List(x, y, z)

  override val executionOrder =
    asmOut1_1.list.reverse :::
      asmOut2_1.list.reverse :::
      asmOut1_2.list.reverse :::
      asmOut1_3.list.reverse :::
      asmOut2_2.list.reverse :::
      asmOut2_3.list.reverse :::
      sorter.reverse :::
      asmS1.list.reverse :::
      asmS2.list.reverse :::
      asmS3.list.reverse :::
      asmS4.list.reverse :::
      asmIn1.list.reverse :::
      asmIn2.list.reverse :::
      asmIn3.list.reverse :::
      asmIn4.list.reverse :::
      asmIn5.list.reverse :::
      asmIn6.list.reverse :::
      asmIn7.list.reverse

  override val system = new SorterCentricSystem(systemConfig, executionOrder, sorter, SystemState())

  private var tsuIdCounter = 0
  private val r = new Random(8)

  private val incomingProcesses = SomeIncomingProcess.processes.map(x => x._1 -> ProcessState(x._2))

  override def incomingProcessHandler(timeMs: Long) = {
    incomingProcesses.foreach(x => {
      val ps = x._2
      ps.nextScheduledEventTimeMs match {
        case ProcessState.NotStarted =>
          if (ps.config.initialTimeShiftMs <= timeMs) {
            ps.startNextScenario(timeMs)
            ps.nextScheduledEventTimeMs = timeMs + ps.nextBagRelTimeMs().toLong
          }
        case ProcessState.Disabled => {}
        case _ =>
          if (timeMs >= ps.nextScheduledEventTimeMs) {

            if (ps.config.action == "bag")
              putBagTo(x._1)
            else
              runCmd(x._1)
            ps.currentBagInScenarioCount = ps.currentBagInScenarioCount + 1
            ps.nextScheduledEventTimeMs = timeMs + ps.nextBagRelTimeMs().toLong
            if (timeMs >= ps.scenarioStopTimeMs) {
              ps.nextScheduledEventTimeMs = ProcessState.NotStarted
              //              if (ps.currentScenarioLoop >= ps.config.scenarioDuration.maxRepetitions)
              //                ps.nextScheduledEventTimeMs = ProcessState.Disabled
            }
          }
      }
    })

  }

  private var veryFirstItem = true

  private def putBagTo(prefix: String): Unit = {
    executionOrder
      .filter(x => x.id.startsWith(prefix))
      .foreach(x => {
        if (x.state.tsu.isEmpty || x.state.tsu.head.backPositionMs(x.config) >= systemConfig.protectedSpaceMs(x.config)) {
          val dst = if (veryFirstItem) {
            veryFirstItem = false
            "FIRST"
          }
          else Sorter.DST_SCREENING_LEVEL_1
          val newTsu = TsuState(Tsu(tsuIdCounter.toString, x.id, Set(dst), 500L + r.nextInt(600)), 0)
          logger.debug(s"Load: new TSU on '${x.id}' '$tsuIdCounter' --> '${newTsu.tsu.dst}'")
          tsuIdCounter = tsuIdCounter + 1
          x.state = x.state.copy(tsu = newTsu +: x.state.tsu)
        }
      })
  }

  override def getColorByDst(dst: Set[String]): Color = if (dst.isEmpty) Color.yellow else dst.head match {
    case Sorter.DST_SCREENING_LEVEL_1 => Color.red
    case Sorter.DST_SCREENING_LEVEL_2 => Color.cyan
    case Sorter.DST_BAGSTORE => Color.blue
    case Sorter.DST_FINAL_SORTER => Color.green
    case _ => Color.orange
  }

  override def makeConnections(): Unit = {
    connect(Terminator, x, 9000, asmIn1)
    connect(Terminator, x, 11200, asmIn2)
    connect(Terminator, x, 14200, asmIn3)
    connect(Terminator, x, 16400, asmIn4)
    connect(Terminator, x, 72700, asmIn5)
    connect(Terminator, x, 80200, asmIn6)
    connect(Terminator, y, 6800, asmIn7)

    connect(z, Terminator, -1, asmOut1_1)
    connect(z, Terminator, -1, asmOut1_2)
    connect(z, Terminator, -1, asmOut1_3)
    connect(z, Terminator, -1, asmOut2_1)
    connect(z, Terminator, -1, asmOut2_2)
    connect(z, Terminator, -1, asmOut2_3)

    x.connections = x.connections
      .copy(input = z)
      .copy(output = y)
      .copy(mergingConveyors = List(
        (asmIn1.list.last, x.id),
        (asmIn2.list.last, x.id),
        (asmIn3.list.last, x.id),
        (asmIn4.list.last, x.id),
        (asmIn5.list.last, x.id),
        (asmIn6.list.last, x.id)
      ))

    val s1PosMs = 144700

    y.connections = y.connections
      .copy(input = x)
      .copy(output = z)
      .copy(mergingConveyors = List(
        (asmIn7.list.last, y.id)
      ))
      .copy(divertingConveyors = List(
        DivertingUnit(s1PosMs - SorterBeltLengthMs, asmS1.list.head, s"${y.id}${Separator.S}${asmS1.list.head.id}"),
        DivertingUnit(s1PosMs - SorterBeltLengthMs + 1300, asmS2.list.head, s"${y.id}${Separator.S}${asmS2.list.head.id}"),
        DivertingUnit(s1PosMs - SorterBeltLengthMs + 2600, asmS3.list.head, s"${y.id}${Separator.S}${asmS3.list.head.id}"),
        DivertingUnit(s1PosMs - SorterBeltLengthMs + 3900, asmS4.list.head, s"${y.id}${Separator.S}${asmS4.list.head.id}")
      ))

    z.connections = z.connections
      .copy(input = y)
      .copy(output = x)
      .copy(divertingConveyors = List(
        DivertingUnit(204000 - 2 * SorterBeltLengthMs, asmOut1_1.list.head, s"${z.id}${Separator.S}${asmOut1_1.list.head.id}"),
        DivertingUnit(204000 + 2200 - 2 * SorterBeltLengthMs, asmOut1_2.list.head, s"${z.id}${Separator.S}${asmOut1_2.list.head.id}"),
        DivertingUnit(204000 + 4400 - 2 * SorterBeltLengthMs, asmOut1_3.list.head, s"${z.id}${Separator.S}${asmOut1_3.list.head.id}"),
        DivertingUnit(244800 - 2 * SorterBeltLengthMs, asmOut2_1.list.head, s"${z.id}${Separator.S}${asmOut2_1.list.head.id}"),
        DivertingUnit(244800 + 2200 - 2 * SorterBeltLengthMs, asmOut2_2.list.head, s"${z.id}${Separator.S}${asmOut2_2.list.head.id}"),
        DivertingUnit(244800 + 4400 - 2 * SorterBeltLengthMs, asmOut2_3.list.head, s"${z.id}-${Separator.S}>${asmOut2_3.list.head.id}"))
      ).copy(mergingConveyors = List(
      (asmS1.list.last, z.id),
      (asmS2.list.last, z.id),
      (asmS3.list.last, z.id),
      (asmS4.list.last, z.id)
    ))

    connect(y, z, 176500 - SorterBeltLengthMs * 2 + 7500, asmS1)
    connect(y, z, 176500 - SorterBeltLengthMs * 2 + 5000, asmS2)
    connect(y, z, 176500 - SorterBeltLengthMs * 2 + 2500, asmS3)
    connect(y, z, 176500 - SorterBeltLengthMs * 2, asmS4)
  }

  override lazy val links = List(
    Link(asmIn1.list, asmIn1.list.last.connections.mergePositionMs, true),
    Link(asmIn2.list, asmIn2.list.last.connections.mergePositionMs, true),
    Link(asmIn3.list, asmIn3.list.last.connections.mergePositionMs, true),
    Link(asmIn4.list, asmIn4.list.last.connections.mergePositionMs, true),
    Link(asmIn5.list, asmIn5.list.last.connections.mergePositionMs, true),
    Link(asmIn6.list, asmIn6.list.last.connections.mergePositionMs, true),
    Link(asmIn7.list, SorterBeltLengthMs + asmIn7.list.last.connections.mergePositionMs, true),
    Link(asmOut1_1.list, SorterBeltLengthMs * 2 + z.connections.divertingConveyors.find(_.conveyor.id == asmOut1_1.list.head.id).get.positionMs, false),
    Link(asmOut1_2.list, SorterBeltLengthMs * 2 + z.connections.divertingConveyors.find(_.conveyor.id == asmOut1_2.list.head.id).get.positionMs, false),
    Link(asmOut1_3.list, SorterBeltLengthMs * 2 + z.connections.divertingConveyors.find(_.conveyor.id == asmOut1_3.list.head.id).get.positionMs, false),
    Link(asmOut2_1.list, SorterBeltLengthMs * 2 + z.connections.divertingConveyors.find(_.conveyor.id == asmOut2_1.list.head.id).get.positionMs, false),
    Link(asmOut2_2.list, SorterBeltLengthMs * 2 + z.connections.divertingConveyors.find(_.conveyor.id == asmOut2_2.list.head.id).get.positionMs, false),
    Link(asmOut2_3.list, SorterBeltLengthMs * 2 + z.connections.divertingConveyors.find(_.conveyor.id == asmOut2_3.list.head.id).get.positionMs, false)

  )

  override lazy val shortcuts = List(
    Shortcut(asmS1.list,
      SorterBeltLengthMs + y.connections.divertingConveyors(0).positionMs,
      SorterBeltLengthMs * 2 + asmS1.list.last.connections.mergePositionMs),
    Shortcut(asmS2.list,
      SorterBeltLengthMs + y.connections.divertingConveyors(1).positionMs,
      SorterBeltLengthMs * 2 + asmS2.list.last.connections.mergePositionMs),
    Shortcut(asmS3.list,
      SorterBeltLengthMs + y.connections.divertingConveyors(2).positionMs,
      SorterBeltLengthMs * 2 + asmS3.list.last.connections.mergePositionMs),
    Shortcut(asmS4.list,
      SorterBeltLengthMs + y.connections.divertingConveyors(3).positionMs,
      SorterBeltLengthMs * 2 + asmS4.list.last.connections.mergePositionMs)

  )
  initialize()

  override def openExtendendLogger() = sorterEventLogger.openExtendendLogger()

  override def extendendLogger(id: String, timestamp: Long, activity: String, flag: Int) = sorterEventLogger.extendendLogger(id, timestamp, activity, flag)

  override def flushExtendendLogger(): Unit = sorterEventLogger.flushExtendendLogger()

  override def closeExtendendLogger(): Unit = sorterEventLogger.closeExtendendLogger()
}

object Sorter {

  val DST_SCREENING_LEVEL_1 = "S1"
  val DST_SCREENING_LEVEL_2 = "S2"
  val DST_BAGSTORE = "A"
  val DST_FINAL_SORTER = "F"

  private val logger = LoggerFactory.getLogger(Sorter.getClass)

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, s"${Sorter.getClass} started")
    try {
      val engine = new Sorter()
      val steps = 20000
      engine.execute(steps)
    } catch {
      case e: Throwable =>
        EH.apply.error(e)
    }
    logger.info(s"App is completed.")
  }

}
