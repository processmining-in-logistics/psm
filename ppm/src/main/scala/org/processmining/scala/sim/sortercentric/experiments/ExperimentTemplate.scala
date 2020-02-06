package org.processmining.scala.sim.sortercentric.experiments

import java.awt.Color
import java.io.PrintWriter

import org.processmining.scala.sim.sortercentric.items.{AbstractConveyor, AbstractSystem, Conveyor, ConveyorConfig, ConveyorState, Engine, ExpIncomingProcessesAndCommands, SensorConfig, SystemConfig}
import org.slf4j.LoggerFactory

case class Link(orderedBelts: List[AbstractConveyor], positionMs: Long, isIncoming: Boolean)

case class LinearConveyorAssembly(list: List[AbstractConveyor]) {

  def replace(id: String, updateConfig: (ConveyorConfig) => ConveyorConfig): LinearConveyorAssembly =
    if (list.exists(_.id == id)) copy(list = list.map(x => if (x.id == id) x.copyConfig(updateConfig(x.config)) else x))
    else throw new IllegalArgumentException(s"Cannot find conveyor '$id' in ${list.map(_.id).mkString("'", ";", "'")}")


  def replace(index: Int, updateConfig: (ConveyorConfig) => ConveyorConfig): LinearConveyorAssembly =
    replace(id(index), updateConfig)

  def id(index: Int) = list(index).id

  def addSensor(index: Int, s: SensorConfig) = {
    def tmp(x: ConveyorConfig) = x.copy(sensors = s :: x.sensors)

    replace(index, tmp _)
  }

}

object LinearConveyorAssembly {
  def apply(prefix: String, lengthMs: Long*) =
    new LinearConveyorAssembly(lengthMs
      .zipWithIndex
      .map(x => if (x._1 >= 0) Conveyor(ConveyorConfig(s"$prefix${x._2}", x._1, false)) else Conveyor(ConveyorConfig(s"$prefix${x._2}", -x._1, true)))
      .toList)


}


case class Shortcut(orderedBelts: List[AbstractConveyor], startPositionMs: Long, endPositionMs: Long) {
  def totalLenghtMs() = orderedBelts.map(_.config.lengthMs).sum
}

abstract class ExperimentTemplate(eventLogFileName: String) {

  val logger = LoggerFactory.getLogger(classOf[ExperimentTemplate].getName)
  val executionOrder: List[AbstractConveyor]
  val sorter: List[AbstractConveyor]
  val system: AbstractSystem

  def getColorByDst(dst: Set[String]): Color = Color.green


  val pw = new PrintWriter(eventLogFileName)
  val StepMs = 100L
  val MaxTsuLengthMs = 1100L
  val systemConfig = SystemConfig(StepMs, MaxTsuLengthMs, MaxTsuLengthMs, MaxTsuLengthMs * 2 / 2)

  def eventLogger(s: String) = {
    logger.info(s)
    pw.println(s)
  }

  def openExtendendLogger()

  def closeExtendendLogger()

  def flushExtendendLogger()

  def extendendLogger(id: String, timestamp: Long, activity: String, flag: Int)


  def makeConnections()

  //must not be called before makeConnections
  def links(): List[Link]

  def shortcuts(): List[Shortcut]

  // must be called after setting up all the conenctions
  def initialize() = {
    makeConnections()
    executionOrder.foreach(x => {
      x.connections.divertingConveyors.foreach(d => if (d.positionMs >= x.config.lengthMs || d.positionMs < 0)
        throw new IllegalArgumentException(s"Wrong position of diverting unit $d for $x")
      )

    })
    logger.info(links().mkString("; ").toString())
    logger.info(system.toString)
  }

  def flush()= {
    pw.flush()
    flushExtendendLogger()
  }

  def initAndGetEngine() = {
    pw.println(s""""case_id","timestamp","activity","flag"""")
    openExtendendLogger()
    new Engine(system, eventLogger, flush, incomingProcessHandler, extendendLogger)
  }

  def unititialize() = {
    pw.close()
    closeExtendendLogger()
  }

  def incomingProcessHandler(timeMs: Long): Unit

  def execute(n: Int): Unit = {
    try {
      initAndGetEngine().execute(n)
    } catch {
      case e: Throwable => {
        logger.error(e.toString)
        unititialize()
        throw e
      }
    }
    unititialize()
  }

  def blockElement(id: String, untilMs: Long) = {
    logger.info(s"Blocking '$id' until $untilMs ms")
    val optItem = executionOrder.find(_.id == id)
    if (optItem.nonEmpty) {
      val item = optItem.get
      item.state = item.state.copy(blockedUntilMs = if (untilMs != ConveyorState.NonBlocked) item.state.timeMs + untilMs else ConveyorState.NonBlocked)
    } else throw new IllegalArgumentException(s"Wrong id='$id'")

  }

  def createLinearPath(prefix: String, lengthMs: Long*) =
    lengthMs
      .zipWithIndex
      .map(x => if (x._1 >= 0) Conveyor(ConveyorConfig(s"$prefix${x._2}", x._1, false)) else Conveyor(ConveyorConfig(s"$prefix${x._2}", -x._1, true)))
      .toList

  def replace(list: List[Conveyor], id: String, copy: ConveyorConfig => ConveyorConfig) =
    if (list.exists(_.id == id))
      list.map(x => if (x.id == id) x.copyConfig(copy(x.config)) else x)
    else throw new IllegalArgumentException(s"Cannot find conveyor '$id' in ${list.map(_.id).mkString("'", ";", "'")}")


  def connect(start: AbstractConveyor, end: AbstractConveyor, mergePositionMs: Long, conv: LinearConveyorAssembly): Unit =
    connect(start, end, mergePositionMs, conv.list: _*)

  def connect(start: AbstractConveyor, end: AbstractConveyor, mergePositionMs: Long, conv: AbstractConveyor*): Unit = {
    conv.head.connections = conv.head.connections.copy(input = start)
    conv.last.connections = conv.last.connections.copy(output = end)
    if (mergePositionMs >= 0) {
      conv.last.connections = conv.last.connections.copy(mergePositionMs = mergePositionMs)
    }
    (0 until conv.size - 1)
      .map(i => {
        val a = conv(i)
        val b = conv(i + 1)
        a.connections = a.connections.copy(output = b)
        b.connections = b.connections.copy(input = a)
      })


  }


  def runCmd(cmd: String) = {
    val splitAsList = cmd.split("\\s+").toList
    splitAsList match {
      case "block" :: id :: timeMsString :: Nil => blockElement(id, timeMsString.toLong)
      case "block" :: id :: Nil => blockElement(id, Long.MaxValue / 2) // TODO: MaxValue
      case "unblock" :: id :: Nil => blockElement(id, ConveyorState.NonBlocked)
      case "unblock" :: id :: _ :: Nil => blockElement(id, ConveyorState.NonBlocked)
      case _ => throw new IllegalArgumentException(cmd)
    }

  }


}
