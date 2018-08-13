package org.processmining.scala.log.common.types

/** Represents IDs */
trait Id {
  def id: String
}

/** Represents timestamps (ms) */
trait Timestamp {
  def timestamp: Long
}

/** Represents activity names */
trait Activity {
  val activity: String
}

/** Represents classes */
trait Clazz {
  val clazz: Int
}
