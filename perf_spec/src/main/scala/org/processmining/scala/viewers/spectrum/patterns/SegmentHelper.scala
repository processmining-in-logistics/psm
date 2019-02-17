package org.processmining.scala.viewers.spectrum.patterns

import org.processmining.scala.viewers.spectrum.model.AbstractDataSource

class SegmentHelper(protected val ds: AbstractDataSource) {
  //private val logger = LoggerFactory.getLogger(classOf[AbstractDataSource].getName)
  protected def getUnorderedSegments(name: String): Seq[(String, Long, Long, Int)] =
    (0 until ds.twCount)
      .map(ds.segments(_).get(name))
      .filter(_.isDefined)
      .map(_.get)
      .flatMap(_.flatMap(y => y._2.map(s => (s._1, s._2, s._2 + s._3, y._1))))


  protected def getOrderedByStartSegments(name: String): Seq[(String, Long, Long, Int)] =
    getUnorderedSegments(name).sortBy(_._2)

  protected def hasIntersections(sortedByStart: Seq[(String, Long, Long, Int)]): Boolean = {
    val sortedByEnd = sortedByStart.sortBy(_._3) //stable sorting of sortedByStart
    sortedByStart != sortedByEnd
  }

}
