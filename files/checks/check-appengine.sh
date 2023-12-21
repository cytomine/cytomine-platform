if [ -z "$APPENGINE_USER" ] || [ -z "$APPENGINE_PASSWORD" ] || [ -z "$APPENGINE_DB" ]; then
   echo "$0" "Skipping App Engine database pre-configuration because APPENGINE_* env variables are empty or undefined"
   exit
fi

echo "$0" "Creating app engine database '$APPENGINE_DB' using user '$POSTGRES_USER'. An error is expected if it already exists.";

# DB
psql -U "$POSTGRES_USER" -c "CREATE DATABASE $APPENGINE_DB" >/dev/null

# Grants
echo "$0" "Grant roles to $APPENGINE_USER for app engine database $APPENGINE_DB. A notice is expected if they already exist.";

psql -U "$POSTGRES_USER" >/dev/null <<- EOSQL
DO
\$do\$
BEGIN
   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$APPENGINE_USER') THEN
      RAISE NOTICE 'Role "$APPENGINE_USER" already exists. Skipping.';
   ELSE
      CREATE ROLE $APPENGINE_USER LOGIN PASSWORD '$APPENGINE_PASSWORD';
      ALTER ROLE $APPENGINE_USER SET client_encoding TO 'utf8';
      ALTER ROLE $APPENGINE_USER SET timezone TO 'UTC';
      GRANT ALL PRIVILEGES ON DATABASE $APPENGINE_DB TO $APPENGINE_USER;
      ALTER DATABASE $APPENGINE_DB OWNER TO $APPENGINE_USER;
      RAISE NOTICE 'Database "$APPENGINE_DB" and user "$APPENGINE_USER" created.';
   END IF;
END
\$do\$;
EOSQL
