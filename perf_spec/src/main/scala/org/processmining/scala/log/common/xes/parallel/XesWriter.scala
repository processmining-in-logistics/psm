package org.processmining.scala.log.common.xes.parallel

import java.io.FileOutputStream
import java.util.Date
import org.deckfour.xes.extension.std.{XConceptExtension, XTimeExtension}
import org.deckfour.xes.factory.{XFactory, XFactoryRegistry}
import org.deckfour.xes.model._
import org.deckfour.xes.out.{XMxmlGZIPSerializer, XesXmlGZIPSerializer, XesXmlSerializer}
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.slf4j.LoggerFactory;

/** Not implemented yet */
object XesWriter {

  private val logger = LoggerFactory.getLogger(XesWriter.getClass)

  def unifiedEventLogToXes(uLog: UnifiedEventLog, factory: XFactory, traceAttributePrefix: String): XLog = {
    val xLog = factory.createLog()
    for ((id, uEvents) <- uLog.traces().seq) {
      val xTrace = factory.createTrace()
      XConceptExtension.instance().assignName(xTrace, id.id)
      for (uEvent <- uEvents) {
        val xEvent = factory.createEvent()
        uEvent.attributes.foreach(a => {
          val (key, isTraceAttr) = if (a._1.startsWith(traceAttributePrefix)) (a._1.substring(traceAttributePrefix.length), true) else (a._1, false)
          if (key != "concept:name" && key != "time:timestamp") {
            val value =
              a._2 match {
                case attrValue: String => factory.createAttributeLiteral(key, attrValue, null)
                case attrValue: Long => factory.createAttributeDiscrete(key, attrValue, null)
                case attrValue: Double => factory.createAttributeContinuous(key, attrValue, null)
                case attrValue: Boolean => factory.createAttributeBoolean(key, attrValue, null)
                case attrValue: Date => factory.createAttributeTimestamp(key, attrValue, null)
                //TODO: add ID
                case attrValue: Any =>
                  logger.warn(s"Cannot detect type of attribute '$a': type is not supported")
                  factory.createAttributeLiteral(key, attrValue.toString, null)
              }
            if (isTraceAttr) xTrace.getAttributes.put(key, value) else xEvent.getAttributes.put(key, value)
          }
        })
        XConceptExtension.instance().assignName(xEvent, uEvent.activity)
        XTimeExtension.instance().assignTimestamp(xEvent, uEvent.timestamp)
        xTrace.add(xEvent)
      }
      xLog.add(xTrace)
    }
    return xLog
  }

  def write(uLog: UnifiedEventLog, logFileName: String, traceAttributePrefix: String): XLog = {
    val factory = XFactoryRegistry.instance.currentDefault
    val xLog = unifiedEventLogToXes(uLog, factory, traceAttributePrefix)

    // TODO: support XESLite parsers by F.Mannhardt: https://svn.win.tue.nl/repos/prom/Packages/XESLite/Trunk
    val xSerializer =
      if (logFileName.endsWith("gz")) new XesXmlGZIPSerializer()
      else if (logFileName.endsWith("xml.zip")) new XMxmlGZIPSerializer()
      else new XesXmlSerializer()

    xSerializer.serialize(xLog, new FileOutputStream(logFileName))
    return xLog
  }
}
