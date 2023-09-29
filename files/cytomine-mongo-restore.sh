#!/bin/bash

echo "\n$(data) Start of the backup script."

# Loading mongo related environment variables
MONGO_ENV_FILE="/tmp/cytomine.mongo.env"
if [ -f "$MONGO_ENV_FILE" ]; then
  source $MONGO_ENV_FILE
else
  echo "$(date) Aborting backup restore: cannot find file $MONGO_ENV_FILE !"
  exit 1
fi

# PostgreSQL database connection parameters
DB_HOST="localhost"                    # Database host
DB_PORT="$MONGO_DB_PORT"               # Database port (default is 27017)
DB_NAME="$MONGO_INIT_DATABASE"         # Database name
DB_USER="$MONGO_INITDB_ROOT_USERNAME"  # Database username
DB_PASS="$MONGO_INITDB_ROOT_PASSWORD"	 # Database password

# Backup directory and filename
RESTORE_DIR="/data/db/backup"      # Specify the backup directory to read the archive to restore from.
RESTORE_FILENAME="restore.tar.gz"  # Use day of the week for unique backup filenames

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

# Prepare a temporary restore directory
TMP_RESTORE_DIR="/tmp/restore"
rm -rf $TMP_RESTORE_DIR
mkdir -p $TMP_RESTORE_DIR

echo -e "\n$(date) Extracting $RESTORE_DIR/$RESTORE_FILENAME to $TMP_RESTORE_DIR"
cd $RESTORE_DIR
tar xzf restore.tar.gz -C $TMP_RESTORE_DIR
if [ $? -ne 0 ]; then
  echo -e "$(date) Could not extract Mongo dump from $RESTORE_DIR/$RESTORE_FILENAME. Aborting restore."
  exit 4
fi

if [ -d "$TMP_RESTORE_DIR/dump" ]; then
  echo -e "$(date) Could not find a Mongo dump. Aborting restore."
  rm -rf $TMP_RESTORE_DIR
  exit 5
fi

echo -e "\n$(date) Restore mongo backups ..."
mongorestore $TMP_RESTORE_DIR
# Check the exit status of mongorestore
if [ $? -ne 0 ]; then
  echo -e "$(date) Could not inject dump. Aborting restore."
  rm -rf $TMP_RESTORE_DIR
  exit 6
fi

echo -e "$(date) The database was successfully restored".

# Clean tmp file
rm -rf $TMP_RESTORE_DIR
exit 0