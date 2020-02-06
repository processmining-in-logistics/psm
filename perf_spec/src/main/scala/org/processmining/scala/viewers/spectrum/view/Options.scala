package org.processmining.scala.viewers.spectrum.view

import java.util.regex.Pattern

case class Options(whiteList: Array[Pattern],
                   blackList: Array[Pattern],
                   ids: String,
                   minCount: Int,
                   maxCount: Int,
                   reverseColors: Boolean,
                   fontSize: Int) {
  def filters(w: Array[Pattern], b: Array[Pattern]) = copy(whiteList = w, blackList = b)

  def setListOfSegments(newWhiteList: Array[String], newBlackList: Array[String]) =
    copy(whiteList = newWhiteList.map(Pattern.compile))
      .copy(blackList = newBlackList.map(Pattern.compile))

}

object Options {
  def apply(): Options = new Options(Array(), Array(), "", 0, 0, false, 20)
}
