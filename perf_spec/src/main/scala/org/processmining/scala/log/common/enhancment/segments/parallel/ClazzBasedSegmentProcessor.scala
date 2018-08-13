package org.processmining.scala.log.common.enhancment.segments.parallel

import org.processmining.scala.log.common.enhancment.segments.common.ClazzBasedSegmentProcessorHelper2
import org.processmining.scala.log.common.types.SegmentWithClazz

import scala.collection.parallel.ParSeq

class ClazzBasedSegmentProcessor[T](config: SegmentProcessorConfig,
                                    classCount: Int,
                                    attributes: List[T], //TODO: ParSeq
                                    id: T => String,
                                    timestamp: T => Long,
                                    clazz: T => Int
                                   ) extends SegmentProcessor(config, classCount) {

  override def getClassifiedSegments(): ParSeq[SegmentWithClazz] = {
    val mapOfAttributes = attributes
      .groupBy(id(_))

    SegmentProcessor.getTracesWithSegments(config.segments)
      .map(x => (x._2, mapOfAttributes.get(x._1)))
      .flatMap { x => ClazzBasedSegmentProcessorHelper2.addAttribute(x._1.sortBy(_.timestamp), x._2, id, timestamp, clazz) }
  }

}
