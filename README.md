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

![BHS simulation model GUI](/docs/figures/sim_model_ui.png)

The main window of the simulation model application is shown in the figure below, where: 
* sliders (1,2) allow horizontal and vertical zooming 
* field (3) shows current simulation time (from the start of the epoch) 
* button (4) pauses/resumes simulation. Note that recording event logs can be only used (copied) during a pause, i.e., after they are flushed to disk.
* text field and button (5) allow to send a command to the simulation engine to interactively block/unblock conveyors. Format for blocking: `block conveyor_id period_ms`, e.g., `block x 10000` to block conveyor `x` for 10.000 milliseconds, and `block conveyor_id`, e.g., `block x` to block conveyor `x` forever (or until explicit unblocking). Format for unblocking: `unblock conveyor_id`, e.g., `unblock x` to unblock conveyor `x`. 
* check boxes (6) allow to show/hide IDs of cases and their final destination.
* other GUI elements may be not implemented yet

### PQR-System

![PQR-System](/docs/figures/pqr-system.png)

As shown in the figure above, the PQR-System panel has the following controls:
1. Zoom
2. Rotation
3. Text field for searching model elements
4. Check-box to show/hide transition labels of Q- and R-proclets
5. Check-box to show/hide transitions of Q- and R-proclets

A user can add/remove segments to the PSM by left/right clicking on places and transitions.
The complete visulalization of the PQR-system is shown in figures (a,b), the views where Q- and R-proclets are hideen are shown in figures (c,d).

### Performance Spectrum Miner-R

![PQR-System](/docs/figures/psm.png)

As shown in the figure above, the PSM has the following controls (relevant to the tool described in this document):

1. Vertical zoom
2. Position
3. Horizontal zoom
4. Check-box to show/hide ongoing segments, where the end timestamps are estimated as fasted possible. Ongoing segments are only estimated for segments whithout choice in the model, e.g., when some activity *a* is only followed by some activity *b*, we estimate end timestamps of occurrences of this segment (a,b). If more activities can follow *a*, we do not estimate as the next activity is unknown. Hiding ongoing segments improves the GUI response.
5. Start button, press it once to allow the PSM to listen to the simulation model events.


Additional functionality is available through mouse clicking:

* Click once on a segment to locate it in the PQR-system visualization. The PQR-System will try to scroll the view port to have a transition related to the first segment activity at the top left corner of the window.
* Double-click on a segment to add the corresponding Q- and R- segments (works only for segments of the P-proclet).
* Right-click on a segment to remove a segment
* Drag and drop segments for sorting


Note that PSM-R is a proof-of-concept implementation that computes spectra in real-time in memory. The stable version of the 'standard' PSM is available [here](https://github.com/processmining-in-logistics/psm/).

## Use case

We consider the same use case as in the video above step by step.

1. Run all the components and choose a suitable layout on you monitor(s).
2. Click Start button in the PSM.
3. Click button '>'  in the Simulation Model.
4. Wait 1-2 minutes until bags reach all the system conveyor.
5. Type command 'block z 100000' and click Send in the Simulation Model.
6. Observe the blocking development in both token animation of the Simulation Model and in the PSM.
7. When normal work resumes, pause the simulation by clicking button '>'  in the Simulation Model again.
8. Identify the segment with the earliest delay, click on this segment to locate it in the model, and double-click to show the corresponding Q- and R-segments.

The root cause of the performance incident is in the bag.


## Troubleshooting

The tool uses UDP ports 60000-60002.
Other software of your computer might also use them.
You can change ports by providing file `config.ini` with the following lines, providing other port numbers:
* `[GENERAL]`
* `psmServerPort = 60000`
* `pqrServerPort = 60001`
* `simServerPort = 60002`




