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

ARG POSTGIS_VERSION
ARG SCRIPTS_REPO_TAG
ARG SCRIPTS_REPO_BRANCH

## Stage 1: downloading provisioning scripts
FROM alpine/git:2.36.3 as downloader
ARG SCRIPTS_REPO_TAG
ARG SCRIPTS_REPO_BRANCH

WORKDIR /root
RUN mkdir scripts
RUN --mount=type=secret,id=scripts_repo_url \
    git clone $(cat /run/secrets/scripts_repo_url) /root/scripts \
    && cd /root/scripts \
    && git checkout tags/${SCRIPTS_REPO_TAG} -b ${SCRIPTS_REPO_BRANCH}


# Stage 2: Postgis
ARG POSTGIS_VERSION
FROM postgis/postgis:${POSTGIS_VERSION}

#set default user (and default DB name) to docker by default
ENV POSTGRES_USER=docker

# database init
COPY files/initdb-cytomine.sh /docker-entrypoint-initdb.d/11_cytomine.sh

# default configuration
RUN mkdir -p /etc/postgres/conf.d
COPY files/postgres.conf /etc/postgres/postgres.conf
COPY files/postgres.default.conf /etc/postgres/00-default.conf

COPY --from=downloader --chmod=774 /root/scripts/cytomine-entrypoint.sh /usr/local/bin/
COPY --from=downloader --chmod=774 /root/scripts/envsubst-on-templates-and-move.sh /docker-entrypoint.d/050-envsubst-on-templates-and-move.sh

ENTRYPOINT ["cytomine-entrypoint.sh", "docker-entrypoint.sh"]
CMD ["postgres", "-c", "config_file=/etc/postgres/postgres.conf"]