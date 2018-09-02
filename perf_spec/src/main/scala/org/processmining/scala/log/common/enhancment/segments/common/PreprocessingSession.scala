package org.processmining.scala.log.common.enhancment.segments.common

import java.io.{File, FileReader}
import java.lang.management.ManagementFactory
import java.net.{InetAddress, UnknownHostException}
import java.util.Date

import javax.xml.bind.{JAXBContext, Marshaller}
import javax.xml.stream.XMLInputFactory
import org.slf4j.Logger

import scala.collection.JavaConversions._


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
                                 durationClassifier: String
                               )


object PreprocessingSession {

  val Version: String = "1.0.2.2018-09-02"

  def apply(startMs: Long,
            endMs: Long,
            twSizeMs: Long,
            classCount: Int,
            aggregationFunction: String,
            durationClassifier: String
           ): PreprocessingSession =
    PreprocessingSession(startMs, endMs, twSizeMs, classCount, new Date().getTime, -1L, getHostName(), "", aggregationFunction, durationClassifier)

  def apply(filename: String): PreprocessingSession = {
    val jaxbContext = JAXBContext.newInstance(classOf[InternalPreProcessingSession])
    val unmarshaller = jaxbContext.createUnmarshaller
    val factory = XMLInputFactory.newInstance
    val fileReader = new FileReader(filename)
    val xmlReader = factory.createXMLStreamReader(fileReader)
    val ips = unmarshaller.unmarshal(xmlReader, classOf[InternalPreProcessingSession]).getValue
    PreprocessingSession(ips.startMs, ips.endMs, ips.twSizeMs, ips.classCount, ips.preprocessingStartMs, ips.preprocessingEndMs, ips.userInfo, ips.legend, ips.aggregationFunction, ips.durationClassifier)
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
      session.durationClassifier
    )

    val file = new File(canonicalFilename)
    val jaxbContext = JAXBContext.newInstance(classOf[InternalPreProcessingSession])
    val jaxbMarshaller = jaxbContext.createMarshaller
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    jaxbMarshaller.marshal(internalPreProcessingSession, file)
  }

  def getVmArguments(): String =
    ManagementFactory
      .getRuntimeMXBean
      .getInputArguments
      .mkString(" ")

  private def toMb(bytes: Long): Long = bytes / (1024 * 1024)

  def getMemoryReport(): String = {
    val r = Runtime.getRuntime
    s"Total memory = ${toMb(r.totalMemory())}Mb " +
      s"Max memory = ${toMb(r.maxMemory())}Mb " +
      s"Free memory = ${toMb(r.freeMemory())}Mb " +
      s"Processors = ${r.availableProcessors()} "

  }


  def getHostName(): String = {
    val systemUsername = Some(System.getProperty("user.name"))
    val username = if (systemUsername.isDefined) systemUsername.get else "Unknown"
    try {
      s"${InetAddress.getLocalHost.getHostName}\\$username"
    } catch {
      case ex: UnknownHostException => s"Unknown\\$username"
    }
  }

  def javaVersion(): String = if (System.getProperty("java.version") != null) System.getProperty("java.version") else "Unknown"

  def javaPlatform(): String = if (System.getProperty("sun.arch.data.model") != null) System.getProperty("sun.arch.data.model") else "Unknown"


  def reportToLog(logger: Logger, title: String) = {
    logger.info(title)
    logger.info("Specification version: " +   getClass().getPackage().getSpecificationVersion())
    logger.info("Implementation version: " +   getClass().getPackage().getImplementationVersion())
    logger.info("Internal version: " + Version)
    //logger.info("User: " + PreprocessingSession.getHostName)
    logger.info("Args: " + PreprocessingSession.getVmArguments)
    logger.info(PreprocessingSession.getMemoryReport)
    logger.info("Java: " + javaVersion)
    logger.info("Platform: " + javaPlatform)
  }

  def isJavaVersionCorrect(): Boolean = javaVersion.startsWith("1.8.")

  def isJavaPlatformCorrect(): Boolean = javaPlatform == "64"


}
