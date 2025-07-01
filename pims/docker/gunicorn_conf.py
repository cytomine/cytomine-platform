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

import json
import logging.config
import multiprocessing
import os

import yaml


def configure_logging(default_log_level='info'):
    log_config = os.getenv("LOG_CONFIG_FILE")
    default_config = not bool(log_config)
    if default_config:
        log_config = "/app/logging-prod.yml"

    if log_config.endswith(".json"):
        with open(log_config) as file:
            loaded_config = json.load(file)
    elif log_config.endswith((".yaml", ".yml")):
        with open(log_config) as file:
            loaded_config = yaml.safe_load(file)
    else:
        print("Log config ignored: unsupported format")
        return

    if default_config:
        loaded_config['root']['level'] = default_log_level.upper()
    logging.config.dictConfig(loaded_config)
    return loaded_config


workers_per_core_str = os.getenv("WORKERS_PER_CORE", "1")
max_workers_str = os.getenv("MAX_WORKERS")
use_max_workers = None
if max_workers_str:
    use_max_workers = int(max_workers_str)
web_concurrency_str = os.getenv("WEB_CONCURRENCY", None)

host = os.getenv("HOST", "0.0.0.0")
port = os.getenv("PORT", "80")
bind_env = os.getenv("BIND", None)
use_loglevel = os.getenv("LOG_LEVEL", "info")
if bind_env:
    use_bind = bind_env
else:
    use_bind = f"{host}:{port}"

cores = multiprocessing.cpu_count()
workers_per_core = float(workers_per_core_str)
default_web_concurrency = workers_per_core * cores
if web_concurrency_str:
    web_concurrency = int(web_concurrency_str)
    assert web_concurrency > 0
else:
    web_concurrency = max(int(default_web_concurrency), 2)
    if use_max_workers:
        web_concurrency = min(web_concurrency, use_max_workers)
accesslog_var = os.getenv("ACCESS_LOG", "-")
use_accesslog = accesslog_var or None
errorlog_var = os.getenv("ERROR_LOG", "-")
use_errorlog = errorlog_var or None
graceful_timeout_str = os.getenv("GRACEFUL_TIMEOUT", "120")
timeout_str = os.getenv("TIMEOUT", "120")
keepalive_str = os.getenv("KEEP_ALIVE", "5")

preload_str = os.getenv("PRELOAD", "0")
max_requests_str = os.getenv("MAX_REQUESTS", "0")
max_requests_jitter_str = os.getenv("MAX_REQUESTS_JITTER", "0")

# Gunicorn config variables
logconfig_dict = configure_logging(use_loglevel)
loglevel = use_loglevel
workers = web_concurrency
bind = use_bind
errorlog = use_errorlog
worker_tmp_dir = "/dev/shm"
accesslog = use_accesslog
graceful_timeout = int(graceful_timeout_str)
timeout = int(timeout_str)
keepalive = int(keepalive_str)
preload = bool(preload_str)
max_requests = int(max_requests_str)
max_requests_jitter = int(max_requests_jitter_str)

# For debugging and testing
if True or loglevel.upper() == "DEBUG":
    log_data = {
        "loglevel": loglevel,
        "workers": workers,
        "bind": bind,
        "graceful_timeout": graceful_timeout,
        "timeout": timeout,
        "keepalive": keepalive,
        "preload": preload,
        "max_requests": max_requests,
        "max_requests_jitter": max_requests_jitter,
        "errorlog": errorlog,
        "accesslog": accesslog,
        # Additional, non-gunicorn variables
        "workers_per_core": workers_per_core,
        "use_max_workers": use_max_workers,
        "host": host,
        "port": port,
    }
    print(json.dumps(log_data))
