# Customized Mongo image

This Dockerfile relies on [official Mongo Dockerfile](https://github.com/docker-library/mongo) with a customized entrypoint allowing to perform custom configuration with shell scripts found in `/docker-entrypoint-cytomine.d/` during container startup.

Currently registered Cytomine script(s):

1. optionally interpolate the environment variable present in any file under `/cm_configs` with a filename suffix `.sample` and move all files to their respective directory (see below)

## Mongo-specific configuration

This image works off-the-shelf but, if additional configuration is required, one can supply a Mongo configuration file. This file can be mounted in `/cm_configs/etc/mongo/conf.d/`. It can optionally be templated with environment variables in which case the file must be suffixed with `.sample`.

**IMPORTANT** By default, `mongo` will not read the configuration file. Therefore, for the file to be read, it has to be explicitely specified as a CLI parameters at `mongo` launch. This parameter can be specified in a docker-compose file as well (by redefining the command). More information on [Mongo DockerHub page](https://hub.docker.com/_/mongo).   

The image also supports environment variables supported by the base Mongo image (see [official documentation](https://registry.hub.docker.com/_/mongo)).

# Backup
This image provides a `backup` script. It will create an archive containing a full `mongodump` of the database.  
The `backup` script runs every 24h. It can be further invoked from the host running the container:
```
docker exec postgis backup
```
It produces a compressed archive in a `backup` subdirectory of the exposed volume: `/data/db/backup`.

## Restore
To restore the database from a backup, follow those steps:
1. In the volume folder `/data/db/backup` (or in the equivalent folder on your host if you mounted the volume), rename the backup archive you want to restore into `restore.tar.gz`:
```
/data/db/backup# cp cytomine_mongo_backup_Tue.tar.gz restore.tar.gz
```
2. Run the `restore` script from the host:
```
docker exec mongo restore
```