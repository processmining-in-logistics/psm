# Predictive Performance Monitoring of Material Handling Systems Using the Performance Spectrum

## Event logs of the simulation model of a Baggage Handling System (BHS)

This branch contains materials for the paper (in submission). 

The BHS simulation event log and the computed Performance Spectrum (see the table below) are available [here](https://www.dropbox.com/sh/ueu5r2o5yg34ezk/AADmDbnVxgxsShVxhlHBWNQja?dl=0).
The source code is available in this branch of the PSM project.


| File        | Description     | 
| ------------- |:-------------:|
| PPM_BHS_Sim_log.zip     | The event log in the XES and CSV formats. |
| PPM_BHS_Sim_PerfSpec_ProM.zip     | The computed Performance Spectrum in the format of the Performance Spectrum Miner  v.1.0.x, [available as a ProM plugin](https://github.com/processmining-in-logistics/psm) |
| PPM_BHS_Sim_PerfSpec.zip     | The computed Performance Spectrum in the format of the new version of the Performance Spectrum Miner  v.1.1.x |

## Why the new version of the Performance Spectrum Miner (PSM)?

The new version of the PSM supports the multi-channel Performance Spectrum (PS), introduced in the paper, while the previous version v1.0.x supports only a single channel of the PS. The multi-channel PS contains more information, so the PS data format was changed to 1) store multiple PS channels and 2) support large datasets of MHS (e.g., we have been working with event logs, which contain up to 250.000.000 events and lead to a multi-channel PS with 800.000 bins on the bin axis. Currently this version is available as a stand-alone tool, we plan to release the corresponding ProM plugin soon.

## How to run the simulation model, build the PS and extract training and test sets

### Runningthe simulation model

|Class for running | Command line arguments|
| ------------- |:-------------:|
|`PreSorterStarterCli`| `output_directory days_to_simulate start_offset_hours duration_hours` |

For example, command line `g:\logs 7 10 12` triggers simulation for 7 days, operating hours start at 10:00am, duration of operating hours is 12 hours.
