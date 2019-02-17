package org.processmining.scala.sim.conveyors.impl

import org.processmining.scala.sim.conveyors.api.{AbstractEquipment, Tsu}
import org.slf4j.LoggerFactory

case class SingleTsuBlockConfig(delayMs: Tsu => Long,
                                eventLogger: (String, Long, String) => Unit
                               ) extends Serializable

case class SingleTsuBlock(config: SingleTsuBlockConfig, state: Option[(Long, Tsu)], currentSimulationTimeMs: Long, override val name: String, override val address: String) extends AbstractEquipment {

  private val logger = LoggerFactory.getLogger(SingleTsuBlock.getClass)

  override def put(bag: Tsu): AbstractEquipment = {
    //logger.debug(s"put $bag to $this")
    config.eventLogger(bag.id, currentSimulationTimeMs, name)
    if (canLoad()) copy(state = Some((config.delayMs(bag), bag))) else throw new IllegalStateException(s"Placing new TSU $bag to $this is impossible")
  }

  override def canLoad(): Boolean = !state.isDefined

  override def shouldOffload(): Boolean = state.isDefined && state.get._1 <= 0

  override def move(stepMs: Long): (Option[Tsu], AbstractEquipment) = {
    //logger.debug(s"move $this")
    if (state.isDefined) {
      val newState = (state.get._1 - stepMs, state.get._2)
      if (shouldOffload()) {
        //logger.debug(s"exit ${newState._2} from $this")
        (Some(newState._2), copy(state = None, currentSimulationTimeMs = currentSimulationTimeMs + stepMs))
      } else (None, copy(state = Some(newState), currentSimulationTimeMs = currentSimulationTimeMs + stepMs))
    } else {
      (None, copy(currentSimulationTimeMs = currentSimulationTimeMs + stepMs))
    }
  }

  override def adjustTime(stepMs: Long): AbstractEquipment = {
    //  logger.debug(s"adjustTime $this")
    if (shouldOffload()) copy(currentSimulationTimeMs = currentSimulationTimeMs + stepMs) else throw new IllegalStateException(s"Changing time for non-blocked conveyor $this is not allowed")
  }

  override def toString: String = {
    val header = s"'$name@$address' at $currentSimulationTimeMs ms"
    val tail = if (state.isDefined) s" left=${state.get._1}ms ${state.get._2}" else " 'empty'"
    header + tail
  }

  override def peek(): Tsu = if (shouldOffload()) state.get._2 else throw new IllegalStateException(s"Cannot peek from $this")
}

object SingleTsuBlock {
  def apply(config: SingleTsuBlockConfig, name: String) = new SingleTsuBlock(config, None, 0L, name, name)
}
