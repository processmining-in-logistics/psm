import sbt._
import Keys._

name := "classifiers"

version := "1.1.5"

scalaVersion := "2.12.8"


resolvers += Resolver.mavenLocal




//retrieveManaged := true
/*
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.last
}
*/
//mainClass in assembly := Some("org.processmining.scala.viewers.spectrum.view.PerformanceSpectrum")
