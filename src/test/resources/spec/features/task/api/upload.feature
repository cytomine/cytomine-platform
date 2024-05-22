Feature: [URS00001-TASK] Upload a task

  Uploading a task make it available to the App Engine for execution.

  Background:
    Given App Engine is up and running
    And File storage service is up and running
    And Registry service is up and running

  Scenario Outline: successful valid task upload
    Given a task uniquely identified by an "<task namespace>" and a "<task version>"
    And this task identified by an "<task namespace>" and a "<task version>" is not yet known to the App Engine
    And this task is represented by a zip archive containing a task descriptor file and a docker image
    And the task descriptor is a YAML file stored in the zip archive named "descriptor.yml" structured following an agreed descriptor schema
    And the Docker image stored in the zip archive is represented by a tar file of which the relative path is "<tar filepath>"
    And "<tar filepath>" is either defined by the field "configuration.image.file" in the task descriptor or, if this field is missing, is "image.tar"

    When user calls POST on endpoint with the zip archive as a multipart file parameter

    Then App Engine unzip the zip archive
    And App Engine successfully validates the task descriptor against the descriptor schema
    And App Engine successfully validates the docker image by checking that the tar file contains a "manifest.json" file
    And App Engine stores relevant task metadata (a subset of the task descriptor content) in the database associated with the "<task uuid>"
    And App Engine creates a unique "<task uuid>" for referencing the task
    And App Engine creates a task storage (e.g. a bucket reserved for the task) in the File Storage service with a unique "<storage identifier>" as follows "task-<task uuid>-def"
    And App Engine stores the task descriptor in the task storage "<storage identifier>" of the File Storage service
    And App Engine pushes the docker image to the Registry service with an "<image name>" built based on the "<task namespace>" and "<task version>" (replace '.' by '/' in "<task namespace>", then append :"<task version>")
    And App Engine returns an HTTP "200" OK response
    And App Engine cleans up any temporary file created during the process (e.g. uploaded zip file, etc)

    Examples:
      | task namespace                                 | task version | tar filepath | task uuid                            | storage identifier                            | image name                                           |
      | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        | image.tar    | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def | com/cytomine/dummy/arithmetic/integer/addition:1.0.0 |
      | com.cytomine.dummy.identity.boolean            | 1.0.0        | image.tar    | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def | com/cytomine/dummy/identity/boolean:1.0.0            |
      | com.cytomine.dummy.identity.enumeration        | 1.0.0        | image.tar    | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def | com/cytomine/dummy/identity/enumeration:1.0.0        |
      | com.cytomine.dummy.identity.number             | 1.0.0        | image.tar    | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def | com/cytomine/dummy/identity/number:1.0.0             |
      | com.cytomine.dummy.identity.string             | 1.0.0        | image.tar    | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def | com/cytomine/dummy/identity/string:1.0.0             |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | image.tar    | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def | com/cytomine/dummy/identity/geometry:1.0.0           |

  Scenario Outline: unsuccessful upload of duplicate task
    Given a task uniquely identified by an "<task namespace>" and a "<task version>"
    And this task is already registered by the App Engine
    And this task is represented by a zip archive containing a task descriptor file and a docker image
    And the task descriptor is a YAML file stored in the zip archive named "descriptor.yml" structured following an agreed descriptor schema

    When user calls POST on endpoint with the zip archive as a multipart file parameter

    Then App Engine reads "<task namespace>" and "<task version>" from the task descriptor
    And App Engine returns an HTTP "409" conflict error because this version of the task exists already
    And App Engine cleans up any temporary file created during the process (e.g. uploaded zip file, etc)
    But App Engine does not create or overwrite the task and related data in the File storage, registry and database services

    Examples:
      | task namespace                                 | task version |
      | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        |
      | com.cytomine.dummy.identity.boolean            | 1.0.0        |
      | com.cytomine.dummy.identity.enumeration        | 1.0.0        |
      | com.cytomine.dummy.identity.number             | 1.0.0        |
      | com.cytomine.dummy.identity.string             | 1.0.0        |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        |

  Scenario Outline: unsuccessful upload of a task with invalid descriptor
    Given a task uniquely identified by an "<task namespace>" and a "<task version>"
    And this task "<task>" is represented by a zip archive containing a task descriptor file "<task version>" and a docker image at "<docker image path>"
    And the "<expected docker image path>" can be retrieved from "configuration.image.file" in the descriptor file or is "image.tar" if the field is missing
    And the task descriptor is a YAML file stored in the zip archive named "descriptor.yml" which does not abide to the agreed descriptor schema for a "<reason>"

    When user calls POST on endpoint with the zip archive as a multipart file parameter

    Then App Engine fails to validate the task descriptor for task "<task>" against the descriptor schema
    And App Engine returns an HTTP "400" bad request error because the descriptor is incorrectly structured
    And App Engine cleans up any temporary file created during the process (e.g. uploaded zip file, etc)
    But App Engine does not create the "<task namespace>" and related data in the File storage, registry and database services

    Examples:
      | task  | task namespace                                 | task version | docker image path | expected docker image path | reason                 |
      | task1 | com.cytomine.dummy.arithmetic.integer.addition |              | image.tar         | image.tar                  | no version             |
      | task2 | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        | image.tar         | docker/image.tar           | invalid image location |
      | task3 | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        | myimage.tar       | image.tar                  | invalid image location |
