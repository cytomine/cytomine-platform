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
ARG NGINX_VERSION="1.22.1"


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

## Stage: nginx
ARG NGINX_VERSION="1.22.1"
FROM nginx:${NGINX_VERSION}-alpine as nginx-server

ARG IMAGE_VERSION
ARG IMAGE_REVISION
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
      org.opencontainers.image.deps.nginx.upload.module.commit=${UPLOAD_MODULE_COMMIT}

COPY --from=modules-builder /usr/src/nginx/objs/*_module.so /etc/nginx/modules/

# copying entrypoint scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=cytomine/entrypoint-scripts:1.3.0 --chmod=774 /cytomine-entrypoint.sh /usr/local/bin/
COPY --from=cytomine/entrypoint-scripts:1.3.0 --chmod=774 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

ENTRYPOINT ["cytomine-entrypoint.sh", "/docker-entrypoint.sh"]
CMD ["nginx"]