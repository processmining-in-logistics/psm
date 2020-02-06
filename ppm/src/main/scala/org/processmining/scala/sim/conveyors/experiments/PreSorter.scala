package org.processmining.scala.sim.conveyors.experiments

import java.time.{Duration, Instant, ZoneId, ZonedDateTime}
import java.util.concurrent.{Executors, TimeUnit}

import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.utils.common.csv.common.{CsvExportHelper, CsvImportHelper}
import org.processmining.scala.log.utils.common.errorhandling.JvmParams
import org.processmining.scala.sim.conveyors.api.{AbstractEquipment, _}
import org.processmining.scala.sim.conveyors.impl._
import org.slf4j.LoggerFactory

import scala.util.Random


final class PreSorter(eventLogPath: String => String,
                      startTime: Long,
                      seed: Int,
                      checkInIncomingFlow: List[(Long, IncomingFlow)],
                      transferInIncomingFlow: List[(Long, IncomingFlow)],
                      numberOfIterations: Int,
                      caseIdPrefix: String
                     )
  extends AbstractEngine(eventLogPath(""), PreSorter.StepMs, startTime, seed, numberOfIterations) {
  override protected val headToHeadMinDistanceMs: Long = 2000
  private val lostInTrackingRnd = new Random(seed)
  private val shufflingRnd = new Random(seed)
  //private val printWriterNoPresorter = new PrintWriter(new File(eventLogPath("no_sorter")))
  val A1 = "A1"
  val A2 = "A2"
  val A3 = "A3"
  val A4 = "A4"

  val Link1 = "Link1"
  val Link1B = "Link1B"
  val Link1S = "Link1S"

  val TransferIn = "TransferIn"
  val TransferOut = "TransferOut"

  val Link2 = "Link2"
  val Link2B = "Link2B"
  val Link2S = "Link2S"

  val Link3A = "Link3A"
  val Link3T = "Link3T"
  val Link3B = "Link3B"
  val Link3S = "Link3S"

  val S_SCAN_1 = "E1.TO_SCAN_1"
  val SCAN_1 = "E2.SCAN_1"
  val SCAN_1_S = "E3.FROM_SCAN_1"


  val S_MC_1 = "F1.TO_MC_1"
  val MC_1 = "F2.MC_1"
  val MC_1_S = "F3.FROM_MC_1"

  val S = "PreSorter"
  val F = "FinalSorter"

  val X1 = "X1"
  val X2 = "X2"

  val DefaultSensorsStepPercents = 50
  val EntryOnly = 100
  val MCsDelay = (15000, 60000)
  val MC_PERCENT = 5
  val ScannersDelay = (1000, 2000)

  private var caseIdSuffix: Int = 0


  //def blockage(pPercent: Double, interval: (Int, Int)) = if (rnd.nextInt(100000) < pPercent * 1000) uniformDelay(interval) else 0

  def zeroBlockage(pPercent: Double, interval: (Int, Int)) = 0

  val currentBlockage: (Double, (Int, Int)) => Int = zeroBlockage

  protected def init(): Unit = {
    addConveyor(A1, 10000, EntryOnly)
    addConveyor(A2, 10000, EntryOnly)
    addConveyor(A3, 10000, EntryOnly)
    addConveyor(A4, 10000, EntryOnly)

    addConveyor(TransferIn, 60000, DefaultSensorsStepPercents)
    addConveyor(TransferOut, 60000, EntryOnly)

    addConveyor(Link1, 60000, EntryOnly)
    addSingleTsuBlock(Link1B, _ => currentBlockage(1, (60000, 120000)))
    addConveyor(Link1S, 2000, EntryOnly)

    addConveyor(Link2, PreSorter.ADDR_EXITS_PRESORTER, 60000, EntryOnly)
    addSingleTsuBlock(Link2B, _ => currentBlockage(1, (60000, 120000)))
    addConveyor(Link2S, 2000, EntryOnly)

    addConveyor(Link3A, PreSorter.ADDR_BACK_TO_PRESORTER, 10000, EntryOnly)
    addConveyor(Link3T, PreSorter.ADDR_BACK_TO_PRESORTER, 50000, EntryOnly)
    addSingleTsuBlock(Link3B, _ => currentBlockage(1, (60000, 120000)))
    addConveyor(Link3S, 2000, EntryOnly)


    addConveyor(S_SCAN_1, PreSorter.ADDR_SCANNERS, 6000, EntryOnly)
    addSingleTsuBlock(SCAN_1, _ => uniformDelay(ScannersDelay))
    addConveyor(SCAN_1_S, 2000, EntryOnly)

    addConveyor(S_MC_1, PreSorter.ADDR_MCS, 6000, EntryOnly)
    addSingleTsuBlock(MC_1, _ => uniformDelay(MCsDelay))
    addConveyor(MC_1_S, 2000, EntryOnly)

    addSorter(S, 320, 1000, 2, 1)
    addSorter(F, 320, 1000, 2, 1)
    addConveyor(X1, PreSorter.ADDR_EXITS, 5000, EntryOnly)
    addConveyor(X2, PreSorter.ADDR_EXITS, 5000, EntryOnly)

  }

  private def exitFunction(bag: Tsu, s: Sorter, e: AbstractEquipment): (Boolean, Tsu) =
    (bag.dst == e.address, bag)


  def changeDst(newDst: String)(bag: Tsu): Tsu = bag.asInstanceOf[Bag].copy(dst = newDst)

  override protected def executeOneStep(stepMs: Long, simTimeMs: Long): Unit = {

    consumeAndMove(X1)
    consumeAndMove(X2)
    consumeAndMove(TransferOut)

    moveSorter(F)
    fromSorter(F, X1, 51000, exitFunction)
    fromSorter(F, X2, 53000, exitFunction)
    fromSorter(F, Link3A, 12000, exitFunction)

    moveSorter(S)
    fromSorter(S, S_MC_1, 5000, exitFunction)
    fromSorter(S, S_SCAN_1, 25000, exitFunction)
    fromSorter(S, Link2, 145000, exitFunction)


    connectWithSorter(Link2S, F, 15000)
    connect(Link2B, Link2S, changeDst(if (lostInTrackingRnd.nextInt(100) < MC_PERCENT) PreSorter.ADDR_BACK_TO_PRESORTER else PreSorter.ADDR_EXITS))

    divert(Link2, TransferOut, Link2B, divertMergeFunctions.availabilityBasedRandomlyBalancedDivert) //divert
    merge(TransferIn, Link3T, Link3B, divertMergeFunctions.mergeRandom)
    connect(Link3B, Link3S)
    connect(Link3A, Link3T, changeDst(PreSorter.ADDR_MCS))


    connectWithSorter(Link1S, S, 1000)
    connectWithSorter(MC_1_S, S, 20000)
    connectWithSorter(SCAN_1_S, S, 100000)
    connectWithSorter(Link3S, S, 105000)

    connect(S_MC_1, MC_1)
    connect(MC_1, MC_1_S, changeDst(PreSorter.ADDR_SCANNERS))
    connect(S_SCAN_1, SCAN_1)
    connect(SCAN_1, SCAN_1_S, changeDst(PreSorter.ADDR_EXITS_PRESORTER))

    connect(Link1B, Link1S)
    connect(Link1, Link1B)

    merge(A3, A4, Link1, divertMergeFunctions.mergeRandom)
    merge(A1, A2, A4, divertMergeFunctions.mergeRandom)


    val checkInInput = shufflingRnd.shuffle(List(A1, A2, A3)).find(state(_).canLoad())
    if (checkInInput.isDefined) addNewTsu(checkInInput.get, simTimeMs, checkInIncomingFlow.find(simTimeMs < _._1).get._2)
    if (state(TransferIn).canLoad())
      addNewTsu(TransferIn, simTimeMs, transferInIncomingFlow.find(simTimeMs < _._1).get._2)
  }

  def addNewTsu(aName: String, simTimeMs: Long, generator: IncomingFlow) = {
    var a = state(aName)
    a = if (a.canLoad())
      if (generator.isAvailable(simTimeMs, aName)) a.put(tsuFactory(a)) else a
    else throw new IllegalStateException(s"Cannot add new TSU to $aName")
    state(aName) = a
  }

  protected override def tsuFactory(a: AbstractEquipment): Tsu = {
    caseIdSuffix += 1
    val toMc = lostInTrackingRnd.nextInt(100) < MC_PERCENT
    Bag(s"${caseIdPrefix}_$caseIdSuffix", a.name, if (toMc) PreSorter.ADDR_MCS else PreSorter.ADDR_SCANNERS)
  }

  override protected def eventLogger(caseId: String, timeMs: Long, activity: String): Unit = {
    super.eventLogger(caseId, timeMs, activity)
    //    if (!activity.matches(s"$S.*"))
    //      logEvent(printWriterNoPresorter, caseId, timeMs, activity, "")
  }

  override protected def onStart() = {
    logger.info(s"Task for '$caseIdPrefix' started.")
    //printWriterNoPresorter.println(EventLogHeader)

  }

  override protected def onStop() = {
    //printWriterNoPresorter.close()
    logger.info(s"Task for '$caseIdPrefix' stopped.")
  }
}


class ScenarioBuilder(seed: Int, durationHours: Int) {
  private val logger = LoggerFactory.getLogger(classOf[ScenarioBuilder])
  private val rnd = new Random(seed)

  private def normallyDistributedValue(median: Int, dev: Double): Unit => Int = _ => (median + rnd.nextGaussian() * dev).intValue()

  private val ZERO_LOAD_DISTRIBUTION: Unit => Int = _ => 0
  private val LOW_LOAD_DISTRIBUTION = normallyDistributedValue(45000, 10000)
  private val NORMAL_LOAD_DISTRIBUTION = normallyDistributedValue(6000, 2000)
  private val HIGH_LOAD_DISTRIBUTION = normallyDistributedValue(1000, 500)
  private val CHECK_IN_NORMAL_LOAD_PERIOD_DURATION_S = 550
  private val CHECK_IN_HIGH_LOAD_PERIOD_DURATION_S = 300
  private val checkInFixedScenario = List((CHECK_IN_NORMAL_LOAD_PERIOD_DURATION_S, NORMAL_LOAD_DISTRIBUTION),
    (CHECK_IN_HIGH_LOAD_PERIOD_DURATION_S, HIGH_LOAD_DISTRIBUTION))
  private val checkInVariableScenario = List(
    (normallyDistributedValue(CHECK_IN_NORMAL_LOAD_PERIOD_DURATION_S, CHECK_IN_NORMAL_LOAD_PERIOD_DURATION_S / 4)(), NORMAL_LOAD_DISTRIBUTION),
    (normallyDistributedValue(CHECK_IN_NORMAL_LOAD_PERIOD_DURATION_S, CHECK_IN_NORMAL_LOAD_PERIOD_DURATION_S / 3)(), LOW_LOAD_DISTRIBUTION), //new
    (normallyDistributedValue(CHECK_IN_HIGH_LOAD_PERIOD_DURATION_S, CHECK_IN_HIGH_LOAD_PERIOD_DURATION_S / 2)(), HIGH_LOAD_DISTRIBUTION),
    (normallyDistributedValue(CHECK_IN_NORMAL_LOAD_PERIOD_DURATION_S, CHECK_IN_NORMAL_LOAD_PERIOD_DURATION_S / 4)(), LOW_LOAD_DISTRIBUTION)) //new

  private val TRANSFER_IN_LOW_LOAD_PERIOD_DURATION_S = 470
  private val TRANSFER_IN_HIGH_LOAD_PERIOD_DURATION_S = 190

  //  val transferInVariableScenario = List(
  //    (normallyDistributedValue(TRANSFER_IN_LOW_LOAD_PERIOD_DURATION_S, TRANSFER_IN_LOW_LOAD_PERIOD_DURATION_S / 4)(), NORMAL_LOAD_DISTRIBUTION),
  //    (normallyDistributedValue(TRANSFER_IN_HIGH_LOAD_PERIOD_DURATION_S, TRANSFER_IN_HIGH_LOAD_PERIOD_DURATION_S / 2)(), HIGH_LOAD_DISTRIBUTION))

  private val transferInStableScenario = List(
    (normallyDistributedValue(TRANSFER_IN_LOW_LOAD_PERIOD_DURATION_S, TRANSFER_IN_LOW_LOAD_PERIOD_DURATION_S / 4)(), NORMAL_LOAD_DISTRIBUTION))

  private val transferInLowLoadScenario = List(
    (normallyDistributedValue(TRANSFER_IN_LOW_LOAD_PERIOD_DURATION_S, TRANSFER_IN_LOW_LOAD_PERIOD_DURATION_S / 4)(), LOW_LOAD_DISTRIBUTION))

  private def getIterationsNumber(timeS: Long) =
    (Duration.ofHours(durationHours).toMillis / 1000.0 / timeS)
      .toInt

  private def getClonedScenario(scenario: Seq[(Int, Unit => Int)]): Seq[(Int, Unit => Int)] = {
    (0 until getIterationsNumber(scenario.map(_._1).sum))
      .flatMap(_ => scenario)
  }

  private def createGenerators(scenario: Seq[(Int, Unit => Int)]): List[(Long, IncomingFlow)] = {
    val initialValue: List[(Long, IncomingFlow)] = List()
    val MS = 1000L
    scenario.foldLeft((0L, initialValue))((z, x) => (z._1 + x._1 * MS, (z._1 + x._1 * MS, IncomingFlowImpl(z._1, x._2)) :: z._2))
      ._2
      .reverse
  }

  private val checkInScenarioPattern = checkInVariableScenario
  val checkInGenerators = createGenerators(getClonedScenario(checkInScenarioPattern))

  private val transferInScenarioPattern = transferInLowLoadScenario
  val transferInGenerators = createGenerators(getClonedScenario(transferInScenarioPattern))


  val executionDurationMs = Math.min(checkInGenerators.last._1, transferInGenerators.last._1)


}

object PreSorter {
  val logger = LoggerFactory.getLogger(PreSorter.getClass)
  val StepMs = 100
  val dateHelper = new CsvImportHelper(CsvExportHelper.FullTimestampPattern, CsvExportHelper.AmsterdamTimeZone)
  val StartTimeSinceEpochBeginningMs = dateHelper.extractTimestamp("01-09-2018 00:00:00.000")

  val ADDR_SCANNERS = "SCANNERS"
  val ADDR_EXITS = "EXITS"
  val ADDR_EXITS_PRESORTER = "EXITS_PRESORTER"
  val ADDR_TRANSFER_OUT = "TRANSFER_OUT"
  val ADDR_BACK_TO_PRESORTER = "ADDR_BACK_TO_PRESORTER"
  val ADDR_MCS = "MCS"

  def getEventLogPrefix(timeMs: Long): String = {
    val zonedTimeMs = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMs), ZoneId.of(CsvExportHelper.AmsterdamTimeZone))
    f"${zonedTimeMs.getYear}%04d.${zonedTimeMs.getMonthValue}%02d.${zonedTimeMs.getDayOfMonth}%02d"
  }

  def startExperiments(days: Int, offsetHours: Int, durationHours: Int, eventLogPath: String): Unit = {
    if (offsetHours + durationHours > 24)
      throw new IllegalArgumentException(s"Wrong initial offsetHours=$offsetHours durationHours=$durationHours")

    val poolSize = Math.min(Runtime.getRuntime.availableProcessors() * 2, days)
    logger.info(s"days=$days offsetHours=$offsetHours durationHours=$durationHours poolSize=$poolSize")
    val executorService = Executors.newFixedThreadPool(poolSize)
    val futures = (0 until days).map {
      day => {
        val seed = day
        val generator = new ScenarioBuilder(seed, durationHours)
        if (Duration.ofHours(offsetHours).toMillis + generator.executionDurationMs > Duration.ofHours(24).toMillis)
          throw new IllegalArgumentException(s"Wrong real duration ${generator.executionDurationMs}")
        val startOfTheDayMs = StartTimeSinceEpochBeginningMs + Duration.ofDays(day).toMillis
        val instance = new PreSorter(
          x => s"$eventLogPath/${getEventLogPrefix(startOfTheDayMs)}_$day${if (x.isEmpty) "" else s"_$x"}.csv",
          startOfTheDayMs + Duration.ofHours(offsetHours).toMillis,
          seed,
          generator.checkInGenerators,
          generator.transferInGenerators,
          (generator.executionDurationMs / StepMs).asInstanceOf[Int],
          s"D$day"
        )
        executorService.submit(instance)
      }
    }

    logger.info("All tasks are submitted.")
    futures.foreach(_.get())
    logger.info("All tasks are done.")
    executorService.shutdown()
    while (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {}
    logger.info("Thread pool is terminated.")

  }
}

object PreSorterStarter {
  val logger = LoggerFactory.getLogger(PreSorterStarter.getClass)
  val EventLogPath = "g:/sim_logs/Scan1"

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, "PreSorterStarter started")
    logger.info(s"Event log path='$EventLogPath'")
    try {
      PreSorter.startExperiments(7, 10, 12, EventLogPath)
    } catch {
      case e: Throwable => logger.error(e.toString)
    }
    logger.info(s"App is completed.")
  }

}


object PreSorterStarterCli {
  val logger = LoggerFactory.getLogger(PreSorterStarterCli.getClass)

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, "PreSorterStarterCli started")
    if (args.isEmpty) {
      logger.info(s"Use the following arguments: OUTPUT_DIR DAYS OFFSET_WITHIN_A_DAY_HOURS DURATION_HOURS")
    } else {

      logger.info(s"Cli args:${args.mkString(",")}")
      try {
        val eventLogPath = args(0)
        logger.info(s"eventLogPath='$eventLogPath'")

        val days = args(1).toInt
        logger.info(s"days=$days")

        val offsetHours = args(2).toInt
        logger.info(s"offsetHours=$offsetHours")

        val durationHours = args(3).toInt
        logger.info(s"durationHours=$durationHours")
        PreSorter.startExperiments(days, offsetHours, durationHours, eventLogPath)
      } catch {
        case e: Throwable => logger.error(e.toString)
      }
    }
    logger.info(s"App is completed.")
  }

}
