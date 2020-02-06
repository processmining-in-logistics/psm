import sbt._
import Keys._

ThisBuild / organization := "org.processmining"
ThisBuild / version      := "1.2.0"
ThisBuild / scalaVersion := "2.12.8"

name := "everything"

resolvers += Resolver.mavenLocal
sources in(Compile, doc) ~= (_ filter (x => !x.getName.contains("Test")))


lazy val perf_spec = project

lazy val classifiers = project
  .dependsOn(perf_spec)

lazy val ppm = project
  .dependsOn(perf_spec)

lazy val framework = project
  .dependsOn(perf_spec)

lazy val sim_ein = project
  .dependsOn(framework, ppm)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.last
}