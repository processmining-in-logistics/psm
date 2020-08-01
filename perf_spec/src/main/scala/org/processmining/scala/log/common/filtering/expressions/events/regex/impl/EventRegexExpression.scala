package org.processmining.scala.log.common.filtering.expressions.events.regex.impl

import org.processmining.scala.log.common.filtering.expressions.events.regex.EventExpression


/**
  * Defines an interface for regex-based filtering expressions
  */
trait EventRegexExpression extends EventExpression {

  /**
    * Defines directly followed events
    * After translation nothing is inserted between this and that : xxxyyy
    *
    * @param that 'this' is directly followed by 'that'
    * @return a new expression object
    */
  def >>(that: EventRegexExpression): EventRegexExpression

  /**
    * Defines eventually followed events
    * After translation .*? is inserted between this and that: xxx.*yyy
    *
    * @param that 'this' is eventually followed by that
    * @return a new expression object
    */
  def >->(that: EventRegexExpression): EventRegexExpression

  /**
    * Pattern should be repeated exactly n times: (xxx){n}
    *
    * @param n number of repetitions
    * @return a new expression object
    */
  def repititions(n: Int): EventRegexExpression

  /**
    * Pattern should be repeated from n until m times (as in regex): (xxx){n,m}
    *
    * @param nm defines a range of repetitions
    * @return a new expression object
    */
  def repititions(nm: (Int, Int)): EventRegexExpression

  /**
    * The expression is marked as an optional one: (xxx)?
    *
    * @return a new expression object
    */
  def zeroOrOne(): EventRegexExpression

  /**
    * The expression is marked as zero or more: (xxx)*
    *
    * @return a new expression object
    */
  def zeroOrMore(): EventRegexExpression

  /**
    * The expression is marked as one or more: (xxx)+
    *
    * @return a new expression object
    */
  def oneOrMore(): EventRegexExpression

  /**
    * Union operator: xxx|yyy
    * No parentheses are inserted!
    *
    * @param that
    * @return a new expression object
    */
  def |(that: EventRegexExpression): EventRegexExpression

  /**
    * Adds parentheses around expression: (xxx)
    *
    * @param expression
    * @return a new expression object
    */
  def parentheses(expression: EventRegexExpression): EventRegexExpression



  /**
    * Use only for debugging
    */
  override def toString: String = translate()
}
