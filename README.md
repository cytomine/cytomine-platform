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

## How to use the Cytomine dev environment

To start a development environment for specific Cytomine services, use the `start-dev` command in the Makefile:

```bash
make dev <service-a> <service-b> ...
```

Where the available services are

| Profile | Description                |
|---------|----------------------------|
| `ae`    | App Engine                 |
| `core`  | Core backend services      |
| `iam`   | Identity Access Management |
| `ims`   | Image Management Server    |
| `ui`    | Web UI frontend            |

> 💡 There must be at least one service in dev mode.

### Examples

Start the App Engine and UI services in development mode:

```bash
make start-dev ae ui
```

> 💡 You can combine multiple profiles as needed.

To stop the development environment, use the `stop-dev` command in the Makefile:

```bash
make stop-dev
```
