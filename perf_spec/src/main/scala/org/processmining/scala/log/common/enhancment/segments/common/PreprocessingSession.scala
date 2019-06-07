package org.processmining.scala.log.common.enhancment.segments.common

import java.io.{File, FileReader}
import java.util.Date
import javax.xml.bind.{JAXBContext, Marshaller}
import javax.xml.stream.XMLInputFactory
import org.processmining.scala.log.utils.common.errorhandling.JvmParams


case class PreprocessingSession(
                                 startMs: Long,
                                 endMs: Long,
                                 twSizeMs: Long,
                                 classCount: Int,
                                 preprocessingStartMs: Long,
                                 preprocessingEndMs: Long,
                                 userInfo: String,
                                 legend: String,
                                 aggregationFunction: String,
                                 durationClassifier: String,
                                 version: String
                               )


object PreprocessingSession {

  val Version: String = JvmParams.getSpecificationVersion()
  val PerformanceSpectrumFormatVersion = "1.1.0"

  def apply(startMs: Long,
            endMs: Long,
            twSizeMs: Long,
            classCount: Int,
            aggregationFunction: String,
            durationClassifier: String
           ): PreprocessingSession =
    PreprocessingSession(startMs, endMs, twSizeMs, classCount, new Date().getTime, -1L, JvmParams.getHostName(), "", aggregationFunction, durationClassifier, PerformanceSpectrumFormatVersion)

  def apply(filename: String): PreprocessingSession = {
    val jaxbContext = JAXBContext.newInstance(classOf[InternalPreProcessingSession])
    val unmarshaller = jaxbContext.createUnmarshaller
    val factory = XMLInputFactory.newInstance
    val fileReader = new FileReader(filename)
    val xmlReader = factory.createXMLStreamReader(fileReader)
    val ips = unmarshaller.unmarshal(xmlReader, classOf[InternalPreProcessingSession]).getValue
    PreprocessingSession(ips.startMs, ips.endMs, ips.twSizeMs, ips.classCount, ips.preprocessingStartMs, ips.preprocessingEndMs, ips.userInfo, ips.legend, ips.aggregationFunction, ips.durationClassifier, ips.version)
  }

  def commit(session: PreprocessingSession): PreprocessingSession
  = session.copy(preprocessingEndMs = new Date().getTime)

  def toDisk(session: PreprocessingSession, canonicalFilename: String, commit: Boolean): Unit = {
    //    val file = new File(canonicalFilename)
    //    val bw = new BufferedWriter(new FileWriter(file))
    //    bw.write((if (commit) PreprocessingSession.commit(session) else session).toXml())
    //    bw.close()

    val internalPreProcessingSession = new InternalPreProcessingSession(
      session.startMs,
      session.endMs,
      session.twSizeMs,
      session.classCount,
      session.preprocessingStartMs,
      session.preprocessingEndMs,
      session.userInfo,
      session.legend,
      session.aggregationFunction,
      session.durationClassifier,
      session.version
    )

    val file = new File(canonicalFilename)
    val jaxbContext = JAXBContext.newInstance(classOf[InternalPreProcessingSession])
    val jaxbMarshaller = jaxbContext.createMarshaller
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    jaxbMarshaller.marshal(internalPreProcessingSession, file)
  }


}
