#!/bin/bash

# PostgreSQL database connection parameters
DB_HOST="localhost"  # Database host
DB_PORT="$MONGO_DB_PORT"  # Database port (default is 27017)
DB_NAME="$MONGO_INIT_DATABASE"  # Database name
DB_USER="$MONGO_INITDB_ROOT_USERNAME"  # Database username
DB_PASS="$MONGO_INITDB_ROOT_PASSWORD"	 # Database password

# Backup directory and filename
BACKUP_DIR="/data/db/backups" 
BACKUP_FILENAME="cytomine_mongo_backup_$(date "+%A")" # Use day of the week for unique backup directories

# Full path to the backup file
BACKUP_TARGET_PATH="$BACKUP_DIR/$BACKUP_FILENAME.tar.gz"
mkdir -p $BACKUP_DIR

# Create and clean temporary directory
BACKUP_TMP_DIR="/tmp/backups"
mkdir -p $BACKUP_TMP_DIR
rm $BACKUP_TMP_DIR/* -r

#logging operation
echo "Backing up cytomine mongo database : $DB_NAME"

# Create the backup
mongodump --host "$DB_HOST" --port "$DB_PORT" --username "$DB_USER" --password "$DB_PASS" --out "$BACKUP_TMP_DIR"
tar -czvf $BACKUP_TARGET_PATH -C $BACKUP_TMP_DIR .

# Clean tmp files
rm $BACKUP_TMP_DIR -r

# Check the exit status of mongodump
if [ $? -eq 0 ]; then
  echo "Backup completed successfully: $BACKUP_DIR"
else
  echo "Backing up cytomine mongo database failed"
fi
