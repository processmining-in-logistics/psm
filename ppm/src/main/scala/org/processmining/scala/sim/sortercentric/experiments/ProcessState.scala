package org.processmining.scala.sim.sortercentric.experiments

import org.apache.commons.math3.random.RandomDataGenerator
import org.processmining.scala.sim.sortercentric.items.{ExpDistrConfig, ExpIncomingProcess}

class ProcessState(val config: ExpIncomingProcess,
                   var currentScenarioLoop: Int,
                   var currentBagInScenarioCount: Int,
                   var nextScheduledEventTimeMs: Long) {

  private val rndScenarioDuration = new RandomDataGenerator()
  private val rndBagInterval = new RandomDataGenerator()
  var scenarioStopTimeMs = -1L

  def restart() = rndScenarioDuration.reSeed(config.scenarioDuration.seed)

  def currentScenarioIndex = currentScenarioLoop % config.scenarios.size

  def startNextScenario(currentTimeMs: Long) = {
    rndBagInterval.reSeed(config.scenarios(currentScenarioIndex).seed)
    currentBagInScenarioCount = 0
    currentBagInScenarioCount = currentBagInScenarioCount + 1
    scenarioStopTimeMs = currentTimeMs + nextScenarioDurationMs.toLong
  }

  restart()

  def nextBagRelTimeMs(): Double = next(config.scenarios(currentScenarioIndex), rndBagInterval)

  def nextScenarioDurationMs(): Double = next(config.scenarioDuration, rndScenarioDuration)

  private def next(config: ExpDistrConfig, rnd: RandomDataGenerator): Double = {
    var rndValue = 0.0
    do {
      rndValue = rnd.nextExponential(config.mean)
    } while (rndValue > config.maxValue || rndValue < config.minValue)
    rndValue
  }


}

object ProcessState {
  def apply(config: ExpIncomingProcess): ProcessState = new ProcessState(config, 0, -1, NotStarted)

  val NotStarted = -1
  val Disabled = Long.MinValue
}

