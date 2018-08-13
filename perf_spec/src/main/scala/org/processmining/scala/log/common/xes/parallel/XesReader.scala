package org.processmining.scala.log.common.xes.parallel

import java.io.{BufferedInputStream, File, FileInputStream}
import java.util.zip.{GZIPInputStream, ZipFile}
import org.slf4j.LoggerFactory;
import org.deckfour.xes.extension.std.{XConceptExtension, XTimeExtension}
import org.deckfour.xes.factory.XFactoryRegistry
import org.deckfour.xes.in.{XMxmlParser, XesXmlParser}
import org.deckfour.xes.model._
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.processmining.scala.log.common.unified.trace.UnifiedTraceIdImpl

import scala.collection.JavaConversions._
import scala.collection.immutable.SortedMap

object XesReader {

  private val logger = LoggerFactory.getLogger(XesReader.getClass)

  val DefaultTraceAttrNamePrefix = "T_"
  val DiscoTraceAttrNamePrefix = "(case)_"
  val DiscoTraceAttrNamePrefixEscaped = "\\(case\\)_"
  val DefaultActivitySep = "-"
  val UnknownAttributeValue = "?"
  val NullValue = "NULL"

  def xesToUnifiedEventLog(xLog: XLog): UnifiedEventLog = xesToUnifiedEventLog(xLog, None, None, DefaultTraceAttrNamePrefix, DefaultActivitySep)

  private def getXattrValue(key: String, xattr: XAttribute): Any =
    if (xattr == null) NullValue else xattr match {
      case attr: XAttributeLiteral => attr.getValue
      case attr: XAttributeDiscrete => attr.getValue
      case attr: XAttributeContinuous => attr.getValue
      case attr: XAttributeBoolean => attr.getValue
      case attr: XAttributeID => attr.getValue.toString // XID is not serializable!
      case attr: XAttributeTimestamp => attr.getValue
      case _ => logger.warn(s"Cannot import attribute '$key': '${xattr.toString}': type is not supported")
        UnknownAttributeValue
    }

  private def extractAttributes(attrs: XAttributeMap, attrNamesToBeIncluded: Option[Set[String]], prefix: String) =
    (if (attrNamesToBeIncluded.isDefined)
      attrNamesToBeIncluded.get.map(x => (prefix + x, getXattrValue(x, attrs.get(x)))).toMap
    else
      attrs.map(x => (prefix + x._1, getXattrValue(x._1, attrs.get(x._1)))).toMap)
      .filter(_._2 != NullValue)


  //  private def extractAttributes(attrs: XAttributeMap, attrNamesToBeIncluded: Option[Set[String]], prefix: String): mutable.Map[String, Any] =
  //    (if (attrNamesToBeIncluded.isDefined)
  //      attrs.filter(x => attrNamesToBeIncluded.get.contains(x._1))
  //    else attrs.filter(x => /*x._1 != XConceptExtension.KEY_NAME &&*/ x._1 != XTimeExtension.KEY_TIMESTAMP))
  //      .map { pair =>
  //        (prefix + pair._1, pair._2 match {
  //          case attr: XAttributeLiteral => attr.getValue
  //          case attr: XAttributeDiscrete => attr.getValue
  //          case attr: XAttributeContinuous => attr.getValue
  //          case attr: XAttributeBoolean => attr.getValue
  //          case attr: XAttributeID => attr.getValue.toString // XID is not serializable!
  //          case attr: XAttributeTimestamp => attr.getValue
  //          case _ => logger.warn(s"Cannot import attribute '$prefix${pair._1}': '${pair._2.toString}': type is not supported")
  //            UnknownAttributeValue
  //        })
  //      }

  def xesToTraces(xLog: XLog,
                  traceLevelAttributes: Option[Set[String]],
                  eventLevelAttributes: Option[Set[String]],
                  sep: String,
                  activityClassifier: String*) = {
    xLog
      .toSeq
      .par // TODO: thread-safe? faster?
      .map { xTrace =>
      val events = xTrace
        .toList
        .map {
          xEvent =>
            val attributes = SortedMap[String, Any]() ++
              extractAttributes(xEvent.getAttributes, eventLevelAttributes, "") ++
              extractAttributes(xTrace.getAttributes, traceLevelAttributes, DefaultTraceAttrNamePrefix)
            val activityName = if (activityClassifier.isEmpty) XConceptExtension.instance.extractName(xEvent)
            else activityClassifier.map(attributes(_).toString).mkString(sep)
            UnifiedEvent(XTimeExtension.instance.extractTimestamp(xEvent).getTime, activityName, attributes, None)
        }.sortBy(_.timestamp)
      (new UnifiedTraceIdImpl(XConceptExtension.instance.extractName(xTrace)), events)
    }
  }


  def xesToUnifiedEventLog(xLog: XLog,
                           traceLevelAttributes: Option[Set[String]],
                           eventLevelAttributes: Option[Set[String]],
                           sep: String,
                           activityClassifier: String*): UnifiedEventLog =
    UnifiedEventLog.fromTraces(
      xesToTraces(xLog, traceLevelAttributes, eventLevelAttributes, sep, activityClassifier: _*)
    )


  def read(filename: String): Seq[UnifiedEventLog] = read(new File(filename))

  //def read(filename: String, sep: String, activityClassifier: String*): Seq[UnifiedEventLog] = read(new File(filename), sep, activityClassifier: _*)

  def read(logFile: File): Seq[UnifiedEventLog] = read(logFile, None, None, DefaultTraceAttrNamePrefix, DefaultActivitySep)


  def readXes(logFile: File): Seq[XLog] = {
    val factory = XFactoryRegistry.instance.currentDefault
    val extension = logFile.getName.substring(logFile.getName.lastIndexOf('.'))
    // TODO: support XESLite parsers by F.Mannhardt: https://svn.win.tue.nl/repos/prom/Packages/XESLite/Trunk
    // choose correct file input stream based on container format
    val fileInputStream = extension match {
      case ".gz" => new GZIPInputStream(new FileInputStream(logFile))
      case ".zip" =>
        // get first entry from zipfile
        val zipFile = new ZipFile(logFile)
        zipFile.getInputStream(zipFile.entries().nextElement())
      case _ => new BufferedInputStream(new FileInputStream(logFile))
    }
    // then choose correct parser for either XES or MXML
    val parser =
      if (logFile.getName.endsWith(".xes") || logFile.getName.endsWith(".xes.gz")) new XesXmlParser(factory)
      else new XMxmlParser(factory)
    val logs = parser.parse(fileInputStream).toSeq
    logger.info(s"File '$logFile' is loaded into XLog object(s). ${logs.size} log(s) found.")
    logs
  }


  def read(logFile: File,
           traceLevelAttributes: Option[Set[String]],
           eventLevelAttributes: Option[Set[String]],
           traceAttributeNamePrefix: String,
           sep: String,
           activityClassifier: String*): Seq[UnifiedEventLog] = {

    val logs = readXes(logFile)
    logs.map(x => xesToUnifiedEventLog(x, traceLevelAttributes, eventLevelAttributes, sep, activityClassifier: _*))
  }

  def read(logs: Seq[XLog],
           traceLevelAttributes: Option[Set[String]],
           eventLevelAttributes: Option[Set[String]],
           traceAttributeNamePrefix: String,
           sep: String,
           activityClassifier: String*): Seq[UnifiedEventLog] = {
    logs.map(x => xesToUnifiedEventLog(x, traceLevelAttributes, eventLevelAttributes, sep, activityClassifier: _*))
  }
}
