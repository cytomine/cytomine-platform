import os, io
import pytest
from datetime import datetime
from PIL import Image
from fastapi import APIRouter

from pims.formats import FORMATS
from pims.importer.importer import FileImporter
from pims.files.file import (ORIGINAL_STEM, Path, SPATIAL_STEM)
from pims.utils.types import parse_float
from pims.files.archive import Archive

from wsidicom import WsiDicom

def get_image(path, filename):
    filepath = os.path.join(path,filename)
    # If image does not exist locally -> download image
    if not os.path.exists(path):
        os.mkdir("/data/pims/upload_test_wsidicom")

    if not os.path.exists(filepath):
        try:
            url = f"https://data.cytomine.coop/private/wsi-dicom/{filename}"
            urllib.request.urlretrieve(url, filepath)
        except Exception as e:
            print("Could not download image")
            print(e)

    if not os.path.exists(os.path.join(path, "processed")):
        try:
            fi = FileImporter(f"/data/pims/upload_test_wsidicom/{filename}")
            fi.upload_dir = "/data/pims/upload_test_wsidicom"
            fi.processed_dir = fi.upload_dir / Path("processed")
            fi.mkdir(fi.processed_dir)
        except Exception as e:
            print(path + "processed could not be created")
            print(e)
    if not os.path.exists(os.path.join(path, "processed/visualisation.WSIDICOM")):
        if os.path.exists(os.path.join(path, "processed")):
            fi = FileImporter(f"/data/pims/upload_test_wsidicom/{filename}")
            fi.upload_dir = "/data/pims/upload_test_wsidicom"
            fi.processed_dir = fi.upload_dir / Path("processed")
        try:
            fi.upload_path = Path(filepath)
            original_filename = Path(f"{ORIGINAL_STEM}.WSIDICOM")
            fi.original_path = fi.processed_dir / original_filename # unsupported operand type(s) for /: 'NoneType' and 'Path'
            archive = Archive.from_path(fi.upload_path)
            archive.extract(fi.original_path)
            new_original_path = fi.processed_dir / original_filename
            fi.move(fi.original_path, new_original_path)
            fi.original_path = new_original_path
            fi.upload_path = fi.original_path
            spatial_filename = Path(f"{SPATIAL_STEM}.WSIDICOM")
            fi.spatial_path = fi.processed_dir / spatial_filename
            fi.mksymlink(fi.spatial_path, fi.original_path)
        except Exception as e:
            print("Importation of images could not be done")
            print(e)

def dictify(ds):
    output = dict()
    for elem in ds:
        if elem.VR != 'SQ':
            output[elem.name] = elem.value
        else:
            output[elem.name] = [dictify(item) for item in elem]
    return output

def parse_acquisition_date(date: str):
        """
        Datetime examples: 20211216163400 -> 16/12/21, 16h34
        """
        try:
            if date:
                str_date = datetime.strptime(date.split('.')[0], "%Y%m%d%H%M%S")
                return f'{str_date}'
                
            else:
                return None
        except (ValueError, TypeError):
            return None
    
def test_wsidicom_exists(image_path_wsidicom):
	# Test if the file exists, either locally either with the OAC
	path, filename = image_path_wsidicom
	get_image(path, filename)
	assert os.path.exists(os.path.join(image_path_wsidicom[0],image_path_wsidicom[1])) == True

def test_wsidicom_info(client, image_path_wsidicom):
    path, filename = image_path_wsidicom
    response = client.get(f'/image/upload_test_wsidicom/{image_path_wsidicom[1]}/info')
    assert response.status_code == 200
    assert "wsidicom" in response.json()['image']['original_format'].lower()
    #view = get_wsidicom_properties(os.path.join(path,filename))
    #list_subdir = [f.path for f in os.scandir(self.format.path) if f.is_dir()]
    wsidicom_object = WsiDicom.open(f"/data/pims/upload_test_wsidicom/processed/original.WSIDICOM/{os.path.splitext(image_path_wsidicom[1])[0]}")
    
    assert response.json()['image']['width'] == wsidicom_object.levels.base_level.size.width
    assert response.json()['image']['height'] == wsidicom_object.levels.base_level.size.height
    metadata = dictify(wsidicom_object.levels.groups[0].datasets[0])
    assert response.json()['image']['significant_bits'] == metadata["Bits Stored"]
    
    assert response.json()['image']['physical_size_x'] == wsidicom_object.levels.groups[0].mpp.width
    assert response.json()['image']['physical_size_y'] == wsidicom_object.levels.groups[0].mpp.height
    
    if 'Objective Lens Power' in metadata['Optical Path Sequence'][0]:
        assert response.json()['instrument']['objective']['nominal_magnification'] == parse_float(metadata['Optical Path Sequence'][0]['Objective Lens Power'])
	
    if 'Acquisition DateTime' in metadata:
        assert response.json()['image']['acquired_at'].replace("T", " ") == parse_acquisition_date(metadata['Acquisition DateTime'])
            
def test_wsidicom_associated(client, image_path_wsidicom):
    path, filename = image_path_wsidicom
    response = client.get(f'/image/upload_test_wsidicom/{image_path_wsidicom[1]}/info')
    wsidicom_object = WsiDicom.open(f"/data/pims/upload_test_wsidicom/processed/original.WSIDICOM/{os.path.splitext(image_path_wsidicom[1])[0]}")
    
    # assume there is no associated thumbnail for now
    # idx = index of the associated image in the response dictionary
    if wsidicom_object.labels:
        idx = 0
        label = wsidicom_object.read_label()
        assert response.json()['associated'][idx]['name'] == "label"
        assert response.json()['associated'][idx]['width'] == label.width
        assert response.json()['associated'][idx]['height'] == label.height
        
    if wsidicom_object.overviews:
        if wsidicom_object.labels:
            idx = 1
        else:
            idx = 0
        macro = wsidicom_object.read_overview()
        assert response.json()['associated'][idx]['name'] == "macro"
        assert response.json()['associated'][idx]['width'] == macro.width
        assert response.json()['associated'][idx]['height'] == macro.height
        
def test_wsidicom_norm_tile(client, image_path_wsidicom):
    _, filename = image_path_wsidicom
    response = client.get(f"/image/upload_test_wsidicom/{filename}/normalized-tile/level/0/ti/0", headers={"accept": "image/jpeg"})
    assert response.status_code == 200

    img_response = Image.open(io.BytesIO(response.content))
    width_resp, height_resp = img_response.size
    assert width_resp == 256
    assert height_resp == 256
	

def test_wsidicom_thumb(client, image_path_wsidicom):
    _, filename = image_path_wsidicom
    response = client.get(f"/image/upload_test_wsidicom/{filename}/thumb", headers={"accept": "image/jpeg"})
    assert response.status_code == 200
    
    im_resp = Image.open(io.BytesIO(response.content))
    width_resp, height_resp = im_resp.size
    assert width_resp == 256 or height_resp == 256
	
def test_wsidicom_macro(client, image_path_wsidicom):
    path, filename = image_path_wsidicom
    response = client.get(f"/image/upload_test_wsidicom/{filename}/associated/macro", headers={"accept": "image/jpeg"})
    assert response.status_code == 200
    
    im_resp = Image.open(io.BytesIO(response.content))
    width_resp, height_resp = im_resp.size
    assert width_resp == 256 or height_resp == 256
    
@pytest.mark.skip(reason="There is no label image associated with this WSI Dicom image")    
def test_wsidicom_label(client, image_path_wsidicom):
    path, filename = image_path_wsidicom
    response = client.get(f"/image/upload_test_wsidicom/{filename}/associated/label", headers={"accept": "image/jpeg"})
    assert response.status_code == 200
    
    im_resp = Image.open(io.BytesIO(response.content))
    width_resp, height_resp = im_resp.size
    assert width_resp == 256 or height_resp == 256
    
def test_wsidicom_resized(client, image_path_wsidicom):
    _, filename = image_path_wsidicom
    response = client.get(f"/image/upload_test_wsidicom/{filename}/resized", headers={"accept": "image/jpeg"})
    assert response.status_code == 200

    im_resp = Image.open(io.BytesIO(response.content))
    width_resp, height_resp = im_resp.size
    assert width_resp == 256 or height_resp == 256

def test_wsidicom_mask(client, image_path_wsidicom):
    _, filename = image_path_wsidicom
    response = client.post(f"/image/upload_test_wsidicom/{filename}/annotation/mask", headers={"accept": "image/jpeg"}, json={"annotations":[{"geometry": "POINT(10 10)"}], "height":50, "width":50})
    assert response.status_code == 200

def test_wsidicom_crop(client, image_path_wsidicom):
    _, filename = image_path_wsidicom
    response = client.post(f"/image/upload_test_wsidicom/{filename}/annotation/crop", headers={"accept": "image/jpeg"}, json={"annotations":[{"geometry": "POINT(10 10)"}], "height":50, "width":50})
    assert response.status_code == 200

