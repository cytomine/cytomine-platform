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

import os
import shutil
from contextlib import contextmanager
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from pims import config

os.environ['CONFIG_FILE'] = "./pims-config.env"


def test_root():
    return get_settings().root


def get_settings():
    return config.Settings(
        _env_file=os.getenv("CONFIG_FILE")
    )


@pytest.fixture
def settings():
    return get_settings()


@pytest.fixture
def app():
    from pims import application as main

    main.app.dependency_overrides[config.get_settings] = get_settings
    return main.app


@pytest.fixture
def client(app):
    return TestClient(app)

@pytest.fixture
def image_path_wsidicom():
	path = "/data/pims/upload_test_wsidicom/"
	image = "melanoma_pilot_003.zip"
	return [path, image]

@contextmanager
def not_raises(expected_exc):
    try:
        yield

    except expected_exc as err:
        raise AssertionError(
            "Did raise exception {0} when it should not!".format(
                repr(expected_exc)
            )
        )

    except Exception as err:
        raise AssertionError(
            "An unexpected exception {0} raised.".format(repr(err))
        )
