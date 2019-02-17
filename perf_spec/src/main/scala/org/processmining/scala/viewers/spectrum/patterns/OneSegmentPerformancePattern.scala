package org.processmining.scala.viewers.spectrum.patterns




case class OneSegmentPerformancePattern(leftTop: Long, rightTop: Long, leftBottom: Long, rightBottom: Long, power: Long, classes: (Int, Int))
