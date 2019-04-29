package org.processmining.scala.viewers.spectrum.builder

import java.io.File
import java.util.Date
import java.util.concurrent.Callable

import org.deckfour.xes.model.XLog
import org.processmining.scala.log.common.enhancment.segments.common.{AbstractClassifier, SegmentUtils}
import org.processmining.scala.log.common.enhancment.segments.parallel.{ClazzBasedSegmentProcessor, NodesProcessor, SegmentProcessor, SegmentProcessorConfig}
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.trace.UnifiedTraceId
import org.processmining.scala.log.common.xes.parallel.XesReader
import org.slf4j.LoggerFactory

class ClassNodeBasedPreProcessor(filename1: String,
                                 originalXLog1: XLog,
                                 filename2: String,
                                 originalXLog2: XLog,
                                 sep: String,
                                 activityClassifier: Array[String],
                                 dir: String,
                                 twSize: Long,
                                 aggregationFunctionCode: Int,
                                 classifierClassName: String,
                                 classCode: Int,
                                 handler: Runnable) extends Callable[String] {
  private val logger = LoggerFactory.getLogger(classOf[PreProcessor].getName)

  override def call(): String = {
    val classifier = AbstractClassifier(classifierClassName)
    new File(dir).mkdirs()

    val xLog1 = if (filename1.nonEmpty) {
      val logs1 = XesReader.readXes(new File(filename1))
      if (logs1.isEmpty) throw new RuntimeException(s"XES file '$filename1' is empty") else logs1.head
    } else originalXLog1

    val xLog2 = if (filename2.nonEmpty) {
      val logs2 = XesReader.readXes(new File(filename2))
      if (logs2.isEmpty) throw new RuntimeException(s"XES file '$filename2' is empty") else logs2.head
    } else originalXLog2

    handler.run()
    val unifiedEventLog1 = XesReader.read(Seq(xLog1), None, None, XesReader.DefaultTraceAttrNamePrefix, sep, activityClassifier: _*).head
    val unifiedEventLog2 = if (xLog2 != null) XesReader.read(Seq(xLog2), None, None, XesReader.DefaultTraceAttrNamePrefix, sep, Array[String](): _*).head else unifiedEventLog1

    val (timestamp1Ms, timestamp2Ms) = unifiedEventLog1.minMaxTimestamp()
    logger.info(s"Log for $dir has timestamps from ${new Date(timestamp1Ms)} to ${new Date(timestamp2Ms)}")
    val segments = unifiedEventLog1
      .map(SegmentUtils.convertToSegments(":", _: (UnifiedTraceId, List[UnifiedEvent])))
    val processorConfig = SegmentProcessorConfig(
      segments,
      timestamp1Ms, timestamp2Ms,
      twSize, PreProcessor.aggregationFunction(aggregationFunctionCode))

    val processor = if (classCode == 0) new ClazzBasedSegmentProcessor[(UnifiedTraceId, UnifiedEvent)](
      processorConfig, classifier.classCount, unifiedEventLog2.events().toList, _._1.id, _._2.timestamp, classifier.func)
    else new NodesProcessor[(UnifiedTraceId, UnifiedEvent)](
      processorConfig, classifier.classCount, unifiedEventLog2.events().toList, _._1.id, _._2.timestamp, classifier.func)
    SegmentProcessor.toCsvV2(
      processor, s"$dir/", classifier.legend)
    dir
  }

}
