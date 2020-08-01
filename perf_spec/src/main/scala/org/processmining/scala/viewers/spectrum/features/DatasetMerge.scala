package org.processmining.scala.viewers.spectrum.features

import java.io.{File, PrintWriter}

import org.apache.log4j.PropertyConfigurator
import org.processmining.scala.log.utils.common.errorhandling.{EH, JvmParams}
import org.slf4j.LoggerFactory

import scala.io.Source

object DatasetMerge {
  private val logger = LoggerFactory.getLogger(DatasetMerge.getClass)

  def main(args: Array[String]): Unit = {
    PropertyConfigurator.configure("./log4j.properties")
    JvmParams.reportToLog(logger, s"${DatasetMerge.getClass} started")
    if (args.isEmpty) {
      logger.info(s"Use the following arguments: DATASET_WITH_TARGET DATASET_TO_BE_MERGE COLUMNS_TO_SKIP OUTPUT")
    } else {
      logger.info(s"Cli args:${args.mkString(",")}")
      try {
        val mainDatasetFilename = args(0)
        val additionalDatasetFilename = args(1)
        val columnsToSkip = args(2).toInt
        val outputFilename = args(3)
        val pw = new PrintWriter(new File(outputFilename))
        val lines1 = Source.fromFile(mainDatasetFilename).getLines()
        val lines2 = Source.fromFile(additionalDatasetFilename).getLines()
        lines1.zipAll(lines2, "", "")
          .foreach { x =>
            if(x._1.isEmpty || x._2.isEmpty) throw new IllegalArgumentException(s"Files must have equal lines number.")
            val line =
              x._2
                .split(",")
                .zipWithIndex
                .filter(_._2 >= columnsToSkip)
                .map(_._1)
                .mkString(",")
            val merged = s"${x._1},$line"
            pw.println(merged)
          }
        pw.close
        logger.info(s"File '$outputFilename' is recorded.")
      } catch {
        case e: Throwable => logger.error(EH.formatError(e.toString, e))
      }
    }
    logger.info(s"App is completed.")
  }

}
