#  Copyright 2023 Cytomine ULiège
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

"""Resnet models"""

import torch
from torch.nn import Linear
from torch.nn.functional import normalize
from torchvision.models import resnet50

from cbir.models.model import Model


class Resnet(Model):
    """Resnet50 model"""

    def __init__(
        self,
        n_features: int = 128,
        device: torch.device = torch.device("cpu"),
    ) -> None:
        super().__init__(n_features, device=device)

        self.model = resnet50(weights="ResNet50_Weights.DEFAULT")
        self.model.fc = Linear(self.model.fc.in_features, n_features)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """Apply forward pass."""
        inputs = x.to(self.device)

        return normalize(self.model(inputs))
