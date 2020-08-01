package org.processmining.scala.viewers.spectrum2.model

import java.io.ByteArrayInputStream
import java.net._
import com.esotericsoftware.kryo.io.Input
import org.processmining.scala.log.common.enhancment.segments.common.SegmentImpl
import org.processmining.scala.log.utils.common.errorhandling.EH
import org.slf4j.LoggerFactory

class SimPacketsSinkServer(val port: Int, val callback: (SegmentName, Segment) => Unit) extends Runnable {

  private val logger = LoggerFactory.getLogger(classOf[SimPacketsSinkServer].getName)

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
        //logger.debug(s"Received ${request.getLength} bytes")
        val is = new ByteArrayInputStream(buf, 0, request.getLength)
        val input = new Input(is)
        val segmentImpl = SinkDatasourceBuilder.kryo.readObject(input, classOf[SegmentImpl])
        val activities = segmentImpl.field0.split("@")
        val segmentName = SegmentName(activities(0), activities(1))
        val segment = Segment(segmentImpl.caseId,
          new SegmentEvent {
            override val minMs: Long = segmentImpl.timeMs
            override val maxMs: Long = segmentImpl.timeMs
          },
          new SegmentEvent {
            override val minMs: Long = segmentImpl.timeMs + segmentImpl.duration
            override val maxMs: Long = segmentImpl.timeMs + segmentImpl.duration
          },
          segmentImpl.clazz
        )
        callback(segmentName, segment)
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