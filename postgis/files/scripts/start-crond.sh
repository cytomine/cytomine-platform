#!/bin/sh

mkdir -p /tmp
printenv | grep POSTGRES > /tmp/cytomine.postgis.env

/usr/sbin/crond -l 8
echo $0 "Started crond."