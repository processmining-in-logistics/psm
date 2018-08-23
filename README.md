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

The PSM project is the result of the joint research project on [Process Mining in Logistics](http://www.win.tue.nl/ais/doku.php?id=research:projects#process_mining_in_logistics) between Eindhoven University of Technology and Vanderlande Industries, and developed by Vadim Denisov, Ekaterina Belkina, and Dirk Fahland (@dfahland).

# How to Install

** TODO Vadim** (system requirements, installation of ProM plugin, stand-alone installation)

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
