Feature: [URS00003-TASK] Execute a task

  Background:
    Given App Engine is up and running
    And File storage service is up and running
    And Scheduler is up and running
    And Registry service is up and running

  @Scheduler
  Scenario Outline: successful run request for a provisioned task run
    Given a task run "<task_run_id>" has successfully been created for a task
    And this task run has been successfully provisioned and is therefore in state 'PROVISIONED'
    When When user calls the endpoint to run task with HTTP method POST
    Then App Engine sends a "200" OK response with a payload containing the success message (see OpenAPI spec)
    And App Engine moves the task run to a state different from "<excluded_transition_states>"
    And App Engine initiates the process of executing the task run

    Examples:
      | task_run_id                          | excluded_transition_states |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 | CREATED,PROVISIONED        |

  @Scheduler
  Scenario Outline: unsuccessful run request of a task which has not been provisioned
    Given a task run "<task_run_id>" has successfully been created for a task
    And this task run has not been successfully provisioned yet and is therefore in state "<initial_state>"
    When When user calls the endpoint to run task with HTTP method POST
    Then App Engine sends a "403" Forbidden response with a payload containing the error message (see OpenAPI spec) and code "<error_code>"
    And this task run remains in state "<initial_state>"
    And App Engine does not initiate the process of executing this task run

    Examples:
      | task_run_id                          | initial_state | error_code                         |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 | CREATED       | APPE-internal-task-run-state-error |

  @Scheduler
  Scenario Outline: unsuccessful run request for a task that was already launched
    Given a task run "<task_run_id>" has successfully been created for a task
    And App Engine has already received a run request for this task run which is therefore not in state "<excluded_states>"
    When When user calls the endpoint to run task with HTTP method POST
    Then App Engine sends a "403" Forbidden response with a payload containing the error message (see OpenAPI spec) and code "<error_code>"
    And this task run state progress is not affected by the request
    And App Engine does not re-initiate the process of executing this task run

    Examples:
      | task_run_id                          | excluded_states     | error_code                         |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 | CREATED,PROVISIONED | APPE-internal-task-run-state-error |

  @Scheduler
  Scenario Outline: successful retrieval of task run information for an existing task run
    Given a task run exists with identifier "<task_run_id>"
    And the task run is in state "<task_run_state>"
    When user calls the endpoint with "<task_run_id>" HTTP method GET
    Then App Engine sends a "200" OK response with a payload containing task run information (see OpenAPI spec)
    And the retrieved task run information matches the expected details
    And the task run state remains as "<task_run_state>"

    Examples:
      | task_run_id                          | task_run_state |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 | PROVISIONED    |
      | 123e4567-e89b-12d3-a456-426614174001 | CREATED        |

  @Scheduler
  Scenario Outline: successful fetch of task run inputs archive in a launched task run
    Given a task run exists with identifier "<task_run_id>"
    And the task run is in state "<task_run_state>"
    And the task run "<task_run_id>" has input parameters: "<param1_name>" of type "<param1_type>" with value "<param1_value>" and "<param2_name>" of type "<param2_type>" with value "<param2_value>"
    When user calls the endpoint to fetch inputs archive with "<task_run_id>" HTTP method GET
    Then App Engine sends a "200 OK" response with a payload containing the inputs archive
    And the archive contains files named "<param1_name>" and "<param2_name>"
    And the content of file "<param1_name>" is "<param1_value>"
    And the content of file "<param2_name>" is "<param2_value>"

    Examples:
      | task_run_id                          | task_run_state | param1_name | param1_type | param1_value | param2_name | param2_type | param2_value |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 | FINISHED       | a           | integer     | 5            | b           | integer     | 10           |
      | 123e4567-e89b-12d3-a456-426614174001 | RUNNING        | a           | integer     | 25           | b           | integer     | 54           |

  @Scheduler
  Scenario Outline: unsuccessful fetch of task run inputs archive in a created task run
    Given a task run exists with identifier "<task_run_id>"
    And the task run is in state "<task_run_state>"
    When user calls the endpoint to fetch inputs archive with "<task_run_id>" HTTP method GET
    Then App Engine sends a "403" forbidden response with a payload containing the error message (see OpenAPI spec) and code "APPE-internal-task-run-state-error"

    Examples:
      | task_run_id                          | task_run_state |
      | acde070d-8c4c-4f0d-9d8a-162843c10334 | CREATED        |

  @Scheduler
  Scenario Outline: successful fetch of task run outputs archive in a finished task run
    Given a task run exists with identifier "<task_run_id>"
    And the task run is in state "FINISHED"
    And the task run "<task_run_id>" has output parameters: "<param1_name>" of type "<param1_type>" with value <param1_value>
    When user calls the endpoint to fetch with "<task_run_id>" HTTP method GET
    Then App Engine sends a "200 OK" response with a payload containing the outputs archive
    And the archive contains files named "<param1_name>"
    And the content of output file "<param1_name>" is "<param1_value>"

    Examples:
      | task_run_id                          | param1_name | param1_type | param1_value |
      | 0000070d-8c4c-4f0d-9d8a-162843c10333 | sum         | integer     | 15           |
      | 00004567-e89b-12d3-a456-426614174001 | sum         | integer     | 28           |

  @Scheduler
  Scenario Outline: unsuccessful fetch of task run outputs archive in a non-finished task run
    Given a task run exists with identifier "<task_run_id>"
    And the task run is in state "<task_run_state>"
    When user calls the endpoint to fetch outputs archive with "<task_run_id>" HTTP method GET
    Then App Engine sends a "403" forbidden response with a payload containing the error message (see OpenAPI spec) and code "APPE-internal-task-run-state-error"

    Examples:
      | task_run_id                          | task_run_state |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 | CREATED        |
      | 123e4567-e89b-12d3-a456-426614174001 | PROVISIONED    |

  @Scheduler
  Scenario Outline: successful fetch of task run outputs in JSON format for a finished task run
    Given a task run exists with identifier "<task_run_id>"
    And the task run is in state "FINISHED"
    And the task run "<task_run_id>" has output parameters: "<param1_name>" of type "<param1_type>" with value <param1_value>
    When user calls the endpoint to fetch outputs json with "<task_run_id>" HTTP method GET
    Then App Engine sends a "200 OK" response with a payload containing task run outputs in JSON format
    And the payload contains the output "<param1_name>" and their corresponding value <param1_value>

    Examples:
      | task_run_id                          | param1_name | param1_type | param1_value |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 | sum         | integer     | 15           |
      | 123e4567-e89b-12d3-a456-426614174001 | sum         | integer     | 25           |

  @Scheduler
  Scenario Outline: unsuccessful fetch of task run outputs in JSON format for a non-finished task run
    Given a task run exists with identifier "<task_run_id>"
    And the task run is in state "<task_run_state>"
    When user calls the endpoint to fetch outputs json with "<task_run_id>" HTTP method GET
    Then App Engine sends a "403" forbidden response with a payload containing the error message (see OpenAPI spec) and code "APPE-internal-task-run-state-error"

    Examples:
      | task_run_id                          | task_run_state |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 | CREATED        |
      | 123e4567-e89b-12d3-a456-426614174001 | PROVISIONED    |

  @Scheduler
  Scenario Outline: successful upload of task run outputs as a valid zip file in running or pending state
    Given a task run exists with identifier "<task_run_id>"
    And the task run is in state "RUNNING" or "PENDING"
    And the task run "<task_run_id>" has output parameters: "<param1_name>" of type "<param1_type>" with value <param1_value>
    And a valid zip file containing one file named "<param1_name>" and contains "<param1_value>"
    When user calls the endpoint to post outputs with "<task_run_id>" HTTP method POST and a valid outputs zip file
    Then App Engine sends a "200" OK response with a payload containing task run outputs in JSON format
    And the payload contains the output parameters and their corresponding values

    Examples:
      | task_run_id                          | param1_name | param1_type | param1_value |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 | sum         | integer     | 15           |
      | 123e4567-e89b-12d3-a456-426614174001 | sum         | integer     | 28           |

  @Scheduler
  Scenario Outline: unsuccessful upload of task run outputs as an invalid zip file in running state
    Given a task run exists with identifier "<task_run_id>"
    And the task run is in state "RUNNING" or "PENDING"
    And the task run has an output parameter "sum"
    And a zip file is used which does not contain a file named "sum"
    When user calls the endpoint to post outputs with "<task_run_id>" HTTP method POST and the zip file as a binary payload
    Then App Engine sends a "400" Bad Request response with a payload containing the error message (see OpenAPI spec) and code "APPE-internal-task-run-unknown-output"

    Examples:
      | task_run_id                          |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 |
      | 123e4567-e89b-12d3-a456-426614174001 |

  @Scheduler
  Scenario Outline: unsuccessful upload of task run outputs as a valid zip file in a non-running non-pending state state
    Given a task run exists with identifier "<task_run_id>"
    And the task run is not in state "RUNNING" or "PENDING" or "QUEUING" or "QUEUED"
    When user calls the endpoint to post outputs with "<task_run_id>" HTTP method POST and a valid outputs zip file
    Then App Engine sends a "403" forbidden response with a payload containing the error message (see OpenAPI spec) and code "APPE-internal-task-run-state-error"

    Examples:
      | task_run_id                          |
      | acde070d-8c4c-4f0d-9d8a-162843c10333 |
      | 123e4567-e89b-12d3-a456-426614174001 |