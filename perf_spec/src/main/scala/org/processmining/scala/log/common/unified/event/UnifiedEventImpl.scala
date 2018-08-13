package org.processmining.scala.log.common.unified.event

import org.processmining.scala.log.common.types.RegexNumericPeriod

import scala.collection.immutable.SortedMap

private class UnifiedEventImpl(
                                override val timestamp: Long,
                                override val activity: String,
                                private val attrs: SortedMap[String, Any],
                                override val aggregatedEvents: Option[Seq[UnifiedEvent]]
                              ) extends UnifiedEvent {


  override def attributes: SortedMap[String, Any] = attrs

  override def regexpableForm(): String =
    s"${UnifiedEventHelper.LeftEventSep}$activity${UnifiedEventHelper.ActivitySep}${RegexNumericPeriod.LongConverter(timestamp)}${UnifiedEventHelper.TimestampSep}${attrsToString()}${UnifiedEventHelper.RightEventSep}"

  override def toString(): String = regexpableForm()

  def attr2String(value: Any): String =
    value match {
      case x: Long => RegexNumericPeriod.LongConverter(x)
      case x: Int => RegexNumericPeriod.IntegerConverter(x)
      case x: Byte => RegexNumericPeriod.ByteConverter(x)
      case _ => value.toString
    }

  private def attr2CsvString(value: Any): String =
    value match {
      case x: Long => x.toString
      case x: Int => x.toString
      case x: Byte => x.toString
      case _ => s""""${value.toString}""""
    }

  private def attrsToString(): String =
    attrs
      .values
      .map(UnifiedEventHelper.LeftAttributeSep + attr2String(_) + UnifiedEventHelper.RightAttributeSep).mkString("")

  override def hasAttribute(attributeName: String): Boolean =
    attrs.keySet.contains(attributeName)

  override def toCsvWithoutAttributes(timestampConverter: Long => String): String =
    s""""${timestampConverter(timestamp)}";"${activity}""""

  override def toCsv(timestampConverter: Long => String, attributeNames: String*): String =
    toCsvWithoutAttributes(timestampConverter) +
      (if (attributeNames.nonEmpty)
        attributeNames.map(getAsString)
          .mkString(";", ";", "")
      else "")

  private def getAsString(name: String): String =
    if (hasAttribute(name)) attr2CsvString(attrs(name)) else ""

  def canEqual(other: Any): Boolean = other.isInstanceOf[UnifiedEventImpl]

  override def equals(other: Any): Boolean = other match {
    case that: UnifiedEventImpl =>
      (that canEqual this) &&
        timestamp == that.timestamp &&
        activity == that.activity &&
        attrs == that.attrs &&
        aggregatedEvents == that.aggregatedEvents
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(timestamp, activity, attrs, aggregatedEvents)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  /** Checks if the type of the attribute is T */
  //override def isInstanceOf[T: ClassTag](name: String) =
  //    attrs(name).isInstanceOf[T]

  /** Throws an exception if an attribute with the given name does not exist */
  override def getAs[T](name: String): T =
    attrs(name).asInstanceOf[T]

  override def hasAttributes(attrNames: Set[String]): Boolean =
    attrs.keySet.intersect(attrNames).size == attrNames.size

  override def intersection(keySet: Set[String]): SortedMap[String, Any] =
    attrs.filter(x => keySet.contains(x._1))

  /** Keeps in only attributes that are provided */
  override def project(attrNames: Set[String]): UnifiedEvent =
    UnifiedEvent(timestamp, activity, attrs.filter(x => attrNames.contains(x._1)), aggregatedEvents)

  override def copy(newActivityName: String) = new UnifiedEventImpl(timestamp, newActivityName, attrs, aggregatedEvents)

  override def copy(attrName: String, attrValue: Any): UnifiedEvent =
    new UnifiedEventImpl(timestamp, activity, attrs + (attrName -> attrValue), aggregatedEvents)

  override def copy(newTimestamp: Long) = new UnifiedEventImpl(newTimestamp, activity, attrs, aggregatedEvents)


}

