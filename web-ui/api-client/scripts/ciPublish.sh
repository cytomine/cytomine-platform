#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** BUILD JAR ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Publish npm for $VERSION_NUMBER"
echo "$NPM_TOKEN"


if [[ $VERSION_NUMBER =~ [0-9]+.[0-9]+.[0-9]+-beta.[0-9]+$ ]]; then
  echo "BETA"
  TAG=beta
elif [[ $VERSION_NUMBER =~ [0-9]+.[0-9]+.[0-9]+$ ]]; then
  echo "OFFICIAL"
  TAG=latest
else
  echo "WARNING: invalid tag for an official release, do not publish; if you want to publish a beta: write something like this: git tag -a v1.2.3-beta.3"
  TAG=none
fi


if [[ $TAG -ne "none" ]]; then
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



