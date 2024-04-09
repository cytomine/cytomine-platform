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

# set default superadmin user postgres + set defaults for component specific databass sdb and user
ENV APPENGINE_DB=appengine
ENV APPENGINE_USER=appengine
ENV IAM_DB=iam
ENV IAM_USER=iam

# database init. Warning: those are only run if data volume is empty
RUN mkdir -p /etc/postgres/conf.d /docker-entrypoint-cytomine.d/ /docker-entrypoint-initdb.d/ /checks/
COPY files/initdb/initdb-cytomine-extensions.sql /docker-entrypoint-initdb.d/11_cytomine-extensions.sql

# default configuration
COPY files/conf/postgres.conf /etc/postgres/postgres.conf
COPY files/conf/postgres.default.conf /etc/postgres/00-default.conf

# entry points. Triggered at every container starts
COPY files/scripts/check-backup-folder.sh /docker-entrypoint-cytomine.d/550-check-backup-folder.sh
COPY files/scripts/start-crond.sh /docker-entrypoint-cytomine.d/600-start-crond.sh
COPY files/scripts/check_dbs_users.sh /docker-entrypoint-cytomine.d/700-check_dbs_users.sh
COPY files/checks /checks

# backup and restore scripts
COPY files/scripts/backup-cron-job /backup-cron-job
COPY files/scripts/cytomine-postgis-backup.sh /usr/local/bin/backup
COPY files/scripts/cytomine-postgis-restore.sh /usr/local/bin/restore

RUN chmod +x /usr/local/bin/backup /usr/local/bin/restore /docker-entrypoint-cytomine.d/*.sh /checks/*.sh && \
    chmod 0644 /backup-cron-job && \
    chmod u+s /usr/bin/crontab && \
    crontab /backup-cron-job && \
    chmod 0700 /checks -R

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

ENTRYPOINT ["cytomine-entrypoint.sh", "docker-entrypoint.sh"]
CMD ["postgres", "-c", "config_file=/etc/postgres/postgres.conf"]
