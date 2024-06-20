import io
import os
import subprocess
import urllib.request

import pytest
from PIL import Image as PILImage

from tests.utils.formats import (
    thumb_test,
    resized_test,
    mask_test,
    crop_test,
    crop_null_annot_test,
    histogram_perimage_test,
)


def get_image(path, filename, root):
    filepath = os.path.join(path, filename)
    # If image does not exist locally -> download image

    if not os.path.exists("/tmp/images"):
        os.mkdir("/tmp/images")

    if not os.path.exists(root):
        os.mkdir(root)

    if not os.path.exists(f"/tmp/images/{filename}"):
        try:
            url = f"https://data.cytomine.coop/open/tests/Test%20special%20char%20%25(_!.tiff"
            urllib.request.urlretrieve(url, f"/tmp/images/{filename}")
        except Exception as e:
            print("Could not download image")
            print(e)
    print(os.path.exists(filepath))
    if not os.path.exists(filepath):
        # os.mkdir(path)
        image_path = f"/tmp/images/{filename}"
        pims_root = root
        importer_path = f"/app/pims/importer/import_local_images.py"  # pims folder should be in root folder
        import_img = subprocess.run(
            ["python3", importer_path, "--path", image_path], stdout=subprocess.PIPE
        )

        subdirs = os.listdir(pims_root)
        for subdir in subdirs:
            if "upload-" in str(subdir):
                subsubdirs = os.listdir(os.path.join(root, subdir))
                for subsubdir in subsubdirs:
                    if filename in str(subsubdir):
                        upload_dir = os.path.join(root, str(subdir))
                        break
        if os.path.exists(path):
            os.unlink(path)

        print(path, root)
        print(os.path.exists(upload_dir))  # existe
        print(os.path.exists(path))  # n'existe pas
        print(os.path.exists(root))  # existe
        os.symlink(upload_dir, path)

# TODO investigate
@pytest.mark.skip(reason="currently failing without apparent reason")
def test_exists(image_path_excentric_filename, root):
    path, filename = image_path_excentric_filename
    get_image(path, filename, root)
    assert os.path.exists(os.path.join(path, filename)) == True

# TODO investigate
@pytest.mark.skip(reason="currently failing without apparent reason")
def test_info(client, image_path_excentric_filename):
    _, filename = image_path_excentric_filename
    response = client.get(f"/image/upload_test_excentric/{filename}/info")
    assert response.status_code == 200
    assert "tiff" in response.json()["image"]["original_format"].lower()

    assert response.json()["image"]["width"] == 46000
    assert response.json()["image"]["height"] == 32914

# TODO investigate
@pytest.mark.skip(reason="currently failing without apparent reason")
def test_norm_tile(client, image_path_excentric_filename):
    path, filename = image_path_excentric_filename
    response = client.get(
        f"/image/upload_test_excentric/{filename}/normalized-tile/zoom/1/ti/0",
        headers={"accept": "image/jpeg"},
    )
    assert response.status_code == 200

    img_response = PILImage.open(io.BytesIO(response.content))
    width_resp, height_resp = img_response.size

    assert width_resp == 256
    assert height_resp == 256

# TODO investigate
@pytest.mark.skip(reason="currently failing without apparent reason")
def test_thumb(client, image_path_excentric_filename):
    _, filename = image_path_excentric_filename
    thumb_test(client, filename, "excentric")

# TODO investigate
@pytest.mark.skip(reason="currently failing without apparent reason")
def test_resized(client, image_path_excentric_filename):
    _, filename = image_path_excentric_filename
    resized_test(client, filename, "excentric")

# TODO investigate
@pytest.mark.skip(reason="currently failing without apparent reason")
def test_mask(client, image_path_excentric_filename):
    _, filename = image_path_excentric_filename
    mask_test(client, filename, "excentric")

# TODO investigate
@pytest.mark.skip(reason="currently failing without apparent reason")
def test_crop(client, image_path_excentric_filename):
    _, filename = image_path_excentric_filename
    crop_test(client, filename, "excentric")

@pytest.mark.skip(reason="Does not return the correct response code")
def test_crop_null_annot(client, image_path_excentric_filename):
    _, filename = image_path_excentric_filename
    crop_null_annot_test(client, filename, "excentric")

# TODO investigate
@pytest.mark.skip(reason="currently failing without apparent reason")
def test_histogram_perimage(client, image_path_excentric_filename):
    _, filename = image_path_excentric_filename
    histogram_perimage_test(client, filename, "excentric")
