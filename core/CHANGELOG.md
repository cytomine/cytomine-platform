# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [5.2.0] - 2025-06-20

### Changed

- Refactor authentication process using IAM service with Keycloak

### Fixed

- Fix large files forwarding from and to the App Engine

### Removed

- Local authentication

## [5.1.1] - 2025-05-09

### Fixed

- Fix image input for App Engine to support both simple images and cropped regions defined by an annotation

## [5.1.0] - 2025-04-18

### Added

- App Engine service support
- Image group feature
- Annotation link feature
- Annotation group feature

### Changed

- Upgrade Spring Boot from 2.6.6 to 3.2.3
- Upgrade Gradle from 7.2 to 7.6
- Migrate from Javax to Jakarta

### Removed

- Legacy software support
- RabbitMQ message queue support

[Unreleased]: https://github.com/cytomine/Cytomine-core/compare/5.2.0..HEAD
[5.2.0]: https://github.com/cytomine/Cytomine-core/releases/tag/5.2.0
[5.1.1]: https://github.com/cytomine/Cytomine-core/releases/tag/5.1.1
[5.1.0]: https://github.com/cytomine/Cytomine-core/releases/tag/5.1.0
