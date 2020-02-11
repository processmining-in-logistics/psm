# Repairing Event Logs with Missing Events to Support Performance Analysis of Systems with Shared Resources

*(under construction)*

## Quick Start

Binaries (for Java 8) are available [here](https://github.com/processmining-in-logistics/psm/releases/tag/1.2.0). 

Execute `start_sim.cmd` to run the simulation model of the Baggage Handling System (BHS) and click the play button. The app (over)writes the logs in the current directory. Click the pause button to save the logs and then open the logs in the PSM (the previous step).

Execute `start_psm.cmd` to run the PSM and open `open_me.xes` to open the provided logs. The logs must be in the same directory as the binaries.

*Click on the Fig. below to watch the BHS simulation model animation*
[![The visualization of the BHS simulation model](/docs/figures/sim_model.png)](https://www.youtube.com/watch?v=O0_tjfRInFo&feature=youtu.be)

## Simulation Model of Baggage Handling System

The simulation model application allows to simulate normal work and scenarios with blockages for a pre-configured sorter of a BHS. The model allows to use various automated scenarios as well as interactive commands from the GUI.

The main window of the simulation model application is shown in the figure below, where: 
* sliders (1,2) allow horizontal and vertical zooming 
* field (3) shows current simulation time (from the start of the epoch) 
* button (4) pauses/resumes simulation. Note that recording event logs can be only used (copied) during a pause, i.e., after they are flushed to disk.
* text field and button (5) allow to send a command to the simulation engine to interactively block/unblock conveyors. Format for blocking: `block conveyor_id period_ms`, e.g., `block x 10000` to block conveyor `x` for 10.000 milliseconds, and `block conveyor_id`, e.g., `block x` to block conveyor `x` forever (or until explict unblocking). Format for unblocking: `unblock conveyor_id`, e.g., `unblock x` to unblock conveyor `x`. 
* check boxes (6) allow to show/hide IDs of cases and their final destination.
* other GUI elements may be not implemented yet

![BHS simulation model GUI](/docs/figures/sim_model_ui.png)

The model records two event logs and several internally used files:
* `!incomplete_log.csv` is an incomplete log (to be repaired)
* `!standard_log.csv` is a complete event log
* files `overlaid_segments_2.csv`, `overlaid_segments_3.csv`, `overlaid_segments_4.csv` are used by PSM to visualize performance spectra computed using partially restored event logs and the ground truth on top.
* file `simple_sorter_system.csv` is used for testing

**All those files are required to be in the path of the PSM to be opened.**

## Performance Spectrum Miner 1.2.x (with Regions)

The Performance Spectrum Miner 1.2.x implements log repair and supports work with performance spectra built from logs with intervals for time-stamps. Additionaly it is possible to project the ground truth on top of the repaired data. Currently it is pre-configured for work with the output of the simulation model (above) and for work with real-life event logs of the same BHS fragment.

The main window of the PSM is shown in the figure below, where: 
* (1) is the segment name
* sliders (2) allows horizontal scrolling
* sliders (3, 5) allow horizontal and vertical zooming 
* field (4) shows the absolute time under the mouse pointer in the performance spectrum
* list box (6) allows to select constraints for log repair (see below)
* check boxes (7) allow to show/hide regions, their right borders and change their color schemas
* list box (8) allows to show/hide the ground truth (if available)
* check boxes (9) allow to show/hide load (base on a repaired log)
* check boxes (10) allow to show/hide ground thruth load and computed error (if the ground truth is available))
* field and button (11) allow to set time-window size (in ms)

![PSM 1.2.x](/docs/figures/psm_regions.png)

## Examples of Performance Spectra

### Performance Spectra Built on Repaired Logs of the Simulation Model

![Performance Spectrum, stable performance](/docs/figures/sim_stable_perf.png)

![Load and its error, stable performance](/docs/figures/sim_stable_perf_load.png)


![Performance Spectrum, unstable performance](/docs/figures/sim_unstable_perf.png)

![Load and its error, unstable performance](/docs/figures/sim_unstable_perf_load.png)

### Performance Spectra Built on Repaired Real-Life Logs of a BHS of a Large European Airport

![Performance Spectrum, mixed performance](/docs/figures/rl_mixed_perf.png)

![Load and its error, mixed performance](/docs/figures/rl_mixed_perf_load.png)




