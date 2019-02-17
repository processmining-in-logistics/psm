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


  def segmentNameToFileName(segmentName: String): String =
    segmentName.replace(':', '!')

  def fileNameToSegmentName(filename: String): String =
    filename.replace('!', ':')


}
