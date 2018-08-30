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
    
## Installation as a ProM plugin

1. Install JRE/JDK 1.8.x, 64bit recommended
1. Download [ProM nightly build](http://www.promtools.org/doku.php?id=nightly). The PSM is tested with version **TODO add a link to version 14.08.18**
1. Run *ProM Package Manager* and install plugin **PerformanceSpectrum**
1. Exit *ProM Package Manager*
1. Recommended for large datasets: open file `ProM.bat` in any text editor and change parameter `–Xmx` from `4` to a value equal to your laptop's RAM size minus 2
1. Execute `ProM.bat` to run the PSM

## Installation of a stand-alone version of the PSM

1. Install JRE/JDK 1.8.x, 64bit recommended
1. Download and unzip **TODO add a link to the uberjar**
1. Execute `java -jar perf_spec-assembly-1.0.2.jar` to run the PSM

# Getting Started

**TODO Dirk** basic steps to get to the performance spectrum (standalone, ProM)

**TODO Dirk** what is the performance spectrum, how to read it

More detailed information can be found in
* the [User Manual](docs/user-manual.md)
* Links to additional materials (papers)

# Project

**TODO Dirk**
* team, affiliation in detail
* links to other pages

# Programmer's Guide

## How to build

**TODO Vadim** how to build, required dependencies

## Roadmap

**TODO Vadim, Dirk**

## How to contribute

Please see the [contribution guidelines for this project](docs/contributing.md)
