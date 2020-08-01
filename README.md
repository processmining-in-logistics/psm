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
* *PQR-System* - the panel visualizing the process model, called a PQR-system, that describes the Process, Queue and Resource dimensions of the simulated BHS
* *Performance Spectrum Miner-R* (PSM) - the tool for work with Performance Spectra (PSa)

The BHS System generates events in real-time and send them to the PSM. The PSM computes the PS in real time, using the PQR-system for the information about the process dimensions. The PQR-system additionally serves as a GUI for PS segments filtering and sorting. 

### MHS System
*Click the figure below to watch the BHS simulation model animation*

[![The visualization of the BHS simulation model](/docs/figures/sim_model.png)](https://youtu.be/O0_tjfRInFo)

The simulation model application allows to simulate normal work and scenarios with blockages for a pre-configured sorter of a BHS. The model allows to use various automated scenarios as well as interactive commands from the GUI.

The main window of the simulation model application is shown in the figure below, where: 
* sliders (1,2) allow horizontal and vertical zooming 
* field (3) shows current simulation time (from the start of the epoch) 
* button (4) pauses/resumes simulation. Note that recording event logs can be only used (copied) during a pause, i.e., after they are flushed to disk.
* text field and button (5) allow to send a command to the simulation engine to interactively block/unblock conveyors. Format for blocking: `block conveyor_id period_ms`, e.g., `block x 10000` to block conveyor `x` for 10.000 milliseconds, and `block conveyor_id`, e.g., `block x` to block conveyor `x` forever (or until explicit unblocking). Format for unblocking: `unblock conveyor_id`, e.g., `unblock x` to unblock conveyor `x`. 
* check boxes (6) allow to show/hide IDs of cases and their final destination.
* other GUI elements may be not implemented yet

### PQR-System

### Performance Spectrum Miner-R


The main window of the PSM is shown in the figure below, where: 
* (1) is the segment name
* sliders (2) allows horizontal scrolling
* sliders (3, 5) allow horizontal and vertical zooming 
* field (4) shows the absolute time under the mouse pointer in the performance spectrum
* list box (6) allows to select constraints for log repair 
* check boxes (7) allow to show/hide regions, their right borders and change their color schemas
* list box (8) allows to show/hide the ground truth if available (see below)
* check boxes (9) allow to show/hide load (base on a repaired log)
* check boxes (10) allow to show/hide ground truth load and computed error (if the ground truth is available))
* field and button (11) allow to set time-window size (in ms)

![PSM 1.2.x](/docs/figures/psm_regions_ui.png)


## Use case

## Limitations

## Other tools using performance spectra





