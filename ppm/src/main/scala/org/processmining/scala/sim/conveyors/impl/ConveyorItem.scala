package org.processmining.scala.sim.conveyors.impl

import org.processmining.scala.sim.conveyors.api.{AbstractEquipment, Tsu}
import org.slf4j.LoggerFactory


case class ConveyorItemConfig(durationMs: Long,
                              speed: Double,
                              sensors: Seq[(String, Double)],
                              minDistanceMs: Long,
                              eventLogger: (String, Long, String) => Unit
                             ) extends Serializable

case class ConveyorItem(config: ConveyorItemConfig, state: List[(Tsu, Double)], currentSimulationTimeMs: Long, override val name: String, override val address: String) extends AbstractEquipment {

  private val logger = LoggerFactory.getLogger(ConveyorItem.getClass)

  override def put(bag: Tsu): AbstractEquipment = {
    //logger.debug(s"put $bag to $this")
    if (canLoad()) copy(state = (bag, ConveyorItem.START_POSITION_PERCENTS) :: state) else throw new IllegalStateException(s"Placing new TSU $bag to $this is impossible")
  }

  override def canLoad(): Boolean = state.isEmpty || percentsToMs(state.head._2) >= config.minDistanceMs

  override def shouldOffload(): Boolean = state.nonEmpty && state.last._2 >= ConveyorItem.HUNDRED_PERCENTS

  override def move(stepMs: Long): (Option[Tsu], AbstractEquipment) = {
    //    logger.debug(s"move $this")
    //val lastTsu = if (state.nonEmpty) Some(state.head) else None
    //val speed = if(lastTsu.isDefined && lastTsu.get._1.attribute("speed").isDefined && config.speed == Double.MinValue) lastTsu.get._1.attribute("speed").get.asInstanceOf[Double] else config.speed
    val speed = config.speed
    val moved = state.map(x => (x._1, msToPercents((percentsToMs(x._2) + stepMs*speed).toLong)))
    state
      .zip(moved.map(_._2))
      .foreach { x =>
        val (start, stop) = (x._1._2, x._2)
        val sensor = config.sensors.find(x => start <= x._2 && stop > x._2)
        if (sensor.isDefined) config.eventLogger(x._1._1.id, currentSimulationTimeMs, sensor.get._1) // timeMs is rounded
      }

    if (shouldOffload())
      (Some(state.last._1), copy(state = moved.reverse.tail.reverse, currentSimulationTimeMs = currentSimulationTimeMs + stepMs))
    else
      (None, copy(state = moved, currentSimulationTimeMs = currentSimulationTimeMs + stepMs))
  }

  override def adjustTime(stepMs: Long): AbstractEquipment = {
    //  logger.debug(s"adjustTime $this")
    if (shouldOffload()) copy(currentSimulationTimeMs = currentSimulationTimeMs + stepMs) else throw new IllegalStateException(s"Changing time for non-blocked conveyor $this is not allowed")
  }

  private def percentsToMs(percents: Double): Long = (percents * config.durationMs / ConveyorItem.HUNDRED_PERCENTS).toLong

  private def msToPercents(ms: Long): Double = ms.toDouble / (config.durationMs.toDouble / ConveyorItem.HUNDRED_PERCENTS)

  override def toString: String = s"'$name@$address' at $currentSimulationTimeMs ms" + state.map(x => s"('${x._1}'/${x._2})").mkString("; ")

  override def peek(): Tsu = if (shouldOffload()) state.last._1 else throw new IllegalStateException(s"Cannot peek from $this")

//  override def updateLastTsu(updatedTsu: Tsu): AbstractEquipment = {
//    val last = state.last
//    if (state.nonEmpty) copy(state = ((updatedTsu, last._2) :: state.reverse.tail).reverse)
//    else throw new IllegalStateException(s"Cannot update $updatedTsu for empty item $this")
//  }
}

object ConveyorItem {
  val HUNDRED_PERCENTS = 100.0
  val START_POSITION_PERCENTS = 0.0

//  def apply(config: ConveyorItemConfig, name: String): AbstractEquipment =
//    new ConveyorItem(config, List(), 0, name, name)

  def apply(config: ConveyorItemConfig, name: String, address: String): AbstractEquipment =
    new ConveyorItem(config, List(), 0, name, address)
}

