# Predictive Performance Monitoring of Material Handling Systems Using the Performance Spectrum

## Event logs of the simulation model of a BHS

This branch contains materials for the paper (in submission). 

The BHS simulation event log and the computed Performance Spectrum (see the table below) are available [here](https://www.dropbox.com/sh/ueu5r2o5yg34ezk/AADmDbnVxgxsShVxhlHBWNQja?dl=0).
The source code is available in this branch of the PSM project.


| File        | Description     | 
| ------------- |:-------------:|
| PPM_BHS_Sim_log.zip     | The event log in XES and CSV formats. |
| PPM_BHS_Sim_PerfSpec_ProM.zip     | The computed Performance Spectrum in the format of the Performance Spectrum Miner  v.1.0.x, [available as a ProM plugin](https://github.com/processmining-in-logistics/psm) |
| PPM_BHS_Sim_PerfSpec.zip     | The computed Performance Spectrum in the format of the new version of the Performance Spectrum Miner  v.1.1.x |

$$ Why the new branch of the the Performance Spectrum Miner (PSM)?

The new version of the PSM supports the multi-channel Performance Spectrum (PS), introduced in the paper, while the previous version v1.0.x supports one channel of the PS. The multi-channel PS contains more information, so the PS data format was changed to 1) store multiple PS channels and 2) to support large datasets of MHS (e.g., we work with evant logs, which contain up to 250.000.000 events and PS with up to 800.000 bins. Currently this version is available as a stand-alone tool, we plan to release the corresponding ProM plugin soon.
