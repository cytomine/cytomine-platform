# Customized PostGIS image

This Dockerfile relies on [official PostGIS Dockerfile](https://github.com/postgis/docker-postgis), which itself relies on [official library Postgres Dockerfile](https://github.com/docker-library/postgres) with a  customized database initialization and custom Postgres configuration overriding.

Currently registered Cytomine script(s):

1. optionally interpolate the environment variable present in any file under `/cm_configs` with a filename suffix `.sample` and move all files to their respective directory (see below).

Currently registered Cytomine db scripts(s):

1. Cytomine-specific database initialization (`ltree` extension loading).

## PostGIS-specific configuration

This image works off-the-shelf but, if additional configuration is required, one can supply one or several postgres configuration files to override the default configuration. These file should follow the naming convention `DD-*.conf` (e.g. `50-additional.conf`) where `DD` is a two digits number between `01` and `99` defining the read order of the override configuration files. These files should be mounted in `/cm_configs/etc/postgres/conf.d/`.

The image also supports environment variables supported by the base Postgres image (see [official documentation](https://registry.hub.docker.com/_/postgres)).
