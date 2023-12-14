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

"""Database communications"""

import os
from typing import List, Tuple

import faiss
import numpy
import torch
from redis import Redis  # type: ignore

from cbir.config import DatabaseSetting
from cbir.models.model import Model


class Database:
    """Database to store the indices."""

    def __init__(
        self,
        settings: DatabaseSetting,
        n_features: int,
        gpu: bool = False,
    ) -> None:
        """Database initialisation."""
        self.settings = settings
        self.redis = Redis(host=settings.host, port=settings.port, db=settings.db)
        self.gpu = gpu

        # Check if the database exists
        if os.path.isfile(settings.filename):
            self.index = faiss.read_index(settings.filename)
        else:
            self.index = faiss.IndexFlatL2(n_features)
            self.index = faiss.IndexIDMap(self.index)

            self.redis.flushdb()
            self.redis.set("last_id", 0)

        if gpu:
            self.resources = faiss.StandardGpuResources()
            self.index = faiss.index_cpu_to_gpu(self.resources, 0, self.index)

    def save(self) -> None:
        """Save the index to the file."""

        index = faiss.index_gpu_to_cpu(self.index) if self.gpu else self.index
        faiss.write_index(index, self.settings.filename)

    def add(self, images: torch.Tensor, names: List[str]) -> None:
        """Index images."""
        last_id = int(self.redis.get("last_id").decode("utf-8"))
        self.index.add_with_ids(
            images,
            numpy.arange(last_id, last_id + images.shape[0]),
        )

        for name in names:
            self.redis.set(str(last_id), name)
            self.redis.set(name, str(last_id))
            last_id += 1

        self.redis.set("last_id", last_id)

    def remove(self, name: str) -> None:
        """Remove an image from the index database."""
        key = self.redis.get(name).decode("utf-8")
        label = int(key)

        id_selector = faiss.IDSelectorRange(label, label + 1)

        if self.gpu:
            self.index = faiss.index_gpu_to_cpu(self.index)

        self.index.remove_ids(id_selector)
        self.save()
        self.redis.delete(key)
        self.redis.delete(name)

        if self.gpu:
            self.index = faiss.index_cpu_to_gpu(self.resources, 0, self.index)

    def index_image(self, model: Model, image: torch.Tensor, filename: str) -> None:
        """Index an image."""

        # Create a dataset of one image
        inputs = torch.unsqueeze(image, dim=0)

        with torch.no_grad():
            outputs = model(inputs.to(model.device)).cpu().numpy()

        self.add(outputs, [filename])
        self.save()

    def search_similar_images(
        self,
        model: Model,
        query: torch.Tensor,
        nrt_neigh: int = 10,
    ) -> Tuple[List[str], List[float]]:
        """Search similar images given a query image."""

        inputs = torch.unsqueeze(query, dim=0)

        with torch.no_grad():
            outputs = model(inputs).cpu().numpy()

        distances, labels = self.index.search(outputs, nrt_neigh)
        distances, labels = distances.squeeze(), labels.squeeze()

        # Return only valid results
        stop = labels.tolist().index(-1) if -1 in labels else len(labels)
        filenames = [self.redis.get(str(l)).decode("utf-8") for l in labels[:stop]]

        return filenames, distances[:stop]
