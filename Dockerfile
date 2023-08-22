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

ARG ENTRYPOINT_SCRIPTS_VERSION=1.3.0
ARG IMAGE_VERSION
ARG IMAGE_REVISION
ARG NGINX_VERSION="1.22.1"
ARG UPLOAD_MODULE_REPO="https://github.com/fdintino/nginx-upload-module"
ARG UPLOAD_MODULE_COMMIT="643b4c1fa6993da6bc1f82e7121ca62a7696ee6b"


## Stage: building nginx and modules builder
FROM nginx:${NGINX_VERSION}-alpine as modules-builder
ARG UPLOAD_MODULE_REPO="https://github.com/fdintino/nginx-upload-module"
ARG UPLOAD_MODULE_COMMIT="643b4c1fa6993da6bc1f82e7121ca62a7696ee6b"


# For latest build deps, see https://github.com/nginxinc/docker-nginx/blob/master/mainline/alpine/Dockerfile
RUN  apk add --no-cache --virtual .build-deps \
    gcc \
    libc-dev \
    make \
    openssl-dev \
    pcre-dev \
    zlib-dev \
    linux-headers \
    libxslt-dev \
    gd-dev \
    geoip-dev \
    perl-dev \
    libedit-dev \
    mercurial \
    bash \
    alpine-sdk \
    findutils

SHELL ["/bin/ash", "-eo", "pipefail", "-c"]

RUN rm -rf /usr/src/nginx \
    && mkdir -p /usr/src \
    && wget --quiet "http://nginx.org/download/nginx-${NGINX_VERSION}.tar.gz" \
    && wget --quiet "${UPLOAD_MODULE_REPO}/archive/${UPLOAD_MODULE_COMMIT}.tar.gz"

# Download sources
RUN tar -xzvf "nginx-${NGINX_VERSION}.tar.gz" \
    && mv "nginx-${NGINX_VERSION}" /usr/src/nginx

RUN tar -xzvf "${UPLOAD_MODULE_COMMIT}.tar.gz" \
    && mv "nginx-upload-module-${UPLOAD_MODULE_COMMIT}" /usr/src/nginx-upload-module

WORKDIR /usr/src/nginx

# https://gist.github.com/hermanbanken/96f0ff298c162a522ddbba44cad31081?permalink_comment_id=3221232#gistcomment-3221232
RUN CONFARGS=$(nginx -V 2>&1 | sed -n -e 's/^.*arguments: //p') \
    sh -c "./configure --with-compat ${CONFARGS} --add-dynamic-module=/usr/src/nginx-upload-module" \
    && make modules

#######################################################################################
## Stage: entrypoint script. Use a multi-stage because COPY --from cannot interpolate variables
FROM cytomine/entrypoint-scripts:${ENTRYPOINT_SCRIPTS_VERSION} as entrypoint-scripts

## Stage: nginx
FROM nginx:${NGINX_VERSION}-alpine as nginx-server

ENV IMAGES_BIOFORMAT="not provided"
ENV IMAGES_CORE="not provided"
ENV IMAGES_MONGO="not provided"
ENV IMAGES_NGINX="not provided"
ENV IMAGES_PIMS="not provided"
ENV IMAGES_PIMS_CACHE="not provided"
ENV IMAGES_POSTGIS="not provided"
ENV IMAGES_RABBITMQ="not provided"
ENV IMAGES_WEB_UI="not provided"
ENV INTERNAL_URLS_CORE=core:8080
ENV INTERNAL_URLS_IMS=pims:5000
ENV INTERNAL_URLS_IMS2=pims:5000
ENV INTERNAL_URLS_IMS3=pims:5000
ENV INTERNAL_URLS_WEB_UI=web_ui
ENV UPLOAD_PATH=/tmp/uploaded
ENV URLS_CORE=cytomine.local
ENV URLS_IMAGE_SERVER=ims.cytomine.local
ENV URLS_IMAGE_SERVER2=ims.cytomine.local
ENV URLS_IMAGE_SERVER3=ims.cytomine.local
ENV URLS_UPLOAD=upload.cytomine.local
ENV VERSIONS_CYTOMINE_COMMERCIAL="not provided"

ARG ENTRYPOINT_SCRIPTS_VERSION
ARG IMAGE_VERSION
ARG IMAGE_REVISION
ARG NGINX_VERSION="1.22.1"
ARG UPLOAD_MODULE_REPO="https://github.com/fdintino/nginx-upload-module"
ARG UPLOAD_MODULE_COMMIT="643b4c1fa6993da6bc1f82e7121ca62a7696ee6b"

LABEL org.opencontainers.image.authors='support@cytomine.com' \
      org.opencontainers.image.url='https://www.cytomine.org/' \
      org.opencontainers.image.documentation='https://doc.cytomine.org/' \
      org.opencontainers.image.source='https://github.com/cytomine/Cytomine-nginx/' \
      org.opencontainers.image.vendor='Cytomine Corporation SA' \
      org.opencontainers.image.version=${IMAGE_VERSION} \
      org.opencontainers.image.revision=${IMAGE_REVISION} \
      org.opencontainers.image.deps.nginx.version=${NGINX_VERSION} \
      org.opencontainers.image.deps.nginx.upload.module.repo=${UPLOAD_MODULE_REPO} \
      org.opencontainers.image.deps.nginx.upload.module.commit=${UPLOAD_MODULE_COMMIT} \
      org.opencontainers.image.deps.entrypoint.scripts.version=${ENTRYPOINT_SCRIPTS_VERSION}

COPY --from=modules-builder /usr/src/nginx/objs/*_module.so /etc/nginx/modules/

# copying entrypoint scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=entrypoint-scripts --chmod=774 /cytomine-entrypoint.sh /usr/local/bin/
COPY --from=entrypoint-scripts --chmod=774 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

COPY --chmod=774 nginx-entrypoint.sh /nginx-entrypoint.sh
COPY --chown=1000:1000 cm_configs_default /cm_configs_default

ENTRYPOINT ["/nginx-entrypoint.sh", "cytomine-entrypoint.sh", "/docker-entrypoint.sh"]
CMD ["nginx"]