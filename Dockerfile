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
ARG NGINX_VERSION
ARG SCRIPTS_REPO_TAG
ARG SCRIPTS_REPO_BRANCH
ARG UPLOAD_MODULE_REPO
ARG UPLOAD_MODULE_COMMIT

## Stage 1: building nginx and modules builder
FROM nginx:${NGINX_VERSION}-alpine as modules-builder
ARG UPLOAD_MODULE_REPO
ARG UPLOAD_MODULE_COMMIT

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
    && wget http://nginx.org/download/nginx-$NGINX_VERSION.tar.gz \
    && wget $UPLOAD_MODULE_REPO/archive/$UPLOAD_MODULE_COMMIT.tar.gz

# Download sources
RUN tar -xzvf nginx-$NGINX_VERSION.tar.gz \
    && mv nginx-$NGINX_VERSION /usr/src/nginx

RUN tar -xzvf $UPLOAD_MODULE_COMMIT.tar.gz \
    && mv nginx-upload-module-${UPLOAD_MODULE_COMMIT} /usr/src/nginx-upload-module

WORKDIR /usr/src/nginx

# https://gist.github.com/hermanbanken/96f0ff298c162a522ddbba44cad31081?permalink_comment_id=3221232#gistcomment-3221232
RUN CONFARGS=$(nginx -V 2>&1 | sed -n -e 's/^.*arguments: //p') \
    sh -c "./configure --with-compat $CONFARGS --add-dynamic-module=/usr/src/nginx-upload-module" \
    && make modules

## Stage 2: downloading provisioning scripts
FROM alpine/git:2.36.3 as scripts-downloader
ARG SCRIPTS_REPO_TAG
ARG SCRIPTS_REPO_BRANCH

WORKDIR /root
RUN mkdir scripts
RUN --mount=type=secret,id=scripts_repo_url \
    git clone $(cat /run/secrets/scripts_repo_url) /root/scripts \
    && cd /root/scripts \
    && git checkout tags/${SCRIPTS_REPO_TAG} -b ${SCRIPTS_REPO_BRANCH}

## Stage 3: nginx
ARG NGINX_VERSION
FROM nginx:${NGINX_VERSION}-alpine as nginx-server

COPY --from=modules-builder /usr/src/nginx/objs/*_module.so /etc/nginx/modules/

# copying entrypoint scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=scripts-downloader --chmod=774 /root/scripts/cytomine-entrypoint.sh /usr/local/bin/
COPY --from=scripts-downloader --chmod=774 /root/scripts/envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

ENTRYPOINT ["cytomine-entrypoint.sh", "/docker-entrypoint.sh"]
CMD ["nginx"]