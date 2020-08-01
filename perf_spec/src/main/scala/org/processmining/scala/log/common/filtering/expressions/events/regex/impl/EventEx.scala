package org.processmining.scala.log.common.filtering.expressions.events.regex.impl

import org.processmining.scala.log.common.types.{AbstractRegexNumericPeriod, RegexNumericPeriod}
import org.processmining.scala.log.common.unified.event.UnifiedEventHelper

import scala.reflect.ClassTag

/**
  * Represents information to be compiled into an expression for  event/trace- level filtering
  * Objects of this class should be created via the factory of the companion object
  * See documentations for base classed for override methods
  * @param byteIntLongConverters converters for Byte, Int and Long values
  * @param activity activity name expression
  * @param timestamp timestamp expression
  * @param attrs expressions to match attributes
  */
case class EventEx(byteIntLongConverters: (AbstractRegexNumericPeriod, AbstractRegexNumericPeriod, AbstractRegexNumericPeriod), activity: Option[String], timestamp: Option[String], attrs: Map[String, String]) extends AbstractEventRegexExpression {

  private val (byteConverter, intConverter, longConverter) = byteIntLongConverters

  /**
    * Defines an interval for timestamps
    * @param from left interval (inclusive). Requires values >=0
    * @param to  right interval (inclusive). Requires values >=0 and (from < to)
    * @return new object with modified timestamp field
    */
  def during(from: Long, to: Long): EventEx = {
    require(from >= 0, "'from' must be >= 0")
    require(from < to, "'from' must be < 'to'")
    copy(timestamp = Some(longConverter(from, to)))
  }

  /**
    * Defines an exact value for timestamps
    * @param timestamp value in ms, >=0
    * @return new object with modified timestamp field
    */
  def at(timestamp: Long): EventEx = {
    require(timestamp >= 0, "'timestamp' must be >= 0")
    copy(timestamp = Some(longConverter(timestamp)))
  }

  /**
    * Defines an expression for matching an exact value of a numeric attribute
    * @param name attribute name, must not be empty
    * @param value attribute value
    * @tparam T type of an attribute. For now only Byte, Int and Long are supported
    * @return new object with modified map of attributes expressions
    */
  def withValue[T <: AnyVal : ClassTag](name: String, value: T): EventEx = {
    require(!name.isEmpty, "'name' must not be empty")
    copy(attrs = attrs + (name -> getStringFromValue(value)))
  }

  /**
    * Defines an expression for matching a string attribute
    * @param name attribute name, must not be empty
    * @param value  attribute value
    * @return new object with a modified map of attributes expressions
    */
  def withValue(name: String, value: String): EventEx = {
    require(!name.isEmpty, "'name' must not be empty")
    copy(attrs = attrs + (name -> value))
  }

  /**
    * Defines and expression for matching a range of numeric attribute values
    * @param name attribute name, must not be empty
    * @param from left interval (inclusive). Requires values >=0
    * @param until right interval (inclusive). Requires values >=0 and (from < to)
    * @tparam T type of an attribute. For now only Byte, Int and Long are supported
    * @return new object with a modified map of attributes expressions
    */
  def withRange[T <: AnyVal : ClassTag](name: String, from: T, until: T): EventEx = {
    require(!name.isEmpty, "'name' must not be empty")
    copy(attrs = attrs + (name -> getStringFromRange(from, until)))
  }


  private def getStringFromValue[T: ClassTag](value: T): String =
    value match {
      case i: Long =>
        require(i >= 0, "'i' must be >= 0")
        longConverter(i)
      case i: Int =>
        require(i >= 0, "'i' must be >= 0")
        intConverter(i)
      case i: Byte =>
        require(i >= 0, "'i' must be >= 0")
        byteConverter(i)
      case _ => value.toString
    }

  private def getStringFromRange[T <: AnyVal : ClassTag](range: (T, T)): String =
    range match {
      case (from: Long, until: Long) =>
        require(from >= 0, "'from' must be >= 0")
        require(from < until, "'from' must be < 'until'")
        longConverter(from, until)
      case (from: Int, until: Int) =>
        require(from >= 0, "'from' must be >= 0")
        require(from < until, "'from' must be < 'until'")
        intConverter(from, until)
      case (from: Byte, until: Byte) =>
        require(from >= 0, "'from' must be >= 0")
        require(from < until, "'from' must be < 'until'")
        byteConverter(from, until)
      case _ => ???

    }

  /** See base classes */
  override def translate(): String = {
    val attrsSeq = attrs.toSeq.sortBy(_._1).map(UnifiedEventHelper.LeftAttributeSep + _._2 + UnifiedEventHelper.RightAttributeSep)
    val anyButRightEventSep_0plus = "[^" + UnifiedEventHelper.RightEventSep + "]*?"
    val anyButRightEventSep_1plus = "[^" + UnifiedEventHelper.RightEventSep + "]+?"

    UnifiedEventHelper.LeftEventSep +
      (if (activity.isDefined) s"${activity.get}" else anyButRightEventSep_1plus) + UnifiedEventHelper.ActivitySep +
      (if (timestamp.isDefined) s"${timestamp.get}" else "\\d+") + UnifiedEventHelper.TimestampSep +
      (if (attrsSeq.isEmpty) anyButRightEventSep_0plus else attrsSeq.mkString(anyButRightEventSep_0plus, anyButRightEventSep_0plus, anyButRightEventSep_0plus)) + UnifiedEventHelper.RightEventSep
  }

}

/** Factory for  class EventEx*/
object EventEx {
  /**
    * Create a new object with a provided expression for the activity name
    * @param activityName an expression for the activity name
    * @return a new object
    */
  def apply(activityName: String): EventEx = new EventEx(RegexNumericPeriod.ByteIntLongConverters, Some(activityName), None, Map())

  /**
    * Create a new object without any expressions defined
    * @return a new object
    */
  def apply(): EventEx = new EventEx(RegexNumericPeriod.ByteIntLongConverters, None, None, Map())
}