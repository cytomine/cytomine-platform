#!/bin/bash

# PostgreSQL database connection parameters
DB_HOST="localhost"     # Database host
DB_PORT="$POSTGRES_PORT"          # Database port (default is 5432)
DB_NAME="$POSTGRES_DB_NAME"    # Database name
DB_USER="$POSTGRES_USER"        # Database username


# Backup directory and filename
BACKUP_DIR="/tmp"  # Specify the backup directory
BACKUP_FILENAME="cytomine_postgis_backup_$(date "+%A").sql"  # Use timestamp for unique backup filenames

# Full path to the backup file
BACKUP_PATH="$BACKUP_DIR/$BACKUP_FILENAME"

#logging operation
echo "Backing up cytomine postgis database : $DB_NAME"

# Create the backup
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$BACKUP_PATH"

# Check the exit status of pg_dump
if [ $? -eq 0 ]; then
  echo "Backup completed successfully: $BACKUP_PATH"
else
  echo "Backup failed"
fi
