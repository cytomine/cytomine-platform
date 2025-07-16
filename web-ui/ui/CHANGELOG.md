# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## Changed

- Authentication and user management is done using IAM service with Keycloak

## [3.1.1] - 2025-05-09

### Fixed

- Fix text overflow for App Engine inputs when the text is larger than the button
- Fix image input for App Engine to support both simple images and cropped regions defined by an annotation

## [3.1.0] - 2025-04-18

### Added

- App Engine service support
- Image group feature
- Annotation link feature
- Annotation group feature
- Metadata panel in image viewer

### Changed

- Upgrade node from v14.21.3 to v16.20.2

### Removed

- Legacy software support
- RabbitMQ message queue support

[Unreleased]: https://github.com/cytomine/Cytomine-web-ui/compare/3.1.1..HEAD
[3.1.1]: https://github.com/cytomine/Cytomine-web-ui/releases/tag/3.1.1
[3.1.0]: https://github.com/cytomine/Cytomine-web-ui/releases/tag/3.1.0
