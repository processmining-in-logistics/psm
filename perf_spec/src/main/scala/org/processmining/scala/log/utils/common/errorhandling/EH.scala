package org.processmining.scala.log.utils.common.errorhandling

import java.io.{PrintWriter, StringWriter}

import javax.swing.JOptionPane
import org.slf4j.LoggerFactory


class EH {

  private val logger = LoggerFactory.getLogger(EH.getClass)

  private var stopByError = false
  private var showMessageBox = false
  private var showLongMessages = false

  def enableStopByError() = stopByError = true

  def enableMessageBoxes() = showMessageBox = true

  def enableLongMessagesInMessageBoxes() = showLongMessages = true

  def error(msg: String, ex: Throwable): Unit = {
    logger.error(msg, ex)
    if (showMessageBox) msgBox(msg, ex, JOptionPane.ERROR_MESSAGE)
    if (stopByError) System.exit(-1)
    if (ex.isInstanceOf[OutOfMemoryError]) System.exit(-2)
  }

  def error(ex: Throwable): Unit = {
    error("Error", ex)
  }

  def errorAndMessageBox(msg: String, ex: Throwable) = {
    logger.error(msg, ex)
    msgBox(msg, ex, JOptionPane.ERROR_MESSAGE)

  }

  def warnAndMessageBox(msg: String, ex: Throwable) = {
    logger.warn(msg, ex)
    msgBox(msg, ex, JOptionPane.WARNING_MESSAGE)

  }


  private def msgBox(msg: String, ex: Throwable, flag: Int): Unit =
    msgBox(if (showLongMessages) EH.formatError(msg, ex) else EH.formatShortError(msg, ex), flag)

  private def msgBox(msg: String, flag: Int): Unit = JOptionPane.showMessageDialog(null, msg, "Application error", flag)
}


object EH {

  private val obj = new EH

  def formatError(msg: String, ex: Throwable) = s"$msg: \n\r${getStackTrace(ex)}"

  def formatShortError(msg: String, ex: Throwable) = s"$msg: \n\r${ex.getMessage}"

  def getStackTrace(ex: Throwable): String = {
    val stack = new StringWriter
    ex.printStackTrace(new PrintWriter(stack))
    stack.toString
  }

  def apply(ignored: Unit): EH = obj

  def apply(): EH = obj
}
