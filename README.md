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
| ppm.zip     | Binaries (unzip to run). Java 8 64bit is required. |

## Why the new version of the Performance Spectrum Miner (PSM)?

The new version of the PSM supports the multi-channel Performance Spectrum (PS), introduced in the paper, while the previous version v1.0.x supports only a single channel of the PS. The multi-channel PS contains more information, so the PS data format was changed to 1) store multiple PS channels and 2) support large datasets of MHS (e.g., we have been working with event logs, which contain up to 250.000.000 events and lead to a multi-channel PS with 800.000 bins on the bin axis. Currently this version is available as a stand-alone tool, we plan to release the corresponding ProM plugin soon.

## Running the simulation model

|Class for running | Command line arguments|
| ------------- |:-------------:|
|`PreSorterStarterCli`| `output_directory days_to_simulate start_offset_hours duration_hours` |


For example, command line 

`java -cp ppm.jar org.processmining.scala.sim.conveyors.experiments.PreSorterStarterCli g:\logs 7 10 12` 

triggers simulation for 7 days, operating hours start at 10:00am, duration of operating hours is 12 hours.

## Building the PS

|Class for running | Command line arguments|
| ------------- |:-------------:|
|`PreSorterLogsToSegmentsApp`| `input_output_directory` |
|`SimSegmentsToSpectrumCli`| `input_directory days start_datetime bin_size_ms output_dir` |

This step is implemented in 2 sub-steps. First, segments are extracted from event log(s). Then a PS is built from the segment files.

For example, provide `g:\logs` for `PreSorterLogsToSegmentsApp`, then the following arguments:
`g:\logs 7 "01-09-2018 00:00:00.000" 20000 g:\logs\ps`
That will build the PS for 7 days, using data of the log starting from "01-09-2018 00:00:00.000", with time window size 20.000ms. The resulting PS will be stored in `g:\logs\ps`.

command line `g:\logs 7 10 12` triggers simulation for 7 days, operating hours start at 10:00am, duration of operating hours is 12 hours.

By default this step generates 3-channel PS: for grouping `start`, `pending`, `stop` and the configured in `SimSegmentsToSpectrumApp` classifier.



## Extracting the Training and Test sets

|Class for running | Command line arguments|
| ------------- |:-------------:|
|`SimSpectrumToDataset`| `ini_filename` |

This step has many parameters configured via an `ini` file.
Example of the ini-file:

`[GENERAL]`

`spectrumRoot = g:/debug/ps`  ; the PS directory

`datasetDir = g:/debug/data`  ; the output root training and test sets directory`

`experimentName = experiment_1` ; textual name of the dataset

`dayStartOffsetHours = 10` ; for each day of the dataset: offset, hours

`dayDurationHours = 12` ; for each day of the dataset: duration of operating hours, hours

`howFarInFutureBins = 6`  ; prediction horizon, bins

`historicalDataDurationBins = 4`       ; duration of the historic spectrum, bins

`historicSegments = A3_0:Link1_0 A2_0:A4_0 A1_0:A4_0`  ; historic segments

`targetSegments = E1.TO_SCAN_1_0:E2.SCAN_1`	; target segments

`binsPerLabel = 2`   ; duration of the target spectrum

`firstDayDateTime = 01-09-2018 00:00:00.000` ; start datetime for feature extraction

`totalDaysFromFirstDayInPerformanceSpectrum = 7` ; how many days should be extracted for the training and test sets

`daysNumberInTrainingValidationDataset = 5` ; how many days of  totalDaysFromFirstDayInPerformanceSpectrum should be used for the test set

## Model training

Before training the [PyTorch](https://pytorch.org/) framework should be installed and configure for your Python environment. The Python scripts for training are located [here](https://github.com/processmining-in-logistics/psm/tree/ppm/ppm/ML).

Model training can be configured in the main file `fnn_app.py`:

|Parameter | Description|
| ------------- |:-------------:|
|`is_linear`| Use a Logistic regression model if True and Feedforward (FF) NN if False|
|`hidden_layers`| Number of the hidden layers of the FF NN. Change also number of layers in the code of class `PreSorterFnn`|
|`hidden_dim`| Size of the hidden layers|
|`experiment_id`| Root folder of the training/test sets in the common folder for the datasets `..data`|
|`n_splits`| k for k-fold cross-validation|

There are more parameters for fine-grained tuning, their meaning is clear from naming.

As the output, the scripts summarize values of MSE, MAE and R squared and can also generate the set with predicted values for the test set (in the subfolder `test`). 




