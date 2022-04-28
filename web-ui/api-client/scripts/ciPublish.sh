#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** BUILD JAR ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

file='./ci/tag'
TAG=$(<"$file")

echo "Publish npm for $VERSION_NUMBER"
echo "$NPM_TOKEN"


echo $TAG
echo $VERSION_NUMBER


if [[ $TAG != "none" ]]; then
docker build --rm -f scripts/docker/Dockerfile-publish.build -t cytomine/cytomine-js-client-publish \
        --build-arg VERSION_NUMBER=$VERSION_NUMBER  \
        --build-arg TAG=$TAG  \
        --build-arg NPM_TOKEN=$NPM_TOKEN  .

containerId=$(docker create cytomine/cytomine-js-client-publish)

docker rm $containerId
fi
#else
#  echo "Snapshot, not publishing it...to be implemented"
#fi



