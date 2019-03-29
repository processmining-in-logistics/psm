package org.processmining.scala.intercase

import org.processmining.scala.log.common.unified.event.UnifiedEvent

trait SegmentBasesFilters {

  def findTargetSegment(a: String, b: String, events: List[UnifiedEvent], startIndex: Int) = {
    val array = events
      .toArray

    val pairs = (0 until array.length - 1)
      .map(i => (array(i), array(i + 1)))
      .zipWithIndex

    pairs.find(x => x._2 >= startIndex && x._1._1.activity == a && x._1._2.activity == b)
  }

  def findTargetSegment(a: String, b: String, events: List[String]) = {
    val array = events
      .toArray

    val pairs = (0 until array.length - 1)
      .map(i => (array(i), array(i + 1)))


    pairs.find(x => x._1 == a && x._2 == b)
  }

  def outcomeForSegment(a: String, b: String)(p: Prefix, events: List[UnifiedEvent]): Option[Outcome] = {
    val optionalSegment = findTargetSegment(a, b, events, p.prefix.size)
    //println(s"""${segment.isDefined} ${events.map(_.activity).mkString("-")}""")
    if (optionalSegment.isDefined) {
      val segment = optionalSegment.get
      Some(Outcome(segment._1._1.timestamp - events(p.prefix.size - 1).timestamp, segment._1._2.timestamp - segment._1._1.timestamp))
    } else None
  }

}
