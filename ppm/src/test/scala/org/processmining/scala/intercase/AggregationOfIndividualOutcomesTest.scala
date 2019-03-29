package org.processmining.scala.intercase

import org.scalatest.FunSuite

class AggregationOfIndividualOutcomesTest extends FunSuite {

  val timestamps = List[Long](5, 7, 14, 24, 25, 26, 52, 55)
  val rt = List[Double](42, 45, 23, 63, 74, 85, 96, 97)
    .zip(timestamps)
    .map(x => x._1 - x._2)

  val binning1 = List(0, 0, 0, 0, 2, 0, 1, 1, 1, 2, 0, 0)
  val binning2 = List(0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0)
  //val binning2 = List(0, 0, 0, 1, 1, 0.5, 1, 1, 1.5, 1, 0, 0)

  test("call_bin_count_1") {
    val obj = new AggregationOfIndividualOutcomes(timestamps, rt, 1, 0, 10, 1, 12, 4).call()
    assert(obj == binning1)
  }

  test("call_bin_count_2") {
    val obj = new AggregationOfIndividualOutcomes(timestamps, rt, 1, 0, 10, 2, 12, 4).call()
    assert(obj == binning2)
  }
}
