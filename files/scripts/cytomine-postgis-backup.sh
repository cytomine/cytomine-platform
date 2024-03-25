#!/bin/bash

echo -e "\n$(date) Start of backup script"

# Loading postgis related environment variables
POSTGIS_ENV_FILE="/tmp/cytomine.postgis.env"
if [ -f "$POSTGIS_ENV_FILE" ]; then
  source $POSTGIS_ENV_FILE
else
  echo "$(date) Aborting backup: cannot find file $POSTGIS_ENV_FILE !"
  exit 1
fi

# PostgreSQL database connection parameters
DB_USER="$POSTGRES_USER"        # Database username

# Backup directory and filename
BACKUP_DIR="/var/lib/postgresql/data/backup"  # Specify the backup directory
BACKUP_FILENAME="cytomine_postgis_backup_$(date "+%a")"  # Use day of the week for unique backup filenames
CURR_MONTH_BACKUP_FILENAME="cytomine_postgis_backup_current_month"
PREV_MONTH_BACKUP_FILENAME="cytomine_postgis_backup_previous_month"

mkdir -p $BACKUP_DIR

# Full path to the backup file
BACKUP_TARGET_PATH="$BACKUP_DIR/$BACKUP_FILENAME.tar.gz"
CURR_MONTH_BACKUP_TARGET_PATH="$BACKUP_DIR/$CURR_MONTH_BACKUP_FILENAME.tar.gz"
PREV_MONTH_BACKUP_TARGET_PATH="$BACKUP_DIR/$PREV_MONTH_BACKUP_FILENAME.tar.gz"
BACKUP_TMP_PATH="/var/lib/postgresql/data/backup/tmp"

#logging operation
echo -e "\n$(date) Backing up cytomine postgis databases ... $DB_USER"

# Create the backup
export LC_TIME=en_US.UTF-8 # to change the language of the date to English
pg_dumpall --username="$DB_USER" --clean -f "/$BACKUP_TMP_PATH/$BACKUP_FILENAME.sql"
# Check the exit status of pg_dump
if [ $? -ne 0 ]; then
  echo -e "$(date) Could not extract SQL dump. Aborting backup."
  exit 1
fi

cd $BACKUP_TMP_PATH
tar -czf "$BACKUP_TARGET_PATH" "$BACKUP_FILENAME".sql 2>&1
# Check the exit status of tar
if [ $? -eq 0 ]; then
  echo "$(date) Backup completed successfully: $BACKUP_TARGET_PATH"
else
  echo "$(date) Backup failed"
  # Clean tmp file
  rm "/$BACKUP_TMP_PATH/$BACKUP_FILENAME.sql"
  exit 2
fi

# Clean tmp file
rm "/$BACKUP_TMP_PATH/$BACKUP_FILENAME.sql"

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


exit 0