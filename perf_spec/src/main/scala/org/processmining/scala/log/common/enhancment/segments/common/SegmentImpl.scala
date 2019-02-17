package org.processmining.scala.log.common.enhancment.segments.common

import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}

case class SegmentImpl(caseId: String, timeMs: Long, duration: Long, field0: String, clazz: Int) extends Serializable


object SegmentImplSerializer extends Serializer[SegmentImpl] {
  override def write(kryo: Kryo, output: Output, s: SegmentImpl): Unit = {
    output.writeString(s.caseId)
    output.writeLong(s.timeMs)
    output.writeLong(s.duration)
    //output.writeString(s.field0)
    output.writeByte(s.clazz)
  }

  override def read(kryo: Kryo, input: Input, t: Class[SegmentImpl]): SegmentImpl =
    SegmentImpl(input.readString(), input.readLong(), input.readLong(), "", input.readByte())

  override def isImmutable: Boolean = true

}

case class MapOfSegments(s: Map[Int, SegmentImpl]) extends Serializable

object MapOfSegmentImplSerializer extends Serializer[MapOfSegments] {
  override def write(kryo: Kryo, out: Output, objExt: MapOfSegments): Unit = {
    val obj = objExt.s
    out.writeInt(obj.size)
    obj.foreach(x => {
      out.writeInt(x._1)
      SegmentImplSerializer.write(kryo, out, x._2)
    }
    )
  }

  override def read(kryo: Kryo, in: Input, t: Class[MapOfSegments]): MapOfSegments = {
    val size = in.readInt()
    MapOfSegments((0 until size)
      .map(_ => in.readInt() -> SegmentImplSerializer.read(kryo, in, classOf[SegmentImpl]))
      .toMap
    )
  }

  override def isImmutable: Boolean = true
}


case class ListOfSegments(s: List[SegmentImpl]) extends Serializable

object ListOfSegmentImplSerializer extends Serializer[ListOfSegments] {
  override def write(kryo: Kryo, out: Output, objExt: ListOfSegments): Unit = {
    val obj = objExt.s
    out.writeInt(obj.size)
    obj.foreach(SegmentImplSerializer.write(kryo, out, _))
  }

  override def read(kryo: Kryo, in: Input, t: Class[ListOfSegments]): ListOfSegments = {
    val size = in.readInt()
    ListOfSegments((0 until size)
      .map(_ => SegmentImplSerializer.read(kryo, in, classOf[SegmentImpl]))
      .toList
    )
  }

  override def isImmutable: Boolean = true
}


case class MapOfInts(s: Map[Int, Int]) extends Serializable

object MapOfIntsSerializer extends Serializer[MapOfInts] {
  override def write(kryo: Kryo, out: Output, objExt: MapOfInts): Unit = {
    val obj = objExt.s
    out.writeInt(obj.size)
    obj.foreach(x => {
      out.writeInt(x._1)
      out.writeInt(x._2)
    }
    )
  }

  override def read(kryo: Kryo, in: Input, t: Class[MapOfInts]): MapOfInts = {
    val size = in.readInt()
    MapOfInts((0 until size)
      .map(_ => in.readInt() -> in.readInt())
      .toMap
    )
  }

  override def isImmutable: Boolean = true
}
