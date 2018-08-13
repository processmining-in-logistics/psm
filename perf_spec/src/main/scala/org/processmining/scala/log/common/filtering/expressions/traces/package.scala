package org.processmining.scala.log.common.filtering.expressions

import java.util.regex.Pattern

import org.processmining.scala.log.common.filtering.expressions.events.regex.{EventExpression}

package object traces {
  implicit def eventExpression2Pattern(eventExpression: EventExpression): Pattern =
    Pattern.compile(eventExpression.translate())

}
