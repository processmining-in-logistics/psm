package org.processmining.scala.sim.conveyors.impl

import org.processmining.scala.sim.conveyors.api.IncomingFlow

case class IncomingFlowImpl(var nextTimeMs: Long, f: Unit => Int) extends IncomingFlow {

  override def isAvailable(simTimeMs: Long, entry: String): Boolean =
    if (nextTimeMs <= simTimeMs) {
      nextTimeMs = simTimeMs + f()
      true
    } else false

}
