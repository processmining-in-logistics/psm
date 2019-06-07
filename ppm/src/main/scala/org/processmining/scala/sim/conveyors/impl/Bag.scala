package org.processmining.scala.sim.conveyors.impl

import org.processmining.scala.sim.conveyors.api.Tsu

case class Bag(override val id: String, override val src: String, override val dst: String, attributes: Map[String, Any]) extends Tsu {

  override def toString: String = s"'$id'('$src' -> '$dst') ${attributes.map(_.toString()).mkString(";")}"

  override def attribute(name: String): Option[Any] = attributes.get(name)

  override def updateAttribute(name: String, value: Any): Tsu =
    copy(attributes = attributes + (name -> value))
}

object Bag {
  def apply(id: String, src: String, dst: String): Bag = new Bag(id, src, dst, Map())
}
