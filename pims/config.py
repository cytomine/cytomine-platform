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

import logging
import os
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict

logger = logging.getLogger("pims.app")


class ReadableSettings(BaseSettings):
    model_config = SettingsConfigDict(extra="ignore")


    api_base_path: str = ""  # if set, must start with /.

    root: str
    pending_path: str = "/tmp/uploaded"
    writing_path: str = "/data/pims/tmp"
    checker_resolution_file: str = "checkerResolution.csv"
    default_image_size_safety_mode: str = "SAFE_REJECT"
    default_annotation_origin: str = "LEFT_TOP"
    output_size_limit: int = 10000
    internal_url_core: str = "http://cytomine.local"

    # Must be TRUE in production.
    cache_enabled: bool = True
    cache_url: str = "redis://pims-cache:6379"
    # Must be TRUE in production. Helpful in dev or debug to disable caching of image metadata without disabling the cache.
    cache_image_format_metadata: bool = True
    # Must be TRUE in production. Helpful in dev or debug to disable caching of image responses without disabling the cache.
    cache_image_responses: bool = True
    # The max-age to set in HTTP Cache-Control for cached image responses.
    image_response_cache_control_max_age: int = 60 * 60 * 24

    task_queue_enabled: bool = True
    task_queue_url: str = "rabbitmq:5672"

    max_pixels_complete_histogram: int = 1024 * 1024
    max_length_complete_histogram: int = 1024

    # Maximum number of operations to cache
    vips_cache_max_items: int = 100
    # Maximum memory in MB to use for this cache
    vips_cache_max_memory: int = 50
    # Maximum number of files to hold open
    vips_cache_max_files: int = 100

    auto_delete_multi_file_format_archive: bool = True
    auto_delete_collection_archive: bool = True
    auto_delete_failed_upload: bool = True


class Settings(ReadableSettings):
    model_config = SettingsConfigDict(env_file="pims-config.env", env_file_encoding="utf-8")


    cytomine_public_key: str
    cytomine_private_key: str

    task_queue_user: str = "router"
    task_queue_password: str = "router"


@lru_cache()
def get_settings():
    env_file = os.getenv("CONFIG_FILE", "pims-config.env")
    logger.info(f"Loading config from {env_file}")
    return Settings(_env_file=env_file)
