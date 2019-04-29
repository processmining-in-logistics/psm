import sbt._
import Keys._

name := "psm"
version := "1.1.0"
scalaVersion := "2.11.8"
resolvers += Resolver.mavenLocal
sources in(Compile, doc) ~= (_ filter (x => !x.getName.contains("Test")))

lazy val commonSettings = Seq(
  organization := "org.processmining",
  version := s"$version-SNAPSHOT",
  scalaVersion := "2.11.8"
)

lazy val psm_utils = project
  .settings(commonSettings)


lazy val perf_spec = project
  .dependsOn(psm_utils)
  .settings(
    commonSettings
  )

lazy val classifiers = project
  .dependsOn(perf_spec)
  .settings(
    commonSettings
  )

lazy val ppm = project
  .dependsOn(perf_spec)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-collections4" % "4.0",
      "org.apache.commons" % "commons-math3" % "3.6.1",
      "org.ini4j" % "ini4j" % "0.5.4",
      "com.thoughtworks.xstream" % "xstream" % "1.4.10",
      "org.xes-standard" % "openxes" % "2.23",
      "org.xes-standard" % "openxes-xstream" % "2.23",
      "org.deckfour" % "Spex" % "1.0",
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "com.opencsv" % "opencsv" % "4.1",
      "org.jfree" % "jfreechart" % "1.0.17"
    )
  )


assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.last
}
