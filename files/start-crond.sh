#!/bin/sh

mkdir -p /tmp
printenv > /tmp/cytomine.postgis.env

/usr/sbin/crond -l 8
echo $0 "Started crond."