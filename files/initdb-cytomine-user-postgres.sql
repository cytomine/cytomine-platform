DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'postgres') THEN

      RAISE NOTICE 'Role "postgres" already exists. Skipping.';
   ELSE
      CREATE ROLE postgres LOGIN PASSWORD 'my_password';
   END IF;
END
$do$;