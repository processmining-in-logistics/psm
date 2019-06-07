package org.processmining.scala.log.common.enhancment.segments.common

import com.esotericsoftware.kryo.Kryo

object KryoFactory {

  def apply(): Kryo = {
    val kryo = new Kryo
    kryo.register(classOf[SegmentImpl], SegmentImplSerializer)
    kryo.register(classOf[BinsImpl], BinsImplSerializer)
    kryo.register(classOf[ListOfSegments], ListOfSegmentImplSerializer)
    kryo.register(classOf[MapOfInts], MapOfIntsSerializer)
    kryo.register(classOf[ListOfBins], ListOfBinsSerializer)
    kryo
  }

}
