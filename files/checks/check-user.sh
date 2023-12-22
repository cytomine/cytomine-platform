#!/bin/bash

CHECK_NAME="$1"
USER="$2"
PASSWORD="$3"
DB="$4"

echo "Starting '${CHECK_NAME}'."

if [ -z "$USER" ] || [ -z "$PASSWORD" ] || [ -z "$DB" ]; then
  echo "$0" "Skipping '${CHECK_NAME}' pre-configuration because * env variables are empty or undefined"
  exit
fi

echo "$0" "Creating database '$DB' using user '$POSTGRES_USER'. An error is expected if it already exists.";

# DB
psql -U "$POSTGRES_USER" -c "CREATE DATABASE $DB" >/dev/null

# Grants
echo "$0" "Grant roles to '$USER' for database '$DB'. A notice is expected if they already exist.";

psql -U "$POSTGRES_USER" >/dev/null <<- EOSQL
DO
\$do\$
BEGIN
   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$USER') THEN
      RAISE NOTICE 'Role "$USER" already exists. Skipping.';
   ELSE
      CREATE ROLE $USER LOGIN PASSWORD '$PASSWORD';
      ALTER ROLE $USER SET client_encoding TO 'utf8';
      ALTER ROLE $USER SET timezone TO 'UTC';
      GRANT ALL PRIVILEGES ON DATABASE $DB TO $USER;
      ALTER DATABASE $DB OWNER TO $USER;
      RAISE NOTICE 'Database "$DB" and user "$USER" created.';
   END IF;
END
\$do\$;
EOSQL
