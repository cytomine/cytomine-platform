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

"""Environment parameters"""

import os

import torch
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Configurable settings."""

    # Faiss index
    filename: str = "db"
    data_path: str = "/data"

    # Database
    host: str = "localhost"
    port: int = 6379
    db: int = 0

    # Deep learning model
    device: torch.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    extractor: str = "resnet"
    n_features: int = 128
    weights: str = f"/weights/{extractor}"

    def get_database_path(self) -> str:
        """Get the database path."""
        return os.path.join(self.data_path, self.filename)


def get_settings() -> Settings:
    """
    Get the settings.

    Returns:
        (Settings): The environment settings.
    """
    return Settings()
