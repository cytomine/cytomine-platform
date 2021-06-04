#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** BUILD JAR ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Publish npm for $VERSION_NUMBER"

if [[ $VERSION_NUMBER =~ [0-9]+.[0-9]+.[0-9]$ ]]; then
  echo "Official release"

  docker build --rm -f scripts/docker/Dockerfile-publish.build -t cytomine/cytomine-js-client-publish \
          --build-arg VERSION_NUMBER=$VERSION_NUMBER  \
          --build-arg NPM_KEY=$NPM_KEY  .

  containerId=$(docker create cytomine/cytomine-js-client-publish)

  docker rm $containerId

else
  echo "Snapshot, not publishing it...to be implemented"
fi



