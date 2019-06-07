import resource
import time
import sys
import traceback
from os import makedirs
import torch
from experiment import start_experiment_fnn, evaluate
from my_utils import read_dataset, MyLogger
import numpy as np
from numpy import genfromtxt


labels_start = 0
labels_dim = 1
features_start = labels_start + labels_dim
hidden_dim = 50
hidden_layers = 1 # change also in class PreSorterFnn!!!

epoch_number = 10000
is_linear = False
logging_step = 250

#experiment_id = '!al_32_off10_d10_start27-09-2017 00_00_00.000_days59_timeToPredict2_historicalData7_binsPerLabel3'
experiment_id = 'experiment_1_off10_d12_start01-09-2018 00_00_00.000_days5_timeToPredict6_historicalData4_binsPerLabel2'
#experiment_id = 'Scan2_off10_d12_start01-09-2018 00_00_00.000_days5_timeToPredict6_historicalData4_binsPerLabel2'

dataset_dir = f'data/{experiment_id}'
learning_rate = 0.01
n_splits = 3
dataset_filename = 'features.csv'

if is_linear:
    learning_rate = 0.01
else:
    learning_rate = 0.1

try:
    app_logger = MyLogger(f'{dataset_dir}/fnn_app_log.txt', 'at')
    try:
        app_logger(f'Python {sys.version_info}. Pytorch {torch.__version__}')
        app_logger(f'experiment_id: {experiment_id}')
        baseline_filename = f'{dataset_dir}/baseline.csv'
        app_logger(f'Loading {baseline_filename}...')
        baseline = genfromtxt(baseline_filename, delimiter=',', dtype=np.float32)
        app_logger(f'Done.')

        app_logger(f'Loading {dataset_dir}/{dataset_filename}...')
        (labels, features, features_dim) = read_dataset(app_logger, f'{dataset_dir}/{dataset_filename}', labels_start,
                                                        labels_dim, features_start)
        app_logger(f'{dataset_dir}/{dataset_filename} loaded.')
        if is_linear:
            model_name = f'linear_{features_dim}-{labels_dim}'
        else:
            model_name = f'fnn_{features_dim}x{hidden_dim}x{labels_dim}xx{hidden_layers}'
        time_prefix = time.strftime("%Y-%m-%d_%H_%M_%S")
        subdir = f'{dataset_dir}/output/{time_prefix}_{experiment_id}_{model_name}_{dataset_filename}_e{epoch_number}_splits{n_splits}'
        app_logger(f'subdir = {subdir}')
        makedirs(subdir, exist_ok=True)
        log_filename = f'{subdir}/log.txt'
        logger = MyLogger(log_filename)
        app_logger(f'Experiment log {log_filename} created')
        logger(f'Started. Max. resident set size={int(resource.getrusage(resource.RUSAGE_SELF).ru_maxrss / 1024)}MB')
        logger(f'experiment_id={experiment_id}')
        logger(f'model_name={model_name}')
        logger(f'dataset_dir={dataset_dir}')
        logger(f'dataset_filename={dataset_filename}')
        logger(f'labels_dim={labels_dim}')
        logger(f'features_start={features_start}')
        logger(f'epoch_number={epoch_number}')
        logger(f'logging_step={logging_step}')
        logger(f'learning_rate={learning_rate}')
        logger(f'n_splits={n_splits}')
        logger(f'is_linear={is_linear}')

        start_experiment_fnn(baseline, is_linear, subdir, logger, dataset_dir, labels, features, features_dim,
                             labels_dim,
                             hidden_dim, epoch_number,
                             logging_step, learning_rate, n_splits,
                             labels_start, features_start, 0, True)
        logger(f'Done. Max. resident set size={int(resource.getrusage(resource.RUSAGE_SELF).ru_maxrss / 1024)}MB')
        logger.close()
    except Exception as e:
        app_logger(traceback.format_exc(), file=sys.stderr)
    app_logger('App is done.')
    app_logger.close()
except Exception as e:
    print(traceback.format_exc(), file=sys.stderr)

# start_experiment_fnn_skorch(experiment_id, dataset_dir, labels, features, features_dim, labels_dim,
#                             hidden_dim, epoch_number,
#                             logging_step, 0.1, 3)
