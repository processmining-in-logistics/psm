package org.processmining.scala.viewers.spectrum.view

import java.io.File
import java.util.Date
import java.util.concurrent.Callable

import org.deckfour.xes.model.XLog
import org.processmining.scala.log.common.enhancment.segments.common._
import org.processmining.scala.log.common.enhancment.segments.parallel.{DurationSegmentProcessor, SegmentProcessor, SegmentProcessorConfig}
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.trace.UnifiedTraceId
import org.processmining.scala.log.common.xes.parallel.XesReader
import org.slf4j.LoggerFactory


class PreProcessor(filename: String,
                   originalXLog: XLog,
                   sep: String,
                   activityClassifier: Array[String],
                   dir: String,
                   twSize: Long,
                   aggregationFunctionCode: Int,
                   durationClassifierCode: Int,
                   handler: Runnable) extends Callable[String] {
  private val logger = LoggerFactory.getLogger(classOf[PreProcessor].getName)

  override def call(): String = {
    new File(dir).mkdirs()
    val xLog = if (filename.nonEmpty) {
      val logs = XesReader.readXes(new File(filename))
      if (logs.isEmpty) throw new RuntimeException(s"XES file '$filename' is empty") else logs.head
    } else originalXLog
    handler.run()
    val unifiedEventLog = XesReader.read(Seq(xLog), None, None, XesReader.DefaultTraceAttrNamePrefix, sep, activityClassifier: _*).head
    val (timestamp1Ms, timestamp2Ms) = unifiedEventLog.minMaxTimestamp()
    logger.info(s"Log for $dir has timestamps from ${new Date(timestamp1Ms)} to ${new Date(timestamp2Ms)}")
    val segments = unifiedEventLog
      .map(SegmentUtils.convertToSegments(":", _: (UnifiedTraceId, List[UnifiedEvent])))
    val processorConfig = SegmentProcessorConfig(
      segments,
      timestamp1Ms, timestamp2Ms,
      twSize,  PreProcessor.aggregationFunction(aggregationFunctionCode))
    val (_, _, durationProcessor) = DurationSegmentProcessor(processorConfig, PreProcessor.durationClassifier(durationClassifierCode))
    SegmentProcessor.toCsvV2(
      durationProcessor, s"$dir/", durationProcessor.adc.legend)
    dir
  }

}


object PreProcessor{
  val InventoryAggregationCode = 0
  val StartAggregationCode = 1
  val EndAggregationCode = 2

  val Q4DurationClassifierCode = 0
  val FasterNormal23VerySlowDurationClassifierCode = 1



  def aggregationFunction(code: Int): AbstractAggregationFunction =
    code match {
      case InventoryAggregationCode =>  InventoryAggregation
      case StartAggregationCode =>  StartAggregation
      case EndAggregationCode =>  EndAggregation
      case _ => throw new RuntimeException(s"Wrong aggregation code $code")
    }

  def durationClassifier(code: Int): AbstractDurationClassifier =
    code match {
      case Q4DurationClassifierCode =>  new Q4DurationClassifier
      case FasterNormal23VerySlowDurationClassifierCode =>  new FasterNormal23VerySlowDurationClassifier
      case _ => throw new RuntimeException(s"Wrong classifier code $code")
    }


}