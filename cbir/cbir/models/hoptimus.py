"""H-Optimus models"""

import timm
import torch
from torch.nn.functional import normalize

from cbir.models.model import Model


class HOptimus(Model):
    """H-optimus-0 model"""

    def __init__(
        self,
        n_features: int = 1536,
        device: torch.device = torch.device("cpu"),
    ) -> None:
        super().__init__(n_features, device=device)

        self.model = timm.create_model(
            "hf-hub:bioptimus/H-optimus-0",
            pretrained=True,
            init_values=1e-5,
            dynamic_img_size=False,
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """Apply forward pass."""

        inputs = x.to(self.device)

        return normalize(self.model(inputs))
