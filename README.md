# Multi-Dimensional Performance Analysis and Monitoring Using Integrated Performance Spectra

*The page is under constraction!*

This tutorial explains how to use the tool presented in V. Denisov, D. Fahland, and W. M. P. van der Aalst, “Multi-Dimensional Performance Analysis and Monitoring Using Integrated Performance Spectra,” in ICPM 2020 demo track (in submission).

## Links

* This documentation as a pdf file is available here: [XXX](https://github.com/processmining-in-logistics/psm/tree/pqr).
* The source code and documentation are available here: [https://github.com/processmining-in-logistics/psm/tree/pqr](https://github.com/processmining-in-logistics/psm/tree/pqr).
* Click the figure below to watch the demo ([https://youtu.be/NI6to1pstKM](https://youtu.be/NI6to1pstKM))
[![Multi Dimensional Performance Analysis and Monitoring Using Integrated Performance Spectra](/docs/figures/components_screenshot.png)](https://youtu.be/NI6to1pstKM)


## How to Install and Run

### System requirements

  * Microsoft Windows 7 or higher. The PSM is *not tested* yet on other OS.
  * 4 GB RAM
  * 200MB hard disk space
  * It is recommended to use 3 monitors for 3 components of the environment.
  
### Prerequisite: Java 8

The PSM is implemented and tested with Java 8 and is not compatible with previous Java version (e.g. with Java 7).

1. Install the most recent JRE/JDK 1.8 64bit
1. Make sure that a correct installation of Java is configured: execute `java -version` in the command line. You should get a response like this:

`java version "1.8.0_251"`

`Java(TM) SE Runtime Environment (build 1.8.0_251-b08)`

`Java HotSpot(TM) 64-Bit Server VM (build 25.2511-b08, mixed mode)`

### How to run

1. Download and unzip the [binaries](https://github.com/processmining-in-logistics/psm/tree/ppm).
1. Execute `run_all.cmd` to run the tool components

## Components

The tool consists of three components:

* *MHS System* - the simulation model of a fragment of an airport Baggage Handling System (BHS), a sub-class of Material handling Systems (MHSs), with animation of executing scenarios.
* *PQR-System* - the panel of a process model, called a PQR-system, that describes the Process, Queue and Resource dimensions of BHS
* *Performance Spectrum Miner-R* (PSM) - the tool for work with Performance Spectra (PSa)

The BHS System generates events in real-time and send them to the PSM. The PSM computes the PS in real time, using the PQR-system for the information about the process dimensions. The PQR-system additionally serves as a GUI for PS segments filtering and sorting.


## Use case

## Limitations

## Other tools using performance spectra





