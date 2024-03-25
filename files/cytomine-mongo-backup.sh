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
CURR_MONTH_BACKUP_FILENAME="cytomine_mongo_backup_current_month"
PREV_MONTH_BACKUP_FILENAME="cytomine_mongo_backup_previous_month"

mkdir -p $BACKUP_DIR

# Full path to the backup file
BACKUP_TARGET_PATH="$BACKUP_DIR/$BACKUP_FILENAME.tar.gz"
CURR_MONTH_BACKUP_TARGET_PATH="$BACKUP_DIR/$CURR_MONTH_BACKUP_FILENAME.tar.gz"
PREV_MONTH_BACKUP_TARGET_PATH="$BACKUP_DIR/$PREV_MONTH_BACKUP_FILENAME.tar.gz"

BACKUP_TMP_DIR="/data/db/backup/tmp/backups"
echo "$(date)  Create and clean temporary directory ${BACKUP_TMP_DIR}"

mkdir -p $BACKUP_TMP_DIR
rm -rf "${BACKUP_TMP_DIR:?}/*"

#logging operation
echo "$(date) Backing up cytomine mongo database : $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME into $BACKUP_TMP_DIR "

# Create the backup
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

# If first of the month, update the current month backup with the daily backup. If not first of the month but current month backup does not exist (new instances), update the current month backup with the daily backup
if [ $(date "+%d") -eq 01 ] || [ ! -f "$CURR_MONTH_BACKUP_TARGET_PATH" ]; then
  # If the current month backup exists, move it to the previous month
  if [ -f "$CURR_MONTH_BACKUP_TARGET_PATH" ]; then
    cp "$CURR_MONTH_BACKUP_TARGET_PATH" "$PREV_MONTH_BACKUP_TARGET_PATH"
    echo "$(date) Archived old monthly backup '$CURR_MONTH_BACKUP_TARGET_PATH' as '$PREV_MONTH_BACKUP_TARGET_PATH'"
  fi
  # Update the current month backup with the daily backup
  cp "$BACKUP_TARGET_PATH" "$CURR_MONTH_BACKUP_TARGET_PATH"
  echo "$(date) Archived new monthly backup as '$CURR_MONTH_BACKUP_TARGET_PATH'"
fi

echo "$(date) Backup completed successfully: $BACKUP_DIR"
exit 0