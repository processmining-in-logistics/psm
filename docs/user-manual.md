# Performance Spectrum Miner - User Manual

## Purpose

copy of intro paragraphs from README.md for completeness

## Overview

outline how the tool works in general (log as input, pre-process with various parameters explained later, stores to disk, then visualize with PSM, many interaction controls to explore)

## Importing Event Logs into Performance Spectrum

### ProM Mode

  * import an event log in ProM using standard ProM capabilities (import of XES and CSV files)
  * select the imported event log and click button 'Use Resource'
  * select **Performance Spectrum Miner** in the list of plugins and click 'Start'
### Stand-Alone Mode
  * click button 'Open...'
    * follow the steps described in the next paragraph

## Parameters for Pre-Processing

An event log has to be pre-processed to obtain information required to draw its performance spectrum. Parameters required for pre-processing cna be configured in dialog 'Event Log Pre-Processing':
  * button **Open...** serves for selecting a XES event log file in the file open dialog (disabled in the ProM mode)
  * field **Bin size** allows to assign a time window size for calculating aggregated part of the performance spectrum. Provide size using the following keywords:

| Keyword        | Meaning           
| ------------- |:-------------
| mo      | month (30 days) 
| w      | week 
| d      | day 
| h      | hour
| m      | minute
| s      | second
| ms      | millisecond

Examples: 

| Line        | Meaning           
| ------------- |:-------------
| 1w 3d      | 10 days 
| 10m      | 10 minutes
| 3d      | 3 days

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

| Class value        | Interval
| ------------- |:-------------
| 0 | \[0; 0.5m\)
| 1 | \[0.5m; 1.5m\)
| 2 | \[1.5m; 2m\)
| 3 | \[2m; 3m\)
| 4 | \[3m; inf.)


* briefly describe structure of storage and .psm file

## Visualizing Performance Spectrum

* how to load .psm
* how to read (TODO: Dirk), lines vs bars
* basic controls (scroll, zoom)
* aggregation (any in import)

## Exploring Performance Spectrum

* selection with mouse controls
* options panel for filtering

## Understanding Time Zones of Time Shown in the PSM

The PSM relies on time zones in timestamps of XES files. While working with performance spectra, it shows the date and time of traces under the mouse pointer, converting them into system time of a user's OS. For example, if a timestamp in a XES event log is `30.08.2018 18:00:00 UTC` and the time zone of the user is `Europe/Amsterdam`, the PSM shows `30.08.2018 20:00:00 UTC`.

Sometimes a user may want to see date/time for a different time zone, for example, for a time zone where the log was recorded, let's say, `Australia/Sydney`. In this case a user should create text file `config.ini` in a folder that contains files of the pre-processed performance spectrum of the log, using any text editor, and add there the following lines:

`[GENERAL]`

`zoneId = Australia/Sydney`

Afterwards the dataset should be re-opened in the PSM. Possible values of zone IDs are available in [Java documentation](https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html)

You can always check a zone ID, which the PSM uses, in the log, enabling `INFO` logging messages:

`30-08-18 17:52:45,559 AppSettings.scala:21 [INFO ] zoneId = Europe/Berlin`

*It does not make sence to use time zone offsets instead of 'geographical' IDs, because an offset does not have information about daylight saving time of the location where events were recorded.*

