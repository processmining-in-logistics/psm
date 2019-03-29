package org.processmining.scala.log.common.csv.parallel

import java.util.regex.Pattern

import org.processmining.scala.log.utils.csv.common.CsvReaderHelper

import scala.collection.parallel.ParSeq
import scala.io.Source
import scala.reflect.ClassTag

class CsvReader(val defaultSep: (String, String) = CsvReaderHelper.EmptySep) {

  def parse[T: ClassTag](lines: ParSeq[Array[String]], create: (Array[String]) => T): ParSeq[T] =
    lines.map {
      create(_)
    }

  def parseSeq[T: ClassTag](lines: Seq[Array[String]], create: (Array[String]) => T): Seq[T] =
    lines.map {
      create(_)
    }

  def read(filename: String): (Array[String], ParSeq[Array[String]]) = {
    val lines = Source.fromFile(filename).getLines.toSeq.par
    val header = lines.head
    val (splitterRegEx, wrap) = if (defaultSep == CsvReaderHelper.EmptySep) CsvReaderHelper.detectSep(header) else defaultSep
    //println(s"Separator for '$filename' is '$splitterRegEx', wrapping charachter is '$wrap'")
    val splitter = CsvReader.splitterImplV2(Pattern.compile(splitterRegEx), wrap, _: String)
    val headerColumns = splitter(header)
    val data =
      lines
        .tail
        .map {
          splitter(_)
        }
    (headerColumns, data)
  }

  //TODO: move to common
  def readNoHeaderSeq(filename: String): Seq[Array[String]] = {
    val lines = Source.fromFile(filename).getLines.toSeq

    val (splitterRegEx, wrap) = if (defaultSep == CsvReaderHelper.EmptySep) CsvReaderHelper.detectSep(lines.head) else defaultSep
    //println(s"Separator for '$filename' is '$splitterRegEx', wrapping charachter is '$wrap'")
    val splitter = CsvReader.splitterImplV2(Pattern.compile(splitterRegEx), wrap, _: String)
    val data =
      lines
        .map {
          splitter(_)
        }
    data
  }
}

object CsvReader {

  //https://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
  def getCsvLineSplitterRegex(sep: String): String = {
    val otherThanQuote = " [^\"] "
    val quotedString = String.format(" \" %s* \" ", otherThanQuote)
    val regex = String.format("(?x) " + // enable comments, ignore white spaces
      "%s                         " + // match a sep
      "(?=                       " + // start positive look ahead
      "  (?:                     " + //   start non-capturing group 1
      "    %s*                   " + //     match 'otherThanQuote' zero or more times
      "    %s                    " + //     match 'quotedString'
      "  )*                      " + //   end group 1 and repeat it zero or more times
      "  %s*                     " + //   match 'otherThanQuote'
      "  $                       " + // match the end of the string
      ")                         ", // stop positive look ahead
      sep, otherThanQuote, quotedString, otherThanQuote)
    regex
  }

  def splitterImplV2(pattern: Pattern, wrap: String, line: String): Array[String] =
    pattern
      .split(line, -1)
      .map(x => if (x.startsWith(wrap)) x.substring(wrap.length) else x)
      .map(x => if (x.endsWith(wrap)) x.substring(0, x.length - wrap.length) else x)


}