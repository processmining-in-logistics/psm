package org.processmining.scala.log.common.filtering.expressions.events.variables

import java.util.regex.Pattern

/**
  * Defines methods for Regex&UDF based filtering expressions
  * Current implementation is very limited
  *
  * @param activityPattern regex pattern to match activity name
  * @param vars            maps attributes to variables
  * @param timestampVar    maps timestamp to a variable
  * @param activityVar     maps activity name to a variable
  */
abstract class AbstractEventVar(val activityPattern: Pattern,
                                val vars: Map[String, String],
                                val timestampVar: Option[String],
                                val activityVar: Option[String]) extends Serializable {

  /**
    * Defines directly followed events
    *
    * @param that 'this' is directly followed by 'that'
    * @return a new expression object
    */
  def >>(that: AbstractEventVar): AbstractEventVarExpression

  /**
    * Defines eventually followed events
    *
    * @param that 'this' is eventually followed by that
    * @return a new expression object
    */
  def >|>(that: AbstractEventVar): AbstractEventVarExpression

}
