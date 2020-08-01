package org.processmining.scala.log.common.filtering.expressions.events.variables

import org.processmining.scala.log.common.filtering.expressions.events.variables.EventVarOperator.EventVarOperator
import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.common.UnifiedEventLog.UnifiedTrace

/**
  * Supported operators on event expressions
  */
private object EventVarOperator extends Enumeration {
  type EventVarOperator = Value
  val Start,
  DFR,
  EFR,
  Last
  = Value
}

/**
  * * Represents information to be compiled into an expression for event/trace- level filtering
  *
  * @param exp       sequence of single event expressions and operators against them
  * @param predicate where- UDF
  */
private class EventVarExpression(val exp: Seq[(AbstractEventVar, EventVarOperator)],
                         val predicate: Option[(Map[String, Any], Map[String, Long], Map[String, String]) => Boolean]) extends AbstractEventVarExpression {

  override def >>(that: AbstractEventVar): AbstractEventVarExpression = new EventVarExpression(exp :+ (that, EventVarOperator.DFR), None)

  override def >|>(that: AbstractEventVar): AbstractEventVarExpression = new EventVarExpression(exp :+ (that, EventVarOperator.EFR), None)

  override def where(p: (Map[String, Any], Map[String, Long], Map[String, String]) => Boolean): AbstractVarPredicate =
    new EventVarExpression(this.exp, Some(p))

  override def contains(trace: UnifiedTrace): Boolean = contains(trace._2, exp, (Map(), Map(), Map()))

  private def addVars(vars: (Map[String, Any], Map[String, Long], Map[String, String]),
                      e: UnifiedEvent,
                      eventVars: Map[String, String],
                      timestampName: Option[String],
                      activityName: Option[String]
                     ): (Map[String, Any], Map[String, Long], Map[String, String]) = {
    //val commonAttrs = e.attrs.schema.filter(eventVars.keySet)
    val x: Map[String, Any] = e
      .intersection(eventVars.keySet)
      .map(x => eventVars(x._1) -> x._2)

    (vars._1 ++ x,
      if (!timestampName.isDefined) vars._2 else vars._2 + (timestampName.get -> e.timestamp),
      if (!activityName.isDefined) vars._3 else vars._3 + (activityName.get -> e.activity))
  }

  private def contains(events: List[UnifiedEvent], exp: Seq[(AbstractEventVar, EventVarOperator)], vars: (Map[String, Any], Map[String, Long], Map[String, String])): Boolean = {
    if (exp.isEmpty) {
      if (!predicate.isDefined) true else predicate.get(vars._1, vars._2, vars._3)
    }
    else events match {
      case Nil => false
      case evHead :: evTail => {
        val ex = exp.head
        ex._2 match {
          case op if op == EventVarOperator.Start || op == EventVarOperator.EFR => {
            events
              .zipWithIndex
              .exists(e => ex._1.activityPattern.matcher(e._1.activity).matches()
                && contains(events.drop(e._2 + 1), exp.tail, addVars(vars, e._1, ex._1.vars, ex._1.timestampVar, ex._1.activityVar)))
          }
          case EventVarOperator.DFR => {
            ex._1.activityPattern.matcher(evHead.activity).matches() &&
              contains(evTail, exp.tail, addVars(vars, evHead, ex._1.vars, ex._1.timestampVar, ex._1.activityVar))
          }

        }

      }
    }
  }
}

/** Factory for EventVarExpression */
object EventVarExpression {
  /**
    * Factory to create the first object in an expression
    *
    * @param first  first single evant expression
    * @param second second single evant expression
    * @param op     operator
    * @return a new object
    */
  def apply(first: AbstractEventVar, second: AbstractEventVar, op: EventVarOperator): AbstractEventVarExpression =
    new EventVarExpression(Seq((first, EventVarOperator.Start)) :+ (second, op), None)
}