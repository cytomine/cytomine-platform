# syntax=docker/dockerfile:1

# Copyright (c) 2009-2022. Authors: see NOTICE file.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ARG MONGO_VERSION=4.4.18-focal
ARG ENTRYPOINT_SCRIPTS_VERSION=1.3.0
ARG IMAGE_VERSION
ARG IMAGE_REVISION

#######################################################################################
## Stage 1: entrypoint script. Use a multi-stage because COPY --from cannot interpolate variables
FROM cytomine/entrypoint-scripts:${ENTRYPOINT_SCRIPTS_VERSION} as entrypoint-scripts

## Stage 2: mongo image
FROM mongo:${MONGO_VERSION}

RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=downloader --chmod=774 /root/scripts/cytomine-entrypoint.sh /usr/local/bin/
COPY --from=downloader --chmod=774 /root/scripts/envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

COPY --chmod=744 mongo-entrypoint.sh /mongo-entrypoint.sh

ENTRYPOINT ["/mongo-entrypoint.sh", "cytomine-entrypoint.sh", "docker-entrypoint.sh"]
CMD ["mongod"]