package org.processmining.scala.log.common.filtering.expressions.events.regex.impl

private [events] class TranslatedEventRegexExpression(val expression: String) extends AbstractEventRegexExpression {
  override def translate(): String = expression
}
