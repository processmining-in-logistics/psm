import sbt._
import Keys._

name := "ppm"

resolvers += Resolver.mavenLocal

sources in (Compile, doc) ~= (_ filter (x => !x.getName.contains("Test")))
