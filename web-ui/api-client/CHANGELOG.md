# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Authentication using IAM service instead of Core

### Removed

- AlgoAnnotationTerm model
- ImageServer model

## [3.0.1] - 2025-05-11

### Changed

- Migrate from Karma and Chai to Jest for tests

### Removed

- Docker support

### Fixed

- Add missing main directive in package.json

## [3.0.0] - 2025-04-17

### Added

- AnnotationGroup model and AnnotationGroupCollection
- AnnotationLink model and AnnotationLinkCollection
- ImageGroup model and ImageGroupCollection
- ImageGroupImageInstance model and ImageGroupImageInstanceCollection

### Removed

- Jenkins CI support
- Travis CI support
- Job model and JobCollection
- ProcessingServer model and ProcessingServerCollection
- Software model and SoftwareCollection
- TrustedSource model and TrustedSourceCollection
- UserJob model and UserJobCollection

[Unreleased]: https://github.com/cytomine/Cytomine-js-client/compare/3.0.1..HEAD
[3.0.1]: https://github.com/cytomine/Cytomine-js-client/releases/tag/3.0.1
[3.0.0]: https://github.com/cytomine/Cytomine-js-client/releases/tag/3.0.0
