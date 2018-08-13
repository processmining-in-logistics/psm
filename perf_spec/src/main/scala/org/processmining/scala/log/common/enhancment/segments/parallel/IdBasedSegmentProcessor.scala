package org.processmining.scala.log.common.enhancment.segments.parallel

import org.processmining.scala.log.common.types.SegmentWithClazz

import scala.collection.parallel.{ParSeq, ParSet}

class IdBasedSegmentProcessor(config: SegmentProcessorConfig, ids: ParSet[String]) extends SegmentProcessor(config, 2) {

  override def getClassifiedSegments(): ParSeq[SegmentWithClazz] =
    SegmentProcessor.getSegments(config.segments)
      .map(x => SegmentWithClazz(x.id, x.key, x.timestamp, x.duration, if (ids.contains(x.id)) 1 else 0))
}
