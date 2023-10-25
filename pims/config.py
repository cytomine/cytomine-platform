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

from pydantic import BaseSettings, Extra

logger = logging.getLogger("pims.app")


class ReadableSettings(BaseSettings):
    root: str
    pending_path: str = "/tmp/uploaded"
    writing_path: str = "/data/pims/tmp"
    checker_resolution_file: str = "checkerResolution.csv"
    default_image_size_safety_mode: str = "SAFE_REJECT"
    default_annotation_origin: str = "LEFT_TOP"
    output_size_limit: int = 10000
    pims_url: str = "http://localhost-ims"

    cache_enabled: bool = True
    cache_url: str = "redis://pims-cache:6379"
    cache_ttl_thumb: int = 60 * 60 * 24 * 15
    cache_ttl_resized: int = 60 * 60 * 24 * 15
    cache_ttl_tile: int = 60 * 60 * 24
    cache_ttl_window: int = 60 * 60 * 24

    memory_lru_cache_capacity: int = 500

    task_queue_enabled: bool = True
    task_queue_url: str = "rabbitmq:5672"

    max_pixels_complete_histogram: int = 1024 * 1024
    max_length_complete_histogram: int = 1024

    vips_allow_leak: bool = False
    vips_cache_max_items: int = 5000
    vips_cache_max_memory: int = 300  # in MB
    vips_cache_max_files: int = 500

    auto_delete_multi_file_format_archive: bool = True
    auto_delete_collection_archive: bool = True

    class Config:
        extra = Extra.ignore


class Settings(ReadableSettings):
    cytomine_public_key: str
    cytomine_private_key: str

    task_queue_user: str = "router"
    task_queue_password: str = "router"

    class Config:
        env_file = "pims-config.env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings():
    env_file = os.getenv("CONFIG_FILE", "pims-config.env")
    logger.info(f"[green]Loading config from {env_file}")
    return Settings(_env_file=env_file)
