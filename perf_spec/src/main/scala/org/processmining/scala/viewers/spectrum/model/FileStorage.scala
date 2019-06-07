package org.processmining.scala.viewers.spectrum.model

import java.io.{BufferedInputStream, FileInputStream, RandomAccessFile}
import java.util.function.Consumer

import com.esotericsoftware.kryo.io.Input
import org.processmining.scala.log.common.enhancment.segments.common._
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag
import scala.reflect._

// RandomAccessFile objects are not immutable!
case class SourceFiles(dataIndex: Map[Int, Int], data: RandomAccessFile, startedIndex: Map[Int, Int], started: RandomAccessFile) {

  def close() = {
    data.close()
    started.close()
  }

}

private class FileStorage(spectrumRootDir: String, segmentNames: Array[String], callback: Consumer[Int]) {
  private val logger = LoggerFactory.getLogger(classOf[FileStorage])
  private val kryo = KryoFactory()
  private val fileNames = SpectrumFileNames(spectrumRootDir)

  private val filesBySegmentNames: Map[String, SourceFiles] =
    segmentNames
      .zipWithIndex
      //.par // is it safe for handles?
      .map { x =>
      callback.accept(x._2)
      x._1 -> createFiles(x._1)
    }
      //.seq
      .toMap

  private def readIndex(filename: String): Map[Int, Int] = {
    try {
      val input = new Input(new BufferedInputStream(new FileInputStream(filename)))
      logger.info(s"Loading '$filename'...")
      val index = kryo.readObject(input, classOf[MapOfInts]).s
      input.close()
      logger.info(s"Loaded '$filename'")
      index
    } catch {
      case ex: Throwable =>
        logger.error(ex.toString)
        throw ex
    }
  }

  private def createFiles(segmentName: String): SourceFiles = {
    val dataIndex = readIndex(fileNames.getDataIndexFileName(segmentName))
    val startedIndex = readIndex(fileNames.getStartedIndexFileName(segmentName))
    val data = new RandomAccessFile(fileNames.getDataFileName(segmentName), "r")
    val started = new RandomAccessFile(fileNames.getStartedFileName(segmentName), "r")
    SourceFiles(dataIndex, data, /*Map()*/ startedIndex, started)
  }


  def close(): Unit = filesBySegmentNames.foreach(x => x._2.close())

  private def readListOfBins(handle: RandomAccessFile, indexInFile: Int): ListOfBins = {
    handle.seek(indexInFile)
    val is = new FileInputStream(handle.getFD)
    val input = new Input(is)
    kryo.readObject(input, classOf[ListOfBins])
  }

  private def readListOfSegments(handle: RandomAccessFile, indexInFile: Int): ListOfSegments = {
    handle.seek(indexInFile)
    val is = new FileInputStream(handle.getFD)
    val input = new Input(is)
    kryo.readObject(input, classOf[ListOfSegments])
  }

  def readData(tw: Int, segment: String): Option[List[BinsImpl]] = {
    val files = filesBySegmentNames(segment)
    val indexInFileOpt = files.dataIndex.get(tw)
    if (indexInFileOpt.isDefined)
      Some(readListOfBins(files.data, indexInFileOpt.get).s)
    else None
  }

  def readStarted(tw: Int, segment: String): Option[List[SegmentImpl]] = {
    val files = filesBySegmentNames(segment)
    val indexInFileOpt = files.startedIndex.get(tw)
    if (indexInFileOpt.isDefined)
      Some(readListOfSegments(files.started, indexInFileOpt.get).s)
    else None
  }


}
