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

ARG ENTRYPOINT_SCRIPTS_VERSION="1.3.0"
ARG IMAGE_VERSION
ARG IMAGE_REVISION
ARG POSTGIS_VERSION="15-3.3-alpine"

#######################################################################################
## Stage: entrypoint script. Use a multi-stage because COPY --from cannot interpolate variables
FROM cytomine/entrypoint-scripts:${ENTRYPOINT_SCRIPTS_VERSION} as entrypoint-scripts

FROM postgis/postgis:${POSTGIS_VERSION}
ARG ENTRYPOINT_SCRIPTS_VERSION
ARG IMAGE_VERSION
ARG IMAGE_REVISION
ARG POSTGIS_VERSION

#set default user (and default DB name) to docker by default
ENV POSTGRES_USER=docker

# database init
COPY files/initdb-cytomine-extensions.sh /docker-entrypoint-initdb.d/11_cytomine_extensions.sh
COPY files/initdb-cytomine-users.sh /docker-entrypoint-initdb.d/12_cytomine_users.sh

# default configuration
RUN mkdir -p /etc/postgres/conf.d
COPY files/postgres.conf /etc/postgres/postgres.conf
COPY files/postgres.default.conf /etc/postgres/00-default.conf

# daily backup configuration
COPY files/cytomine_postgis_backup.sh /etc/poeriodic/daily/cytomine_postgis_backup.sh
RUN chmod +x /etc/periodic/daily/cytomine_postgis_backup.sh


RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=entrypoint-scripts --chmod=774 /cytomine-entrypoint.sh /usr/local/bin/
COPY --from=entrypoint-scripts --chmod=774 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

LABEL org.opencontainers.image.authors='support@cytomine.com' \
      org.opencontainers.image.url='https://www.cytomine.org/' \
      org.opencontainers.image.documentation='https://doc.cytomine.org/' \
      org.opencontainers.image.source='https://github.com/cytomine/Cytomine-postgis' \
      org.opencontainers.image.vendor='Cytomine Corporation SA' \
      org.opencontainers.image.version=${IMAGE_VERSION} \
      org.opencontainers.image.revision=${IMAGE_REVISION} \
      org.opencontainers.image.deps.postgis.version=${POSTGIS_VERSION} \
      org.opencontainers.image.deps.entrypoint.scripts.version=${ENTRYPOINT_SCRIPTS_VERSION}

VOLUME ["/var/lib/postgresql/data"]
VOLUME ["/data/backups"]

ENTRYPOINT ["cytomine-entrypoint.sh", "docker-entrypoint.sh"]
CMD ["postgres", "-c", "config_file=/etc/postgres/postgres.conf"]
