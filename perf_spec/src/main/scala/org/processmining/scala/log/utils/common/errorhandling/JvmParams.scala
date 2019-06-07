package org.processmining.scala.log.utils.common.errorhandling

import java.lang.management.ManagementFactory
import java.net.{InetAddress, UnknownHostException}

import org.slf4j.Logger

object JvmParams {
  def getVmArguments(): String =
    List(ManagementFactory
      .getRuntimeMXBean
      .getInputArguments)
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

  def getSpecificationVersion(): String = getClass().getPackage().getSpecificationVersion()

  def getImplementationVersion(): String = getClass().getPackage().getImplementationVersion()

  def reportToLog(logger: Logger, title: String) = {
    logger.info(title)
    logger.info("Specification version: " + getSpecificationVersion())
    logger.info("Implementation version: " + getImplementationVersion())
    //logger.info("User: " + PreprocessingSession.getHostName)
    logger.info("Args: " + getVmArguments)
    logger.info(getMemoryReport)
    logger.info("Java: " + javaVersion)
    logger.info("Platform: " + javaPlatform)
  }

  def isJavaVersionCorrect(): Boolean = javaVersion.startsWith("1.8.")

  def isJavaPlatformCorrect(): Boolean = javaPlatform == "64"

}