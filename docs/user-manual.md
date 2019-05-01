# Performance Spectrum Miner - User Manual

## Purpose

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

## Overview

Analyzing the Performance Spectrum of a process with the PSM has the following steps that are explained in the following.
1. **Importing** event log data into the PSM (different performance classifiers can be used).
   * The import allows for choosing various parameters to classify the performance in the process explained later.
   * The results of the import are stored on disk (together with meta-data information).
1. **Opening** the imported data for analysis with the PSM
1. **Exploring** the Performance Spectrum through
   * zooming and panning across the visualization
   * choosing different features to visualize
   * interactively selecting particular cases of the process to analyze
   * filtering of process steps to analyze
   * advanced features to aggregate and order data in a particular way

## Importing Event Logs into Performance Spectrum

### ... in ProM 

1. Load the event log into [ProM](http://www.promtools.org/) via the *Import...* button. ([screenshot](/docs/figures/getting_started_prom_01_load_log.png))
1. Select the imported event log and click *Use Resource* or go to the *Action* Tab and select the *Performance Spectrum Miner* plugin from the action list  > Click *Start* ([screenshot](/docs/figures/getting_started_prom_02_choose_psm.png))
1. Choose parameters for generating and storing the performance spectrum data as described below. 
   * In contrast to many other ProM-plugins, the imported performance spectrum data has to be stored on disk in an intermediate storage directory together with a meta-data file (`session.psm`). You can load this transformed data also later into ProM by loading the `session.psm` meta-data file.
  
### ... in the stand-alone PSM
1. Load the event log (XES format) via the *Open...* button  ([screenshot](/docs/figures/getting_started_prom_01_load_log.png))
1. Choose parameters for generating and storing the performance spectrum data as described next.

### Parameters for Importing

An event log has to be imported into a specific format to obtain information required to draw its performance spectrum. Parameters required for importing can be configured in the *Event Log Pre-Processing* dialog:

![Dialog for Importing Event Log into Performance Spectrum](/docs/figures/getting_started_psm_02_choose_parameters.png)

  * button **Open...** serves for selecting a XES event log file in the file open dialog (in ProM mode, the log is provided by the ProM framework)
  * field **Bin size** allows to assign a time window size for calculating aggregated part of the performance spectrum. Provide size using the following keywords:

| Keyword        | Meaning           
| ------------- |:-------------
| `mo`      | month (30 days) 
| `w`      | week 
| `d`      | day 
| `h`      | hour
| `m`      | minute
| `s`      | second
| `ms`      | millisecond

Examples: 

| Line        | Meaning           
| ------------- |:-------------
| `1w 3d`      | 10 days 
| `10m`      | 10 minutes
| `3d`      | 3 days

* values of combobox **Duration classifier** are explained in the following table:

| Function name        | Segments classification
| ------------- |:-------------
| Quartile-based  | A class value is assigned to a segment according a quartile number where its duration sits: 0 for the first quartile, 1 for the second and so on.
| Median-proportional  | A class value is assigned to a segment according to intervals, defined in terms of the median duration for the segment. The intervals are presented in the table below

| Class value        | Quartile (Quartile-based classifier) | Interval (Median-proportional classifier)
| ------------- |:------------- |:-------------
| `0` | Q1 | \[0; 0.5m\) 
| `1` | Q2 | \[0.5m; 1.5m\)
| `2` | Q3 | \[1.5m; 2m\)
| `3` | Q4 | \[2m; 3m\)
| `4` | - | \[3m; inf.)

  * field **Activity classifier** allows to override a default activity classifier in an XES event log file. In order to do that, a list of mandatory attributes, separated by spaces, should be provided. Example: `org:resource (case)_department`.
  
  * field **intermediate storage directory** specifies a path to an empty or non-existing folder where the performance spectrum data of the imported event data can be stored. 
     * The performance spectrum data is stored together with a `session.psm` meta-data file. 
     * The stored performance spectrum data can also be loaded directly by opening the `session.psm` file in the PSM (via *Import...* in ProM or *Open...* in the stand-alone mode).
     * We recommended to use descriptive directory names. 

  * button **Process and open** starts importing the event log and processing it into the performance spectrum data.
     * The time and memory needed for the transformation depends on the *Bin size* chosen. Imports for larger bin sizes are faster and require less memory.

 ## Opening performance spectrum data for analysis with the PSM

The import step stores the data on disk together with some meta-data. The stored data can be loaded in ProM and in the stand-alone version.

*  By choosing *Process & open* during data transformation, the transformed data will be opened automatically. 
*  Alternatively, you can also load a previously transformed data set by opening the `session.psm` meta-data file (via *Import* in ProM, and choosing *Performance Spectrum Miner View* plugin, or via *Open* in the stand-alone version).

The way the data is then opened in the PSM can be influenced in two ways

* By parameters in a dialog
* By additional configuration files in the intermediate storage directory of the performance spectrum data 

### Parameters for opening performance spectrum data

![Dialog for opening performance spectrum data for visualization](/docs/figures/getting_started_psm_03_open_dataset_parameters.png) 

  * combobox **Activity aggregation (before/after)** allows to aggregate segments of a pre-processed performance spectrum as follows:

| Aggregation type        | Meaning
| ------------- |:-------------
| None  | No aggregation (default value)
| A->A  | All segments with identical starting activity are merged into one
| Any->A  | All segments with identical ending activity are merged into one

* combobox **Caching** allows to choose desired caching strategy:

| Caching strategy        | Meaning
| ------------- |:-------------
| Load on open  | All required data are loaded into memory while opening the dataset
| Load on demand  | Required segments are loaded on demand, while scrolling and zooming (recommended for large dataset that do not fit into memory)


### Configuration files in intermediate storage directories (Advanced)

On the top level an intermediate storage directory contains the following files and directories:

| File name        | Data contained
| ------------- |:-------------
| dir. `data`  | A set of CSV files for every bin, each of which contains non-zero values of the chosen aggregation function for each segment and class
| dir. `segments`  | A set of CSV files for every bin, each of which contains case ID, timestamp, duration and class of segemnts that start in this bin
| file `max.csv`  | The file contains maximal values of the chosen aggregation function
| file `sorting_order.txt`  | User-defined soring order of segments (optional)
| file `aggregator.ini`  | User-defined activity aggregation (optional)
| file `config.ini`  | User-defined visualization parameters (optional)
| file `session.psm`  | An XML file that contains short information about the dataset and is used to import datasets into the PSM

#### Custom Sorting Order

By default the PSM sort segments alphabetically. Quite often it is required to define another order, e.g. according a process model. A user can create text file `sorting_order.txt` in an intermediate storage directory and provide segment names in the required order, one name per line. Example:

`Create Fine:Payment`

`Create Fine:Send Fine`

`Send Fine:Insert Fine Notification`

#### Activity-Based Aggregation

The PSM allows to rename activities or merge several activities into one. A user can create file `aggregator.ini` in an intermediate storage directory and provide aggregation rules there. This file must contain section `[MAPPING]`, which contains one line per every new activity. Each line starts from a name of a new activity and one or more regular expressions, separated by spaces. each expression defines a pattern for activities that should be aggregated into the new one. Example:

`[MAPPING]`

`NEW_ACTIVITY_NAME_1 a1 a2 a3`

`NEW_ACTIVITY_NAME_2 a4`

For that configuration the PSM will change activities  `a1 a2 a3` to `NEW_ACTIVITY_NAME_1` (aggregation) and `a4` to `NEW_ACTIVITY_NAME_2` (renaming). Such aggregation is performed in memory and not stored to the pre-processed files.

#### Understanding Time Zones of Time Shown in the PSM

The PSM relies on time zones in timestamps of XES files. While working with performance spectra, it shows the date and time of traces under the mouse pointer, converting them into system time of a user's OS. For example, if a timestamp in a XES event log is `30.08.2018 18:00:00 UTC` and the time zone of the user is `Europe/Amsterdam`, the PSM shows `30.08.2018 20:00:00 UTC`.

Sometimes a user may want to see date/time for a different time zone, for example, for a time zone where the log was recorded, let's say, `Australia/Sydney`. In this case a user should create text file `config.ini` in a folder that contains files of the pre-processed performance spectrum of the log, using any text editor, and add there the following lines:

`[GENERAL]`

`zoneId = Australia/Sydney`

Afterwards the dataset should be re-opened in the PSM. Possible values of zone IDs are available in [Java documentation](https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html)

You can always check a zone ID, which the PSM uses, in the log, enabling `INFO` logging messages:

`30-08-18 17:52:45,559 AppSettings.scala:21 [INFO ] zoneId = Europe/Berlin`

*It does not make sense to use time zone offsets instead of 'geographical' IDs, because an offset does not have information about daylight saving time of the location where events were recorded.*



## Visualizing Performance Spectrum

![Main Windows of the Performance Spectrum Miner](/docs/figures/getting_started_exploring_01_main_view.png)

The main window of the Performance Spectrum Miner is divided into
1. a panel visualizing the performance spectrum of the event long
1. a control and filtering panel at the bottom that particular contains one sliders to scroll horizontally and two sliders to zoom vertically and horizontally

In the visualization panel, each horizontal segment shows how cases move over time (x-axis) from one activity to the next activity (y-axis). 

By default the visualization shows *Lines*. In the figure below, each colored line describes *one* case moving from *Send Fine* to *Insert Fine Notification*. The x-coordinates of the start and end point of each line visualize the moments in time when *Send Fine* and *Insert Fine Notification* occurred, respectively. The color of the line depends on the classification that was chosen in the transformation step, which can be retrieved via the *Legend* button in the control and filtering panel.

![A segment of the Performance Spectrum Miner showing Lines](/docs/figures/getting_started_exploring_02_one_segment.png)

The performance spectrum shows among other things:
* There are cases that are processed very fast (near vertical dark-blue lines) and there are cases processed much slower (sloped lines in light-blue, yellow, and orange). 
* The slower cases all have in common that *Send Fine* occurred for them together with many other cases (at the same moment in time) in a batch, whereas *Insert Fine Notification* happened individually for each case. 
* Batching for *Send Fine* occurs at irregular intervals and the amount of cases per batch varies greatly over time.

While the *Lines* show the speed of cases, the amount of cases over time can be visualized by checking *Bars* in the control and filtering panel.

![A segment of the Performance Spectrum Miner showing Bars](/docs/figures/getting_started_exploring_03_one_segment_bars.png)

The stacked bars provide aggreate information about how many cases started, ended, or were pending in particular time-window between the two activities of the segment. A grouping can be chosen by the combo box: 

| Grouping name        | Meaning           
| ------------- |:-------------
| No bars  | Bars are hidden
| Intersections (pending)  | How many segments intersect a bin
| Starts  | How many segments start within a bin
| Ends  | How many segments stop within a bin
| Sum | The sum of all the groupings

In the example above, the stacked bars show that the process experienced a very high amount of cases going from *Send Fine* to *Insert Fine Notification* in particular period (the exact time will be shown on the bottom left when hovering the mouse over the respective part of the visualization). The coloring indicates that in this period, the cases were processed much slower than in other period. The number *2988* in the label of the segment tells that there were at a maximum 2988 cases transitioning together through this part of the process.

## Exploring Performance Spectrum

The Performance Spectrum can be explored in various ways through selections (with mouse controls) and through filtering (via the options panel).

### Filtering via the Options Panel

![Filtering using the Performance Spectrum Miner](/docs/figures/getting_started_exploring_05_filtering.png)

* **Filter in** allows to show only those segments whose activities match the given regular expression. All non-matching segments are excluded from the view. 
    * Each segment name has the form `first_activity:second_activity` with colon (`:`) as separator
    * Multiple matching phrases can be concatenated using semi-colon (`;`) 
    * Examples:
      * `.*Pay.*` will include all segments involving activities containing the word 'Pay'
      * `Payment:.*` will include all segments starting with activity 'Payment' and leading to some other activity
      * `.*Pay.*;.*Fine.*` will include all segments involving activities containing the word 'Pay' or the word 'Fine'
* **Filter out** allows to remove all segments whose activities match the given regular expression. All non-matching segments remain in the view. The expression notation is the same as for *Filtering in*
* **Case ID** allows to show only cases matching the given case ID specified as comma-separated list. All non-matching cases will be shown as grey lines in the performance spectrum.
    * **Load IDs...** allows to load a list of case IDs from a `.txt` file
    * **Clear IDs** allows to clear the selection of cases so that all cases will be shown again in the performance spectrum view
* The **Throughput** fields allow to specify the lower and upper bound for the maximum throughput of cases (starting/pending/ending cases) in a segment in a time window. Segments who have less/more than the given maximum throughput will be filtered from the view.    
* Selecting the **Reverse colors order** changes the order in which colors are rendered in the performance spectrum
    * For *Bars* the bottom classes are now placed at the top of the stacked bars
    * For *Lines* the lines previous rendered first are now rendered last, placing them on top of the other lines and making them visible. 
   
### Selection with Mouse Controls

![Selecting cases using the Performance Spectrum Miner](/docs/figures/getting_started_exploring_06_selecting.png)

* **Clicking the left mouse button** on the visualization panel shows a context menu with the classes of the classification chosen in the transformation step. Selecting one of them shows only the cases of this class, e.g., only cases whose performance is in the 1st quartile of each segment.
* **Right-clicking and dragging a selection box** around cases in one segment allows to highlight the selected cases in all other segments (the non-selected cases will be shown in grey). 
* The **Clear** button in the control panel removes this selection.

## Look and Feel

### Font of Segment Names

To modify segments names font or colors of line, create/edit file `config.ini` in your dataset root directory and specify font name and/or size in section `GENERAL`, for example:

`[GENERAL]`

`fontSize = 45`

`fontName = Courier New`

`paletteId` = 1

Here `paletteId` corresponds to several pre-defined palettes, use numbers from 0 to 3.
