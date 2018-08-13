package org.processmining.scala.log.common.enhancment.segments.parallel

import org.processmining.scala.log.common.enhancment.segments.common.NodesProcessorHelper2
import org.processmining.scala.log.common.types.SegmentWithClazz

import scala.collection.parallel.ParSeq

class NodesProcessor[T](config: SegmentProcessorConfig,
                        classCount: Int,
                        attributes: List[T],
                        id: T => String,
                        timestamp: T => Long,
                        clazz: T => Int
                       ) extends SegmentProcessor(config, classCount) {

  override def getClassifiedSegments(): ParSeq[SegmentWithClazz] = {
    val mapOfAttributes = attributes
      .groupBy(id(_))

    SegmentProcessor.getTracesWithSegments(config.segments)
      .map(x => (x._2, mapOfAttributes.get(x._1)))
      .flatMap { x => NodesProcessorHelper2.addAttribute[T](x._1, x._2, id, timestamp, clazz) }
  }
}
