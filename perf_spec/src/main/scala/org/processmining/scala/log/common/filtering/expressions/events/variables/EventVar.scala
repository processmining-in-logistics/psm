package org.processmining.scala.log.common.filtering.expressions.events.variables

import java.util.regex.Pattern

/**
  * Represents information to be compiled into an expression for matching of a single event for event/trace- level filtering
  * Objects of this class should be created via the factory of the companion object
  * See documentations for base classed for override methods
  * @param activityPattern regex pattern to match activity name
  * @param vars            maps attributes to variables
  * @param timestampVar    maps timestamp to a variable
  * @param activityVar     maps activity name to a variable
  */
case class EventVar(override val activityPattern: Pattern,
                    override val vars: Map[String, String],
                    override val timestampVar: Option[String],
                    override val activityVar: Option[String]) extends AbstractEventVar(activityPattern, vars, timestampVar, activityVar) {

  override def >>(that: AbstractEventVar): AbstractEventVarExpression =
    EventVarExpression(this, that, EventVarOperator.DFR)

  override def >|>(that: AbstractEventVar): AbstractEventVarExpression =
    EventVarExpression(this, that, EventVarOperator.EFR)

  /**
    * Defines a variable for an attribute
    * @param varName variable identifier
    * @param attr attribute description
    * @return new object with modified map of attribute variables
    */
  def defineVar(varName: String, attr: String): EventVar = copy(vars = vars + (attr -> varName))

  /**
    * Defines a variable for the timestamp
    * @param varName variable identifier
    * @return new object with modified description of the timestamp variable
    */
  def defineTimestamp(varName: String): EventVar = copy(timestampVar = Some(varName))

  /**
    * Defines a variable for the activity name
    * @param varName variable identifier
    * @return new object with modified description of the activity name variable
    */
  def defineActivity(varName: String): EventVar = copy(activityVar = Some(varName))
}

/** Factory for class EventVar*/
object EventVar {
  /**
    * Create a new object with a provided expression for the activity name
    * @param activityPattern an expression for the activity name
    * @return a new object
    */
  def apply(activityPattern: String): EventVar = new EventVar(Pattern.compile(activityPattern), Map(), None, None)
}