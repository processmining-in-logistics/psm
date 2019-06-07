package org.processmining.scala.sim.conveyors.api

trait Tsu extends Serializable {
  def id: String

  def src: String

  def dst: String

  def attribute(name: String): Option[Any]

  def updateAttribute(name: String, value: Any): Tsu
}



trait Equipment extends Serializable {
  def name: String

  def address: String
}

trait AbstractEquipment extends Equipment {

  def peek(): Tsu

  def shouldOffload(): Boolean

  def canLoad(): Boolean

  def move(stepMs: Long): (Option[Tsu], AbstractEquipment)

  def adjustTime(stepMs: Long): AbstractEquipment

  def put(bag: Tsu): AbstractEquipment


}
