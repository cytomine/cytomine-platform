# Customized Mongo image

This Dockerfile relies on [official Mongo Dockerfile](https://github.com/postgis/docker-postgis), which itself relies on [official library Mongo Dockerfile](https://github.com/docker-library/postgres) with a  customized database initialization and custom Mongo configuration overriding.

Currently registered Cytomine script(s):

1. optionally interpolate the environment variable present in any file under `/cm_configs` with a filename suffix `.sample` and move all files to their respective directory (see below)

## PostGIS-specific configuration

This image works off-the-shelf but, if additional configuration is required, one can supply a Mongo configuration file. This file can be mounted in `/cm_configs/etc/mongo/conf.d/`. It can optionally be templated with environment variables in which case the file must be suffixed with `.sample`.

**IMPORTANT** By default, `mongo` will not read the configuration file. Therefore, for the file to be read, it has to be explicitely specified as a CLI parameters at `mongo` launch. This parameter can be specified in a docker-compose file as well (by redefining the command). More information on [Mongo DockerHub page](https://hub.docker.com/_/mongo).   

The image also supports environment variables supported by the base Mongo image (see [official documentation](https://registry.hub.docker.com/_/mongo)).

## Building the Docker image

Simply run 
```
bash build.sh
```

This scripts supports the following environment variables (optionally located in a `.env` file) as inputs:

* `MONGO_VERSION`: the Mongo version used in the image. It must be a [valid Docker tag for official `mongo` image](https://github.com/docker-library/docs/blob/master/mongo/README.md#supported-tags-and-respective-dockerfile-links).
* `IMAGE_VERSION`: version of the custom image
* `DOCKER_NAMESPACE` is the Docker namespace for the built image
* `SCRIPTS_REPO_URL`: full url (http/https) of the git repository containing the initialization scripts (including git credentials if necessary).  
* `SCRIPTS_REPO_TAG`: tag of the commit from which the scripts must be extracted 
* `SCRIPTS_REPO_BRANCH`: branch from which the scripts must be extracted 

It builds an image `NAMESPACE/postgis:POSTGIS_VERSION-IMAGE_VERSION`.
