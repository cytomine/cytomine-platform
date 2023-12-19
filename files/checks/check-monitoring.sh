if [ -z "$MONITORING_USER" ] || [ -z "$MONITORING_PASSWORD" ] || [ -z "$MONITORING_DB" ]; then
   echo "Skipping monitoring database pre-configuration because MONITORING_* env variables are empty or undefined"
   exit
fi

echo "Creating monitoring database '$MONITORING_DB' using user '$POSTGRES_USER'.";

# DB
psql -U "$POSTGRES_USER" -c "CREATE DATABASE $MONITORING_DB"

# Grants
echo "Grant roles to $MONITORING_USER for monitoring database $MONITORING_DB";

psql -U "$POSTGRES_USER" <<- EOSQL
DO
\$do\$
BEGIN
   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$MONITORING_USER') THEN
      RAISE NOTICE 'Role "$MONITORING_USER" already exists. Skipping.';
   ELSE
      CREATE ROLE $MONITORING_USER LOGIN PASSWORD '$MONITORING_PASSWORD';
      ALTER ROLE $MONITORING_USER SET client_encoding TO 'utf8';
      ALTER ROLE $MONITORING_USER SET timezone TO 'UTC';
      GRANT ALL PRIVILEGES ON DATABASE $MONITORING_DB TO $MONITORING_USER;
      ALTER DATABASE $MONITORING_DB OWNER TO $MONITORING_USER;
      RAISE NOTICE 'Database "$MONITORING_DB" and user "$MONITORING_USER" created.';
   END IF;
END
\$do\$;
EOSQL
