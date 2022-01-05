import sbt._
import Keys._

name := "perf_spec"

//Compile/mainClass := Some("org.processmining.scala.viewers.spectrum.view.Form")

resolvers += Resolver.mavenLocal

sources in (Compile, doc) ~= (_ filter (x => !x.getName.contains("Test")))
                                                                     
libraryDependencies += "org.apache.commons" % "commons-math3" % "3.6.1"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4"
libraryDependencies += "org.ini4j" % "ini4j" % "0.5.4"
libraryDependencies += "org.xes-standard" % "openxes" % "2.27"
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.25"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"
libraryDependencies += "log4j" % "log4j" % "1.2.16"
libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.2.11"
libraryDependencies += "com.google.guava" % "guava" % "31.0.1-jre"
libraryDependencies += "com.esotericsoftware" % "kryo" % "3.0.3"
libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "2.1.1"

mainClass in assembly := Some("org.processmining.scala.viewers.spectrum.view.Form")
