# Customized Nginx image

This Dockerfile relies on [official library Dockerfile](https://github.com/nginxinc/docker-nginx) with a customized entrypoint allowing to perform custom configuration with shell scripts found in `/docker-entrypoint.d/` during container startup.

Currently registered script(s):

1. optionally interpolate the environment variable present in any file under `/cm_configs` with a filename suffix `.sample` and move all files to their respective directory (see below).

## Nginx-specific configuration:

TODO logs ? servicves ?

## Building the Docker image

Simply run 
```
bash build.sh
```

This scripts supports the following environment variables (optionally located in a `.env` file) as inputs:

* `NGINX_VERSION`: the Nginx version used in the image. It must be a [valid Docker tag for official `nginx` image](https://hub.docker.com/_/nginx).
* `IMAGE_VERSION`: version of the custom image
* `DOCKER_NAMESPACE` is the Docker namespace for the built image
* `SCRIPTS_REPO_URL`: full url (http/https) of the git repository containing the initialization scripts (including git credentials if necessary).  
* `SCRIPTS_REPO_TAG`: tag of the commit from which the scripts must be extracted
* `SCRIPTS_REPO_BRANCH`: branch from which the scripts must be extracted

It builds an image `NAMESPACE/nginx:NGINX_VERSION-IMAGE_VERSION`.
