package org.processmining.scala.sim.sortercentric.items

import java.awt.Color

import org.processmining.scala.viewers.spectrum2.model.Separator

case class SensorConfig(id: String, positionMs: Long) {
  override def toString: String = s"'$id'@${positionMs}"
}

case class ConveyorConfig(id: String,
                          lengthMs: Long,
                          sensors: List[SensorConfig],
                          routingTable: Set[String],
                          isSorter: Boolean,
                          isQueue: Boolean,
                          routing: Set[String] => Set[String],
                          delayOnTsuReceived: (TsuState, AbstractConveyor, SystemConfig) => Long,
                          updateTsuOnExit: TsuState => TsuState
                         ) {
  override def toString: String = {
    val routingTableString = routingTable.mkString("; ")
    s"'$id' L=$lengthMs ${sensors.mkString(" ")} $routingTableString ${if (isSorter) " sorter" else ""} ${if (isQueue) " Q" else ""}"
  }
}

object ConveyorConfig {

  def defaultDelayOnTsuReceived(tsu: TsuState, conv: AbstractConveyor, conf: SystemConfig) = 0L

  val Empty = new ConveyorConfig("Empty", 0, List(), Set(), false, false,
    x => x, ConveyorConfig.defaultDelayOnTsuReceived, x => x)

  def apply(id: String,
            lengthMs: Long,
            isSorter: Boolean,
            routingTable: Set[String] = Set(),
            routing: Set[String] => Set[String] = x => x,
            delayOnTsuReceived: (TsuState, AbstractConveyor, SystemConfig) => Long = defaultDelayOnTsuReceived
           ) = {
    //val sensorPositions = List((0.25 * lengthMs).toLong, (0.5 * lengthMs).toLong, (0.75 * lengthMs).toLong)
    val sensorPositions = List[Long]()

    new ConveyorConfig(id, lengthMs,
      sensorPositions.zipWithIndex.map(x => SensorConfig(s"${id}_${x._2}", x._1)),
      routingTable, isSorter, false, routing, delayOnTsuReceived, x => x
    )
  }

  def queue(id: String,
            lengthMs: Long,
            routing: Set[String] => Set[String] = x => x,
            delayOnTsuReceived: (TsuState, AbstractConveyor, SystemConfig) => Long = defaultDelayOnTsuReceived
           ) = {

    new ConveyorConfig(id, lengthMs,
      List(),
      Set(), false, true, routing, delayOnTsuReceived, x => x
    )
  }
}

case class Tsu(id: String, src: String, dst: Set[String], lengthMs: Long)

case class TsuState(tsu: Tsu, headPositionMs: Long) {
  def backPositionMs(config: ConveyorConfig) =
    headPositionMs - (if (config.isSorter) tsu.lengthMs / 2 else tsu.lengthMs)
}


case class ConveyorState(tsu: Vector[TsuState],
                         timeMs: Long,
                         blockedUntilMs: Long,
                         wasMovedDuringLastStep: Boolean) {
  override def toString = s"${tsu.mkString("; ")} @$timeMs blockedUntil $blockedUntilMs wasMovedDuringLastStep=$wasMovedDuringLastStep"

  def isBlocked() = blockedUntilMs != ConveyorState.NonBlocked
}

object ConveyorState {
  val NonBlocked = -1L

  def apply() = new ConveyorState(Vector(), 0, NonBlocked, true)
}

case class DivertingUnit(positionMs: Long, conveyor: AbstractConveyor, sensorId: String) {
  override def toString = s"'${conveyor.id}'@$positionMs logs '$sensorId'"
}

case class ConveyorConnections(input: AbstractConveyor,
                               output: AbstractConveyor,
                               mergePositionMs: Long,
                               divertingConveyors: List[DivertingUnit],
                               mergingConveyors: List[(AbstractConveyor, String)]) {

  lazy val sensorsForExtendedMergeDivert: List[SensorConfig] = {
    val divertingSensors = divertingConveyors
      .map(x => SensorConfig(x.sensorId, x.positionMs))

    val mergingSensors = mergingConveyors
      .map(x => SensorConfig(s"${x._1.id}${Separator.S}${x._2}", x._1.connections.mergePositionMs))

    divertingSensors ::: mergingSensors
  }

  override def toString: String =
    s"\tInput: ${input.id} ${SystemConfig.NewLine}" +
      s"\tOutput: ${output.id} ${SystemConfig.NewLine}" +
      s"\tMerging at: ${mergePositionMs}${SystemConfig.NewLine}" +
      s"\tDiverting to: ${divertingConveyors.mkString(SystemConfig.NewLine)}" +
      s"\tMerging conveyor: ${mergingConveyors.size}"
}

object ConveyorConnections {
  def apply() = new ConveyorConnections(null, null, 0, List(), List())
}

abstract class AbstractConveyor {
  val config: ConveyorConfig

  var connections: ConveyorConnections

  var state: ConveyorState

  override def toString: String = s"$config${SystemConfig.NewLine}" +
    s"$connections"

  s"\tState: $state"

  def id = config.id

  def copyConfig(newConfig: ConveyorConfig): AbstractConveyor
}

object Terminator extends AbstractConveyor {
  override val config = ConveyorConfig.Empty
  override var connections = ConveyorConnections()
  override var state = ConveyorState()

  override def copyConfig(newConfig: ConveyorConfig): AbstractConveyor = Terminator
}

case class Conveyor(override val config: ConveyorConfig,
               override var connections: ConveyorConnections,
               override var state: ConveyorState
              ) extends AbstractConveyor {
  override def copyConfig(newConfig: ConveyorConfig): AbstractConveyor = copy(config = newConfig)
}


object Conveyor {
  def apply(config: ConveyorConfig): Conveyor = new Conveyor(config, ConveyorConnections(), ConveyorState())
}