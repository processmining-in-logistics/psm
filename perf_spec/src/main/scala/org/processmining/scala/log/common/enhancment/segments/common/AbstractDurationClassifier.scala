package org.processmining.scala.log.common.enhancment.segments.common

trait AbstractDurationClassifier extends Serializable {
  def classify(duration: Long, q2: Double, median: Double, q4: Double): Int

  def sparkSqlExpression(attrNameDuration: String, attrNameClazz: String): String

  def legend: String

}

class FasterNormal23VerySlowDurationClassifier extends AbstractDurationClassifier {
  def classify(duration: Long, q2: Double, median: Double, q4: Double): Int =
    duration match {
      case n if n < median * 0.5 => 0
      case n if n < median * 1.5 => 1
      case n if n < median * 2 => 2
      case n if n < median * 3 => 3
      case _ => 4
    }

  override def sparkSqlExpression(attrNameDuration: String, attrNameClazz: String) =
    s"""SELECT segments.*,
       | CASE WHEN segments.${attrNameDuration} < stat.median * 0.5 THEN 0
       | ELSE CASE WHEN segments.${attrNameDuration} < stat.median * 1.5 THEN 1
       | ELSE CASE WHEN segments.${attrNameDuration} < stat.median * 2 THEN 2
       | ELSE CASE WHEN segments.${attrNameDuration} < stat.median * 3 THEN 3
       | ELSE 4 END END END END
       | AS ${attrNameClazz}
       | FROM segments
       | JOIN stat ON stat.key == segments.key""".stripMargin

  override val legend = "DURATION%Faster%Normal%2 times slower%3 times slower%Very slow"
}

class NormalSlowVerySlowDurationClassifier extends AbstractDurationClassifier {
  def classify(duration: Long, q2: Double, median: Double, q4: Double): Int =
    duration match {
      case n if n < median * 1.25 => 0
      case n if n < median * 3 => 1
      case _ => 2
    }

  override def sparkSqlExpression(attrNameDuration: String, attrNameClazz: String) =
    s"""SELECT segments.*,
       | CASE WHEN segments.${attrNameDuration} < stat.median * 1.25 THEN 0
       | ELSE CASE WHEN segments.${attrNameDuration} < stat.median * 3 THEN 1
       | ELSE 2 END END
       | AS ${attrNameClazz}
       | FROM segments
       | JOIN stat ON stat.key == segments.key""".stripMargin

  override val legend = "DURATION%Normal%3 times slower%Very slow"
}


class Q4DurationClassifier extends AbstractDurationClassifier {
  override def classify(duration: Long, q2: Double, median: Double, q4: Double): Int =
    duration match {
      case n if n < q2 => 0
      case n if n < median => 1
      case n if n < q4 => 2
      case _ => 3
    }

  override def sparkSqlExpression(attrNameDuration: String, attrNameClazz: String) =
    s"""SELECT segments.*,
       | CASE WHEN segments.${attrNameDuration} <= stat.q2 THEN 0
       | ELSE CASE WHEN segments.${attrNameDuration} <= stat.median THEN 1
       | ELSE CASE WHEN segments.${attrNameDuration} <= stat.q4 THEN 2
       | ELSE 3 END END END
       | AS ${attrNameClazz}
       | FROM segments
       | JOIN stat ON stat.key == segments.key""".stripMargin

  override val legend = "DURATION%Q1%Q2%Q3%Q4"
}

class Q3DurationClassifier extends AbstractDurationClassifier {
  override def classify(duration: Long, q2: Double, median: Double, q4: Double): Int =
    duration match {
      case n if n < q2 => 0
      case n if n < q4 => 1
      case _ => 2
    }

  override def sparkSqlExpression(attrNameDuration: String, attrNameClazz: String) =
    s"""SELECT segments.*,
       | CASE WHEN segments.${attrNameDuration} <= stat.q2 THEN 0
       | ELSE CASE WHEN segments.${attrNameDuration} <= stat.q4 THEN 1
       | ELSE 2 END END
       | AS ${attrNameClazz}
       | FROM segments
       | JOIN stat ON stat.key == segments.key""".stripMargin

  override val legend = "DURATION%Q1%Q2-3%Q4"
}

