package org.processmining.scala.log.common.unified.event

import org.processmining.scala.log.common.types._

import scala.collection.immutable.SortedMap

/** Defines the interface for events */
trait UnifiedEvent extends Serializable with Timestamp with Activity {

  def intersection(keySet: Set[String]): Map[String, Any]

  def attributes: SortedMap[String, Any]

  def hasAttributes(attrNames: Set[String]): Boolean

  /** Checks if an attribute with the given name exists */
  def hasAttribute(attributeName: String): Boolean

  /** Checks if the type of the attribute is T */
  //def isInstanceOf[T: ClassTag](name: String): Boolean

  /** Throws an exception if an attribute with the given name does not exist */
  def getAs[T](name: String): T

  /** Aggregated events (if any) */
  def aggregatedEvents: Option[Seq[UnifiedEvent]]

  /** Returns a textual representation of events for regex matching */
  def regexpableForm(): String

  /** Keeps in only attributes that are provided */
  def project(attrNames: Set[String]): UnifiedEvent

  /** Copy everything but provide a new activity name */
  def copy(activity: String): UnifiedEvent

  /** Copy everything but add/modify an attribute */
  def copy(attrName: String, attrValue: Any): UnifiedEvent

  /** Copy everything but provide a new timestamp (would not change nested aggregated events!) */
  def copy(newTimestamp: Long): UnifiedEvent

  /** Returns a CSV string without optional attributes */
  protected[log] def toCsvWithoutAttributes(timestampConverter: Long => String): String

  /** Returns a CSV string with optional attributes */
  protected[log] def toCsv(timestampConverter: Long => String, attributeNames: String*): String
}

/** Events factory */
object UnifiedEvent {

  /** Creates an event without optional attributes */
  def apply(
             timestamp: Long,
             activity: String
           ): UnifiedEvent =
    new UnifiedEventImpl(timestamp, activity, SortedMap[String, Any](), None)


  /**
    * Creates an event with optional attributes
    *
    * @param timestamp        timestamp (ms)
    * @param activity         activity name
    * @param attrs            optional attributes
    * @param aggregatedEvents aggregated events if any
    * @return a new event
    */
  def apply(
             timestamp: Long,
             activity: String,
             attrs: SortedMap[String, Any],
             aggregatedEvents: Option[Seq[UnifiedEvent]]
           ): UnifiedEvent =
    new UnifiedEventImpl(timestamp, activity, attrs, aggregatedEvents)
}


private[common] object UnifiedEventHelper {
  val LeftEventSep = "<"
  val RightEventSep = ">"
  val ActivitySep = "@"
  val TimestampSep = "#"
  val LeftAttributeSep = "&"
  val RightAttributeSep = "%"
}


