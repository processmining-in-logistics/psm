import torch.nn as nn


class PreSorterFnn(nn.Module):
    def __init__(self, input_dim, hidden_dim, output_dim):
        super(PreSorterFnn, self).__init__()

        # Linear function 1: input_dim --> hidden_dim
        self.fc1 = nn.Linear(input_dim, hidden_dim)
        # Non-linearity 1
        self.relu1 = nn.ReLU()

        # # Linear function 2
        # self.fc2 = nn.Linear(hidden_dim, hidden_dim)
        # # Non-linearity 2
        # self.relu2 = nn.ReLU()
        #
        # # Linear function 3
        # self.fc3 = nn.Linear(hidden_dim, hidden_dim)
        # # Non-linearity 3
        # self.relu3 = nn.ReLU()


        # Linear function 4 (readout): hidden_dim --> output_dim
        self.fc4 = nn.Linear(hidden_dim, output_dim)

    def forward(self, x):
        out = self.fc1(x)
        out = self.relu1(out)
        # out = self.fc2(out)
        # out = self.relu2(out)
        # out = self.fc3(out)
        # out = self.relu3(out)
        out = self.fc4(out)
        return out
