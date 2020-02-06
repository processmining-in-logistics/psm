package org.processmining.scala.viewers.spectrum2.experiments

import org.processmining.scala.log.common.unified.event.UnifiedEvent
import org.processmining.scala.log.common.unified.log.parallel.UnifiedEventLog
import org.processmining.scala.viewers.spectrum2.model.{A, SegmentName}

//mutable
class Node(val activity: String, val ttp: Long, var parent: Option[Node], val children: scala.collection.mutable.Set[Node]) {
  def addParent(newParent: Node) = {
    parent = Some(newParent)
    newParent.children.add(this)
  }
}

object Node {
  def apply(activity: String, ttp: Long): Node = new Node(activity, ttp, None, scala.collection.mutable.Set())
}

class SorterTree(val root: Node, val leaves: Map[String, Node]) {

  private def toString(n: Node): List[String] = {
    val str = s"${n.activity}:${if (n.parent.isDefined) n.parent.get.activity else ""}=${n.ttp}"
    str :: n.children.flatMap(toString).toList
  }

  override def toString(): String = toString(root).mkString("\n")
}

class PreSorter50TreeBuilder(log: UnifiedEventLog) {

  val md = log.traces
    .flatMap(t => t._2.foldLeft((None: Option[UnifiedEvent], List[(SegmentName, Long)]()))((p: (Option[UnifiedEvent], List[(SegmentName, Long)]), e: UnifiedEvent) => {
      if (p._1.isEmpty) (Some(e), p._2)
      else
        (Some(e),
          (SegmentName(p._1.get.activity, e.activity), e.timestamp - p._1.get.timestamp) :: p._2)

    })._2
    ).groupBy(_._1).map(x => (x._1, x._2.map(_._2).min))

  def apply(): SorterTree = {





    val sz_TO_x = Node(A.z_TO_x, 9000)
    val sIN_1_0 = Node(A.IN_1_0, 66700)
    val sIN_2_0 = Node(A.IN_2_0, 234000)
    val sIN_3_0 = Node(A.IN_3_0, 22300)
    val sIN_4_0 = Node(A.IN_4_0, 27600)
    val sIN_5_0 = Node(A.IN_5_0, 33400)
    val sIN_6_0 = Node(A.IN_6_0, 27200)
    val sIN_7_0 = Node(A.IN_7_0, 21800)
    val nIN_1_3_TO_x = Node(A.IN_1_3_TO_x, 2200)
    val nIN_2_3_TO_x = Node(A.IN_2_3_TO_x, 3000)
    val nIN_3_3_TO_x = Node(A.IN_3_3_TO_x, 2200)
    val nIN_4_3_TO_x = Node(A.IN_4_3_TO_x, 56300)
    val nIN_5_3_TO_x = Node(A.IN_5_3_TO_x, 7500)
    val nIN_6_3_TO_x = Node(A.IN_6_3_TO_x, 11600)
    val nIN_7_3_TO_x = Node(A.IN_7_3_TO_x, 52900)
    val ny_TO_S1_0 = Node(A.y_TO_S1_0, 1300)
    val ny_TO_S2_0 = Node(A.y_TO_S2_0, 980)
    val ny_TO_S3_0 = Node(A.y_TO_S3_0, 1600)
    val ny_TO_S4_0 = Node(A.y_TO_S4_0, 1000)
    val ny_TO_S5_0 = Node(A.y_TO_S5_0, 980)
    val ny_TO_S6_0 = Node(A.y_TO_S6_0, 1600)
    val ry_TO_S7_0 = Node(A.y_TO_S7_0, 0)

    sz_TO_x.addParent(nIN_1_3_TO_x)
    sIN_1_0.addParent(nIN_1_3_TO_x)
    sIN_2_0.addParent(nIN_2_3_TO_x)
    sIN_3_0.addParent(nIN_3_3_TO_x)
    sIN_4_0.addParent(nIN_4_3_TO_x)
    sIN_5_0.addParent(nIN_5_3_TO_x)
    sIN_6_0.addParent(nIN_6_3_TO_x)
    sIN_7_0.addParent(nIN_7_3_TO_x)
    nIN_1_3_TO_x.addParent(nIN_2_3_TO_x)
    nIN_2_3_TO_x.addParent(nIN_3_3_TO_x)
    nIN_3_3_TO_x.addParent(nIN_4_3_TO_x)
    nIN_4_3_TO_x.addParent(nIN_5_3_TO_x)
    nIN_5_3_TO_x.addParent(nIN_6_3_TO_x)
    nIN_6_3_TO_x.addParent(nIN_7_3_TO_x)
    nIN_7_3_TO_x.addParent(ny_TO_S1_0)
    ny_TO_S1_0.addParent(ny_TO_S2_0)
    ny_TO_S2_0.addParent(ny_TO_S3_0)
    ny_TO_S3_0.addParent(ny_TO_S4_0)
    ny_TO_S4_0.addParent(ny_TO_S5_0)
    ny_TO_S5_0.addParent(ny_TO_S6_0)
    ny_TO_S6_0.addParent(ry_TO_S7_0)
    val tree = new SorterTree(ry_TO_S7_0, List(sz_TO_x, sIN_1_0, sIN_2_0, sIN_3_0, sIN_4_0, sIN_5_0, sIN_6_0, sIN_7_0).map(x => x.activity -> x).toMap)
    println(tree.toString())
    tree

  }


}

object SorterTree {

  def pathToRoot(node: Option[Node]): List[Node] =
    if (node.isEmpty) List() else node.get :: pathToRoot(node.get.parent)

  def getRootAndChildren(node: Option[Node]): List[Node] =
    if (node.isEmpty) List()
    else node.get :: node.get.children.flatMap(x => getRootAndChildren(Some(x))).toList

  def getRootAndChildrenAsMap(node: Option[Node]): Map[String, Node] =
    getRootAndChildren(node).map(x => x.activity -> x).toMap


}