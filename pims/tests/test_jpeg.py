import io
import os
import urllib.request

import pytest
from PIL import Image as PILImage

from pims.api.utils.models import HistogramType
from pims.files.file import (
    HISTOGRAM_STEM, ORIGINAL_STEM, Path,
    SPATIAL_STEM, Image
)
from pims.formats.utils.factories import FormatFactory
from pims.importer.importer import FileImporter
from pims.processing.histograms.utils import build_histogram_file
from tests.utils.formats import info_test, thumb_test, resized_test, mask_test, crop_test, crop_null_annot_test, \
    histogram_perimage_test


def get_image(path, filename):
    filepath = os.path.join(path, filename)
    # If image does not exist locally -> download image
    if not os.path.exists(path):
        os.mkdir(path)

    if not os.path.exists(filepath):
        try:
            url = f"https://data.cytomine.coop/open/tests/{filename}" #OAC
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

    if not os.path.exists(os.path.join(path,"processed/visualisation.JPEG")):
        if os.path.exists(os.path.join(path, "processed")):
            fi = FileImporter(filepath)
            fi.upload_dir = path
            fi.processed_dir = fi.upload_dir / Path("processed")
        try:
            fi.upload_path = Path(filepath)
            original_filename = Path(f"{ORIGINAL_STEM}.JPEG")
            fi.original_path = fi.processed_dir / original_filename
            fi.mksymlink(fi.original_path, fi.upload_path)
            spatial_filename = Path(f"{SPATIAL_STEM}.JPEG")
            fi.spatial_path = fi.processed_dir / spatial_filename
            fi.mksymlink(fi.spatial_path, fi.original_path)
        except Exception as e:
            print("Importation of images could not be done")
            print(e)

    if not os.path.exists(os.path.join(path, "processed/histogram")):
        if os.path.exists(os.path.join(path, "processed")):
            fi = FileImporter(filepath)
            fi.upload_dir = path
            fi.processed_dir = fi.upload_dir / Path("processed")
            original_filename = Path(f"{ORIGINAL_STEM}.JPEG")
            fi.original_path = fi.processed_dir / original_filename
        try:
            fi.histogram_path = fi.processed_dir/Path(HISTOGRAM_STEM) #/data/pims/upload1641567540187798/processed/histogram
            format = FormatFactory().match(fi.original_path)
            fi.original = Image(fi.original_path, format=format)
            fi.histogram = build_histogram_file(fi.original, fi.histogram_path, HistogramType.FAST)
        except Exception as e:
            print("Creation of histogram representation could not be done")
            print(e)
            
def test_jpeg_exists(image_path_jpeg):
    path, filename = image_path_jpeg
    get_image(path, filename)
    assert os.path.exists(os.path.join(path, filename)) == True
    
def test_jpeg_info(client, image_path_jpeg):
    _, filename = image_path_jpeg
    info_test(client, filename, "jpeg")
    
def test_jpeg_norm_tile(client, image_path_jpeg):
    path, filename = image_path_jpeg
    response = client.get(f"/image/upload_test_jpeg/{filename}/normalized-tile/zoom/1/ti/0", headers={"accept": "image/jpeg"})
    assert response.status_code == 200
    
    img_response = PILImage.open(io.BytesIO(response.content))
    width_resp, height_resp = img_response.size

    img_original = PILImage.open(os.path.join(path, filename))
    width, height = img_original.size
    assert width_resp == 256 or width_resp == width
    assert height_resp == 256 or height_resp == height
    
def test_jpeg_thumb(client, image_path_jpeg):
    _, filename = image_path_jpeg
    thumb_test(client, filename, "jpeg")
        
def test_jpeg_resized(client, image_path_jpeg):
    _, filename = image_path_jpeg
    resized_test(client, filename, "jpeg")
        
def test_jpeg_mask(client, image_path_jpeg):
    _, filename = image_path_jpeg
    mask_test(client, filename, "jpeg")
    
def test_jpeg_crop(client, image_path_jpeg):
    _, filename = image_path_jpeg
    crop_test(client, filename, "jpeg")

@pytest.mark.skip(reason="Does not return the correct response code")
def test_crop_null_annot(client, image_path_jpeg):
    _, filename = image_path_jpeg
    crop_null_annot_test(client, filename, "jpeg")
    
def test_histogram_perimage(client, image_path_jpeg):
    _, filename = image_path_jpeg
    histogram_perimage_test(client, filename, "jpeg")
