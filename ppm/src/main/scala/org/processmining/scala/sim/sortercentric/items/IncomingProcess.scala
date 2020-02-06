package org.processmining.scala.sim.sortercentric.items

/***
  *
  * @param mean = 1/lambda
  * @param minValue
  * @param maxValue
  * @param seed
  */
case class ExpDistrConfig(mean: Double, minValue: Double, maxValue: Double, seed: Int)

case class ExpIncomingProcess(scenarios: Vector[ExpDistrConfig], scenarioDuration: ExpDistrConfig, initialTimeShiftMs: Long, action: String)

case class CommandConfig(cmd: String, interval: ExpDistrConfig, initialTimeShiftMs: Long)

case class ExpIncomingProcessesAndCommands(processes: Map[String, ExpIncomingProcess])


