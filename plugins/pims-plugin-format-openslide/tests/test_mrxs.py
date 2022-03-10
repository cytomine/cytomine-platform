from PIL import Image
import os
import urllib.request
from fastapi import APIRouter
from pims.formats import FORMATS
import io
from pims.importer.importer import FileImporter

from tests.utils.formats import info_test, thumb_test, resized_test, mask_test, crop_test, crop_null_annot_test, histogram_perimage_test

from pims.files.file import (
    EXTRACTED_DIR, HISTOGRAM_STEM, ORIGINAL_STEM, PROCESSED_DIR, Path,
    SPATIAL_STEM, UPLOAD_DIR_PREFIX, unique_name_generator
)
from pims.files.archive import Archive, ArchiveError

import pytest

def get_image(path, image):
	filepath = os.path.join(path, image)
	# If image does not exist locally -> download image
	if not os.path.exists(path):
		os.mkdir("/data/pims/upload_test_mrxs")
	
	if not os.path.exists(filepath):
		try:
			url = f"https://data.cytomine.coop/open/openslide/mirax-mrxs/{image}" #OAC
			urllib.request.urlretrieve(url, filepath)
		except Exception as e:
			print("Could not download image")
			print(e)
	
	if not os.path.exists(os.path.join(path, "processed")):
		try:
			fi = FileImporter(f"/data/pims/upload_test_mrxs/{image}")
			fi.upload_dir = "/data/pims/upload_test_mrxs"
			fi.processed_dir = fi.upload_dir / Path("processed")
			fi.mkdir(fi.processed_dir)
		except Exception as e:
			print(path + "processed could not be created")
			print(e)

	if not os.path.exists(os.path.join(path, "processed/visualisation.MRXS")):
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
			print("Importation of images could not be done")
			print(e)
		
def test_mrxs_exists(image_path_mrxs):
	# Test if the file exists, either locally either with the OAC
	get_image(image_path_mrxs[0], image_path_mrxs[1])
	assert os.path.exists(os.path.join(image_path_mrxs[0],image_path_mrxs[1])) == True

def test_mrxs_info(client, image_path_mrxs):
	response = client.get(f'/image/upload_test_mrxs/{image_path_mrxs[1]}/info')
	assert response.status_code == 200
	assert "mrxs" in response.json()['image']['original_format'].lower()
	assert response.json()['image']['width'] == 109240
	assert response.json()['image']['height'] == 220696
	
def test_mrxs_metadata(client, image_path_mrxs):
	response = client.get(f'/image/upload_test_mrxs/{image_path_mrxs[1]}/metadata')
	assert response.status_code == 200
	assert response.json()['items'][5]['key'] == 'ImageWidth'
	assert response.json()['items'][5]["value"] == 854
	assert response.json()['items'][6]['key'] == 'ImageHeight'
	assert response.json()['items'][6]["value"] == 1724
	assert response.json()['items'][2]["key"] == 'XResolution'
	assert response.json()['items'][2]["value"] == 1
	assert response.json()['items'][3]["key"] == 'YResolution'
	assert response.json()['items'][3]["value"] == 1
	assert response.json()['items'][1]["key"] == 'ResolutionUnit'
	assert response.json()['items'][1]["value"] == "None"

# For a non-normalized tile, the width is 124
# To have a 256 x 256, the zoom level needs to be high enough
def test_mrxs_norm_tile(client, image_path_mrxs):
	response = client.get(f"/image/upload_test_mrxs/{image_path_mrxs[1]}/normalized-tile/zoom/4/ti/3", headers={"accept": "image/jpeg"})
	assert response.status_code == 200
	
	img_response = Image.open(io.BytesIO(response.content))
	width_resp, height_resp = img_response.size
	assert width_resp == 256
	assert height_resp == 256

def test_mrxs_thumb(client, image_path_mrxs):
	thumb_test(client, image_path_mrxs[1], "mrxs")
		
def test_mrxs_resized(client, image_path_mrxs):
	resized_test(client, image_path_mrxs[1], "mrxs")	

def test_mrxs_mask(client, image_path_mrxs):
	mask_test(client, image_path_mrxs[1], "mrxs")
	
def test_mrxs_crop(client, image_path_mrxs):
	crop_test(client, image_path_mrxs[1], "mrxs")
	
@pytest.mark.skip
def test_mrxs_crop_null_annot(client, image_path_mrxs):
	crop_null_annot_test(client, image_path_mrxs[1], "mrxs")

def test_mrxs_histogram_perimage(client, image_path_mrxs):
	histogram_perimage_test(client, image_path_mrxs[1], "mrxs")
