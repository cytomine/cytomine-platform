#!/bin/bash

# PostgreSQL database connection parameters
DB_HOST="localhost"  # Database host
DB_PORT="$MONGODB_PORT"  # Database port (default is 27017)
DB_NAME="$MONGODB_DB_NAME"  # Database name
DB_USER="$MONGODB_USER"  # Database username
DB_PASS="$MONGODB_PASS"	 # Database password

# Backup directory and filename
BACKUP_DIR="/backups" 
BACKUP_FILENAME="cytomine_mongo_backup_$(date "+%A")" # Use day of the week for unique backup directories

mkdir -p $BACKUP_DIR

# Full path to the backup file
BACKUP_TARGET_PATH="$BACKUP_DIR/$BACKUP_FILENAME.tar.gz"
BACKUP_TMP_PATH="/tmp/$BACKUP_FILENAME.sql"

#logging operation
echo "Backing up cytomine mongo database : $DB_NAME"

# Create the backup
export LC_TIME=en_US.UTF-8 # to change the language of the date to English
mongodump --host "$DB_HOST" --port "$DB_PORT" --username "$DB_USER" --password "$DB_PASS" --out "$BACKUP_DIR"
tar -czvf $BACKUP_TARGET_PATH $BACKUP_TMP_PATH

# Clean tmp file
rm $BACKUP_TMP_PATH

# Check the exit status of mongodump
if [ $? -eq 0 ]; then
  echo "Backup completed successfully: $BACKUP_DIR"
else
  echo "Backing up cytomine mongo database failed"
fi
