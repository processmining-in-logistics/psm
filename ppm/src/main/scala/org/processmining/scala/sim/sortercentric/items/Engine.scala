package org.processmining.scala.sim.sortercentric.items

import org.processmining.scala.log.utils.common.csv.common.CsvExportHelper
import org.processmining.scala.viewers.spectrum2.model.Separator
import org.slf4j.LoggerFactory

abstract class SimEngine {

  protected val logger = LoggerFactory.getLogger(classOf[SimEngine])

  val stepMs = instance.config.stepMs

  val instance: AbstractSystem

  def execute(n: Int): Unit = {
    (0 until n).foreach(_ => executeStep())
  }

  def executeStep(): Unit

  def rewindToStart(): Unit

  def flushEventLogFile(): Unit

}


sealed class Engine(override val instance: AbstractSystem,
                    eventLog: String => Unit,
                    flushEventLog: () => Unit,
                    incomingProcessHandler: Long => Unit,
                    extendedLogger: (String, Long, String, Int) => Unit
                   ) extends SimEngine {

  private val csvExportHelper = new CsvExportHelper(CsvExportHelper.DefaultTimestampPattern, CsvExportHelper.AmsterdamTimeZone, ",")

  override def rewindToStart(): Unit = ???

  //First divert then move
  override def executeStep() = {
    logger.debug(s"executeStep @ ${instance.state.timeMs}")
    incomingProcessHandler(instance.state.timeMs)
    instance.executionOrder.foreach(x => {
      if (x.state.isBlocked()) {
        x.state = x.state.copy(blockedUntilMs = if (x.state.blockedUntilMs <= instance.state.timeMs) ConveyorState.NonBlocked else x.state.blockedUntilMs)
      }
      diverte(x)
      if (x.connections.mergePositionMs == 0) move(x) else merge(x)
      if (x.state.wasMovedDuringLastStep) logSensors(x)
    })
    instance.state = instance.state.copy(timeMs = instance.state.timeMs + stepMs)
  }

  def log(id: String, timestamp: Long, activity: String, flag: Int) = {
    val timestampString = csvExportHelper.timestamp2String(timestamp)
    eventLog(s""""$id", $timestampString,"$activity",$flag""")
    extendedLogger(id, timestamp, activity, flag)
  }

  def move(thisConveyor: AbstractConveyor): Unit = {
    val thatConveyor = thisConveyor.connections.output
    val thatProtectedSpaceMs = instance.config.protectedSpaceMs(thatConveyor.config)
    logger.debug(s"move: '${thisConveyor.id}' -> '${thatConveyor.id}'")
    val canMove =
      if (thisConveyor.state.isBlocked) {
        logger.debug(s"move NO: blocked")
        false
      }
      else if (thisConveyor.state.tsu.isEmpty || thatConveyor == Terminator || thatConveyor.state.tsu.isEmpty) {
        logger.debug(s"move OK: empty etc...")
        true
      }
      else {
        val lastTsu = thisConveyor.state.tsu.last
        val tsuAhead = thatConveyor.state.tsu.head
        if (tsuAhead.backPositionMs(thatConveyor.config) < 0 && !thatConveyor.state.wasMovedDuringLastStep) {
          logger.debug(s"move NO: next one is blocked")
          false
        }
        else {
          val distanceMs = thisConveyor.config.lengthMs - lastTsu.headPositionMs + tsuAhead.backPositionMs(thatConveyor.config)
          if (distanceMs + instance.config.stepMs > thatProtectedSpaceMs) {
            logger.debug(s"move OK: distanceMs=$distanceMs thatProtectedSpaceMs=$thatProtectedSpaceMs")
            true
          } else {
            logger.debug(s"move NO: distanceMs=$distanceMs thatProtectedSpaceMs=$thatProtectedSpaceMs")
            false
          }
        }
      }

    val newStatePhase1TimeUpdated = thisConveyor.state.copy(timeMs = thisConveyor.state.timeMs + stepMs)
    thisConveyor.state =
      if (!canMove) newStatePhase1TimeUpdated.copy(wasMovedDuringLastStep = false)
      else {
        val newTsu = newStatePhase1TimeUpdated.tsu.map(x => x.copy(headPositionMs = x.headPositionMs + stepMs))
        val tsuAndOutgoingTsu = if (newTsu.nonEmpty) {
          val lastTsu = newTsu.last
          val diffMs = lastTsu.headPositionMs - thisConveyor.config.lengthMs
          if (diffMs < 0) (newTsu, None) else (newTsu.init, Some(TsuState(lastTsu.tsu, diffMs)))
        } else (Vector(), None)
        if (tsuAndOutgoingTsu._2.isDefined) {
          thatConveyor.state = thatConveyor.state.copy(tsu = updateTsuStateDst(tsuAndOutgoingTsu._2.get, thatConveyor, thisConveyor) +: thatConveyor.state.tsu)
          logger.debug(s"TSU ${tsuAndOutgoingTsu._2.get.tsu.id} was moved: ${thisConveyor.id}->${thatConveyor.id}")
          log(tsuAndOutgoingTsu._2.get.tsu.id, newStatePhase1TimeUpdated.timeMs, s"${thisConveyor.id}${Separator.S}${thatConveyor.id}", Engine.ExtendedMoveEvent)
        }
        val newStatePhase2TsuAndMovingUpdated = newStatePhase1TimeUpdated.copy(wasMovedDuringLastStep = true).copy(tsu = tsuAndOutgoingTsu._1)

        fireOnDelay(thisConveyor, newStatePhase2TsuAndMovingUpdated)

        //        if (newStatePhase2TsuAndMovingUpdated.tsu.nonEmpty) {
        //          val head = newStatePhase2TsuAndMovingUpdated.tsu.head
        //          val backPos = head.backPositionMs(thisConveyor.config)
        //          if (backPos >= 0 && backPos < stepMs) {
        //            val delay = thisConveyor.config.delayOnTsuReceived(head, thisConveyor, instance.config)
        //            newStatePhase2TsuAndMovingUpdated.copy(blockedUntilMs = if (delay >= 0) thisConveyor.state.timeMs + delay else thisConveyor.state.blockedUntilMs
        //            )
        //          } else newStatePhase2TsuAndMovingUpdated
        //        } else newStatePhase2TsuAndMovingUpdated

      }
    logger.debug(s"New state: ${thisConveyor.state}")
  }

  private def fireOnDelay(thisConveyor: AbstractConveyor, ret: ConveyorState) = {

    if (ret.tsu.nonEmpty) {
      val head = ret.tsu.head
      val backPos = head.backPositionMs(thisConveyor.config)
      if (backPos >= 0 && backPos < stepMs) {
        val delay = thisConveyor.config.delayOnTsuReceived(head, thisConveyor, instance.config)
        ret.copy(blockedUntilMs = if (delay > 0) thisConveyor.state.timeMs + delay else thisConveyor.state.blockedUntilMs
        )
      } else ret
    } else ret
  }


  private def updateTsuStateDst(tsuState: TsuState, thatConveyor: AbstractConveyor, thisConveyor: AbstractConveyor) = {
    val newDst = thatConveyor.config.routing(tsuState.tsu.dst)
    thisConveyor.config.updateTsuOnExit(tsuState.copy(tsu = tsuState.tsu.copy(dst = newDst)))
  }

  def logSensors(thisConveyor: AbstractConveyor): Unit = {

    def logSensor(sensorConfig: SensorConfig, tsuState: TsuState, flag: Int) = {
      val distanceMs = sensorConfig.positionMs - tsuState.headPositionMs
      if (distanceMs >= 0 && distanceMs < stepMs) log(tsuState.tsu.id, instance.state.timeMs, s"${sensorConfig.id}", flag)
    }

    thisConveyor.state.tsu.foreach(t => {
      thisConveyor.config.sensors.foreach(logSensor(_, t, Engine.StandardEvent))
//      if (thisConveyor.id == "x") {
//        println(thisConveyor.connections.sensorsForExtendedMergeDivert)
//        println("")
//      }


      thisConveyor.connections.sensorsForExtendedMergeDivert.foreach(logSensor(_, t, Engine.ExtendedMergeDivertEvent))
    }
    )
  }


  def isSpaceAvailable(mergePositionHeadMs: Long, thatConveyor: AbstractConveyor): Boolean = {
    val protectedSpaceMs = instance.config.protectedSpaceMs(thatConveyor.config)
    val p1 = mergePositionHeadMs - instance.config.maxTsuLengthMs - protectedSpaceMs
    val p2 = mergePositionHeadMs + protectedSpaceMs
    val tsuInside = thatConveyor.state.tsu.find(t => (t.headPositionMs >= p1 && t.headPositionMs <= p2) ||
      (t.backPositionMs(thatConveyor.config) >= p1 && t.backPositionMs(thatConveyor.config) <= p2))
    !tsuInside.isDefined
  }

  def merge(thisConveyor: AbstractConveyor): Unit = {
    val thatConveyor = thisConveyor.connections.output
    //val thatProtectedSpaceMs = instance.config.protectedSpaceMs(thatConveyor.config)
    logger.debug(s"merge: '${thisConveyor.id}' -> '${thatConveyor.id}'")
    val (canMove, outgoingTsu) =
      if (thisConveyor.state.isBlocked) {
        logger.debug(s"merge NO: blocked")
        (false, None)
      }
      else if (thisConveyor.state.tsu.isEmpty) {
        logger.debug(s"merge OK: empty etc...")
        (true, None)
      }
      else {
        val lastTsu = thisConveyor.state.tsu.last
        if (thisConveyor.config.lengthMs - lastTsu.headPositionMs > stepMs) {
          logger.debug(s"merge OK: has not reach yet")
          (true, None)
        } else {
          val isAvailable = isSpaceAvailable(thisConveyor.connections.mergePositionMs, thatConveyor)
          if (isAvailable) {
            logger.debug(s"merge OK: space is available")
            (true, Some(lastTsu))
          } else {
            logger.debug(s"merge NO: space is unavailable")
            (false, Some(lastTsu))
          }
        }
      }
    val newStateTimeUpdated = thisConveyor.state.copy(timeMs = thisConveyor.state.timeMs + stepMs)
    thisConveyor.state =
      if (!canMove) newStateTimeUpdated.copy(wasMovedDuringLastStep = false)
      else {
        val newTsu = newStateTimeUpdated.tsu.map(x => x.copy(headPositionMs = x.headPositionMs + stepMs))
        val finalTsu = if (outgoingTsu.isDefined) {
          val tsuToBeMerged = TsuState(outgoingTsu.get.tsu, thisConveyor.connections.mergePositionMs)
          thatConveyor.state = thatConveyor.state.copy(tsu = (tsuToBeMerged +: thatConveyor.state.tsu).sortBy(_.headPositionMs))
          logger.debug(s"TSU ${tsuToBeMerged.tsu.id} was merged: ${thisConveyor.id}->${thatConveyor.id}")
          log(tsuToBeMerged.tsu.id, newStateTimeUpdated.timeMs, s"${thisConveyor.id}${Separator.S}${thatConveyor.id}", Engine.StandardMergeEvent)
          newTsu.init
        } else newTsu

        fireOnDelay(thisConveyor, newStateTimeUpdated.copy(wasMovedDuringLastStep = true).copy(tsu = finalTsu))
      }
    logger.debug(s"New state: ${thisConveyor.state}")
  }


  //  private def fireOnDelay(thisConveyor: AbstractConveyor, newStateTimeUpdated: ConveyorState, finalTsu: Vector[TsuState]) = {
  //    val ret = newStateTimeUpdated.copy(wasMovedDuringLastStep = true).copy(tsu = finalTsu)
  //    if (ret.tsu.nonEmpty) {
  //      val head = ret.tsu.head
  //      val backPos = head.backPositionMs(thisConveyor.config)
  //      if (backPos >= 0 && backPos < stepMs) {
  //        val delay = thisConveyor.config.delayOnTsuReceived(head, thisConveyor, instance.config)
  //        ret.copy(blockedUntilMs = if (delay >= 0) thisConveyor.state.timeMs + delay else thisConveyor.state.blockedUntilMs
  //        )
  //      } else ret
  //    } else ret
  //    ret
  //  }

  def diverte(thisConveyor: AbstractConveyor): Unit = {
    if (thisConveyor.state.tsu.nonEmpty) {
      thisConveyor.connections.divertingConveyors.foreach(x => {
        val thatConveyor = x.conveyor
        logger.debug(s"diverting '${thisConveyor.id}->${thatConveyor.id}'")
        val protectedSpaceMs = instance.config.protectedSpaceMs(thatConveyor.config)
        val isReadyToReceive = thatConveyor.state.tsu.isEmpty ||
          ((thatConveyor.state.tsu.head.backPositionMs(thatConveyor.config) - protectedSpaceMs) > 0)
        if (isReadyToReceive) {
          val p1 = x.positionMs
          val p2 = x.positionMs + stepMs
          val tsuOptional = thisConveyor.state.tsu.find(t => {
            t.headPositionMs > p1 && t.headPositionMs <= p2 && thatConveyor.config.routingTable.intersect(t.tsu.dst).nonEmpty
          }
          )
          if (tsuOptional.isDefined) {
            thatConveyor.state = thatConveyor.state.copy(tsu =
              updateTsuStateDst(tsuOptional.get, thatConveyor, thisConveyor).copy(headPositionMs = 0) +: thatConveyor.state.tsu)
            logger.debug(s"TSU ${tsuOptional.get.tsu.id} was diverted: '${x.sensorId}'")
            log(tsuOptional.get.tsu.id, instance.state.timeMs, s"${x.sensorId}", Engine.StandardDivertEvent)
            thisConveyor.state = thisConveyor.state.copy(tsu = thisConveyor.state.tsu.filter(_.tsu.id != tsuOptional.get.tsu.id))
            logger.debug(s"New state: ${thisConveyor.state}")
          }
        }
      }
      )
    }
  }

  override def flushEventLogFile(): Unit = flushEventLog()
}


object Engine {
  val StandardEvent = 0
  val StandardMergeEvent = 1
  val StandardDivertEvent = 2
  val ExtendedMergeDivertEvent = 3
  val ExtendedMoveEvent = 4


}