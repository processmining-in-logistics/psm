package org.processmining.scala.log.common.filtering.expressions.events.regex.impl


/**
  * Implements many methods of regex-based expressions
  */
trait AbstractEventRegexExpression extends EventRegexExpression {

  override def >>(that: EventRegexExpression): EventRegexExpression =
    new TranslatedEventRegexExpression(s"${translate()}${that.translate()}")

  override def >->(that: EventRegexExpression): EventRegexExpression =
    new TranslatedEventRegexExpression(s"${translate()}.*?${that.translate()}")

  override def |(that: EventRegexExpression): EventRegexExpression =
    new TranslatedEventRegexExpression(s"${translate()}|${that.translate()}")

  override def repititions(n: Int): EventRegexExpression = new TranslatedEventRegexExpression(s"(${translate()}){$n}")

  override def repititions(nm: (Int, Int)): EventRegexExpression = new TranslatedEventRegexExpression(s"(${translate()}){${nm._1},${nm._2}}")

  override def zeroOrOne(): EventRegexExpression = new TranslatedEventRegexExpression(s"(${translate()})?")

  override def zeroOrMore(): EventRegexExpression = new TranslatedEventRegexExpression(s"(${translate()})*")

  override def oneOrMore(): EventRegexExpression = new TranslatedEventRegexExpression(s"(${translate()})+")

  override def parentheses(expression: EventRegexExpression): EventRegexExpression= new TranslatedEventRegexExpression(s"(${translate()})")
}
