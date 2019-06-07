import torch.nn as nn


class PreSorterLinear(nn.Module):
    def __init__(self, input_dim, output_dim):
        super(PreSorterLinear, self).__init__()
        self.linear = nn.Linear(input_dim, output_dim)

    def forward(self, x):
        out = self.linear(x)
        return out
