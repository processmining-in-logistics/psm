package org.processmining.scala.log.common.enhancment.segments.common

import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.trace.UnifiedTraceId

trait AbstractClassifier extends Serializable {
  def func(e: (UnifiedTraceId, UnifiedEvent)): Int

  def classCount: Int

  def legend: String
}

object AbstractClassifier {
  def apply(name: String): AbstractClassifier =
    Class.forName(name).newInstance().asInstanceOf[AbstractClassifier]
}
