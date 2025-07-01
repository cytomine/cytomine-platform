#  * Copyright (c) 2020-2022. Authors: see NOTICE file.
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

import asyncio

from starlette.concurrency import run_in_threadpool


async def exec_func_async(func, *args, **kwargs):
    is_async = asyncio.iscoroutinefunction(func)
    if is_async:
        return await func(*args, **kwargs)
    else:
        return await run_in_threadpool(func, *args, **kwargs)