# How to Build the PSM from Sources

## 1. Building from Command Line

1. Install [JDK 8 64bit](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (do not use JRE!)
1. Install [git](https://git-scm.com/) or [Mercurial](https://www.mercurial-scm.org/wiki/Download) (depends on your repository type)
1. Install [sbt 1.2.x](https://www.scala-sbt.org/download.html) or higher
v Install [maven](https://maven.apache.org/download.cgi) and add its folder `bin` into the Windows [PATH](https://www.java.com/en/download/help/path.xml)
1. Download [XES implementation](http://xes-standard.org/): [`OpenXes-XXX.jar`](http://code.deckfour.org/Spex/), [`openxes-xstream-XXX.jar`]((http://www.xes-standard.org/openxes/download)), [`Spex-XXX.jar`]((http://www.xes-standard.org/openxes/download))
1. Install XES log implementation into your local maven repository, see an example (use the system command line, e.g, `cmd.exe`): 
1. `mvn install:install-file -Dfile=c:\openxes\OpenXES-XStream-20170216.jar -DgroupId=org.xes-standard -DartifactId=openxes-xstream -Dversion=2.23 -Dpackaging=jar`
1. `mvn install:install-file -Dfile=c:\openxes\OpenXES-20170216.jar -DgroupId=org.xes-standard -DartifactId=openxes -Dversion=2.23 -Dpackaging=jar`
1. `mvn install:install-file -Dfile=c:\openxes\spex-1.jar -DgroupId=org.deckfour -DartifactId=Spex -Dversion=1.0 -Dpackaging=jar`
1. check out the PSM repository
1. run `sbt` from your source root directory and execute commands `compile`, `package`

## 2. Working from an IDE (Optional)
1. make sure that you successfully built the project in the command line (**strongly recommended**)
1. install your favorite IDE that supports Java and Scala development. This project is developed in [Intellij IDEA](https://www.jetbrains.com/idea/download/#section=windows)
1. import the **pre-built project** (use 'import the project structure from sbt files')
1. setup [Scala SDK](https://www.scala-lang.org/download/)

Configuration stpes depend on the IDE.

 

## 3. Building an Überjar

To build an [Überjar](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar), go to your source root directory and run `sbt "set test in assembly := {}" clean assembly` from command line.
