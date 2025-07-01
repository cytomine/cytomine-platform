# Customized Nginx image

This Dockerfile relies on [official library Dockerfile](https://github.com/nginxinc/docker-nginx) with a customized entrypoint allowing to perform custom configuration with shell scripts found in `/docker-entrypoint.d/` during container startup.

Currently registered script(s) :

1. optionally interpolate the environment variable present in any file under `/cm_configs` with a filename suffix `.sample` and move all files to their respective directory (see below).

## Nginx-specific configuration:

The file `/cm_configs_default/etc/nginx/nginx.conf.sample` will be provided by default. The `docker-entrypoint` scripts will interpolate env vars into it and produce the final `/etc/nginx/nginx.conf`.