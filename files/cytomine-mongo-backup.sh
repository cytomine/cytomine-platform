#!/bin/bash

echo "\n$(date) Start of the backup script."

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
BACKUP_FILENAME="cytomine_mongo_backup_$(date "+%a")" # Use day of the week for unique backup directories

# Full path to the backup file
BACKUP_TARGET_PATH="$BACKUP_DIR/$BACKUP_FILENAME.tar.gz"
mkdir -p $BACKUP_DIR

BACKUP_TMP_DIR="/tmp/backups"
echo "$(date)  Create and clean temporary directory ${BACKUP_TMP_DIR}"
mkdir -p $BACKUP_TMP_DIR
rm -rf "${BACKUP_TMP_DIR:?}/*"

#logging operation
echo "$(date) Backing up cytomine mongo database : $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME into $BACKUP_TMP_DIR "

# Create the backup
# mongodump --host localhost --port 27017 --username=mongoadmin --password=password --db=cytomine --authenticationDatabase cytomine --out="/tmp/backups/"
# -> Auth failure
mongodump --host "$DB_HOST" --port "$DB_PORT" --username "$DB_USER" --password "$DB_PASS" --out "$BACKUP_TMP_DIR"
# Check the exit status of mongodump
if [ $? -ne 0 ]; then
  echo -e "$(date) Could not extract Mongo dump. Aborting backup."
  exit 2
fi

cd $BACKUP_TMP_DIR || exit 3
tar -czf "$BACKUP_TARGET_PATH" ./* 2>&1
# Check the exit status of tar
if [ $? -eq 0 ]; then
  echo "$(date) Backup completed successfully: $BACKUP_TARGET_PATH"
else
  echo "$(date) Backup failed"
  # Clean tmp file
  rm -rf "${BACKUP_TMP_DIR:?}/*"
  exit 4
fi

# Clean tmp files
rm $BACKUP_TMP_DIR -rf

echo "$(date) Backup completed successfully: $BACKUP_DIR"
exit 0