import sbt._
import Keys._

name := "ppm"

resolvers += Resolver.mavenLocal

sources in (Compile, doc) ~= (_ filter (x => !x.getName.contains("Test")))

libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.10.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
