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
import pytest 

def get_image(path, filename):
    filepath = os.path.join(path,filename)
    # If image does not exist locally -> download image
    if not os.path.exists(path):
        os.mkdir("/data/pims/upload_test_ndpi")
	    
    if not os.path.exists(filepath):
        try:
            url = f"https://data.cytomine.coop/open/uliege/{filename}"
            urllib.request.urlretrieve(url, filepath)
        except Exception as e:
            print("Could not download image")
            print(e)
	
    if not os.path.exists(os.path.join(path, "processed")):
        try:
            fi = FileImporter(f"/data/pims/upload_test_ndpi/{filename}")
            fi.upload_dir = "/data/pims/upload_test_ndpi"
            fi.processed_dir = fi.upload_dir / Path("processed")
            fi.mkdir(fi.processed_dir)
        except Exception as e:
            print(path + "processed could not be created")
            print(e)
    if not os.path.exists(os.path.join(path, "processed/visualisation.NDPI")):
        if os.path.exists(os.path.join(path, "processed")):
            fi = FileImporter(f"/data/pims/upload_test_ndpi/{filename}")
            fi.upload_dir = "/data/pims/upload_test_ndpi"
            fi.processed_dir = fi.upload_dir / Path("processed")
        try:
            fi.upload_path = Path(filepath)
            original_filename = Path(f"{ORIGINAL_STEM}.NDPI")
            fi.original_path = fi.processed_dir / original_filename
            fi.mksymlink(fi.original_path, fi.upload_path)
            spatial_filename = Path(f"{SPATIAL_STEM}.NDPI")
            fi.spatial_path = fi.processed_dir / spatial_filename
            fi.mksymlink(fi.spatial_path, fi.original_path)
        except Exception as e:
            print("Importation of images could not be done")
            print(e)
			
    if not os.path.exists(os.path.join(path, "processed/histogram")):
        if os.path.exists(os.path.join(path, "processed")):
            fi = FileImporter(f"/data/pims/upload_test_ndpi/{filename}")
            fi.upload_dir = Path("/data/pims/upload_test_ndpi")
            fi.processed_dir = fi.upload_dir / Path("processed")
            original_filename = Path(f"{ORIGINAL_STEM}.NDPI")
            fi.original_path = fi.processed_dir / original_filename
        try:
            from pims.files.image import Image
            fi.histogram_path = fi.processed_dir/Path(HISTOGRAM_STEM) #/data/pims/upload1641567540187798/processed/histogram
            format = FormatFactory().match(fi.original_path)
            fi.original = Image(fi.original_path, format=format)
            fi.histogram = build_histogram_file(fi.original, fi.histogram_path, HistogramType.FAST)
        except Exception as e:
            print("Creation of histogram representation could not be done")
            print(e)
			
def test_ndpi_exists(image_path_ndpi):
	# Test if the file exists, either locally either with the OAC
	path, filename = image_path_ndpi
	get_image(path, filename)
	assert os.path.exists(os.path.join(path, filename)) == True

def test_ndpi_info(client, image_path_ndpi):
    path, filename = image_path_ndpi 
    response = client.get(f'/image/upload_test_ndpi/{filename}/info')
    assert response.status_code == 200
    assert "ndpi" in response.json()['image']['original_format'].lower()
    assert response.json()['image']['width'] == 71424
    assert response.json()['image']['height'] == 24064

def test_metadata(client, image_path_ndpi):
    _, filename = image_path_ndpi 
    response = client.get(f'/image/upload_test_ndpi/{filename}/metadata')
    assert response.status_code == 200

    assert response.status_code == 200
    lst = response.json()['items']

    index = next((index for (index, d) in enumerate(lst) if d["key"] == "XResolution"), None)
    assert response.json()['items'][index]["value"] == '(43784, 1)'

    index = next((index for (index, d) in enumerate(lst) if d["key"] == "YResolution"), None)
    assert response.json()['items'][index]["value"] == '(43784, 1)'

    index = next((index for (index, d) in enumerate(lst) if d["key"] == "ResolutionUnit"), None)
    assert response.json()['items'][index]["value"] == "CENTIMETER"

# For a non-normalized tile, the width is 124
def test_ndpi_norm_tile(client, image_path_ndpi):
    _, filename = image_path_ndpi 
    response = client.get(f"/image/upload_test_ndpi/{filename}/normalized-tile/zoom/3/ti/3", headers={"accept": "image/jpeg"})
    assert response.status_code == 200

    img_response = Image.open(io.BytesIO(response.content))
    width_resp, height_resp = img_response.size
    assert width_resp == 256
    assert height_resp == 256

def test_ndpi_thumb(client, image_path_ndpi):
    _, filename = image_path_ndpi 
    thumb_test(client, filename, "ndpi")
	
def test_ndpi_resized(client, image_path_ndpi):
    _, filename = image_path_ndpi 
    resized_test(client, filename, "ndpi")
	
def test_ndpi_mask(client, image_path_ndpi):
    _, filename = image_path_ndpi 
    mask_test(client, filename, "ndpi")
		
def test_ndpi_crop(client, image_path_ndpi):
    _, filename = image_path_ndpi 
    crop_test(client, filename, "ndpi")

@pytest.mark.skip(reason="Does not return the correct response code")
def test_ndpi_crop_null_annot(client, image_path_ndpi):
    _, filename = image_path_ndpi 
    crop_null_annot_test(client, filename, "ndpi")

def test_ndpi_histogram_perimage(client, image_path_ndpi):
    _, filename = image_path_ndpi 
    histogram_perimage_test(client, filename, "ndpi")
	
