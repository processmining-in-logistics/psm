package org.processmining.scala.viewers.spectrum2.view

import java.io.ByteArrayInputStream
import java.net.{DatagramPacket, DatagramSocket}

import com.esotericsoftware.kryo.io.Input
import org.processmining.scala.log.common.enhancment.segments.common.SegmentImpl
import org.processmining.scala.log.utils.common.errorhandling.EH
import org.processmining.scala.viewers.spectrum2.model.{Segment, SegmentEvent, SegmentName, SinkDatasourceBuilder}
import org.processmining.scala.viewers.spectrum2.pqr.PlaceTransition
import org.slf4j.LoggerFactory

class PqrPacketsSinkServer(val port: Int, val callback: PlaceTransition => Unit) extends Runnable {

  private val logger = LoggerFactory.getLogger(classOf[PqrPacketsSinkServer].getName)

  override def run(): Unit = {
    logger.info("Tread listening to a server UDP port started")
    val socket = new DatagramSocket(port)
    logger.info(s"UDP server port $port is opened")
    val BufSize = 1024
    var exitFlag = false
    while (!exitFlag) {
      val buf = new Array[Byte](BufSize)
      val request = new DatagramPacket(buf, buf.length)
      try {
        socket.receive(request)
        logger.debug(s"Received ${request.getLength} bytes")
        val is = new ByteArrayInputStream(buf, 0, request.getLength)
        val input = new Input(is)
        val pt = SinkDatasourceBuilder.kryo.readObject(input, classOf[PlaceTransition])
        callback(pt)
      }
      catch {
        case _: InterruptedException => {
          exitFlag = true
        }
        case ex: Exception => {
          EH.apply.error("UDP server", ex)
          exitFlag = true
        }
      }
    }
    logger.info("Tread listening to a server UDP port quited")
  }
}