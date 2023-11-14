if [ -z "$APPENGINE_USER" ] || [ -z "$APPENGINE_PASSWORD" ] || [ -z "$APPENGINE_DB" ]; then
   echo "Skipping App Engine database pre-configuration because APPENGINE_* env variables are empty or undefined"
   exit
fi


psql -c "CREATE DATABASE $APPENGINE_DB;"

psql <<- EOSQL
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
   END IF;
END
\$do\$;
EOSQL
