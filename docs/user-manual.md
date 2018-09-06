# Performance Spectrum Miner - User Manual

## Purpose

copy of intro paragraphs from README.md for completeness

## Overview

outline how the tool works in general (log as input, pre-process with various parameters explained later, stores to disk, then visualize with PSM, many interaction controls to explore)


## Importing Event Logs into Performance Spectrum

### ProM Mode

1. Load the event log into ProM via the *Import...* button. ([screenshot](/figures/getting_started_prom_01_load_log.png))
1. Select the imported event log and click *Use Resource* or go to the *Action* Tab and select the *Performance Spectrum Miner* plugin from the action list  > Click *Start* ([screenshot](/figures/getting_started_prom_02_choose_psm.png))
1. Choose parameters for generating the performance spectrum data. A configuration dialog will show providing default values for the transformation (described below).
   * The transformed data will be stored on disk in the *Intermediate storage directory* together with a meta-data file (`session.psm`). You can load this transformed data also later into ProM by loading the `session.psm` meta-data file.
   * The transformation may require some time and main memory depending on the *Bin size* chosen. Transformation for larger bin sizes are faster and require less memory.
  
### Stand-Alone Mode
1. Load the event log (XES format) via the *Open...* button
1. Choose parameters for generating the performance spectrum data as described next.

## Parameters for Pre-Processing

An event log has to be pre-processed to obtain information required to draw its performance spectrum. Parameters required for pre-processing cna be configured in dialog 'Event Log Pre-Processing':
  * button **Open...** serves for selecting a XES event log file in the file open dialog (disabled in the ProM mode)
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

* values of combobox **Aggregation function** are explained in the following table:

| Function name        | Meaning           
| ------------- |:-------------
| Cases pending  | How many segments intersect a bin, or start/stop within a bin
| Cases started  | How many segments start within a bin
| Cases stopped  | How many segments stop within a bin

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
  
  * field **intermediate storage directory** specifies a path to an empty or non-existing folder where pre-processed data of a log performance spectrum will be stored. It's recommended to use meaningful names. Such pre-processed datasets can be re-used for opening performance spectra without the pre-processing step.
  
  * button **Process and open** starts pre-processing.

## Importing Pre-Processing Datasets

During the pre-processing step all required files are stored into an intermediate storage directory. Dialog 'Open pre-processing dataset' serves to configure the following parameters:
  * combobox **Activity aggregation (before/after)** allows to aggregate segments of a pre-processed performance spectrum as follows:

| Aggregation type        | Meaning
| ------------- |:-------------
| None  | No aggregation (default value)
| A->A  | All segments with identical starting activity are merged into one
| Any->A  | All segments with identical ending activity are merged into one

* combobox **Caching** llows to choose desired caching strategy:

| Caching strategy        | Meaning
| ------------- |:-------------
| Load on open  | All required data are loaded into memory while opening the dataset
| Load on demand  | Required segments are loaded on demand, while scrolling and zooming (recommended for large dataset that do not fit into memory)


### Files structure in intermediate storage directories

On the top level an intermediate storage directory contains the following files and directories:

| File name        | Data contained
| ------------- |:-------------
| dir. `data`  | A set of CSV files for every bin, each of which contains non-zero values of the chosen aggregation function for each segment and class
| dir. `segments`  | A set of CSV files for every bin, each of which contains case ID, timestamp, duration and class of segemnts that start in this bin
| file `max.csv`  | The file contains maximal values of the chosen aggregation function
| file `sorting_order.ini`  | User-defined soring order of segments (optional)
| file `aggregator.ini`  | User-defined activity aggregation (optional)
| file `config.ini`  | User-defined visualization parameters (optional)
| file `session.psm`  | An XML file that contains short information about the dataset and is used to import datasets into the PSM

 ## Opening already transformed data for analysis with the PSM

 The transformation step stores the data on disk together with some meta-data. This transformed data can be loaded in ProM and in the stand-alone version

1. By choosing *Process & open* during data transformation, the transformed data will be opened automatically. Alternatively, you can also load a previously transformed data set by opening the .psm meta-data file (via *Import* in ProM, and choosing *Performance Spectrum Miner View*, or via *Open* in the stand-alone version).
1. Choose parameters for opening. For now use the default values provided, the purpose of the other parameters is explained below. 

## Visualizing Performance Spectrum

![Main Windows of the Performance Spectrum Miner](/docs/figures/getting_started_exploring_01_main_view.png)

The main window of the Performance Spectrum Miner is divided into
1. a panel visualizing the performance spectrum of the event long
1. a control and filtering panel at the bottom that particular contains one sliders to scroll horizontally and two sliders to zoom vertically and horizontally

In the visualization panel, each horizontal segment shows how cases move over time (x-axis) from one activity to the next activity (y-axis). 

By default the visualization shows *Lines*. In the figure below, each colored line describes *one* case moving from *Send Fine* to *Insert Fine Notification*. The x-coordinates of the start and end point of each line visualize the moments in time when *Send Fine* and *Insert Fine Notification* occurred, respectively. The color of the line depends on the classification that was chosen in the transformation step, which can be retrieved via the *Legend* button in the control and filtering panel.

![A segment of the Performance Spectrum Miner](/docs/figures/getting_started_exploring_02_one_segment.png)

The performance spectrum shows among other things:
* There are cases that are processed very fast (near vertical dark-blue lines) and there are cases processed much slower (sloped lines in light-blue, yellow, and orange). 
* The slower cases all have in common that *Send Fine* occurred for them together with many other cases (at the same moment in time) in a batch, whereas *Insert Fine Notification* happened individually for each case. 
* Batching for *Send Fine* occurs at irregular intervals and the amount of cases per batch varies greatly over time.

While the *Lines* show the speed of cases, the amount of cases over time can be visualized by checking *Bars* in the control and filtering panel.

![A segment of the Performance Spectrum Miner](/docs/figures/getting_started_exploring_03_one_segment_bars.png)

The stacked bars provide aggreate information about how many cases started, ended, or were pending in particular time-window between the two activities of the segment. The parameters of this aggregation are chosen in the transformation step, see the [User Manual](docs/user-manual.md) for details. In the example above, the stacked bars show that the process experienced a very high amount of cases going from *Send Fine* to *Insert Fine Notification* in particular period (the exact time will be shown on the bottom left when hovering the mouse over the respective part of the visualization). The coloring indicates that in this period, the cases were processed much slower than in other period. The number *2988* in the label of the segment tells that there were at a maximum 2988 cases transitioning together through this part of the process.

## Exploring Performance Spectrum

The Performance Spectrum can be explored in various ways through selections (with mouse controls) and through filtering (via the options panel).

### Selection with Mouse Controls

to be added

### Filtering via the Options Panel

to be added

## Aggregation during Loading

to be added

## Advanced Features

### Custom Sorting Order

By default the PSM sort segments alphabetically. Quite often it is required to define another order, e.g. according a process model. A user can create text file `sorting_order.ini` in an intermediate storage directory and provide segment names in the required order, one name per line. Example:

`Create Fine:Payment`

`Create Fine:Send Fine`

`Send Fine:Insert Fine Notification`

### Activity-Based Aggregation

The PSM allows to rename activities or merge several activities into one. A user can create file `aggregator.ini` in an intermediate storage directory and provide aggregation rules there. This file must contain section `[MAPPING]`, which contains one line per every new activity. Each line starts from a name of a new activity and one or more regular expressions, separated by spaces. each expression defines a pattern for activities that should be aggregated into the new one. Example:

`[MAPPING]`

`NEW_ACTIVITY_NAME_1 a1 a2 a3`

`NEW_ACTIVITY_NAME_2 a4`

For that configuration the PSM will change activities  `a1 a2 a3` to `NEW_ACTIVITY_NAME_1` (aggregation) and `a4` to `NEW_ACTIVITY_NAME_2` (renaming). Such aggregation is performed in memory and not stored to the pre-processed files.

### Understanding Time Zones of Time Shown in the PSM

The PSM relies on time zones in timestamps of XES files. While working with performance spectra, it shows the date and time of traces under the mouse pointer, converting them into system time of a user's OS. For example, if a timestamp in a XES event log is `30.08.2018 18:00:00 UTC` and the time zone of the user is `Europe/Amsterdam`, the PSM shows `30.08.2018 20:00:00 UTC`.

Sometimes a user may want to see date/time for a different time zone, for example, for a time zone where the log was recorded, let's say, `Australia/Sydney`. In this case a user should create text file `config.ini` in a folder that contains files of the pre-processed performance spectrum of the log, using any text editor, and add there the following lines:

`[GENERAL]`

`zoneId = Australia/Sydney`

Afterwards the dataset should be re-opened in the PSM. Possible values of zone IDs are available in [Java documentation](https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html)

You can always check a zone ID, which the PSM uses, in the log, enabling `INFO` logging messages:

`30-08-18 17:52:45,559 AppSettings.scala:21 [INFO ] zoneId = Europe/Berlin`

*It does not make sense to use time zone offsets instead of 'geographical' IDs, because an offset does not have information about daylight saving time of the location where events were recorded.*

