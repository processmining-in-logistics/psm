import sys
import time
import torch
import numpy as np
from numpy import genfromtxt


def read_dataset(logger, filename, labels_start, labels_dim, features_start, delimiter=','):
    data = genfromtxt(filename, delimiter=delimiter, dtype=np.float32)
    features_end = data.shape[1]
    features_dim = features_end - features_start
    logger(f"Rows={data.shape[0]} Columns={data.shape[1]} LabelsDim={labels_dim} FeaturesDim={features_dim} ")
    labels = data[:, labels_start:(labels_start+labels_dim)]
    features = data[:, features_start:features_end]
    logger(f"Labels shape={labels.shape} type={labels.dtype}")
    logger(f"Features shape={features.shape}  type={features.dtype}")
    return labels, features, features_dim


def write_to_disk(filename, t, fmt='%.4f', delimiter=';', flags='wb'):
    handle = open(filename, flags)
    np.savetxt(handle, t, fmt=fmt, delimiter=delimiter)
    handle.close()


class MyLogger:
    def __init__(self, filename, flags='wt', flush_every_time=True):
        self.handle = open(filename, flags)
        self.flush_every_time = flush_every_time

    def __call__(self, s, file=sys.stdout):
        msg = f'{time.strftime("%Y-%m-%d %H:%M:%S")} {s}'
        print(msg, file=self.handle)
        print(msg, file=file)
        if self.flush_every_time:
            self.flush()

    def flush(self):
        self.handle.flush()

    def close(self):
        self.handle.close()


def save_model_parameters(the_model, path):
    torch.save(the_model.state_dict(), path)
