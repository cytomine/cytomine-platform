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
from enum import Enum
from importlib import import_module
from typing import Callable

from celery import Celery
from starlette.background import BackgroundTasks

from pims.config import get_settings

logger = logging.getLogger("pims.app")

settings = get_settings()

broker_url = f"{settings.task_queue_user}:{settings.task_queue_password}@{settings.task_queue_url}"
celery_app = Celery(
    "worker",
    broker=f"amqp://{broker_url}//",
    backend=f"rpc://{broker_url}//"
)

celery_app.conf.update(
    task_serializer="pickle",
    result_serializer="pickle",
    accept_content=["pickle"],
    broker_transport_options={
        "max_retries": 3,
        "interval_start": 0,
        "interval_step": 0.2,
        "interval_max": 0.5
    }
)

celery_app.conf.task_routes = {
    "pims.tasks.worker.run_import": "pims-import-queue",
    "pims.tasks.worker.run_import_with_cytomine": "pims-import-queue",
}


class Task(str, Enum):
    IMPORT = "IMPORT"
    IMPORT_WITH_CYTOMINE = "IMPORT_WITH_CYTOMINE"
    IMPORT_WITH_FILE = "IMPORT_WITH_FILE"


CELERY_TASK_MAPPING = {
    Task.IMPORT: "pims.tasks.worker.run_import",
    Task.IMPORT_WITH_CYTOMINE: "pims.tasks.worker.run_import_with_cytomine",
}

BG_TASK_MAPPING = {
    Task.IMPORT: "pims.tasks.worker.run_import_fallback",
    Task.IMPORT_WITH_CYTOMINE: "pims.tasks.worker.run_import_with_cytomine_fallback",
}


def func_from_str(mod_fuc_name: str) -> Callable:
    module, func = mod_fuc_name.rsplit('.', 1)
    task_func = getattr(import_module(module), func)
    return task_func


def send_task(name, args=None, kwargs=None, starlette_background: BackgroundTasks = None):
    """
    Send a task to PIMS queue.

    The task is normally sent to a Celery worker through RabbitMQ message broker. If the task
    queue is disabled by configuration or if the broker is unreachable, the task is sent to
    FastAPI/Starlette background tasks feature, provided that `starlette_background` is set.
    Otherwise, the task is not executed.

    A Celery worker introduces overhead due to required messaging but executes tasks outside of
    running PIMS server, preventing to block regular response serving from the main app.

    https://fastapi.tiangolo.com/tutorial/background-tasks/

    Parameters
    ----------
    name : Task
        The task name

    args : list or tuple (optional)
        Sequence of args to pass to task function.

    kwargs : dict (optional)
        Dictionary of keyword args to pass to task function.

    starlette_background : BackgroundTasks (optional)
        The background task context from FastAPI/Starlette. Mandatory to send the task to this
        queue context.
    """
    def _try_starlette_background():
        if starlette_background is not None:
            bg_task_name = BG_TASK_MAPPING.get(name)
            if bg_task_name is None:
                logger.error(f"Task {name} cannot be sent to Background tasks.")
                return

            task_func = func_from_str(bg_task_name)
            if task_func is None:
                logger.error(f"Task {name} cannot be sent to Background tasks.")
                return

            valid_args = args if args else ()
            valid_kwargs = kwargs if type(kwargs) is dict else dict()
            starlette_background.add_task(task_func, *valid_args, **valid_kwargs)

    if not settings.task_queue_enabled:
        _try_starlette_background()
        return

    try_bg = "Will try Background tasks." if starlette_background else ""
    try:
        task_name = CELERY_TASK_MAPPING.get(name)
        if task_name is not None:
            celery_app.send_task(task_name, args=args, kwargs=kwargs)
        else:
            log_func = logger.debug if try_bg else logger.error
            log_func(f"Task {name} cannot be sent to Celery worker. {try_bg}")
            _try_starlette_background()
    except Exception:  # noqa
        log_func = logger.warning if try_bg else logger.error
        log_func(
            f"Task {name} cannot be sent to Celery worker due to communication error. "
            f"{try_bg}"
        )
        _try_starlette_background()
