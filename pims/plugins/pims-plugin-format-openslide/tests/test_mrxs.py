from PIL import Image
import os
import urllib.request
from fastapi import APIRouter
from pims.formats import FORMATS
import io
from pims.importer.importer import FileImporter

from utils.formats import info_test, thumb_test, resized_test, mask_test, crop_test, crop_null_annot_test, histogram_perimage_test
from pims.formats.utils.factories import FormatFactory
from pims.api.utils.models import HistogramType
from pims.processing.histograms.utils import build_histogram_file
from pims.files.file import (
    EXTRACTED_DIR, HISTOGRAM_STEM, ORIGINAL_STEM, PROCESSED_DIR, Path,
    SPATIAL_STEM, UPLOAD_DIR_PREFIX
)
from pims.files.archive import Archive, ArchiveError

import pytest

def get_image(path, filename):
    filepath = os.path.join(path,filename)
    # If image does not exist locally -> download image
    if not os.path.exists(path):
        os.mkdir(path)

    if not os.path.exists(filepath):
        try:
            url = f"https://data.cytomine.coop/open/openslide/mirax-mrxs/{filename}"
            urllib.request.urlretrieve(url, filepath)
        except Exception as e:
            print("Could not download image")
            print(e)

    if not os.path.exists(os.path.join(path, "processed")):
        try:
            fi = FileImporter(filepath)
            fi.upload_dir = path
            fi.processed_dir = fi.upload_dir / Path("processed")
            fi.mkdir(fi.processed_dir)
        except Exception as e:
            print(path + "processed could not be created")
            print(e)
    if not os.path.exists(os.path.join(path, "processed/visualisation.MRXS")):
        if os.path.exists(os.path.join(path, "processed")):
            fi = FileImporter(filepath)
            fi.upload_dir = path
            fi.processed_dir = fi.upload_dir / Path("processed")
        try:
            fi.upload_path = Path(filepath)
            original_filename = Path(f"{ORIGINAL_STEM}.MRXS")
            fi.original_path = fi.processed_dir / original_filename
            archive = Archive.from_path(fi.upload_path)
            archive.extract(fi.original_path)
            new_original_path = fi.processed_dir / original_filename
            fi.move(fi.original_path, new_original_path)
            fi.original_path = new_original_path
            fi.upload_path = fi.original_path
            spatial_filename = Path(f"{SPATIAL_STEM}.MRXS")
            fi.spatial_path = fi.processed_dir / spatial_filename
            fi.mksymlink(fi.spatial_path, fi.original_path)
        except Exception as e:
            print("Creation of original/spatial representations could not be done")
            print(e)

    if not os.path.exists(os.path.join(path, "processed/histogram")):
        if os.path.exists(os.path.join(path, "processed")):
            fi = FileImporter(filepath)
            fi.upload_dir = Path(path)
            fi.processed_dir = fi.upload_dir / Path("processed")
            original_filename = Path(f"{ORIGINAL_STEM}.MRXS")
            fi.original_path = fi.processed_dir / original_filename
        try:
            from pims.files.image import Image
            fi.histogram_path = fi.processed_dir/Path(HISTOGRAM_STEM)
            format = FormatFactory().match(fi.original_path)
            fi.original = Image(fi.original_path, format=format)
            fi.histogram = build_histogram_file(fi.original, fi.histogram_path, HistogramType.FAST)
        except Exception as e:
            print("Creation of histogram representation could not be done")
            print(e)
            
def test_mrxs_exists(image_path_mrxs):
    # Test if the file exists, either locally either with the OAC
    path, filename = image_path_mrxs
    get_image(path, filename)
    assert os.path.exists(os.path.join(path,filename)) == True

def test_mrxs_info(client, image_path_mrxs):
    _, filename = image_path_mrxs
    response = client.get(f'/image/upload_test_mrxs/{filename}/info')
    assert response.status_code == 200
    assert "mrxs" in response.json()['image']['original_format'].lower()
    assert response.json()['image']['width'] == 109240
    assert response.json()['image']['height'] == 220696
	
def test_mrxs_metadata(client, image_path_mrxs):
    _, filename = image_path_mrxs
    response = client.get(f'/image/upload_test_mrxs/{filename}/metadata')
    assert response.status_code == 200

    lst = response.json()['items']

    index = next((index for (index, d) in enumerate(lst) if d["key"] == "ImageWidth"), None)
    assert response.json()['items'][index]["value"] == 854

    index = next((index for (index, d) in enumerate(lst) if d["key"] == "ImageHeight"), None)
    assert response.json()['items'][index]["value"] == 1724

    index = next((index for (index, d) in enumerate(lst) if d["key"] == "XResolution"), None)
    assert response.json()['items'][index]["value"] == 1

    index = next((index for (index, d) in enumerate(lst) if d["key"] == "YResolution"), None)
    assert response.json()['items'][index]["value"] == 1

    index = next((index for (index, d) in enumerate(lst) if d["key"] == "ResolutionUnit"), None)
    assert response.json()['items'][index]["value"] == "None"

# For a non-normalized tile, the width is 124
# To have a 256 x 256, the zoom level needs to be high enough
def test_mrxs_norm_tile(client, image_path_mrxs):
    _, filename = image_path_mrxs
    response = client.get(f"/image/upload_test_mrxs/{filename}/normalized-tile/zoom/4/ti/3", headers={"accept": "image/jpeg"})
    assert response.status_code == 200

    img_response = Image.open(io.BytesIO(response.content))
    width_resp, height_resp = img_response.size
    assert width_resp == 256
    assert height_resp == 256

def test_mrxs_thumb(client, image_path_mrxs):
    _, filename = image_path_mrxs
    thumb_test(client, filename, "mrxs")
		
def test_mrxs_resized(client, image_path_mrxs):
    _, filename = image_path_mrxs
    resized_test(client, filename, "mrxs")	

def test_mrxs_mask(client, image_path_mrxs):
    _, filename = image_path_mrxs
    mask_test(client, filename, "mrxs")
	
def test_mrxs_crop(client, image_path_mrxs):
    _, filename = image_path_mrxs
    crop_test(client, filename, "mrxs")

@pytest.mark.skip(reason='Does not return the correct response code')
def test_mrxs_crop_null_annot(client, image_path_mrxs):
    _, filename = image_path_mrxs
    crop_null_annot_test(client, filename, "mrxs")

def test_mrxs_histogram_perimage(client, image_path_mrxs):
    _, filename = image_path_mrxs
    histogram_perimage_test(client, filename, "mrxs")
