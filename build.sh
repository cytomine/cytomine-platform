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
#
#
# Input environment variables:
# - IMAGE_VERSION: [REQUIRED] an cytomine specific image version to append to nginx version
# - SCRIPTS_REPO_URL: the complete url of the docker scripts repository including credentials (if necessary)
# - SCRIPTS_REPO_TAG: version for extracting initialization scripts
# - NGINX_VERSION: version of the nginx official docker image
# - DOCKER_NAMESPACE: the namespace of the cytomine nginx image built by this repository

set -e

if [ -f .env ]
then
  export $(cat .env | xargs)
fi

IMAGE_VERSION=${IMAGE_VERSION}
NGINX_VERSION=${NGINX_VERSION:-"1.22.1-alpine"}
SCRIPTS_REPO_TAG=${SCRIPTS_REPO_TAG:-"v1.0.0"}
DOCKER_NAMESPACE=${DOCKER_NAMESPACE:-"cytomine"}
SCRIPTS_REPO_BRANCH=${SCRIPTS_REPO_BRANCH:-"master"}

ME=$(basename $0)

if [ -z "$IMAGE_VERSION" ]; then
  echo >&2 "$ME: ERROR: IMAGE_VERSION not provided"
  exit 1
fi

cat Dockerfile | docker build \
  --build-arg NGINX_VERSION=$NGINX_VERSION \
  --build-arg SCRIPTS_REPO_TAG=$SCRIPTS_REPO_TAG \
  --build-arg SCRIPTS_REPO_BRANCH=$SCRIPTS_REPO_BRANCH \
  --secret id=scripts_repo_url,env=SCRIPTS_REPO_URL \
  -t "$DOCKER_NAMESPACE/nginx:$NGINX_VERSION-$IMAGE_VERSION" -