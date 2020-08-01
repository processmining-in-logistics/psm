package org.processmining.scala.sim.sortercentric.items

case class SystemConfig(stepMs: Long,
                        maxTsuLengthMs: Long,
                        protectedDistanceLinearConveyorMs: Long,
                        protectedSpaceSorterMs: Long) {
  override def toString: String = s"stepMs=$stepMs maxTsuLengthMs=$maxTsuLengthMs protectedDistanceLinearConveyorMs=$protectedDistanceLinearConveyorMs protectedSpaceSorterMs=$protectedSpaceSorterMs"

  def protectedSpaceMs(conveyorConfig: ConveyorConfig): Long =
    if (conveyorConfig.isQueue) 0
    else {
      if (conveyorConfig.isSorter) protectedSpaceSorterMs else protectedDistanceLinearConveyorMs
    }
}

object SystemConfig {
  val NewLine = "\n\r"
}

case class SystemState(timeMs: Long) {
  override def toString: String = s"current time=$timeMs"
}

object SystemState {
  //val someInitialDate = 1572519600000L
  def apply() = new SystemState(0)
}


abstract class AbstractSystem {
  val config: SystemConfig
  val executionOrder: List[AbstractConveyor]
  val sorter: List[AbstractConveyor]
  var state: SystemState
  //val loadGenerators: List[(Long, List[AbstractConveyor]) => Unit]

  override def toString: String = s"System config: $config${SystemConfig.NewLine}" +
    s"Execution order:${SystemConfig.NewLine}${executionOrder.mkString(SystemConfig.NewLine)}${SystemConfig.NewLine}" +
    s"Sorter conveyors: ${sorter.map(x => s"'${x.config.id}'").mkString(", ")}${SystemConfig.NewLine}" +
    s"System state: $state"
}

class SorterCentricSystem(override val config: SystemConfig,
                          override val executionOrder: List[AbstractConveyor],
                          override val sorter: List[AbstractConveyor],
                          override var state: SystemState) extends AbstractSystem

