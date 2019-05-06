# How to Build the PSM from Sources

## 1. Building from Command Line

* Install [JDK 8 64bit](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (do not use JRE!)
* Install [git](https://git-scm.com/) or [Mercurial](https://www.mercurial-scm.org/wiki/Download) (depends on your repository type)
* Install [sbt 1.2.x](https://www.scala-sbt.org/download.html) or higher
* Install [maven](https://maven.apache.org/download.cgi) and add its folder `bin` into the Windows PATH
* Download [XES implementation](http://xes-standard.org/): [`OpenXes-XXX.jar`](http://code.deckfour.org/Spex/), [`openxes-xstream-XXX.jar`]((http://www.xes-standard.org/openxes/download)), [`Spex-XXX.jar`]((http://www.xes-standard.org/openxes/download))
* Install XES log implementation into your local maven repository, see an example (use the system command line, e.g, `cmd.exe`): 
* `mvn install:install-file -Dfile=c:\openxes\OpenXES-XStream-20170216.jar -DgroupId=org.xes-standard -DartifactId=openxes-xstream -Dversion=2.23 -Dpackaging=jar`
* `mvn install:install-file -Dfile=c:\openxes\OpenXES-20170216.jar -DgroupId=org.xes-standard -DartifactId=openxes -Dversion=2.23 -Dpackaging=jar`
* `mvn install:install-file -Dfile=c:\openxes\spex-1.jar -DgroupId=org.deckfour -DartifactId=Spex -Dversion=1.0 -Dpackaging=jar`
* check out the PSM repository
* run `sbt` from your source root directory and execute commands `compile`, `package`
* to build an [Überjar](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar), exit sbt and run `sbt "set test in assembly := {}" clean assembly` from command line

## 2. Working from an IDE (Optional)
* make sure that you successfully built the project in the command line (**strongly recommended**)
* install your favorite IDE that supports Java and Scala development. This project is developed in [Intellij IDEA](https://www.jetbrains.com/idea/download/#section=windows)
* import the **pre-built project** (use 'import the project structure from sbt files')
* setup [Scala SDK](https://www.scala-lang.org/download/)

Configuration stpes depend on the IDE.

 
