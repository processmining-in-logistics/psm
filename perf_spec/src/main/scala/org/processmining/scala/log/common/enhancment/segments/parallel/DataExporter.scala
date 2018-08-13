package org.processmining.scala.log.common.enhancment.segments.parallel

import java.io.File

import org.processmining.scala.log.common.csv.common.CsvWriter
import org.processmining.scala.log.common.enhancment.segments.parallel.SegmentProcessor.ProcessedDataFileName
import org.processmining.scala.log.common.types.SegmentWithClazz

import scala.collection.parallel.ParSeq

object DataExporter {
  def toFile(groupedSegTw: ParSeq[SegmentVisualizationItem], filename: String): Unit =
    CsvWriter.eventsToCsvLocalFilesystem[SegmentVisualizationItem](groupedSegTw.seq, SegmentVisualizationItem.csvHeader, _.toCsv, filename)

  def toDirs(groupedSegTw: ParSeq[SegmentVisualizationItem], dirName: String): Unit = {
    new File(dirName).mkdirs()
    groupedSegTw
      .groupBy(_.index)
      .foreach(x => {
        new File(s"$dirName/${x._1}").mkdir()
        toFile(x._2, s"$dirName/${x._1}/$ProcessedDataFileName")
      })
  }


  // Directories structure: twIndex -> Clazz
  def segmentsToDirs(dirName: String, segments: ParSeq[(Long, SegmentWithClazz)]): Unit = {
    new File(dirName).mkdirs()
    val mapping = segments
      .groupBy(_._1)
      .map { x => (x._1, x._2.map(_._2)) }
    mapping.foreach { twLevel => {
      val twDirName = s"$dirName/${twLevel._1}"
      new File(twDirName).mkdir()
      CsvWriter.eventsToCsvLocalFilesystem[SegmentWithClazz](
        twLevel._2.seq, SegmentWithClazz.CsvHeader, _.toCsv(_.toString), s"$twDirName/${SegmentProcessor.SegmentsFileName}")
    }
    }
  }

}
