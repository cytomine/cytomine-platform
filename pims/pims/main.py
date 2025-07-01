# * Copyright (c) 2020. Authors: see NOTICE file.
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# *      http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
import os
from distutils.util import strtobool

if __name__ == "__main__":
    import uvicorn

    log_config = "logging.yml"
    debug = bool(strtobool(os.getenv('DEBUG', 'false')))
    if debug:
        log_config = "logging-debug.yml"
    log_config = os.getenv('LOG_CONFIG_FILE', log_config)

    uvicorn.run(
        "pims.application:app",
        host="0.0.0.0",
        port=5000,
        log_config=log_config,
        reload=debug
    )
