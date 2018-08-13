package org.processmining.scala.log.common.utils.common

import java.io.{PrintWriter, StringWriter}

import javax.swing.JOptionPane
import org.slf4j.LoggerFactory


class EH {

  private val logger = LoggerFactory.getLogger(EH.getClass)

  private var stopByError = false
  private var showMessageBox = false

  def enableStopByError() = stopByError = true

  def enableMessageBoxes() = showMessageBox = true

  def error(msg: String, ex: Throwable) = {
    logger.error(msg, ex)
    if (showMessageBox) msgBox(EH.formatError(msg, ex))
    if (stopByError) System.exit(-1)
    if(ex.isInstanceOf[OutOfMemoryError]) System.exit(-2)
  }

  def errorAndMessageBox(msg: String, ex: Throwable) = {
    logger.error(msg, ex)
    msgBox(EH.formatError(msg, ex))

  }

  private def msgBox(msg: String) = JOptionPane.showMessageDialog(null, msg, "Application error", JOptionPane.ERROR_MESSAGE)
}


object EH {

  private val obj = new EH

  def formatError(msg: String, ex: Throwable) = s"$msg: \n\r${getStackTrace(ex)}"

  def getStackTrace(ex: Throwable): String = {
    val stack = new StringWriter
    ex.printStackTrace(new PrintWriter(stack))
    stack.toString
  }

  def apply: EH = obj
}
