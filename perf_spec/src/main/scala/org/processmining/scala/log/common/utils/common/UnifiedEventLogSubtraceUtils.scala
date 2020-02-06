package org.processmining.scala.log.common.utils.common

case class SubtraceEntry[T](middle: List[T], left: Option[T], right: Option[T], isFinalized: Boolean) {
  def isEmpty() = middle.isEmpty && left.isEmpty && right.isEmpty

  def toList(): List[T] = (left :: middle.map(Some(_)) ::: List(right)).filter(_.isDefined).map(_.get)
}

object SubtraceEntry {
  def apply[T](): SubtraceEntry[T] = new SubtraceEntry[T](List(), None, None, false)
}

object UnifiedEventLogSubtraceUtils {

  /**
    * 'map' for subsequences (including empty ones) within a given sequence
    * see unit tests for better understanding
    * @param seq initial sequence
    * @param empty  an empty object of T (serves as an end-of-sequence internally)
    * @param p1 subsequence left-border predicate (border cases are always included)
    * @param p2 subsequence right-border predicate (border cases are always included)
    * @param f function to transform a subsequence within the borders (borders cannot be changed using this function)
    * @tparam T type of a sequence element
    * @return resulting sequence
    */
  def mapSubtrace[T](seq: List[T], empty: T)(p1: T => Boolean, p2: T => Boolean, f: (List[T], Option[T], Option[T]) => List[T]): List[T] = {
    if (seq.isEmpty) throw new IllegalArgumentException("Empty sequences are not allowed")
    val ret = (seq ::: List(empty))
      .foldLeft((SubtraceEntry[T](), List[SubtraceEntry[T]]()))((z: (SubtraceEntry[T], List[SubtraceEntry[T]]), e: T) => {

        val acc = z._1
        val newAcc = if (acc.isEmpty()) {
          if (p1(e)) SubtraceEntry(List(), Some(e), None, false) else SubtraceEntry(List(e), None, None, false)
        } else {
          if (!p2(e) && e != empty) acc.copy(middle = e :: acc.middle) else {
            (if (e != empty) acc.copy(right = Some(e)) else acc).copy(isFinalized = true)
          }
        }
        if (newAcc.isFinalized) {
          val eventsToProcess = newAcc.middle.reverse
          val processedMiddle = if (newAcc.middle.isEmpty && e == empty) eventsToProcess else f(eventsToProcess, newAcc.left, newAcc.right)
          (SubtraceEntry[T](List(), Some(e), None, false), newAcc.copy(middle = processedMiddle) :: z._2)
        } else (newAcc, z._2)

      })._2.reverse

    ret.foldLeft(List[T]())((z: List[T], entry: SubtraceEntry[T]) => {
      if (!entry.isFinalized) throw new IllegalStateException("Entry must be finalized")
      val list = entry.toList()
      if (z.isEmpty) list else if (z.last != list.head) z ::: list else z ::: list.tail
    })
  }
}

