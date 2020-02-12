package org.processmining.scala.viewers.spectrum2.model

import org.processmining.scala.viewers.spectrum2.view.SortingOrderEntry

import scala.util.Random


object Separator {
  val S = ":"
}

object A {
  val IN_1_0 = "IN_1_0"
  val IN_2_0 = "IN_2_0"
  val IN_3_0 = "IN_3_0"
  val IN_4_0 = "IN_4_0"
  val IN_5_0 = "IN_5_0"
  val IN_6_0 = "IN_6_0"
  val IN_7_0 = "IN_7_0"

  val IN_1_3_TO_x = s"IN_1_3${Separator.S}x"
  val IN_2_3_TO_x = s"IN_2_3${Separator.S}x"
  val IN_3_3_TO_x = s"IN_3_3${Separator.S}x"
  val IN_4_3_TO_x = s"IN_4_3${Separator.S}x"
  val IN_5_3_TO_x = s"IN_5_3${Separator.S}x"
  val IN_6_3_TO_x = s"IN_6_3${Separator.S}x"
  val IN_7_3_TO_x = s"IN_7_3${Separator.S}y"

  val z_TO_x = s"z${Separator.S}x"
  val y_TO_S1_0 = s"y${Separator.S}S1_0"
  val y_TO_S2_0 = s"y${Separator.S}S2_0"
  val y_TO_S3_0 = s"y${Separator.S}S3_0"
  val y_TO_S4_0 = s"y${Separator.S}S4_0"
  val y_TO_S5_0 = s"y${Separator.S}S5_0"
  val y_TO_S6_0 = s"y${Separator.S}S6_0"
  val y_TO_S7_0 = s"y${Separator.S}S7_0"
}

object FakeDataSource {

  val FirstLevel = "1. Observed PS"
  val Levels = Vector(FirstLevel, "2. Process", "3. Resource", "4. Everything")

  def sortingOrder(): Vector[SortingOrderEntry] = {

    val sorter = Vector(

      SegmentName(A.z_TO_x, A.IN_1_3_TO_x),
      SegmentName(A.IN_1_3_TO_x, A.IN_2_3_TO_x),
      SegmentName(A.IN_2_3_TO_x, A.IN_3_3_TO_x),
      SegmentName(A.IN_3_3_TO_x, A.IN_4_3_TO_x),
      SegmentName(A.IN_4_3_TO_x, A.IN_5_3_TO_x),
      SegmentName(A.IN_5_3_TO_x, A.IN_6_3_TO_x),
      SegmentName(A.IN_6_3_TO_x, A.IN_7_3_TO_x),
      SegmentName(A.IN_7_3_TO_x, A.y_TO_S1_0)
//      SegmentName(A.y_TO_S1_0, A.y_TO_S2_0),
//      SegmentName(A.y_TO_S2_0, A.y_TO_S3_0),
//      SegmentName(A.y_TO_S3_0, A.y_TO_S4_0),
//      SegmentName(A.y_TO_S4_0, A.y_TO_S5_0),
//      SegmentName(A.y_TO_S5_0, A.y_TO_S6_0),
//      SegmentName(A.y_TO_S6_0, A.y_TO_S7_0)
    )

    Vector(
      SortingOrderEntry("", sorter, Map(
        SegmentName(A.z_TO_x, A.IN_1_3_TO_x) -> SegmentName(A.IN_1_0, A.IN_1_3_TO_x),
        SegmentName(A.IN_1_3_TO_x, A.IN_2_3_TO_x) -> SegmentName(A.IN_2_0, A.IN_2_3_TO_x),
        SegmentName(A.IN_2_3_TO_x, A.IN_3_3_TO_x) -> SegmentName(A.IN_3_0, A.IN_3_3_TO_x),
        SegmentName(A.IN_3_3_TO_x, A.IN_4_3_TO_x) -> SegmentName(A.IN_4_0, A.IN_4_3_TO_x),
        SegmentName(A.IN_4_3_TO_x, A.IN_5_3_TO_x) -> SegmentName(A.IN_5_0, A.IN_5_3_TO_x),
        SegmentName(A.IN_5_3_TO_x, A.IN_6_3_TO_x) -> SegmentName(A.IN_6_0, A.IN_6_3_TO_x),
        SegmentName(A.IN_6_3_TO_x, A.IN_7_3_TO_x) -> SegmentName(A.IN_7_0, A.IN_7_3_TO_x)
      ))
    )
  }

  val activityToMinSegmentDuration: Map[String, Long] = Map(
    A.z_TO_x -> 9000,
    A.IN_1_3_TO_x -> 2200,
    A.IN_2_3_TO_x -> 2200,
    A.IN_3_3_TO_x -> 2200,
    A.IN_4_3_TO_x -> 56300,
    A.IN_5_3_TO_x -> 56300,
    A.IN_6_3_TO_x -> 11600,
    A.IN_7_3_TO_x -> 52900,
    A.y_TO_S1_0 -> 52900
  )
}