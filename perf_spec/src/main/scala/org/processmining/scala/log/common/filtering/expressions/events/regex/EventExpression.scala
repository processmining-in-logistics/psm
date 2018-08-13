package org.processmining.scala.log.common.filtering.expressions.events.regex


/**
  * Defines an interface for regex-based filtering expressions
  */
trait EventExpression extends Serializable {
  /**
    * Translates the expression object into a regular expression string
    *
    * @return translated regular expression string
    */
  def translate(): String
}

