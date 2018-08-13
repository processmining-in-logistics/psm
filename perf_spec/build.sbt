import sbt._
import Keys._

name := "perf_spec"

version := "0.9.1"

scalaVersion := "2.11.8"

Compile/mainClass := Some("org.processmining.scala.viewers.spectrum.view.Form")


resolvers += Resolver.mavenLocal

sources in (Compile, doc) ~= (_ filter (x => !x.getName.contains("Test")))

libraryDependencies += "org.apache.commons" % "commons-collections4" % "4.0"
libraryDependencies += "org.apache.commons" % "commons-math3" % "3.6.1"
libraryDependencies += "org.ini4j" % "ini4j" % "0.5.4"
libraryDependencies += "org.xes-standard" % "openxes" % "2.23"
libraryDependencies += "org.xes-standard" % "openxes-xstream" % "2.23"
libraryDependencies += "org.deckfour" % "Spex" % "1.0"
libraryDependencies += "com.opencsv" % "opencsv" % "4.1"
//libraryDependencies += "org.jfree" % "jfreechart" % "1.0.17"
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.25" % Test
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"
libraryDependencies += "log4j" % "log4j" % "1.2.17"
libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.0"



//retrieveManaged := true

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.last
}

mainClass in assembly := Some("org.processmining.scala.viewers.spectrum.view.PerformanceSpectrum")
