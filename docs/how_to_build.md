# How to Build and Run the PSM from Sources

## 1. Building from Command Line

1. Install [JDK 8 64bit](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (do not use JRE!)
1. Install [git](https://git-scm.com/) or [Mercurial](https://www.mercurial-scm.org/wiki/Download) (depends on your repository type)
1. Install [sbt 1.2.x](https://www.scala-sbt.org/download.html) or higher
1. Install [maven](https://maven.apache.org/download.cgi) and add its folder `bin` into the Windows [PATH](https://www.java.com/en/download/help/path.xml)
1. Increase the sbt heap: add system variable `SBT_OPTS` and set it to 4-8Gb: `-Xmx8G` (use the same Windows tab as for the previous step)
1. Download XES implementation [`OpenXes-XXX.jar`](http://xes-standard.org/openxes/download) and install it into your local maven repository: `mvn install:install-file -Dfile=c:\openxes\OpenXES-20211004.jar -DgroupId=org.xes-standard -DartifactId=openxes -Dversion=2.27 -Dpackaging=jar`
1. check out the PSM repository
1. run `sbt` from your source root directory and execute commands `compile`, `package`

## 2. Building an Überjar and Running the PSM as a Stand-Alone Application

Read about an Überjar [here](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar).

1. Go to your source root directory and run `sbt "set test in assembly := {}" clean assembly` from command line.
1. Move resulting `jar` file into a separate directory
1. Copy file [`log4j.properties`](../res/log4j.properties) into the same directory
1. Execute `java -cp "perf_spec-assembly-1.1.0.jar" org.processmining.scala.viewers.spectrum.view.Form`

Increase the JVM heap size for working with large event logs using `-Xmx` JVM option, e.g., set the heap size to `30Gb` if your machine has `32Gb` RAM as follows: `java -Xmx30G -cp "perf_spec-assembly-1.1.0.jar" org.processmining.scala.viewers.spectrum.view.Form`

## 3. Working in an IDE

1. First, build the project in the `sbt` command line: executed `compile` then `package`. 
1. Install your favorite IDE that supports Java and Scala development. This project has being developed in [Intellij IDEA](https://www.jetbrains.com/idea/download/#section=windows)
1. Import the **pre-built project** (use 'import the project structure from sbt files')
1. Setup [Scala 2.12.8 SDK](https://www.scala-lang.org/download/). **Earlier versions are not binary compatible.**
1. Use Java 8 

Configuration steps depend on the IDE.

If Intellij IDEA highlights lines of `.sbt` files with red, try to reimport all `sbt` files through the sbt tool panel (button 'Reload All sbr Projects').
