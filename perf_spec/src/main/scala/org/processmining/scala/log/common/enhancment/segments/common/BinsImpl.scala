package org.processmining.scala.log.common.enhancment.segments.common

import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}

case class BinsImpl(clazz: Int, countStart: Int, countIntersect: Int, countStop: Int) extends Serializable

object BinsImplSerializer extends Serializer[BinsImpl] {
  override def write(kryo: Kryo, output: Output, s: BinsImpl): Unit = {
    output.writeByte(s.clazz)
    output.writeInt(s.countStart)
    output.writeInt(s.countIntersect)
    output.writeInt(s.countStop)
  }

  override def read(kryo: Kryo, input: Input, t: Class[BinsImpl]): BinsImpl =
    BinsImpl(input.readByte(), input.readInt(), input.readInt(), input.readInt())

  override def isImmutable: Boolean = true

}


case class ListOfBins(s: List[BinsImpl]) extends Serializable

object ListOfBinsSerializer extends Serializer[ListOfBins] {
  override def write(kryo: Kryo, out: Output, objExt: ListOfBins): Unit = {
    val obj = objExt.s
    out.writeInt(obj.size)
    obj.foreach(BinsImplSerializer.write(kryo, out, _))
  }

  override def read(kryo: Kryo, in: Input, t: Class[ListOfBins]): ListOfBins = {
    val size = in.readInt()
    ListOfBins((0 until size)
      .map(_ => BinsImplSerializer.read(kryo, in, classOf[BinsImpl]))
      .toList
    )
  }

  override def isImmutable: Boolean = true
}

