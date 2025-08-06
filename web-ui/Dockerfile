#
# Copyright (c) 2009-2020. Authors: see NOTICE file.
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

ARG WEB_UI_VERSION
ARG WEB_UI_REVISION
ARG NODE_VERSION=20.19.4-alpine3.22
ARG NODE_DEV_VERSION=20.19.4-bookworm-slim
ARG NGINX_VERSION=1.22.1
ARG ENTRYPOINT_SCRIPTS_VERSION=1.4.0

#######################################################################################
## Stage: build front-end
FROM node:${NODE_VERSION} AS frontend-builder

WORKDIR /app

# explicit listing for copy, to avoid copying dangling ./node_modules or alike

COPY ./lerna.json /app/lerna.json
COPY ./package.json /app/package.json
COPY ./package-lock.json /app/package-lock.json

COPY ./ui/src /app/ui/src
COPY ./ui/public /app/ui/public
COPY ./ui/scripts /app/ui/scripts
COPY ./ui/package.json /app/ui/package.json
COPY ./ui/babel.config.js /app/ui/babel.config.js
COPY ./ui/.eslintrc.js /app/ui/.eslintrc.js

COPY ./api-client/src /app/api-client/src
COPY ./api-client/package.json /app/api-client/package.json
COPY ./api-client/webpack.config.js /app/api-client/webpack.config.js

WORKDIR /app
RUN npm install \
    && npm run build -w ui

#######################################################################################
## Stage: entrypoint script. Use a multi-stage because COPY --from cannot interpolate variables
FROM cytomine/entrypoint-scripts:${ENTRYPOINT_SCRIPTS_VERSION} AS entrypoint-scripts

#######################################################################################
## Stage: dev image (with hot swap)
FROM node:${NODE_DEV_VERSION} AS dev-server

# ui source code should be mounted in /app
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends gettext git

RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=entrypoint-scripts --chmod=744 /cytomine-entrypoint.sh /usr/local/bin/
COPY --from=entrypoint-scripts --chmod=744 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh
COPY --from=entrypoint-scripts --chmod=744 /run-npm-install.sh /docker-entrypoint-cytomine.d/750-run-npm-install.sh
COPY ./ui/scripts/configuration.json /cm_configs/app/ui/public/configuration.json.sample

ENTRYPOINT ["cytomine-entrypoint.sh", "docker-entrypoint.sh"]
# rebuild (ignoring current node-module folder) then serve
CMD ["npm", "run", "serve", "-w", "ui"]

#######################################################################################
## Stage: building nginx image serving the front-end
FROM nginx:${NGINX_VERSION}-alpine AS prod-server

ARG WEB_UI_VERSION
ARG WEB_UI_REVISION
ARG NGINX_VERSION
ARG NODE_VERSION
ARG ENTRYPOINT_SCRIPTS_VERSION

LABEL org.opencontainers.image.authors="uliege@cytomine.org" \
      org.opencontainers.image.url="https://uliege.cytomine.org/" \
      org.opencontainers.image.documentation="https://doc.cytomine.com/" \
      org.opencontainers.image.source="https://github.com/cytomine/Cytomine-web-ui" \
      org.opencontainers.image.vendor="Cytomine ULiège" \
      org.opencontainers.image.version=${WEB_UI_VERSION} \
      org.opencontainers.image.revision=${WEB_UI_REVISION} \
      org.opencontainers.image.deps.nginx.version=${NGINX_VERSION} \
      org.opencontainers.image.deps.node.version=${NODE_VERSION} \
      org.opencontainers.image.deps.entrypoint.scripts.version=${ENTRYPOINT_SCRIPTS_VERSION}

WORKDIR /app

# copying entrypoint scripts
RUN mkdir /docker-entrypoint-cytomine.d/
# COPY --from cannot interpolate variables
COPY --from=entrypoint-scripts --chmod=774 /cytomine-entrypoint.sh /usr/local/bin/
COPY --from=entrypoint-scripts --chmod=774 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

COPY ./ui/docker/configs/nginx.conf /etc/nginx/nginx.conf
COPY --chown=1000:1000 ./ui/scripts/configuration.json /cm_configs/app/configuration.json.sample

# actual front end
COPY --from=frontend-builder /app/ui/dist /app

ENTRYPOINT ["cytomine-entrypoint.sh", "/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
