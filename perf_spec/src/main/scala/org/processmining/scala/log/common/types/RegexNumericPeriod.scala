package org.processmining.scala.log.common.types

/** Creates regex for numbers and intervals of numbers */
trait AbstractRegexNumericPeriod  extends Serializable{

  /**
    * Creates a regex for a range
    * @param from from (inclusive)
    * @param to to (inclusive)
    * @return regex
    */
  def apply(from: Long, to: Long): String

  /** Creates a regex for a number */
  def apply(exactTime: Long): String

}

/**
  *
  * @param width required width of numbers (for Longs cannot be more than 19)
  */
private [log] class RegexNumericPeriod(width: Int) extends AbstractRegexNumericPeriod {


  override def apply(from: Long, to: Long): String = {
    if (from >= to) {
      throw new IllegalArgumentException(s"from must be greater than to: $from <= $to")
    }
    val format: String = s"%0${width}d"
    val fromString = from.formatted(format)
    val toString = to.formatted(format)

    if (fromString.length != toString.length) {
      throw new IllegalArgumentException(s"Lengths are not equal: ${fromString.length} != ${toString.length}")
    }
    if (fromString.length != width) {
      throw new IllegalArgumentException(s"Lengths are too long $width != ${fromString.length}")
    }
    process(fromString.toList, toString.toList, width)
  }


  override def apply(exactTime: Long): String = {

    val format: String = s"%0${width}d"
    val exactTimeString = exactTime.formatted(format)

    if (exactTimeString.length != width) {
      throw new IllegalArgumentException(s"Length is too long $width != ${exactTimeString.length}")
    }
    exactTimeString
  }

  private def process(from: List[Char], to: List[Char], currentWidth: Int): String = {
    (from, to) match {
      case (Nil, Nil) => ""
      case (x :: xTail, y :: yTail) =>
        if (x == y) x + process(xTail, yTail, currentWidth -1)
        else s"($x${processLeft(xTail, currentWidth -1)}|$y${processRight(yTail, currentWidth-1)}${if (y - x > 1) s"|[${(x + 1).toChar}-${(y - 1).toChar}]\\d*" else ""})"
      case _ => throw new IllegalStateException()
    }
  }

  private def processLeft(from: List[Char], currentWidth: Int): String = {
    from match {
      case Nil => ""
      case x => {
        val x1 = x.mkString("").toLong
        val x2 = (Math.pow(10, currentWidth) - 1).toLong
        val format: String = s"%0${currentWidth}d"
        s"(${process(x1.formatted(format).toList, x2.formatted(format).toList, currentWidth)})"
      }

    }
  }

  private def processRight(from: List[Char], currentWidth: Int): String = {
    from match {
      case Nil => ""
      case x => {
        val x1 = 0
        val x2 = x.mkString("").toLong
        val format: String = s"%0${currentWidth}d"
        s"(${process(x1.formatted(format).toList, x2.formatted(format).toList, currentWidth)})"
      }
    }
  }
}


object RegexNumericPeriod{
  val ByteConverter = new RegexNumericPeriod(Byte.MaxValue.toString.length)
  val IntegerConverter = new RegexNumericPeriod(Int.MaxValue.toString.length)
  val LongConverter = new RegexNumericPeriod(Long.MaxValue.toString.length)
  val ByteIntLongConverters = (ByteConverter, IntegerConverter, LongConverter)

}