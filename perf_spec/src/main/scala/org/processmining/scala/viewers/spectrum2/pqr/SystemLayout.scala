package org.processmining.scala.viewers.spectrum2.pqr

import java.awt.Color

import org.processmining.scala.viewers.spectrum2.model.{SegmentName, Separator}

case class Activity(name: String)

case class RadialFeeder(activities: List[Activity], connector: Activity, isIncoming: Boolean)

class SystemLayout {

  val sorterActivities = List(
    "IN_1_3-x",
    "IN_2_3-x",
    "IN_3_3-x",
    "IN_4_3-x",
    "IN_5_3-x",
    "IN_6_3-x",
    "x-y",
    "IN_7_3-y",
    "y-S1_0",
    "y-S2_0",
    "y-S3_0",
    "y-S4_0",
    "y-z",
    "S4_7-z",
    "S3_7-z",
    "S2_7-z",
    "S1_7-z",
    "z-OUT_1_0",
    "z-OUT_2_0",
    "z-OUT_3_0",
    "z-OUT_4_0",
    "z-OUT_5_0",
    "z-OUT_6_0",
    "z-x",
  ).map(Activity)


  def inFeederActivityFactory(id: Int) =
    List(s"IN_${id}_0",
      s"IN_${id}_0${Separator.S}IN_${id}_1",
      s"IN_${id}_1${Separator.S}IN_${id}_2",
      s"IN_${id}_2${Separator.S}IN_${id}_3",
    )

  def outFeederActivityFactory(id: Int) =
    List(
      s"OUT_${id}_0${Separator.S}OUT_${id}_1",
      s"OUT_${id}_1-Empty"
    )

  def inFeederFactory(id: Int, connector: String) = {
    val list = inFeederActivityFactory(id)
    RadialFeeder(list.map(Activity), Activity(connector), true)
  }

  def outFeederFactory(id: Int, connector: String) = {
    val list = outFeederActivityFactory(id)
    RadialFeeder(list.map(Activity), Activity(connector), false)
  }


  private val sorterActivitiesStartCompleteZipped = sorterActivities.flatMap(x => List(
    Activity(s"${x.name}${SystemLayout.Sep}${SystemLayout.Start}"),
    Activity(s"${x.name}${SystemLayout.Sep}${SystemLayout.Complete}")
  )).zipWithIndex


  private val sorterActivitiesZipped = sorterActivities.zipWithIndex

  val sorterSegmentsStartComplete =
    sorterActivitiesStartCompleteZipped.map(x => SegmentName(x._1.name, sorterActivitiesStartCompleteZipped((x._2 + 1) % sorterActivitiesStartCompleteZipped.size)._1.name))
  val sorterSegments = sorterActivitiesZipped.map(x => SegmentName(x._1.name, sorterActivitiesZipped((x._2 + 1) % sorterActivitiesZipped.size)._1.name))

  val initialSortingOrder = //List(SegmentName("IN_4_0-IN_4_1", "IN_4_1-IN_4_2"),
//    SegmentName("IN_4_1-IN_4_2", "IN_4_2-IN_4_3"),
//    SegmentName("IN_4_2-IN_4_3", "IN_4_3-x"),
//    SegmentName("IN_4_3-x", "IN_5_3-x")
//
//  )


  sorterSegments


  val feeders = List(
    inFeederFactory(1, "IN_1_3-x"),
    inFeederFactory(2, "IN_2_3-x"),
    inFeederFactory(3, "IN_3_3-x"),
    inFeederFactory(4, "IN_4_3-x"),
    inFeederFactory(5, "IN_5_3-x"),
    inFeederFactory(6, "IN_6_3-x"),
    inFeederFactory(7, "IN_7_3-y"),
    outFeederFactory(1, "z-OUT_1_0"),
    outFeederFactory(2, "z-OUT_2_0"),
    outFeederFactory(3, "z-OUT_3_0"),
    outFeederFactory(4, "z-OUT_4_0"),
    outFeederFactory(5, "z-OUT_5_0"),
    outFeederFactory(6, "z-OUT_6_0"),
  )

  def feederSegments(rf: RadialFeeder) = {
    val act = (if (rf.isIncoming) rf.activities ::: List(rf.connector) else rf.connector :: rf.activities).zipWithIndex
    (0 until act.size - 1) map (i => SegmentName(act(i)._1.name, act(i + 1)._1.name))
  }

  def feederSegmentsStartComplete(rf: RadialFeeder) = {
    val startComplete = rf.activities.flatMap(x => List(
      s"${x.name}${SystemLayout.Sep}${SystemLayout.Start}",
      s"${x.name}${SystemLayout.Sep}${SystemLayout.Complete}"
    ))
    val startCompleteWithConnector = (if (rf.isIncoming) startComplete ::: List(s"${rf.connector.name}${SystemLayout.Sep}${SystemLayout.Complete}")
    else s"${rf.connector.name}${SystemLayout.Sep}${SystemLayout.Complete}" :: startComplete).zipWithIndex
    (0 until startCompleteWithConnector.size - 1) map (i => SegmentName(startCompleteWithConnector(i)._1, startCompleteWithConnector(i + 1)._1))
  }

  def allFeederSegments = feeders.flatMap(feederSegments)

  def allFeederSegmentsStartComplete = feeders.flatMap(feederSegmentsStartComplete)


  val allActivities = sorterActivities ::: feeders.flatMap(_.activities)

  val allRSegments = allActivities.map(x => SegmentName(s"${x.name}${SystemLayout.Sep}${SystemLayout.Complete}", s"${x.name}${SystemLayout.Sep}${SystemLayout.Start}"))


  def psmSegments() =
    sorterSegments ::: allFeederSegments ::: sorterSegmentsStartComplete ::: allFeederSegmentsStartComplete ::: allRSegments :::
      List(SegmentName("y-S1_0", "None"), SegmentName("y-S2_0", "None"), SegmentName("y-S3_0", "None"), SegmentName("y-S4_0", "None"))


  val sorterStepSector = 2 * Math.PI / sorterActivities.size

  var sorterStartRadians = math.Pi

  //First transition than place
  def getAngle(a: Activity, isStart: Boolean, isPlace: Boolean): (Double, Boolean) = {
    val pos = sorterActivities.zipWithIndex.find(_._1.name == a.name).get._2
    val angle = 2 * Math.PI - pos * sorterStepSector + sorterStepSector / 2 - (
      (if (isStart) +sorterStepSector / 2 + sorterStepSector / 16 else (sorterStepSector / 16 + sorterStepSector / 2)) + (if (isPlace) sorterStepSector / 4 else 0)

      )
    (angle + sorterStartRadians, pos % 2 == 0)
  }
}

object SystemLayout {


  val Sep = "^"
  val QSep = ":"

  val PlSep = ":"
  val Start = "start"
  val Complete = "compl"
  val Enqueue = "enq"
  val Dequeue = "deq"

  val pColor = Color.red
  val qColor = Color.blue
  val rColor = new Color(0, 128, 0)

  def apply() = new SystemLayout()

  def renameP(x: String, isStart: Boolean) =
    s"${x}${SystemLayout.Sep}${if (isStart) Start else Complete}"

  def renameQ(surroundings: String => (String, String))(x: String, isStart: Boolean) =
    if (isStart) s"$Dequeue$Sep${surroundings(x)._1}$QSep$x" else s"$Enqueue$Sep$x$QSep${surroundings(x)._2}"

  def renamePl(surroundings: String => (String, String))(x: String, isStart: Boolean) =
    s"$x$Sep$Complete$PlSep${surroundings(x)._2}$Sep$Start"

  def renamePlOutgoing(surroundings: String => (String, String))(x: String, isStart: Boolean) =
    s"${surroundings(x)._1}$Sep$Complete$PlSep$x$Sep$Start"

  def renameQConnector(connector: String)(x: String, isStart: Boolean) =
    if (isStart) s"$Dequeue$Sep$connector$QSep$x" else s"$Enqueue$Sep$x$QSep$connector"

  def renameR(x: String, isStart: Boolean) =
    if (isStart) s"$Start$Sep$x" else s"$Complete$Sep$x"

  def surroundings(activities: List[Activity])(x: String): (String, String) = {
    val pair = activities.zipWithIndex.find(_._1.name == x).get
    (if (pair._2 == 0) activities.last.name else activities(pair._2 - 1).name,
      if (pair._2 == activities.size - 1) activities.head.name else activities(pair._2 + 1).name)
  }
}