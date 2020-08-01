package org.processmining.scala.log.common.enhancment.segments.common

/**
  * Base class for segment classifiers
  */
abstract class AbstractDurationClassifier extends Serializable {

  /**
    * segment classifier
    *
    * @param duration           segment duration (ms)
    * @param q2                 second quartile (duration in ms)
    * @param median             third quartile (duration in ms)
    * @param q4                 fourth quartile (duration in ms)
    * @param caseId             case ID
    * @param timestamp          segment start
    * @param segmentName        segment name
    * @param medianAbsDeviation median abs. deviation
    * @return zero-based class value
    */
  def classify(duration: Long, q2: Double, median: Double, q4: Double, caseId: String, timestamp: Long, segmentName: String, medianAbsDeviation: Double): Int

  /**
    * Required only for some Spark-based implementations
    * Implement it as 'not implemented yet'
    *
    * @param attrNameDuration
    * @param attrNameClazz
    * @return
    */
  def sparkSqlExpression(attrNameDuration: String, attrNameClazz: String): String = ???

  /**
    *
    * @return a string of the legend in format "TITLE%CLASS0_NAME%CLASS1_NAME%CLASSn_NAME"
    */
  def legend: String

  /**
    *
    * @return number of class values
    */
  def classCount: Int

  /**
    * Classifier initialization (if required), e.g., through a UI dialog
    *
    * @return if initialization not cancelled (false means cancelled)
    */
  def initialize(): Boolean = true

}


object AbstractDurationClassifier {
  def apply(className: String): AbstractDurationClassifier =
    Class
      .forName(className)
      .newInstance()
      .asInstanceOf[AbstractDurationClassifier]

  def apply(className: String, arg1: String): AbstractDurationClassifier =
    Class.forName(className)
      .getDeclaredConstructor(classOf[String])
      .newInstance(arg1)
      .asInstanceOf[AbstractDurationClassifier]

}


class FasterNormal23VerySlowDurationClassifier extends AbstractDurationClassifier {
  def classify(duration: Long, q2: Double, median: Double, q4: Double, caseId: String, timestamp: Long, segmentName: String, medianAbsDeviation: Double): Int =
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

  override val classCount: Int = 5

  override def toString: String = "Median-based duration classifier"
}


class Normal12VerySlowDurationClassifier extends AbstractDurationClassifier {
  def classify(duration: Long, q2: Double, median: Double, q4: Double, caseId: String, timestamp: Long, segmentName: String, medianAbsDeviation: Double): Int =
    duration match {
      case n if n < median * 1.5 => 0
      case n if n < median * 2 => 1
      case n if n < median * 3 => 2
      case _ => 3
    }

  override def sparkSqlExpression(attrNameDuration: String, attrNameClazz: String) = ???

  override val legend = "DURATION%Normal%2 times slower%3 times slower%Very slow"

  override val classCount: Int = 4
}


class NormalSlowVerySlowDurationClassifier extends AbstractDurationClassifier {
  def classify(duration: Long, q2: Double, median: Double, q4: Double, caseId: String, timestamp: Long, segmentName: String, medianAbsDeviation: Double): Int =
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

  override val classCount: Int = 3
}


class Q4DurationClassifier extends AbstractDurationClassifier {
  override def classify(duration: Long, q2: Double, median: Double, q4: Double, caseId: String, timestamp: Long, segmentName: String, medianAbsDeviation: Double): Int =
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

  override val classCount: Int = 4

  override def toString: String = "Quartile-based duration classifier"
}

class Q3DurationClassifier extends AbstractDurationClassifier {
  override def classify(duration: Long, q2: Double, median: Double, q4: Double, caseId: String, timestamp: Long, segmentName: String, medianAbsDeviation: Double): Int =
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

  override val classCount: Int = 3
}

class NormalSlowDurationClassifier extends AbstractDurationClassifier {
  def classify(duration: Long, q2: Double, median: Double, q4: Double, caseId: String, timestamp: Long, segmentName: String, medianAbsDeviation: Double): Int =
    duration match {
      case n if n < median * 3 => 0
      case _ => 1
    }

  override def sparkSqlExpression(attrNameDuration: String, attrNameClazz: String) = ???

  override val legend = "DURATION%Normal%Slow"

  override val classCount: Int = 2
}

class DummyDurationClassifier extends AbstractDurationClassifier {
  def classify(duration: Long, q2: Double, median: Double, q4: Double, caseId: String, timestamp: Long, segmentName: String, medianAbsDeviation: Double): Int = 0


  override def sparkSqlExpression(attrNameDuration: String, attrNameClazz: String) = ???

  override val legend = "DURATION%Normal"

  override val classCount: Int = 1

  override def toString: String = "Single-class duration classifier"
}

case class SegmentDirClassifier(override val legend: String, override val classCount: Int, name: String) extends AbstractDurationClassifier {
  def classify(duration: Long, q2: Double, median: Double, q4: Double, caseId: String, timestamp: Long, segmentName: String, medianAbsDeviation: Double): Int = ???

  override def toString: String = name
}