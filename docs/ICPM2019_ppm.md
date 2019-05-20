# Predictive Performance Monitoring of Material Handling Systems Using the Performance Spectrum

This document contains additional materials for paper [Predictive Performance Monitoring of Material Handling Systems Using the Performance Spectrum (ICPM2019)](https://www.researchgate.net/publication/332877292_Predictive_Performance_Monitoring_of_Material_Handling_Systems_Using_the_Performance_Spectrum) (in press). 


## Event logs of the simulation model of a Baggage Handling System (BHS)

The BHS simulation event log and the computed Performance Spectrum (see the table below) are available along with other release files [here](https://github.com/processmining-in-logistics/psm/releases/tag/1.1.6). The source code is available in this branch of the PSM project.


| File        | Description     | 
| ------------- |:-------------:|
| PPM_BHS_Sim_log.zip     | The event log in the XES and CSV formats. |
| PPM_BHS_Sim_PerfSpec.zip     | The computed Performance Spectrum in the format of the Performance Spectrum Miner  v.1.1.6 |
| perf_spec-assembly-1.1.6.jar | The PSM 1.1.6
| ppm-assembly-1.1.6.jar     | The binaries for running the command line scripts |



## Running the simulation model

|Class for running | Command line arguments|
| ------------- |:-------------:|
|`org.processmining.scala.sim.conveyors.experiments.PreSorterStarterCli`| `output_directory days_to_simulate start_offset_hours duration_hours` |


For example, command line 

`java -cp ppm-assembly-1.1.6.jar org.processmining.scala.sim.conveyors.experiments.PreSorterStarterCli g:\logs 7 10 12` 

triggers simulation for 7 days, operating hours start at 10:00am, duration of operating hours is 12 hours.

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


## Data-Driven Inter-Case Feature Encoding

The [Data-Driven Inter-Case Feature Encoding](https://www.sciencedirect.com/science/article/pii/S0306437918300292?via%3Dihub) is implemented in class `DdeInterCaseFeatureEncoder`, package [`org.processmining.scala.prediction.preprocessing.ppm_experiments.intercase`](https://github.com/processmining-in-logistics/psm/tree/ppm/ppm/src/main/scala/org/processmining/scala/intercase). The sample code is in `ProductionLogExample`.




