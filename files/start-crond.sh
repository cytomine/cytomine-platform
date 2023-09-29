#!/bin/sh

mkdir -p /var/lib/postgresql/data/backup /tmp
printenv > /tmp/cytomine.postgis.env

/usr/sbin/crond -l 8
echo $0 "Started crond."