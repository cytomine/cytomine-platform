DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'docker') THEN

      RAISE NOTICE 'Role "docker" already exists. Skipping.';
   ELSE
      CREATE ROLE docker LOGIN PASSWORD 'my_password';
   END IF;
END
$do$;