"""Indexer class for indexing images and their features."""

import os
from typing import List, Tuple

import faiss
import numpy as np
import torch


class Indexer:
    """Indexer class for indexing images and their features."""

    def __init__(
        self,
        data_path: str,
        storage_name: str,
        index_name: str,
        n_features: int,
        gpu: bool = False,
    ) -> None:
        """
        Indexer initialisation.

        Args:
            data_path (str): Path to the base storage.
            n_features (int): Number of features in the index.
            storage_name (str): The name of the storage.
            index_name (str): The name of the index.
            gpu (bool): Whether to use GPU for indexing or not.
        """

        self.n_features = n_features
        self.gpu = gpu

        self.index_path = os.path.join(data_path, storage_name, index_name)

        if os.path.isfile(self.index_path):
            self.index = faiss.read_index(self.index_path)
        else:
            self.index = faiss.IndexFlatL2(n_features)
            self.index = faiss.IndexIDMap(self.index)

        if self.gpu:
            self.resources = faiss.StandardGpuResources()
            self.index = faiss.index_cpu_to_gpu(self.resources, 0, self.index)

    def save(self) -> None:
        """Save the index to a file."""

        index = faiss.index_gpu_to_cpu(self.index) if self.gpu else self.index
        faiss.write_index(index, self.index_path)

    def add(self, last_id: int, images: torch.Tensor) -> List[int]:
        """
        Index the given images in the index.

        Args:
            last_id (int): The last ID in the index.
            images (torch.Tensor): The images to be indexed.

        Returns:
            List[int]: A list of IDs of the indexed images.
        """

        ids = np.arange(last_id, last_id + images.shape[0])
        self.index.add_with_ids(images, ids)
        self.save()

        return ids.tolist()

    def remove(self, label: int) -> None:
        """
        Remove an image from the given index.

        Args:
            label (int): The ID of the image to be removed.
        """

        id_selector = faiss.IDSelectorRange(label, label + 1)

        index = faiss.index_gpu_to_cpu(self.index) if self.gpu else self.index
        index.remove_ids(id_selector)

        self.save()

    def search(
        self,
        image: np.ndarray,
        nrt_neigh: int,
    ) -> Tuple[List[str], List[float]]:
        """
        Search similar images given a query image.

        Args:
            image (np.array): The query image.
            nrt_neigh (int): The number of nearest neighbours to search.

        Returns:
            Tuple[List[str], List[float]]: the list of image IDs and their distances
        """

        distances, labels = self.index.search(image, nrt_neigh)
        distances, labels = distances.squeeze().tolist(), labels.squeeze().tolist()

        if nrt_neigh == 1:
            distances = [distances]
            labels = [labels]

        # Return only valid results
        stop = labels.index(-1) if -1 in labels else len(labels)

        return labels[:stop], distances[:stop]
