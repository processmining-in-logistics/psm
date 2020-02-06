package org.processmining.scala.log.common.filtering.expressions.events.variables

/** Defines the interface for expressions for event/trace- level filtering */
trait AbstractEventVarExpression extends AbstractVarPredicate {

  /** Defines eventually followed relations */
  def >|>(that: AbstractEventVar): AbstractEventVarExpression

  /** Defines directly followed relations */
  def >>(that: AbstractEventVar): AbstractEventVarExpression

  /**
    * Define a UDF as a predicate (like WHERE clause)
    *
    * @param predicate UDF
    * @return predicate expression
    */
  def where(predicate: ((Map[String, Any], Map[String, Long], Map[String, String]) => Boolean)): AbstractVarPredicate


}
