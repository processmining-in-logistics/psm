package org.processmining.scala.viewers.spectrum2.pqr

import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}

object PlaceTransitionSerializer extends Serializer[PlaceTransition] {
  override def write(kryo: Kryo, output: Output, pt: PlaceTransition): Unit = {
    output.writeBoolean(pt.isTransition)
    output.writeInt(pt.qpr)
    output.writeString(pt.name)
    output.writeBoolean(pt.isSorter)
    output.writeBoolean(pt.isIncoming)
    output.writeInt(pt.button)
  }

  override def read(kryo: Kryo, input: Input, t: Class[PlaceTransition]): PlaceTransition =
    PlaceTransition(input.readBoolean(), input.readInt(), input.readString(), input.readBoolean(), input.readBoolean(), input.readInt())

  override def isImmutable: Boolean = true
}
