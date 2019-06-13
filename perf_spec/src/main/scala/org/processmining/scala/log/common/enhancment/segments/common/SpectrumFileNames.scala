package org.processmining.scala.log.common.enhancment.segments.common

case class SpectrumFileNames(spectrumRootDir: String) {
  val aggregatedDir = SpectrumFileNames.getAggregatedDir(spectrumRootDir)
  val startedDir = SpectrumFileNames.getStartedDir(spectrumRootDir)

  def getDataDir(segmentName: String): String = s"$aggregatedDir/${SpectrumFileNames.segmentNameToFileName(segmentName)}"

  def getDataFileName(segmentName: String): String = s"${getDataDir(segmentName)}/data.bin"

  def getDataIndexFileName(segmentName: String): String = s"${getDataDir(segmentName)}/index.bin"

  def getStartedDir(segmentName: String): String = s"$startedDir/${SpectrumFileNames.segmentNameToFileName(segmentName)}"

  def getStartedFileName(segmentName: String): String = s"${getStartedDir(segmentName)}/segments.bin"

  def getStartedIndexFileName(segmentName: String): String = s"${getStartedDir(segmentName)}/index.bin"
}

object SpectrumFileNames {
  def getAggregatedDir(spectrumRootDir: String) = s"$spectrumRootDir/data"

  def getStartedDir(spectrumRootDir: String) = s"$spectrumRootDir/started"

  def segmentNameToFileName(segmentName: String): String = {
    val noExclamations = segmentName.map {
      case '!' => "[exclamation]"
      case x: Char => s"$x"
    }.mkString
    val escaped = noExclamations
      .replaceFirst(":", "!")
      .map {
        case '/' => "[slash]"
        case '\\' => "[backslash]"
        case '<' => "[lessthan]"
        case '>' => "[greaterthan]"
        case '"' => "[doublequote]"
        case '|' => "[pipe]"
        case '?' => "[questionmark]"
        case '*' => "[asterisk]"
        case ':' => "[colon]"
        case x: Char => s"$x"
      }.mkString
    if (escaped.endsWith(" ")) escaped.substring(0, escaped.length - 1) + "[space]" else escaped
  }

  def fileNameToSegmentName(filename: String): String =
    filename
      .replace('!', ':')
      .replace("[exclamation]", "!")
      .replace("[slash]", "/")
      .replace("[backslash]", "\\")
      .replace("[lessthan]", "<")
      .replace("[greaterthan]", ">")
      .replace("[doublequote]", "\"")
      .replace("[pipe]", "|")
      .replace("[questionmark]", "?")
      .replace("[asterisk]", "*")
      .replace("[colon]", ":")
      .replace("[space]", " ")

}
