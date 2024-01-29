#!/bin/sh

BACKUP_FOLDER=/var/lib/postgresql/backup
BACKUP_LOG_FILE=$BACKUP_FOLDER/backup.log

mkdir -p $BACKUP_FOLDER
touch $BACKUP_LOG_FILE
chmod 777 $BACKUP_LOG_FILE

if [ $? -ne 0 ]; then
  echo "$0" "Could not touch backup log file '$BACKUP_LOG_FILE', please check mount folder permissions."
  exit 1
else
  echo "$0" "Could touch backup log file '$BACKUP_LOG_FILE', proceeding ..."
fi

# cleaning up
rm -r $BACKUP_FOLDER