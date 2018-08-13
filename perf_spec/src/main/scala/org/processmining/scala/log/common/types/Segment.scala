package org.processmining.scala.log.common.types

import org.processmining.scala.log.common.unified.event.CommonAttributeSchemas

/** Represents segments keys */
trait Key {
  val key: String
}

/** Represents duration of events (ms) */
trait Duration {
  val duration: Long
}

@deprecated
class BaseSegment(override val id: String, override val key: String, override val timestamp: Long, override val duration: Long)
  extends Serializable with Id with Timestamp with Key with Duration {
  override def toString: String = s"""$timestamp ($duration) "$id": "$key""""
}

@deprecated
case class Segment(override val id: String, override val key: String, override val timestamp: Long, override val duration: Long)
  extends BaseSegment(id, key, timestamp, duration) {

  def toCsv(msToStringTimestamp: (Long) => String) = s""""$id";"$key";"${msToStringTimestamp(timestamp)}";"$duration""""
}

@deprecated
object Segment {
  val CsvHeader = s""""id";"key";"timestamp";"${CommonAttributeSchemas.AttrNameDuration}""""
}

/** Represents classified segments */
case class SegmentWithClazz(override val id: String, override val key: String, override val timestamp: Long, override val duration: Long, override val clazz: Int)
  extends BaseSegment(id, key, timestamp, duration) with Clazz {

  def toCsv(msToStringTimestamp: Long => String): String =
    s""""${id}";"${key}";"${msToStringTimestamp(timestamp)}";"${duration}";"${clazz}""""

  override def toString: String = s"""${super.toString} $clazz"""
}


object SegmentWithClazz {

  val CsvHeader = s""""id";"key";"timestamp";"${CommonAttributeSchemas.AttrNameDuration}";"${CommonAttributeSchemas.AttrNameClazz}""""

}

