package org.processmining.scala.log.common.csv.common

import java.util.regex.Pattern

import org.processmining.scala.log.utils.common.csv.common.CsvReaderHelper

import scala.io.Source
import scala.reflect.ClassTag

class CsvReader(val defaultSep: (String, String) = CsvReaderHelper.EmptySep) {

  def parse[T: ClassTag](lines: Seq[Array[String]], create: (Array[String]) => T): Seq[T] =
    lines.map {
      create(_)
    }

  def read(filename: String): (Array[String], Seq[Array[String]]) = {
    val lines = Source.fromFile(filename).getLines.toSeq
    val header = lines.head
    val (splitterRegEx, wrap) = if (defaultSep == CsvReaderHelper.EmptySep) CsvReaderHelper.detectSep(header) else defaultSep
    //println(s"Separator for '$filename' is '$splitterRegEx', wrapping charachter is '$wrap'")
    val splitter = org.processmining.scala.log.common.csv.parallel.CsvReader.splitterImplV2(Pattern.compile(splitterRegEx), wrap, _: String)
    val headerColumns = splitter(header)
    val data =
      lines
        .tail
        .map {
          splitter(_)
        }
    (headerColumns, data)
  }
}
