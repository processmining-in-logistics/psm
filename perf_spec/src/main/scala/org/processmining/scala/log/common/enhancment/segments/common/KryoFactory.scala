package org.processmining.scala.log.common.enhancment.segments.common

import com.esotericsoftware.kryo.Kryo
import org.processmining.scala.viewers.spectrum2.pqr.{PlaceTransition, PlaceTransitionSerializer}

object KryoFactory {

  def apply(): Kryo = {
    val kryo = new Kryo
    kryo.register(classOf[SegmentImpl], SegmentImplSerializer)
    kryo.register(classOf[BinsImpl], BinsImplSerializer)
    kryo.register(classOf[ListOfSegments], ListOfSegmentImplSerializer)
    kryo.register(classOf[MapOfInts], MapOfIntsSerializer)
    kryo.register(classOf[ListOfBins], ListOfBinsSerializer)
    kryo.register(classOf[PlaceTransition], PlaceTransitionSerializer)
    kryo
  }

}
