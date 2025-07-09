# Cytomine Platform

Cytomine is an open-source platform for collaborative analysis of large-scale imaging data.

This repository provides the necessary files and instructions to build and launch the Cytomine product using Docker.

## How to Launch Cytomine

### Build the Docker Images

To build all required Docker images for Cytomine, run:

```sh
make build
```

### Start the Cytomine platform

To deploy the Cytomine platform, run:

```sh
make start
```

### Stop the Cytomine platform

To stop the Cytomine platform, run:

```sh
make stop
```

### Stop and delete the data from the Cytomine platform

To stop the Cytomine platform, run:

```sh
make clean
```

It completely removes all Cytomine containers, networks, volumes, and data. It also deletes generated configuration files, such as `.env`, `cytomine.yml`, and `docker-compose.override.yml` files.

> This command need the sudo permissions to delete the created data
