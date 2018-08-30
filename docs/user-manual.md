# Performance Spectrum Miner - User Manual

## Purpose

copy of intro paragraphs from README.md for completeness

## Overview

outline how the tool works in general (log as input, pre-process with various parameters explained later, stores to disk, then visualize with PSM, many interaction controls to explore)

## Importing Event Logs into Performance Spectrum

* input requirements: XES event log
* parameters for pre-processing
* briefly describe structure of storage and .psm file

## Visualizing Performance Spectrum

* how to load .psm
* how to read (TODO: Dirk), lines vs bars
* basic controls (scroll, zoom)
* aggregation (any in import)

## Exploring Performance Spectrum

* selection with mouse controls
* options panel for filtering

## Understanding Time Zones of Date/Time Shown in the PSM

The PSM relies on time zones in timestamps of XES files. While working with performance spectra, it shows the date and time of traces under the mouse pointer, converting them into system time of a user's OS. For example, if a timestamp in a XES event log is `30.08.2018 18:00:00 UTC` and the time zone of the user is `Europe/Amsterdam`, the PSM shows `30.08.2018 20:00:00 UTC`.

Sometimes a user may want to see date/time for a different time zone, for example, for a time zone where the log was recorded, let's say, `Australia/Sydney`. In this case a user should create text file `config.ini` in a folder that contains files of the pre-processed performance spectrum of the log, using any text editor, and add there the following lines:

`[GENERAL]`

`zoneId = Australia/Sydney`

Afterwards the dataset should be re-opened in the PSM. Possible values of zone IDs are available in [Java documentation](https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html)


