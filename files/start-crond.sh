#!/bin/sh

# The file /mongo.env is needed by the backup script 
# to retrieve mongo db credentials to run the backups.
printenv | grep MONGO >> /tmp/cytomine.mongo.env

service cron start

echo $0 "Started crond."
