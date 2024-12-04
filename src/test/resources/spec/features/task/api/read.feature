Feature: [URS00002-TASK] Read task information

  Read task information via Task API, technically documented in "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file.

  Background:
    Given App Engine is up and running
    And File storage service is up and running

  Scenario Outline: successful fetch of all App Engine available tasks

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/tasks'

    Given a set of valid tasks has been successfully uploaded
    When user calls the endpoint "<endpoint>" (excluding version prefix, e.g. '/api/v1') with HTTP method "<method>"
    Then App Engine retrieves relevant data from the database
    And App Engine sends a "200" OK response with a payload containing the descriptions of the available tasks as a JSON payload (see OpenAPI spec)

    Examples:
      | method | endpoint |
      | GET    | tasks    |

  Scenario Outline: successful download of the descriptor file with namespace and version

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/tasks/{namespace}/{version}/descriptor.yml'

    Given a valid task has a "<task namespace>", a "<task version>" and "<task uuid>" has been successfully uploaded
    And the task descriptor is stored in the file storage service in storage "<task storage identifier>" under filename "descriptor.yml"
    When user calls the download endpoint with "<task namespace>" and "<task version>" with HTTP method GET
    Then App Engine retrieves the descriptor file "descriptor.yml" from the file storage
    And App Engine sends a "200" OK response with the descriptor file as a binary payload (see OpenAPI spec)

    Examples:
      | task   | task namespace                                 | task version | task uuid                            | task storage identifier                       |
      | Task 1 | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def |
      | Task 2 | com.cytomine.dummy.identity.boolean            | 1.0.0        | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def |
      | Task 3 | com.cytomine.dummy.identity.enumeration        | 1.0.0        | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def |
      | Task 4 | com.cytomine.dummy.identity.number             | 1.0.0        | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def |
      | Task 5 | com.cytomine.dummy.identity.string             | 1.0.0        | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def |
      | Task 6 | com.cytomine.dummy.identity.geometry           | 1.0.0        | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def |
      | Task 7 | com.cytomine.dummy.identity.image              | 1.0.0        | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def |
      | Task 7 | com.cytomine.dummy.identity.wsi                | 1.0.0        | acde070d-8c4c-4f0d-9d8a-162843c10333 | task-acde070d-8c4c-4f0d-9d8a-162843c10333-def |


  Scenario Outline: successful download of the descriptor file with id

  See "docs/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/tasks/{id}/descriptor.yml'

    Given a valid task has a "<task uuid>" has been successfully uploaded
    And the task descriptor is stored in the file storage service in storage "<task storage identifier>" under filename "descriptor.yml"
    When user calls the download endpoint with "<task uuid>" with HTTP method GET
    Then App Engine retrieves the descriptor file "descriptor.yml" from the file storage
    And App Engine sends a "200" OK response with the descriptor file as a binary payload (see OpenAPI spec)

    Examples:
      | task   | task uuid                            | task storage identifier                       |
      | Task 1 | acde070d-8c4c-4f0d-9d8a-162843c10353 | task-acde070d-8c4c-4f0d-9d8a-162843c10353-def |

  Scenario Outline: successful fetch of a task description using namespace and version

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/tasks/{namespace}/{version}'

    Given a valid task has a "<task namespace>", a "<task version>" has been successfully uploaded
    When user calls the endpoint "/task/namespace/version" with "<task namespace>" and "<task version>" with HTTP method GET
    Then App Engine retrieves task with "<task namespace>", a "<task version>"  from the database
    And App Engine sends a "200" OK response with a payload containing the task description as a JSON payload (see OpenAPI spec)

    Examples:
      | task   | task namespace                                 | task version |
      | Task 1 | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        |
      | Task 2 | com.cytomine.dummy.identity.boolean            | 1.0.0        |
      | Task 3 | com.cytomine.dummy.identity.enumeration        | 1.0.0        |
      | Task 4 | com.cytomine.dummy.identity.number             | 1.0.0        |
      | Task 5 | com.cytomine.dummy.identity.string             | 1.0.0        |
      | Task 6 | com.cytomine.dummy.identity.geometry           | 1.0.0        |
      | Task 7 | com.cytomine.dummy.identity.image              | 1.0.0        |
      | Task 8 | com.cytomine.dummy.identity.wsi                | 1.0.0        |


  Scenario Outline: successful fetch of a task description using id

  See "docs/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/tasks/{id}'

    Given a valid task has a "<task uuid>" has been successfully uploaded
    When user calls the endpoint "/task/id" with id "<task uuid>" HTTP method GET
    Then App Engine retrieves task with "<task uuid>" from the database
    And App Engine sends a "200" OK response with a payload containing the task description as a JSON payload (see OpenAPI spec)

    Examples:
      | task   | task uuid                            |
      | Task 1 | acde070d-8c4c-4f0d-9d8a-162843c10333 |


  Scenario Outline: successful fetch of a task's inputs using namespace and version

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/tasks/{namespace}/{version}/inputs'

    Given a valid task has a "<task namespace>", a "<task version>" has been successfully uploaded
    When user calls the endpoint "/task/namespace/version/inputs" with "<task namespace>" and "<task version>" HTTP method GET
    Then App Engine retrieves task inputs with "<task namespace>", a "<task version>"  from the database
    And App Engine sends a "200" OK response with a payload containing the task inputs as a JSON payload (see OpenAPI spec)

    Examples:
      | task   | task namespace                                 | task version |
      | Task 1 | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        |
      | Task 2 | com.cytomine.dummy.identity.boolean            | 1.0.0        |
      | Task 3 | com.cytomine.dummy.identity.enumeration        | 1.0.0        |
      | Task 4 | com.cytomine.dummy.identity.number             | 1.0.0        |
      | Task 5 | com.cytomine.dummy.identity.string             | 1.0.0        |
      | Task 6 | com.cytomine.dummy.identity.geometry           | 1.0.0        |
      | Task 7 | com.cytomine.dummy.identity.image              | 1.0.0        |
      | Task 8 | com.cytomine.dummy.identity.wsi                | 1.0.0        |


  Scenario Outline: successful fetch of a task's inputs using id

  See "docs/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/tasks/{id}/inputs'

    Given a valid task has a "<task uuid>" has been successfully uploaded
    When user calls the endpoint "/tasks/{id}/inputs" with "<task uuid>" HTTP method GET
    Then App Engine retrieves task inputs with "<task uuid>" from the database
    And App Engine sends a "200" OK response with a payload containing the task inputs as a JSON payload (see OpenAPI spec)

    Examples:
      | task   | task uuid                            |
      | Task 1 | acde070d-8c4c-4f0d-9d8a-162843c10333 |


  Scenario Outline: successful fetch of a task's outputs with namespace and version

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/tasks/{namespace}/{version}/outputs'

    Given a valid task has a "<task namespace>", a "<task version>" has been successfully uploaded
    When user calls the outputs endpoint "/task/namespace/version/outputs" with "<task namespace>" and "<task version>" HTTP method GET
    Then App Engine retrieves task outputs with "<task namespace>", a "<task version>"  from the database
    And App Engine sends a "200" OK response with a payload containing the task outputs as a JSON payload (see OpenAPI spec)

    Examples:
      | task   | task namespace                                 | task version |
      | Task 1 | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        |
      | Task 2 | com.cytomine.dummy.identity.boolean            | 1.0.0        |
      | Task 3 | com.cytomine.dummy.identity.enumeration        | 1.0.0        |
      | Task 4 | com.cytomine.dummy.identity.number             | 1.0.0        |
      | Task 5 | com.cytomine.dummy.identity.string             | 1.0.0        |
      | Task 6 | com.cytomine.dummy.identity.geometry           | 1.0.0        |
      | Task 7 | com.cytomine.dummy.identity.image              | 1.0.0        |
      | Task 8 | com.cytomine.dummy.identity.wsi                | 1.0.0        |


  Scenario Outline: successful fetch of a task's outputs with id

  See "docs/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/tasks/{id}/outputs'

    Given a valid task has a "<task uuid>" has been successfully uploaded
    When user calls the outputs endpoint "/task/id/outputs" with "<task uuid>" HTTP method GET
    Then App Engine retrieves task outputs with "<task uuid>" from the database
    And App Engine sends a "200" OK response with a payload containing the task outputs as a JSON payload (see OpenAPI spec)

    Examples:
      | task   | task uuid                            |
      | Task 1 | acde070d-8c4c-4f0d-9d8a-162843c10333 |


  Scenario Outline: fetching information for an unknown task

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file.

    Given a task unknown to the App Engine has a "<task namespace>" and a "<task version>" and a "<uuid>"
    When user calls the fetch endpoint "<endpoint>" with HTTP method "<method>"
    Then App Engine sends a "404" HTTP error with a standard error payload containing code "<error_code>"

    Examples:
      | task namespace                                    | task version | uuid                                 | method | endpoint                                 | error_code                   |
      | com.cytomine.dummy.arithmetic.integer.subtraction | 1.0.0        |                                      | GET    | /task/namespace/version/outputs          | APPE-internal-task-not-found |
      |                                                   |              | acde070d-8c4c-4f0d-9d8a-162843c19333 | GET    | /task/id/outputs                         | APPE-internal-task-not-found |
      | com.cytomine.dummy.arithmetic.integer.subtraction | 1.0.0        |                                      | GET    | /task/namespace/version/inputs           | APPE-internal-task-not-found |
      |                                                   |              | acde070d-8c4c-4f0d-9d8a-162843c19333 | GET    | /task/id/inputs                          | APPE-internal-task-not-found |
      | com.cytomine.dummy.arithmetic.integer.subtraction | 1.0.0        |                                      | GET    | /task/namespace/version                  | APPE-internal-task-not-found |
      |                                                   |              | acde070d-8c4c-4f0d-9d8a-162843c10313 | GET    | /task/id                                 | APPE-internal-task-not-found |
      | com.cytomine.dummy.arithmetic.integer.subtraction | 1.0.0        |                                      | GET    | /task/namespace/version/descriptor.yml   | APPE-internal-task-not-found |
      |                                                   |              | acde070d-8c4c-4f0d-9d8b-162843c10333 | GET    | /task/id/descriptor.yml                  | APPE-internal-task-not-found |
