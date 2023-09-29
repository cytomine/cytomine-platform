#!/bin/bash

source /tmp/cytomine.postgis.env
echo -e "\n$(date) Start of restore script"

# PostgreSQL database connection parameters
DB_USER="$POSTGRES_USER"        # Database username


# Backup directory and filename
RESTORE_DIR="/var/lib/postgresql/data/backup"   # Specify the backup directory to read the archive to restore from.
RESTORE_FILENAME="restore.tar.gz"               # Use day of the week for unique backup filenames

# Check if restore directory is there
if [ ! -d "$RESTORE_DIR" ]; then
  echo "Directory $RESTORE_DIR DOES NOT exists. Please provide the backup archive you want to restore at $RESTORE_DIR/$RESTORE_FILENAME. Aborting."
  exit 1
fi

# Check if restore archive is there
if [ ! -f "$RESTORE_DIR/$RESTORE_FILENAME" ]; then
  echo "Cannot find backup archive $RESTORE_DIR/$RESTORE_FILENAME. Make sure to place your backup archive there with this filename, case sensitive. Aborting."
  exit 2
fi

# Check restore archive size
if (( $(stat -c%s "$RESTORE_DIR/$RESTORE_FILENAME") < 1000 )); then
  echo "The archive $RESTORE_DIR/$RESTORE_FILENAME seems incomplete. Aborting"
  exit 3
fi

echo -e "\n$(date) Extracting $RESTORE_DIR/$RESTORE_FILENAME to /tmp/db_to_restore.sql $DB_USER"
cd $RESTORE_DIR
mkdir -p /tmp/cytomine-restore
tar xzf restore.tar.gz -C /tmp/cytomine-restore
if [ $? -ne 0 ]; then
  echo -e "$(date) Could not extract SQL dump from $RESTORE_DIR/$RESTORE_FILENAME. Aborting restore."
  exit 4
fi

mv /tmp/cytomine-restore/*.sql /tmp/cytomine-restore/restore.sql
if [ $? -ne 0 ]; then
  echo -e "$(date) Could not find a SQL file dump. Aborting restore."
  rm -rf /tmp/cytomine-restore
  exit 5
fi


echo -e "\n$(date) Drop database ..."
dropdb --force --if-exists --username="$DB_USER" docker
if [ $? -ne 0 ]; then
  echo -e "$(date) Could not drop database. Aborting restore."
  rm -rf /tmp/cytomine-restore
  exit 6
fi

echo -e "\n$(date) Import postgis databases ..."
psql --username="$DB_USER" --dbname="postgres" --file=/tmp/cytomine-restore/restore.sql
# Check the exit status of pg_dupsqlmp
if [ $? -ne 0 ]; then
  echo -e "$(date) Could not inject dump. Aborting restore."
  rm -rf /tmp/cytomine-restore
  exit 6
fi

echo -e "$(date) The database was successfully restored".

# Clean tmp file
rm -rf /tmp/cytomine-restore
exit 0