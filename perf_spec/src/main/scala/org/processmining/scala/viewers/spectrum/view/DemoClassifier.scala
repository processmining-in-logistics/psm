package org.processmining.scala.viewers.spectrum.view

import org.processmining.scala.log.common.enhancment.segments.common.AbstractClassifier
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.trace.UnifiedTraceId

class DemoClassifier extends AbstractClassifier {
  def func(e: (UnifiedTraceId, UnifiedEvent)): Int = e._2.activity.length % 5

  override def classCount: Int = 5

  override def legend: String = "SOME_CLASS%Q1%Q2%Q3%Q4%Q5"
}


//class RtfmAmountClassifier extends AbstractClassifier {
//  def func(e: (UnifiedTraceId, UnifiedEvent)): Int = {
//    val amountOpt = e._2.attributes.get("amount")
//    if (!amountOpt.isDefined) 0 else amountOpt.get.asInstanceOf[Double] match {
//      case a if a < 30.0 => 1
//      case a if a < 60.0 => 2
//      case a if a < 100.0 => 3
//      case _ => 4
//    }
//  }
//
//  override def classCount: Int = 5
//
//  override def legend: String = "Amount%Unknown%<30%30-60%60-100%>100"
//}
