import os
import statistics
import shutil
import torch
import torch.nn as nn
from numpy import genfromtxt
import numpy as np
from sklearn.metrics import mean_squared_error, mean_absolute_error
from sklearn.metrics import r2_score
from sklearn.model_selection import cross_validate, KFold
from skorch import NeuralNetRegressor
from torch.autograd import Variable

from my_utils import write_to_disk, save_model_parameters, read_dataset
from pre_sorter import PreSorterLinear
from pre_sorter_fnn import PreSorterFnn


def evaluation(model, x_test, y_test, learning_rate):
    model = model.eval()
    model = model.cuda()
    inputs = Variable(torch.from_numpy(x_test).cuda())
    labels = Variable(torch.from_numpy(y_test).cuda())
    criterion = nn.MSELoss()
    optimizer = torch.optim.SGD(model.parameters(), lr=learning_rate)
    # Clear gradients w.r.t. parameters
    optimizer.zero_grad()
    # Forward to get output
    outputs = model(inputs)
    # Calculate Loss
    loss = criterion(outputs, labels)
    return outputs.cpu(), loss.data.item()


def experiment(logger, model, x_train, y_train, epoch_number, logging_step, learning_rate):
    model = model.train()

    # model = torch.nn.DataParallel(model, device_ids=[0, 1, 2, 3, 4, 5])
    model = model.cuda()
    model = torch.nn.DataParallel(model)

    criterion = nn.MSELoss()
    optimizer = torch.optim.SGD(model.parameters(), lr=learning_rate)

    for epoch in range(epoch_number):
        inputs = Variable(torch.from_numpy(x_train).cuda())
        labels = Variable(torch.from_numpy(y_train).cuda())
        # Clear gradients w.r.t. parameters
        optimizer.zero_grad()
        # Forward to get output
        outputs = model(inputs)
        # Calculate Loss
        loss = criterion(outputs, labels)
        # Getting gradients w.r.t. parameters
        loss.backward()
        # Updating parameters
        optimizer.step()
        # Logging
        if epoch % logging_step == 0:
            logger('    epoch {}, loss {}'.format(epoch, loss.data.item()))
    return model


def start_experiment_fnn(baseline, is_linear, subdir, logger, dataset_dir, labels, features, features_dim,
                         labels_dim,
                         hidden_dim, epoch_number, logging_step, learning_rate,
                         n_splits,
                         labels_start, features_start, random_state=0, shuffle=True):
    logger(f'start_experiment_fnn random_state={random_state} shuffle={shuffle} n_splits={n_splits}')
    kf = KFold(n_splits=n_splits, random_state=random_state, shuffle=shuffle)
    iteration = 0
    mean_squared_error_list = list()
    mean_absolute_error_list = list()
    r2_score_list = list()
    test_mean_squared_error_list = list()
    test_mean_absolute_error_list = list()
    test_r2_score_list = list()
    test_baseline_mean_squared_error_list = list()
    test_baseline_mean_absolute_error_list = list()
    test_baseline_r2_score_list = list()
    for train_index, test_index in kf.split(features):
        x_train, x_test = features[train_index], features[test_index]
        y_train, y_test = labels[train_index], labels[test_index]
        baseline_test =  baseline[test_index]
        write_to_disk(f'{subdir}/baseline_{iteration}.csv', baseline_test)
        # print("TRAIN:", train_index, "TEST:", test_index)
        # print(f'x_train={x_train}')
        # print(f'x_test={x_test}')
        # print(f'y_train={y_train}')
        # print(f'y_test={y_test}')
        if is_linear:
            model = PreSorterLinear(features_dim, labels_dim)
            logger("Linear model")
        else:
            model = PreSorterFnn(features_dim, hidden_dim, labels_dim)
            logger("FNN model")

        model = experiment(logger, model, x_train, y_train, epoch_number, logging_step, learning_rate)
        (outputs, loss_value) = evaluation(model, x_test, y_test, learning_rate)
        outputs = outputs.cpu().detach().numpy()
        logger(f'Iteration={iteration}: loss {loss_value}')
        write_to_disk(f'{subdir}/y_test_{iteration}.csv', y_test)
        write_to_disk(f'{subdir}/outputs_{iteration}.csv', outputs)

        #logger(f'debug_baseline_test_mean_squared_error={mean_squared_error(y_test, baseline_test)}')
        mean_squared_error_val = mean_squared_error(y_test, outputs)
        mean_squared_error_list.append(float(mean_squared_error_val))
        mean_absolute_error_val = mean_absolute_error(y_test, outputs)
        mean_absolute_error_list.append(float(mean_absolute_error_val))
        r2_score_val = r2_score(y_test, outputs)
        r2_score_list.append(float(r2_score_val))
        logger(f'mean_squared_error={mean_squared_error_val}')
        logger(f'mean_absolute_error_val={mean_absolute_error_val}')
        logger(f'r2_score={r2_score_val}')
        model_params_filename = f'{subdir}/model_parameters_{iteration}.bin'
        save_model_parameters(model, model_params_filename)
        logger(f'model parameters are saved in {model_params_filename}')
        logger('\nTest:')
        test_subdir = f'{subdir}/{iteration}'
        os.makedirs(test_subdir, exist_ok=True)
        shutil.copy2(f'{dataset_dir}/test/baseline.csv', test_subdir)
        shutil.copy2(f'{dataset_dir}/test/max.csv', test_subdir)
        (test_mean_squared_error, test_mean_absolute_error, test_r2_score, test_baseline_mean_squared_error, test_baseline_mean_absolute_error, test_baseline_r2_score) = start_test(is_linear, dataset_dir, test_subdir, model_params_filename, logger, labels_dim, hidden_dim,
                                                                                                                                                                                     learning_rate,
                                                                                                                                                                                     labels_start, features_start)
        test_mean_squared_error_list.append(float(test_mean_squared_error))
        test_mean_absolute_error_list.append(float(test_mean_absolute_error))
        test_r2_score_list.append(float(test_r2_score))
        test_baseline_mean_squared_error_list.append(float(test_baseline_mean_squared_error))
        test_baseline_mean_absolute_error_list.append(float(test_baseline_mean_absolute_error))
        test_baseline_r2_score_list.append(float(test_baseline_r2_score))
        iteration = iteration + 1
    logger('\n\tSummary cross-validation:')
    print_metrics_lists(logger, mean_squared_error_list, mean_absolute_error_list, r2_score_list)
    logger('\n\tSummary test:')
    print_metrics_lists(logger, test_mean_squared_error_list, test_mean_absolute_error_list, test_r2_score_list)
    logger('\n\tSummary baseline:')
    print_metrics_lists(logger, test_baseline_mean_squared_error_list, test_baseline_mean_absolute_error_list, test_baseline_r2_score_list)

def print_metrics_lists(logger, mean_squared_error_list, mean_absolute_error_list, r2_score_list):
    logger('mean_squared_error=%0.4f +- %0.4f' %
           (statistics.mean(mean_squared_error_list), statistics.stdev(mean_squared_error_list)))
    logger('mean_absolute_error=%0.4f +- %0.4f' %
           (statistics.mean(mean_absolute_error_list), statistics.stdev(mean_absolute_error_list)))
    logger('r2_score=%0.4f +- %0.4f' %
           (statistics.mean(r2_score_list), statistics.stdev(r2_score_list)))

# def printMetricWithTrainScore(name, scores):
#     train = f'train_{name}'
#     test = f'test_{name}'
#     print("%s train/test:\t%0.4f (+/- %0.4f)\t%0.4f (+/- %0.4f)" % (
#         name, scores[train].mean(), scores[train].std() * 2, scores[test].mean(), scores[test].std() * 2))


# def printMetricWithoutTrainScore(name, scores):
#     test = f'test_{name}'
#     print("%s:\t%0.4f (+/- %0.4f)" % (
#         name, scores[test].mean(), scores[test].std() * 2))


# def start_experiment_fnn_skorch(experiment_id, dataset_dir, labels, features, features_dim,
#                                 labels_dim, hidden_dim,
#                                 epoch_number, logging_step, learning_rate, cv):
#     model = PreSorterFnn(features_dim, hidden_dim, labels_dim)
#     net = NeuralNetRegressor(
#         model,
#         criterion=nn.MSELoss,
#         lr=learning_rate,
#         max_epochs=epoch_number,
#         batch_size=-1
#
#     )
#     scoring = ['neg_mean_squared_error', 'r2']
#     scores = cross_validate(net, features, labels, scoring=scoring, cv=cv, return_train_score=False, n_jobs=-1)
#     print(sorted(scores.keys()))
#     printMetricWithoutTrainScore('neg_mean_squared_error', scores)
#     printMetricWithoutTrainScore('r2', scores)


def start_test(is_linear, dataset_dir, subdir, model_params_filename, logger, labels_dim, hidden_dim, learning_rate,
               labels_start, features_start):
    dataset_filename = f'{dataset_dir}/test/features.csv'
    (labels, features, features_dim) = read_dataset(logger, dataset_filename, labels_start, labels_dim, features_start)
    logger(f'{dataset_filename} loaded.')
    return evaluate(is_linear, subdir, model_params_filename, logger, labels, features, features_dim, labels_dim, hidden_dim,
             learning_rate)


def print_model(logger, model):
    for name, param in model.named_parameters():
        if param.requires_grad:
            logger(f'{name} {param.data}')


def evaluate(is_linear, dataset_dir, parameters_filename, logger, y_test, x_test, features_dim, labels_dim, hidden_dim,
             learning_rate):
    if is_linear:
        model = PreSorterLinear(features_dim, labels_dim)
        logger(f'Linear model is loaded from {parameters_filename}')
    else:
        model = PreSorterFnn(features_dim, hidden_dim, labels_dim)
        logger(f'FNN model is loaded from {parameters_filename}')

    model = model.cuda()
    model = torch.nn.DataParallel(model)
    model.load_state_dict(torch.load(parameters_filename))
    model.eval()
    print_model(logger, model)
    (outputs, loss_value) = evaluation(model, x_test, y_test, learning_rate)
    outputs = outputs.cpu().detach().numpy()
    logger(f'Loss value = {loss_value}')
    write_to_disk(f'{dataset_dir}/y_test.csv', y_test, '%.6f', ',')
    write_to_disk(f'{dataset_dir}/outputs.csv', outputs, '%.6f', ',')
    mean_squared_error_val = mean_squared_error(y_test, outputs)
    mean_absolute_error_val = mean_absolute_error(y_test, outputs)
    r2_score_val = r2_score(y_test, outputs)
    logger(f'mean_squared_error={mean_squared_error_val}')
    logger(f'mean_absolute_error_val={mean_absolute_error_val}')
    logger(f'r2_score={r2_score_val}')
    baseline = genfromtxt(f'{dataset_dir}/baseline.csv', dtype=np.float32)
    logger(f'baseline.shape={baseline.shape}')
    if baseline.shape[0] != y_test.shape[0]:
        raise ValueError(f'Bad baseline shape {baseline.shape}')
    baseline_mean_squared_error_val = mean_squared_error(y_test, baseline)
    baseline_mean_absolute_error_val = mean_absolute_error(y_test, baseline)
    baseline_r2_score_val = r2_score(y_test, baseline)
    logger(f'baseline mean_squared_error={baseline_mean_squared_error_val}')
    logger(f'baseline mean_absolute_error_val={baseline_mean_absolute_error_val}')
    logger(f'baseline r2_score={baseline_r2_score_val}')
    return mean_squared_error_val, mean_absolute_error_val, r2_score_val, baseline_mean_squared_error_val, baseline_mean_absolute_error_val, baseline_r2_score_val
