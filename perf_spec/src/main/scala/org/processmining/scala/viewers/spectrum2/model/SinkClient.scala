package org.processmining.scala.viewers.spectrum2.model

import java.io.{ByteArrayOutputStream, IOException}
import java.net.{DatagramPacket, DatagramSocket, InetAddress}

import com.esotericsoftware.kryo.io.Output
import org.processmining.scala.log.common.enhancment.segments.common.{ListOfBins, SegmentImpl}
import org.processmining.scala.log.utils.common.errorhandling.EH
import org.processmining.scala.viewers.spectrum2.pqr.PlaceTransition
import org.slf4j.LoggerFactory

class SinkClient(hostName: String, dstPort: Int) {

  private val logger = LoggerFactory.getLogger(classOf[SinkClient].getName)
  val address: InetAddress = InetAddress.getByName(hostName)
  val socket = new DatagramSocket()

  def send(segmentName: SegmentName, segment: Segment): Unit = {
      val segmentImpl = SegmentImpl(segment.caseId, segment.start.minMs, segment.end.minMs - segment.start.minMs, s"${segmentName.a}@${segmentName.b}", 0)
      send(segmentImpl)

  }
//  def send(segmentName: SegmentName, segment: Segment): Unit = {
//    try {
//      val segmentImpl = SegmentImpl(segment.caseId, segment.start.minMs, segment.end.minMs - segment.start.minMs, s"${segmentName.a}@${segmentName.b}", 0)
//      val byteOutputStream = new ByteArrayOutputStream()
//      val output = new Output(byteOutputStream)
//      SinkDatasourceBuilder.kryo.writeObject(output, segmentImpl)
//      output.close()
//      val ba = byteOutputStream.toByteArray
//      byteOutputStream.close
//      val request = new DatagramPacket(ba, ba.length, address, dstPort)
//      socket.send(request)
//      logger.info("Sent")
//    }
//    catch {
//      case ex: IOException => {
//        logger.debug(ex.toString)
//      }
//      case ex: Exception => {
//        logger.error(ex.toString)
//        EH.apply.error("UDP client", ex)
//      }
//    }
//  }

  def send[T](pt: T): Unit = {
    try {
      val byteOutputStream = new ByteArrayOutputStream()
      val output = new Output(byteOutputStream)
      SinkDatasourceBuilder.kryo.writeObject(output, pt)
      output.close()
      val ba = byteOutputStream.toByteArray
      byteOutputStream.close
      val request = new DatagramPacket(ba, ba.length, address, dstPort)
      socket.send(request)
      logger.info("Sent")
    }
    catch {
      case ex: IOException => {
        logger.debug(ex.toString)
      }
      case ex: Exception => {
        logger.error(ex.toString)
        EH.apply.error("UDP client", ex)
      }
    }
  }


}
