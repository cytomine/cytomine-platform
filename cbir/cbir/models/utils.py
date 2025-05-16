"""Utilities functions for the model."""

import os
import torch
from contextlib import nullcontext

from cbir.config import Settings
from cbir.models.hoptimus import HOptimus
from cbir.models.model import Model
from cbir.models.resnet import Resnet


def get_model_class(name: str) -> type[Model]:
    """Get the model class given its name."""

    models = {
        "resnet": Resnet,
        "hoptim": HOptimus,
    }

    try:
        return models[name]
    except KeyError as exception:
        raise ValueError(f"Model {name} not found.") from exception


def load_model(settings: Settings) -> Model:
    """Load the model given by the settings."""

    model_class = get_model_class(settings.extractor)
    model = model_class(device=settings.device)

    # Load the default weights
    if settings.extractor == "resnet":
        state = torch.load("/app/weights/resnet", map_location=settings.device)
        model.load_state_dict(state, strict=True)

    # Load the custom weights if provided
    if os.path.exists(settings.weights):
        state = torch.load(settings.weights, map_location=settings.device)
        model.load_state_dict(state, strict=True)

    model.to(settings.device)

    return model


def resnet_forward(model: Model, inputs: torch.Tensor) -> torch.Tensor:
    """Forward pass for Resnet model."""

    with torch.no_grad():
        outputs = model(inputs)

    return outputs.cpu().numpy()


def hoptim_forward(model: Model, inputs: torch.Tensor) -> torch.Tensor:
    """Forward pass for H-Optimus model."""

    device = model.device

    amp_context = (
        torch.autocast(device_type='cuda', dtype=torch.float16)
        if model.device.type == 'cuda' else nullcontext()
    )

    with amp_context, torch.inference_mode():
        outputs = model(inputs)

    return outputs.cpu().numpy()


def run_inference(model, inputs):
    forwards = {
        HOptimus: lambda m, x: hoptim_forward(m, x),
        Resnet: lambda m, x: resnet_forward(m, x),
    }

    extractor = type(model)
    if extractor not in forwards:
        raise ValueError(f"Unsupported extractor: {extractor}")

    return forwards[extractor](model, inputs)
