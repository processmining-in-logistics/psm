package org.processmining.scala.sim.conveyors.impl

import org.processmining.scala.sim.conveyors.api.{Equipment, Tsu}
import org.slf4j.LoggerFactory

case class SorterConfig(traysNumber: Int,
                        trayLengthMs: Long,
                        sensors: Seq[(String, Long)],
                        eventLogger: (String, Long, String) => Unit,
                        traysBeforeAfter: Int
                       ) extends Serializable


case class Sorter(config: SorterConfig, bags: Vector[Option[Tsu]], firstTrayPosition: Long, currentSimulationTimeMs: Long, override val name: String, override val address: String, val loopNumber: Int) extends Equipment {
  def updateTsu(updatedTsu: Tsu): Sorter =
    copy(bags = bags.map(x => if(x.isDefined && x.get.id == updatedTsu.id) Some(updatedTsu) else x))


  protected val logger = LoggerFactory.getLogger(classOf[Sorter])
  private val totalLengthMs: Long = config.traysNumber * config.trayLengthMs

  override def toString: String = s"'$name' loop $loopNumber at $firstTrayPosition"

  private def normalizePosition(pos: Long): Long = if (pos >= totalLengthMs) pos - totalLengthMs else pos

  def move(stepMs: Long): Sorter = {
    bags
      .zipWithIndex
      .map(x => (x._1,
        normalizePosition(firstTrayPosition + x._2 * config.trayLengthMs),
        normalizePosition(firstTrayPosition + stepMs + x._2 * config.trayLengthMs)))
      .filter(_._1.isDefined)
      .foreach {
        x => {
          val sensor = config.sensors.find(s => x._2 <= s._2 && x._3 > s._2)
          if (sensor.isDefined) config.eventLogger(x._1.get.id, currentSimulationTimeMs, sensor.get._1)
        }
      }
    val newFirstTrayPosition = normalizePosition(firstTrayPosition + stepMs)
    val newLoopNumber = if (firstTrayPosition < newFirstTrayPosition) loopNumber else loopNumber + 1
    //logger.debug(s"Sorter.move: newLoopNumber=$newLoopNumber newFirstTrayPosition=$newFirstTrayPosition")
    copy(firstTrayPosition = newFirstTrayPosition,
      currentSimulationTimeMs = currentSimulationTimeMs + stepMs,
      loopNumber = newLoopNumber

    )
  }

  private def positionToIndex(absPosition: Long): Int = {
    //logger.debug(s"absPosition=$absPosition firstTrayPosition=$firstTrayPosition totalLengthMs=$totalLengthMs trayLengthMs=${config.trayLengthMs}")
    (if (absPosition >= firstTrayPosition) (absPosition - firstTrayPosition) / config.trayLengthMs
    else (totalLengthMs - (firstTrayPosition - absPosition)) / config.trayLengthMs).toInt
  }

  private def normalizeIndex(i: Int): Int = if (i < 0) i + bags.length else {
    if (i >= bags.length) i - bags.length else i
  }

  def canLoad(absPosition: Long): Boolean = {
    val index = positionToIndex(absPosition)
    (index - config.traysBeforeAfter to index + config.traysBeforeAfter)
      .map(normalizeIndex)
      .forall(!bags(_).isDefined)
  }

  def peek(absPosition: Long): Option[Tsu] = bags(positionToIndex(absPosition))

  def offload(absPosition: Long): (Tsu, Sorter) = {
    val index = positionToIndex(absPosition)
    (bags(index).get, copy(bags = bags.updated(index, None)))
  }


  def load(absPosition: Long, bag: Tsu): Sorter =
    if (canLoad(absPosition)) {
      val index = positionToIndex(absPosition)
      copy(bags = bags.updated(index, Some(bag)))
    } else throw new IllegalArgumentException(s"Cannot load $bag to position $absPosition of sorter $this")

}
