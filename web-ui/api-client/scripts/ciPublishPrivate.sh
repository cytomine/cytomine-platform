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
docker build --rm -f scripts/docker/Dockerfile-publish-private.build -t cytomine/cytomine-js-client-publish-private \
        --build-arg VERSION_NUMBER=$VERSION_NUMBER  \
        --build-arg TAG=$TAG  \
        --build-arg NPM_TOKEN=$NPM_TOKEN  .

containerId=$(docker create cytomine/cytomine-js-client-publish-private)

docker start -ai  $containerId
docker cp "$containerId:/app/cytomine-client-$VERSION_NUMBER.tgz" ./ci

docker rm $containerId
docker rmi cytomine/cytomine-js-client-publish-private

ls

echo "Publish package"
ssh -p 50004 cytomine@185.35.173.82 "mkdir -p /data/js-client" || true
scp -P 50004 "./ci/cytomine-client-$VERSION_NUMBER.tgz" cytomine@185.35.173.82:/data/js-client

CORE_PATH="/data/js-client/cytomine-client-$VERSION_NUMBER.tgz"
echo $CORE_PATH
echo $CORE_PATH > ./ci/path



fi
#else
#  echo "Snapshot, not publishing it...to be implemented"
#fi



