#! /usr/bin/env bash

#
#  * Copyright (c) 2020-2021. Authors: see NOTICE file.
#  *
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *      http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.
#

SCRIPT_PATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
VERSION=$(cd $SCRIPT_PATH && cd .. && python -c "from pims import __version__; print(__version__)")
NAMESPACE=cytomineuliege

build_without_plugins() {
  TAG="v${VERSION}"
  # PIMS core
  docker build -f ../docker/backend.dockerfile \
  -t ${NAMESPACE}/pims:${TAG} \
  ..
  # PIMS worker
  docker build -f ../docker/worker.dockerfile \
  --build-arg FROM_NAMESPACE=$NAMESPACE \
  --build-arg FROM_VERSION=$TAG \
  -t ${NAMESPACE}/pims-worker:${TAG} \
  ..
}

build_with_community_plugins() {
  PLUGIN_CSV=$(cat ./plugin-list.csv)
  TAG="v${VERSION}-community-plugins"
  # PIMS core
  docker build -f ../docker/backend.dockerfile \
  --build-arg PLUGIN_CSV="${PLUGIN_CSV}" \
  -t ${NAMESPACE}/pims:${TAG} \
  ..
  # PIMS worker
  docker build -f ../docker/worker.dockerfile \
  --build-arg FROM_NAMESPACE=$NAMESPACE \
  --build-arg FROM_VERSION=$TAG \
  -t ${NAMESPACE}/pims-worker:${TAG} \
  ..
}

build_without_plugins
build_with_community_plugins
