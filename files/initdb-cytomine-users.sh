#!/bin/bash

set -e

# Perform all actions as $POSTGRES_USER
export PGUSER="$POSTGRES_USER"

create_user () {
USERNAME=$1
"${psql[@]}" --dbname="$POSTGRES_DB" <<EOSQL
  DO
  \$do\$
  BEGIN
    IF EXISTS (
        SELECT FROM pg_catalog.pg_roles
        WHERE  rolname = '$USERNAME') THEN

        RAISE NOTICE 'User "$USERNAME" already exists. Skipping.';
    ELSE
        CREATE USER $USERNAME WITH SUPERUSER PASSWORD '$POSTGRES_PASSWORD';
    END IF;
  END
  \$do\$;
EOSQL
}

echo "Loading other Cytomine-required extensions into $POSTGRES_DB"
create_user 'postgres'
create_user 'docker'


