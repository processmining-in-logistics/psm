package org.processmining.scala.viewers.spectrum.export

import java.io.PrintWriter

import org.processmining.scala.log.common.enhancment.segments.common.SpectrumFileNames
import org.processmining.scala.viewers.spectrum.model.AbstractDataSource

class PsExporter(ds: AbstractDataSource) {

  def export(path: String): Unit = {
    val pwMap =
      ds
        .segmentNames
        .map(name => name -> new PrintWriter(s"$path/${SpectrumFileNames.segmentNameToFileName(name)}.csv"))
        .toMap
    pwMap.values.foreach(_.println(""""id","key","timestamp","duration","clazz""""))
    (0 until ds.twCount).foreach(tw =>
      ds
        .segments(tw)
        .foreach(x => {
          val pw = pwMap(x._1)
          x._2.foreach(y =>
            y._2.foreach(s => pw.println(s""""${s._1}","${x._1}",${s._2},${s._3},${y._1}"""))
          )
        })
    )
    pwMap.foreach(_._2.close())
    val pw = new PrintWriter(s"$path/max.csv")
    pw.println(""""key","max";"maxIntersect","maxStop","maxSum"""")
    ds
      .segmentNames
      .foreach(name => {
        val (start, pending, end, sum) = ds.maxSegmentsCount(name)
        pw.println(s""""$name",$start,$pending,$end,$sum""")

      })
    pw.close()
  }
}
