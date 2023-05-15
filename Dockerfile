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

ARG POSTGIS_VERSION="15-3.3-alpine"

FROM postgis/postgis:${POSTGIS_VERSION}
LABEL maintainer="Cytomine <dev@cytomine.com>"

#set default user (and default DB name) to docker by default
ENV POSTGRES_USER=docker

# database init
COPY files/initdb-cytomine-extensions.sh /docker-entrypoint-initdb.d/11_cytomine_extensions.sh
COPY files/initdb-cytomine-users.sh /docker-entrypoint-initdb.d/12_cytomine_users.sh

# default configuration
RUN mkdir -p /etc/postgres/conf.d
COPY files/postgres.conf /etc/postgres/postgres.conf
COPY files/postgres.default.conf /etc/postgres/00-default.conf

RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=cytomine/entrypoint-scripts:1.2.0 --chmod=774 /cytomine-entrypoint.sh /usr/local/bin/
COPY --from=cytomine/entrypoint-scripts:1.2.0 --chmod=774 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

VOLUME ["/var/lib/postgresql/data"]

ENTRYPOINT ["cytomine-entrypoint.sh", "docker-entrypoint.sh"]
CMD ["postgres", "-c", "config_file=/etc/postgres/postgres.conf"]