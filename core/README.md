# Cytomine Core

Cytomine Core is a Spring Boot application built with Spring Framework, providing a robust and scalable solution for Cytomine. The application is designed to run with minimal setup and configuration.

## Develop & run locally using Docker

### Requirements

- Docker
- Docker Compose

### Usage

To run the development server with live reload locally:
```bash
docker compose up -d --build
```

Inside the container, run the following command to start the application:
```bash
gradle bootRun --args="--spring.profiles.active=dev"
```

### Run tests

To run the development server with live reload locally:
```bash
docker compose up -d --build
```

Inside the container, run the following command to start the tests:
```bash
gradle :test
```

### Stop the development server

To stop the development server:
```bash
docker compose down
```

## License

Apache 2.0
