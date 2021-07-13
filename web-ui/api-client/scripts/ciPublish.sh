#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** BUILD JAR ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Publish npm for $VERSION_NUMBER"
echo "$NPM_TOKEN"

if [[ $VERSION_NUMBER =~ v[0-9]+.[0-9]+.[0-9]$ ]]; then
  echo "Official release"

  docker build --rm -f scripts/docker/Dockerfile-publish.build -t cytomine/cytomine-js-client-publish \
          --build-arg VERSION_NUMBER=$VERSION_NUMBER  \
          --build-arg NPM_TOKEN=$NPM_TOKEN  .

  containerId=$(docker create cytomine/cytomine-js-client-publish)

  docker rm $containerId

else
  echo "Snapshot, not publishing it...to be implemented"
fi



