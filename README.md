# Performance Spectrum Miner

The Performance Spectrum Miner (PSM) is a visual analytics tool for event data. It takes as input an event log (of events, timestamps, and case identifier) of past process or system executions in CSV or XES format. The PSM visualizes the flow of all cases over all process over time, and gives detailed insights performance characteristics.

![The performance spectrum miner is a visual analytics tool to visualize process performance from event log data on a detailed level in a comprehensive way.](/docs/figures/performance_spectrum_promo.jpg)

The PSM visualization 
* **shows how the performance of a process varies** over time regarding throughput, volume, steadiness levels, peaks, and drops,
* allows to **analyze detailed performance characteristics** of each step such as variability in waiting, prioritization of cases, delays and synchronization behavior effecting multiple cases together,
* reveals various **performance patterns** such as queueing disciplines, batching, prioritization and overtaking, slow movers, temporary bottlenecks, changes in process, and many more, and thereby
* gives insights into different **performance variants** of the process within each step and across steps, and how these change over time.

The PSM project provides two implementations of the Performance Spectrum Miner as a plugin to the [Process Mining Framework ProM](http://www.promtools.org/) and as a stand-alone application.

![Screenshots of the standalone application and of the ProM plugin of the Performance Spectrum Miner](/docs/figures/performance_spectrum_miner_standalone_prom_plugin.jpg)

The PSM project is the result of the joint research project on [Process Mining in Logistics](http://www.win.tue.nl/ais/doku.php?id=research:projects#process_mining_in_logistics) between Eindhoven University of Technology and Vanderlande Industries, and developed by [Vadim Denisov](https://github.com/vadimmidavvv), [Elena Belkina](https://github.com/ebelkina), and [Dirk Fahland](https://github.com/dfahland).

# How to Install

## System requirements

  * Microsoft Windows 10/8/7. The PSM is not *tested* yet on other OS.
  * 2 GB RAM minimum, 8 GB RAM recommended
  * 100MB hard disk space for ProM, 2 GB hard disk space for caches recommended
  * 1024x768 minimum screen resolution
  
## Prerequisite: Java 8

The PSM is implemented and tested with Java 8 and is not compatible with previous Java version (e.g. with Java 7).

1. Install JRE/JDK 1.8, 64bit recommended
1. Make sure that a correct installation of Java is configured: execute `java -version` in the command line. You should get a response like this:

`java version "1.8.0_171"`

`Java(TM) SE Runtime Environment (build 1.8.0_171-b11)`

`Java HotSpot(TM) 64-Bit Server VM (build 25.171-b11, mixed mode)`


*If you do not want to change your current Java installation to Java 8, you can download Java 8 and explicitly call it while starting the PSM or ProM (in 'ProM.bat'), for example:*

`"C:\Program Files\Java\jre1.8.0_171\bin\java.exe" -jar perf_spec-assembly-1.0.2.jar`
  
   
## Installation of the PSM as a ProM plugin

1. Download [ProM nightly build](http://www.promtools.org/doku.php?id=nightly). The PSM has been tested with versions 14th August 2018 and later.
1. Run *ProM Package Manager* (execute `PackageManager.bat`)
1. Go to tab 'Not installed', find and install plugin **PerformanceSpectrum**.
1. Exit *ProM Package Manager*
1. Recommended for large datasets: open file `ProM.bat` in any text editor and change parameter `–Xmx` from `4` to a value equal to your laptop's RAM size minus 2
1. Execute `ProM.bat` to run the PSM

## Update of the PSM ProM plugin

1. Close ProM (if opened) and run *ProM Package Manager* (execute `PackageManager.bat`)
1. Go to tab 'Out of date', find and update plugin **PerformanceSpectrum**.
1. Exit *ProM Package Manager*

## Installation of a stand-alone version of the PSM

1. Download and unzip **TODO add a link to the uberjar**
1. Execute `java -jar perf_spec-assembly-1.0.2.jar` to run the PSM

# Getting Started

Analyzing the Performance Spectrum of a process with the PSM has three steps
1. Transform an event log (in XES format) into data suitable for the PSM (different performance classifiers and aggregation functions can be used).
1. Opening the transformed data for analysis with the PSM
1. Exploring the Performance Spectrum

## Transforming an event log for Performance Spectrum Analysis

### ... in ProM
1. Load the XES event log into ProM via the //Import...// button.
1. Go to the //Action// Tab and select the //Performance Spectrum Miner// plugin from the action list.
1. Choose parameters for generating the performance spectrum data.
   * A configuration dialog will show providing default values for the transformation.
   * See the [User Manual](docs/user-manual.md) for details.
   * The transformed data will be stored on disk in the //Intermediate storage directory// together with a meta-data file (`session.psm`). You can load this transformed data also later into ProM by loading the `session.psm` meta-data file.
   * Choose //Process & open//
   * The transformation may require some time and main memory depending on the //Bin size// chosen. Transformation for larger bin sizes are faster and require less memory.
1. In ProM the transformed data will then also be opened automatically

### ... in PSM standalone
1. Load an XES event log via the //Open...// button.
1. Choose parameters for generating the performance spectrum data.
   * A configuration dialog will show providing default values for the transformation.
   * See the [User Manual](docs/user-manual.md) for details.
   * The transformed data will be stored on disk in the //Intermediate storage directory// together with a meta-data file (`session.psm`). You can load this transformed data also later via the //Open...// button.
   * Choose //Process & open//
   * The transformation may require some time and main memory depending on the //Bin size// chosen. Transformation for larger bin sizes are faster and require less memory.

 ## Opening the transformed data for analysis with the PSM

1. By choosing //Process & open// during data transformation, the transformed data will be opened automatically. Alternatively, you can also load a previously transformed data set by opening the .psm meta-data file (via //Import// in ProM, and choosing //Performance Spectrum Miner View//, or via //Open// in the stand-alone version).
1. Choose parameters for opening. For now use the default values provided, see the [User Manual](docs/user-manual.md) for details. 

## Exploring the Performance Spectrum

![Main Windows of the Performance Spectrum Miner](/docs/figures/getting_started_exploring_01_main_view.png)

The main window of the Performance Spectrum Miner is divided into
1. a panel visualizing the performance spectrum of the event long
1. a control and filtering panel at the bottom that particular contains one sliders to scroll horizontally and two sliders to zoom vertically and horizontally

In the visualization panel, each horizontal segment shows how cases move over time (x-axis) from one activity to the next activity (y-axis). 

By default the visualization shows //Lines//. In the figure below, each colored line describes //one// case moving from //Send Fine// to //Insert Fine Notification//. The x-coordinates of the start and end point of each line visualize the moments in time when //Send Fine// and //Insert Fine Notification// occurred, respectively. The color of the line depends on the classification that was chosen in the transformation step, which can be retrieved via the //Legend// button in the control and filtering panel.

![A segment of the Performance Spectrum Miner](/docs/figures/getting_started_exploring_02_one_segment.png)

The performance spectrum shows among other things:
* There are cases that are processed very fast (near vertical dark-blue lines) and there are cases processed much slower (sloped lines in light-blue, yellow, and orange). 
* The slower cases all have in common that //Send Fine// occurred for them together with many other cases (at the same moment in time) in a batch, whereas //Insert Fine Notification// happened individually for each case. 
* Batching for //Send Fine// occurs at irregular intervals and the amount of cases per batch varies greatly over time.

While the //Lines// show the speed of cases, the amount of cases over time can be visualized by checking //Bars// in the control and filtering panel.

![A segment of the Performance Spectrum Miner](/docs/figures/getting_started_exploring_03_one_segment_bars.png)

The stacked bars provide aggreate information about how many cases started, ended, or were pending in particular time-window between the two activities of the segment. The parameters of this aggregation are chosen in the transformation step, see the [User Manual](docs/user-manual.md) for details. In the example above, the stacked bars show that the process experienced a very high amount of cases going from //Send Fine// to //Insert Fine Notification// in particular period (the exact time will be shown on the bottom left when hovering the mouse over the respective part of the visualization). The coloring indicates that in this period, the cases were processed much slower than in other period. The number //2988// in the label of the segment tells that there were at a maximum 2988 cases transitioning together through this part of the process.

The Performance Spectrum can be explored in various ways.
* The //Options...// dialog allows to
  * Filter out segments whose throughput of cases (starting/pending/ending cases) in one time window is below/above a certain threshold, e.g, to focus on the most frequently used segments in a process.
  * Filtering in and out segments by activity names, using regular expressions, for example
    * `.*Send Fine.*` will include all segments involving //Send Fine//
    * `Send Fine:.*` will include all segments starting with //Send Fine// and leading to some other activity
  * see the [User Manual](docs/user-manual.md) for details
* Clicking the //left mouse button// on the visualization panel shows a context menu with the classes of the classification chosen in the transformation step. Selecting one of them shows only the cases of this class, e.g., only cases whose performance is in the 1st quartile of each segment.
* Right-clicking and dragging a selection box around cases in one segment allows to highlight the selected cases in all other segments (the non-selected cases will be shown in grey). The //Clear// button in the control panel removes this selection.


More detailed information can be found in
* the [User Manual](docs/user-manual.md)
* Links to additional materials (papers)

# Project

**TODO Dirk**
* team, affiliation in detail
* links to other pages

# Programmer's Guide

## How to build

1. Install JDK 8
1. Install [sbt](https://www.scala-sbt.org/download.html) 1.2.1 or a higher version.
1. Clone this repository into a folder on your local disk
1. Go to subfolder `perf_spec` and run `sbt`
1. Execute command `package` to build the PSM without dependecies or command `assembly` to build an Überjar

*You can also use your favorite IDE with support of Java and Scala languages, e.g. [IntelliJ IDEA Community](https://www.jetbrains.com/idea/download/#section=windows), to build the PSM or work with the sources.*

## Roadmap

**TODO Vadim, Dirk**
  * https://github.com/processmining-in-logistics/psm/issues/3
  * https://github.com/processmining-in-logistics/psm/issues/4
  * https://github.com/processmining-in-logistics/psm/issues/7
  * https://github.com/processmining-in-logistics/psm/issues/8

## How to contribute

Please see the [contribution guidelines for this project](docs/contributing.md)
