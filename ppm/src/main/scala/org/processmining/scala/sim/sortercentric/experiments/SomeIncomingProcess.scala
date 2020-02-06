package org.processmining.scala.sim.sortercentric.experiments

import org.processmining.scala.sim.sortercentric.items.{CommandConfig, ExpDistrConfig, ExpIncomingProcess, ExpIncomingProcessesAndCommands}

object SomeIncomingProcess {

  val load1 = ExpDistrConfig(35000, 25000, 55000, 1)

  val stepsOfCheckInLinkLoad = ExpDistrConfig(5 * 60 * 1000, 1 * 60 * 1000, 10 * 60 * 1000, 1)

  val process = ExpIncomingProcess(Vector(load1), stepsOfCheckInLinkLoad, 1000, "bag")

  val cmd1Distr = ExpDistrConfig(5 * 60 * 1000,
    4 * 60 * 1000,
    6 * 60 * 1000,
    1)

  val cmd1DistrBigStep = ExpDistrConfig(1000000L * 60 * 1000, 100000L * 60 * 1000, 5 * 1000000L * 60 * 1000, 1)

  val cmd1Process = ExpIncomingProcess(Vector(cmd1Distr), cmd1DistrBigStep, 2*60*1000, "cmd")

  val processes = (1 until 8).map(i => s"IN_${i}_0" -> process).toMap ++
    Map(
      //"block z 20000" -> cmd1Process,
      //"block x 25000" -> cmd1Process,
//      "block OUT_1_1 30000" -> cmd1Process,
//      "block OUT_2_1 30000" -> cmd1Process,
//      "block OUT_4_1 30000" -> cmd1Process,
//      "block OUT_5_1 30000" -> cmd1Process,

    )

  val everything = ExpIncomingProcessesAndCommands(processes)
}
