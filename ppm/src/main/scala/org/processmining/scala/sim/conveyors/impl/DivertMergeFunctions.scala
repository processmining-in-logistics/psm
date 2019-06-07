package org.processmining.scala.sim.conveyors.impl

import org.processmining.scala.sim.conveyors.api.{AbstractEquipment, Tsu}

import scala.util.Random

class DivertMergeFunctions(rnd: Random) {

  def availabilityBasedRandomlyBalancedDivert(tsu: Tsu, b1: AbstractEquipment, b2: AbstractEquipment): Int = {
    val availability = (b1.canLoad(), b2.canLoad())
    availability match {
      case (true, true) => if (rnd.nextBoolean()) DivertMergeFunctions.First else DivertMergeFunctions.Second
      case (true, false) => DivertMergeFunctions.First
      case (false, true) => DivertMergeFunctions.Second
      case (false, false) => DivertMergeFunctions.Stop
    }
  }


  def mergeRandom(): Int = if (rnd.nextBoolean()) DivertMergeFunctions.First else DivertMergeFunctions.Second

}

object DivertMergeFunctions {
  val First = 1
  val Second = 2
  val Stop = 0

  def mergeP1(): Int = DivertMergeFunctions.First

  def mergeP2(): Int = DivertMergeFunctions.Second
}
