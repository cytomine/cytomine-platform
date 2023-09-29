#!/bin/bash

echo "\n$(data) Start of the backup script."

# Loading mongo related environment variables
MONGO_ENV_FILE="/tmp/cytomine.mongo.env"
if [ -f "$MONGO_ENV_FILE" ]; then
  source $MONGO_ENV_FILE
else
  echo "$(date) Aborting backup: cannot find file $MONGO_ENV_FILE !"
  exit 1
fi

# PostgreSQL database connection parameters
DB_HOST="localhost"  # Database host
DB_PORT="$MONGO_DB_PORT"  # Database port (default is 27017)
DB_NAME="$MONGO_INIT_DATABASE"  # Database name
DB_USER="$MONGO_INITDB_ROOT_USERNAME"  # Database username
DB_PASS="$MONGO_INITDB_ROOT_PASSWORD"	 # Database password

# Backup directory and filename
BACKUP_DIR="/data/db/backup" 
BACKUP_FILENAME="cytomine_mongo_backup_$(date "+%A")" # Use day of the week for unique backup directories

# Full path to the backup file
BACKUP_TARGET_PATH="$BACKUP_DIR/$BACKUP_FILENAME.tar.gz"
mkdir -p $BACKUP_DIR

# Create and clean temporary directory
BACKUP_TMP_DIR="/tmp/backups"
mkdir -p $BACKUP_TMP_DIR
rm $BACKUP_TMP_DIR/* -rf

#logging operation
echo "$(date) Backing up cytomine mongo database : $DB_NAME"

# Create the backup
mongodump --host "$DB_HOST" --port "$DB_PORT" --username "$DB_USER" --password "$DB_PASS" --out "$BACKUP_TMP_DIR"
# Check the exit status of mongodump
if [ $? -ne 0 ]; then
  echo -e "$(date) Could not extract Mongo dump. Aborting backup."
  exit 1
fi

cd $BACKUP_TMP_PATH
tar -czvf $BACKUP_TARGET_PATH -C $BACKUP_TMP_DIR . 2>&1
# Check the exit status of tar
if [ $? -eq 0 ]; then
  echo "$(date) Backup completed successfully: $BACKUP_TARGET_PATH"
else
  echo "$(date) Backup failed"
  # Clean tmp file
  rm "$BACKUP_TMP_DIR/*" -rf
  exit 2
fi

# Clean tmp files
rm $BACKUP_TMP_DIR -rf

echo "$(date) Backup completed successfully: $BACKUP_DIR"
