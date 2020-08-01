package org.processmining.scala.viewers.spectrum2.model

case class SegmentTimeInterval(t1: Long, t2: Long, t2Max: Long)

case class SegmentLoad(bins: Array[Int], max: Int)


class LoadSource(ds: AbstractDataSource) {

  private var currentBinSizeMs: Long = 0
  private var currentOverlaidBinSizeMs: Long = 0
  val firstPointMs = ds.startTimeMs
  val lastPointMs = ds.endTimeMs

  private def getIndex(t: Long) = ((t - firstPointMs) / currentBinSizeMs).toInt

  private def update(interval: (Long, Long), loadArray: Array[Int]): Unit =
    (getIndex(interval._1) to getIndex(interval._2)).foreach(i => loadArray.update(i, loadArray(i) + 1))

  private def getNPoints = ((lastPointMs - firstPointMs) / currentBinSizeMs).toInt + 1

  // counts how many intervals have at least on point in the bin
  private def getLoad(intervals: Vector[(Long, Long)]): SegmentLoad = {
    val nPoints = getNPoints
    val loadArray = new Array[Int](nPoints)
    intervals.foreach(update(_, loadArray))
    SegmentLoad(loadArray, loadArray.max)
  }


  private def getCertainAndUncertainParts(intervals: Vector[Segment]): (SegmentLoad, SegmentLoad) = {
    val nPoints = getNPoints
    val certainLoadArray = new Array[Int](nPoints)
    val uncertainLoadArray = new Array[Int](nPoints)
    intervals.foreach(interval => {
      val i1Min_ = getIndex(interval.start.minMs)
      val i1Max_ = getIndex(interval.start.maxMs)
      val i2Min_ = getIndex(interval.end.minMs)
      val i2Max_ = getIndex(interval.end.maxMs)

      //temp. workaround
      val i1Min = Math.min(i1Min_, i1Max_)
      val i1Max = Math.max(i1Min_, i1Max_)
      val i2Min = Math.min(i2Min_, i2Max_)
      val i2Max = Math.max(i2Min_, i2Max_)


      val nTmp = i2Max - i1Min + 1
      val certainTmp = Array.fill(nTmp) {
        0
      }
      val uncertainTmp = Array.fill(nTmp) {
        1
      }
      if (i1Max <= i2Min)
        (i1Max to i2Min).foreach(i => {
          certainTmp.update(i - i1Min, 1)
          uncertainTmp.update(i - i1Min, 0)
        })

      (i1Min to i2Max).foreach(i => {
        certainLoadArray.update(i, certainLoadArray(i) + certainTmp(i - i1Min))
        uncertainLoadArray.update(i, uncertainLoadArray(i) + uncertainTmp(i - i1Min))
      })
    })
    (SegmentLoad(certainLoadArray, certainLoadArray.max), SegmentLoad(uncertainLoadArray, uncertainLoadArray.max))

  }

  def segmentLoad(binSizeMs: Long): Map[SegmentName, (SegmentLoad, SegmentLoad)] = {
    if (currentBinSizeMs != binSizeMs) {
      currentBinSizeMs = binSizeMs
      segmentLoadCache =
        ds.segments.map(x => (x._1, getCertainAndUncertainParts(x._2)))
    }
    segmentLoadCache
  }

  def overlaidSegmentLoad(srcSegmentName: SegmentName, binSizeMs: Long): SegmentLoad = {
    if (currentOverlaidBinSizeMs != binSizeMs || !overlaidSegmentLoadCache.contains(srcSegmentName)) {
      if (currentOverlaidBinSizeMs != binSizeMs) {
        overlaidSegmentLoadCache = Map()
        currentOverlaidBinSizeMs = binSizeMs
      }
      val tmp = ds.overlaidSegments(srcSegmentName)
      val load = getLoad(tmp.map(x => (x.timestamp1, x.timestamp2)))
      overlaidSegmentLoadCache = overlaidSegmentLoadCache + (srcSegmentName -> load)
    }
    overlaidSegmentLoadCache(srcSegmentName)
  }

  private var segmentLoadCache: Map[SegmentName, (SegmentLoad, SegmentLoad)] = Map()
  private var overlaidSegmentLoadCache: Map[SegmentName, SegmentLoad] = Map()


}
