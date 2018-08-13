package org.processmining.scala.log.common.unified.event

/** Contains predefined attributes schemas */
object CommonAttributeSchemas{

  /** Long */
  val AttrNameDuration: String = "duration"

  /** Int */
  val AttrNameSize: String = "size"

  /** Byte */
  val AttrNameClass: String = "class"

  //TODO: change to "class" ???
  /** Int ??? */
  val AttrNameClazz: String = "clazz"

  val AggregatedEventsSchema = Set(AttrNameDuration, AttrNameSize)

  val DfrEventSchema= Set(AttrNameDuration)

  val ClassifiedDfrEventSchema = Set(AttrNameDuration, AttrNameClass)
}



///** Contains predefined attributes schemas */
//object CommonAttributeSchemas {
//
//  /** Schema for events without any optional attributes */
//  val EmptySchema = new StructType()
//
//  /** Schema of aggregated events */
//  val AggregatedEventsSchema = new StructType(Array(
//    StructField("duration", LongType, false),
//    StructField("size", IntegerType, false)
//  ))
//
//  /** Schema of segements */
//  val DfrEventSchema = new StructType(Array(
//    StructField("duration", LongType, false)
//  ))
//
//  /** Schema of classified segments */
//  val ClassifiedDfrEventSchema = new StructType(Array(
//    StructField("duration", LongType, false),
//    StructField("class", ByteType, false)
//  ))
//
//}
//
///** Predefined empty row */
//object CommonAttributeSets {
//  val EmptyAttrs = new GenericRowWithSchema(Array(), CommonAttributeSchemas.EmptySchema)
//}
