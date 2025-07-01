#  * Copyright (c) 2020-2021. Authors: see NOTICE file.
#  *
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *      http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.
from __future__ import annotations

import shutil
from typing import Tuple

import numpy as np
import zarr as zarr
from skimage.exposure import histogram

from pims.api.utils.models import Colorspace, HistogramType
from pims.api.utils.output_parameter import get_thumb_output_dimensions
from pims.config import get_settings
from pims.files.file import Path, Histogram
from pims.processing.histograms import ZarrHistogramFormat
from pims.processing.histograms.format import (
    ZHF_ATTR_FORMAT, ZHF_ATTR_TYPE, ZHF_BOUNDS, ZHF_HIST, ZHF_PER_CHANNEL, ZHF_PER_IMAGE,
    ZHF_PER_PLANE
)
from pims.processing.pixels import ImagePixels
from pims.utils.math import max_intensity

MAX_PIXELS_COMPLETE_HISTOGRAM = get_settings().max_pixels_complete_histogram
MAX_LENGTH_COMPLETE_HISTOGRAM = get_settings().max_length_complete_histogram


def argmin_nonzero(arr, axis=-1):
    """
    Get the smallest indices of non-zero values along the specified axis.
    """
    return np.argmax(arr != 0, axis=axis)


def argmax_nonzero(arr, axis=-1):
    """
    Get the biggest indices of non-zero values along the specified axis,
    in the reverted array. It gives the index of the last non-zero value in
    the array.
    """
    return arr.shape[axis] - np.argmax(np.flip(arr, axis=axis) != 0, axis=axis) - 1


def clamp_histogram(hist, bounds=None) -> Tuple[np.ndarray, np.ndarray]:
    """
    Clamp a 1D histogram between bounds (inclusive). If bounds are not set,
    bounds are computed so that the histogram is clamped between first and last
    non-zero values.

    Returns
    -------
    hist, indices
    """
    if bounds is None:
        inf = argmin_nonzero(hist)
        sup = argmax_nonzero(hist)
    else:
        inf, sup = bounds
    return hist[inf:sup + 1], np.arange(inf, sup + 1)


def rescale_histogram(hist: np.ndarray, bitdepth: int) -> np.ndarray:
    """
    Rescale a 1D histogram or a list of 1D histograms for a given bit depth.
    It is used to rescale values so that out = in * (pow(2, bitdepth) - 1)
    """
    multi = hist.ndim > 1
    if multi:
        hist = np.atleast_2d(hist)

    hist = hist.reshape(
        (hist.shape[0], max_intensity(bitdepth, count=True), -1)
    ).sum(axis=2)

    if multi:
        return np.squeeze(hist)
    return hist


def change_colorspace_histogram(
    hist: np.ndarray, colorspace: Colorspace
) -> np.ndarray:
    """
    Transform a 1D histogram or a list of 1D histograms colorspace if
    needed.
    """
    multi = hist.ndim > 1
    if multi:
        hist = np.atleast_2d(hist)

    n_channels = hist.shape[0]
    if colorspace == Colorspace.GRAY and n_channels != 1:
        n_used_channels = min(n_channels, 3)
        luminance = np.array([[0.2125, 0.7154, 0.0721]])
        hist = luminance[:, :n_used_channels] @ hist[:n_used_channels]
    elif colorspace == Colorspace.COLOR and n_channels != 3:
        return np.vstack((hist, hist, hist))

    if multi:
        return np.squeeze(hist)
    return hist


def _extract_np_thumb(image):
    tw, th = get_thumb_output_dimensions(
        image, length=MAX_LENGTH_COMPLETE_HISTOGRAM, allow_upscaling=False
    )
    ratio = image.n_pixels / (tw * th)

    c_chunk_size = 256
    for t in range(image.duration):
        for z in range(image.depth):
            for i in range(0, image.n_channels, c_chunk_size):
                c = range(i, min(image.n_channels, i + c_chunk_size))

                thumb = ImagePixels(
                    image.thumbnail(tw, th, precomputed=False, c=list(c), t=t, z=z)
                ).int_clip()
                yield thumb.np_array(), c, z, t, ratio


def build_histogram_file(
    in_image, dest, hist_type: HistogramType,
    overwrite: bool = False
):
    """
    Build an histogram for an image and save it as zarr file.
    Parameters
    ----------
    in_image : Image
        The image from which histogram has to be extracted.
    dest : Path
        The path where the histogram file will be saved.
    hist_type : HistogramType
        The type of histogram to build (FAST or COMPLETE)
    overwrite : bool (default: False)
        Whether overwrite existing histogram file at `dest` if any

    Returns
    -------
    histogram : Histogram
        The zarr histogram file in read-only mode
    """
    n_values = 2 ** min(in_image.significant_bits, 16)

    if in_image.n_pixels <= MAX_PIXELS_COMPLETE_HISTOGRAM:
        extract_fn = _extract_np_thumb
        hist_type = HistogramType.COMPLETE
    else:
        if hist_type == HistogramType.FAST:
            extract_fn = _extract_np_thumb
        else:
            extract_fn = in_image.tile
            raise NotImplementedError()  # TODO

    if not overwrite and dest.exists():
        raise FileExistsError(dest)

    # While the file is not fully built, we save it at a temporary location
    tmp_dest = dest.parent / Path(f"tmp_{dest.name}")
    zroot = zarr.open_group(str(tmp_dest), mode='w')
    zroot.attrs[ZHF_ATTR_TYPE] = hist_type
    zroot.attrs[ZHF_ATTR_FORMAT] = "PIMS-1.0"

    # Create the group for plane histogram
    # TODO: usa Dask to manipulate Zarr arrays (for bounds)
    #  so that we can fill the zarr array incrementally
    # https://github.com/zarr-developers/zarr-python/issues/446
    shape = (in_image.duration, in_image.depth, in_image.n_channels)
    zplane = zroot.create_group(ZHF_PER_PLANE)
    npplane_hist = np.zeros(shape=shape + (n_values,), dtype=np.uint64)
    for data, c_range, z, t, ratio in extract_fn(in_image):
        for read, c in enumerate(c_range):
            h, _ = histogram(data[:, :, read], source_range='dtype')
            npplane_hist[t, z, c, :] += np.rint(h * ratio).astype(np.uint64)
    zplane.array(ZHF_HIST, npplane_hist)
    zplane.array(
        ZHF_BOUNDS,
        np.stack(
            (argmin_nonzero(npplane_hist),
             argmax_nonzero(npplane_hist)), axis=-1
        )
    )

    # Create the group for channel histogram
    zchannel = zroot.create_group(ZHF_PER_CHANNEL)
    npchannel_hist = np.sum(npplane_hist, axis=(0, 1))
    zchannel.array(ZHF_HIST, npchannel_hist)
    zchannel.array(
        ZHF_BOUNDS,
        np.stack(
            (argmin_nonzero(npchannel_hist),
             argmax_nonzero(npchannel_hist)), axis=-1
        )
    )

    # Create the group for image histogram
    zimage = zroot.create_group(ZHF_PER_IMAGE)
    npimage_hist = np.sum(npchannel_hist, axis=0)
    zimage.array(ZHF_HIST, npimage_hist)
    zimage.array(
        ZHF_BOUNDS,
        [argmin_nonzero(npimage_hist), argmax_nonzero(npimage_hist)]
    )

    # Remove redundant data
    if in_image.duration == 1 and in_image.depth == 1:
        del zroot[ZHF_PER_PLANE]
        if in_image.n_channels == 1:
            del zroot[ZHF_PER_CHANNEL]

    # Move the zarr file (directory) to final location
    if overwrite and dest.exists():
        shutil.rmtree(dest)
    tmp_dest.replace(dest)
    return Histogram(dest, format=ZarrHistogramFormat)
