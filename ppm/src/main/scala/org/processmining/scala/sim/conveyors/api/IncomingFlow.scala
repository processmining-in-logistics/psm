package org.processmining.scala.sim.conveyors.api

trait IncomingFlow {
  def isAvailable(time: Long, entry: String): Boolean

}
