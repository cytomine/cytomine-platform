Feature: [URS00003-TASK] Provision a task run

  Background:
    Given App Engine is up and running
    And File storage service is up and running

  Scenario Outline: successful creation of a task run

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/tasks/{task_id}/runs'
  - '/tasks/{namespace}/{version}/runs'

    Given a task has been successfully uploaded
    And this task has "<task_namespace>" and "<task_version>"
    And this task has at least one input parameter
    When user calls the endpoint "<endpoint>" with HTTP method POST
    Then a task run is created on the App Engine
    And this task run is attributed an id in UUID format
    And this task run is attributed the state "CREATED"
    And a storage for the task run is created in the file service under name "task-run-"+UUID
    And the App Engine returns a '200 OK' HTTP response with the updated task run information as JSON payload

    Examples:
      | task_namespace                                    | task_version | endpoint                     |
      | com.cytomine.dummy.arithmetic.integer.addition    | 1.0.0        | /task/namespace/version/runs |
      | com.cytomine.dummy.arithmetic.integer.addition    | 1.0.0        | /task/id/runs                |
      | com.cytomine.dummy.arithmetic.integer.subtraction | 1.0.0        | /task/namespace/version/runs |
      | com.cytomine.dummy.arithmetic.integer.subtraction | 1.0.0        | /task/id/runs                |
      | com.cytomine.dummy.identity.boolean               | 1.0.0        | /task/namespace/version/runs |
      | com.cytomine.dummy.identity.boolean               | 1.0.0        | /task/id/runs                |
      | com.cytomine.dummy.identity.enumeration           | 1.0.0        | /task/namespace/version/runs |
      | com.cytomine.dummy.identity.enumeration           | 1.0.0        | /task/id/runs                |
      | com.cytomine.dummy.identity.number                | 1.0.0        | /task/namespace/version/runs |
      | com.cytomine.dummy.identity.number                | 1.0.0        | /task/id/runs                |
      | com.cytomine.dummy.identity.string                | 1.0.0        | /task/namespace/version/runs |
      | com.cytomine.dummy.identity.string                | 1.0.0        | /task/id/runs                |
      | com.cytomine.dummy.identity.geometry              | 1.0.0        | /task/namespace/version/runs |
      | com.cytomine.dummy.identity.geometry              | 1.0.0        | /task/id/runs                |
      | com.cytomine.dummy.identity.image                 | 1.0.0        | /task/namespace/version/runs |
      | com.cytomine.dummy.identity.image                 | 1.0.0        | /task/id/runs                |


  Scenario Outline: successful provisioning of a task run with one input parameter using provisioning endpoint
  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/task-runs/{run_id}/input-provisions/{param_name}'

    Given a task has been successfully uploaded
    And this task has "<task_namespace>" and "<task_version>"
    And this task has only one input parameter "<param_name>" of type "<param_type>"
    And this parameter has no validation rules
    And a task run has been created for this task
    And this task run is attributed an id in UUID format
    And this task run has not been provisioned yet and is therefore in state "<task_run_initial_state>"
    When a user calls the provisioning endpoint with "<payload>" to provision parameter "<param_name>" with "<param_value>" of type "<param_type>"
    Then the value "<param_value>" is saved and associated parameter "<param_name>" in the database
    And a input file named "<param_name>" is created in the task run storage "task-run-"+UUID with content "<param_file_content>"
    And the task run states changes to "<task_run_new_state>" because the task is now completely provisioned
    And the App Engine returns a '200 OK' HTTP response with the updated task run information as JSON payload

    Examples:
      | task_namespace                                 | task_version | param_name | param_type  | payload                                                                                                                                                                                                                             | param_value                                                                                                                                                                                 | task_run_initial_state | task_run_new_state | param_file_content                                                                                                                                                                          |
      | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        | a          | integer     | {\"param_name\": \"a\", \"value\": 18}                                                                                                                                                                                              | 18                                                                                                                                                                                          | CREATED                | PROVISIONED        | 18                                                                                                                                                                                          |
      | com.cytomine.dummy.identity.boolean            | 1.0.0        | input      | boolean     | {\"param_name\": \"input\", \"value\": true}                                                                                                                                                                                        | true                                                                                                                                                                                        | CREATED                | PROVISIONED        | true                                                                                                                                                                                        |
      | com.cytomine.dummy.identity.enumeration        | 1.0.0        | input      | enumeration | {\"param_name\": \"input\", \"value\": A}                                                                                                                                                                                           | A                                                                                                                                                                                           | CREATED                | PROVISIONED        | A                                                                                                                                                                                           |
      | com.cytomine.dummy.identity.number             | 1.0.0        | input      | number      | {\"param_name\": \"input\", \"value\": 2.25}                                                                                                                                                                                        | 2.25                                                                                                                                                                                        | CREATED                | PROVISIONED        | 2.25                                                                                                                                                                                        |
      | com.cytomine.dummy.identity.string             | 1.0.0        | input      | string      | {\"param_name\": \"input\", \"value\": \"my_value\"}                                                                                                                                                                                | \"my_value\"                                                                                                                                                                                | CREATED                | PROVISIONED        | \"my_value\"                                                                                                                                                                                |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | input      | geometry    | {\"param_name\": \"input\", \"value\": { \"type\": \"Point\", \"coordinates\": [1.0, 2.0] }}                                                                                                                                        | { \"type\": \"Point\", \"coordinates\": [1.0, 2.0] }                                                                                                                                        | CREATED                | PROVISIONED        | { \"type\": \"Point\", \"coordinates\": [1.0, 2.0] }                                                                                                                                        |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | input      | geometry    | {\"param_name\": \"input\", \"value\": { \"type\": \"MultiPoint\", \"coordinates\": [[1.0, 2.0], [2.0, 3.0]] }}                                                                                                                     | { \"type\": \"MultiPoint\", \"coordinates\": [[1.0, 2.0], [2.0, 3.0]] }                                                                                                                     | CREATED                | PROVISIONED        | { \"type\": \"MultiPoint\", \"coordinates\": [[1.0, 2.0], [2.0, 3.0]] }                                                                                                                     |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | input      | geometry    | {\"param_name\": \"input\", \"value\": { \"type\": \"LineString\", \"coordinates\": [[1.0, 2.0], [2.0, 3.0]] }}                                                                                                                     | { \"type\": \"LineString\", \"coordinates\": [[1.0, 2.0], [2.0, 3.0]] }                                                                                                                     | CREATED                | PROVISIONED        | { \"type\": \"LineString\", \"coordinates\": [[1.0, 2.0], [2.0, 3.0]] }                                                                                                                     |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | input      | geometry    | {\"param_name\": \"input\", \"value\": { \"type\": \"MultiLineString\", \"coordinates\": [[[1.0, 2.0], [2.0, 3.0]], [[5.0, 2.0], [6.0, 3.0]]] }}                                                                                    | { \"type\": \"MultiLineString\", \"coordinates\": [[[1.0, 2.0], [2.0, 3.0]], [[5.0, 2.0], [6.0, 3.0]]] }                                                                                    | CREATED                | PROVISIONED        | { \"type\": \"MultiLineString\", \"coordinates\": [[[1.0, 2.0], [2.0, 3.0]], [[5.0, 2.0], [6.0, 3.0]]] }                                                                                    |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | input      | geometry    | {\"param_name\": \"input\", \"value\": { \"type\": \"Polygon\", \"coordinates\": [[[10.0, 0.0], [11.0, 0.0], [11.0, 1.0], [10.0, 1.0], [10.0, 0.0]]] }}                                                                             | { \"type\": \"Polygon\", \"coordinates\": [[[10.0, 0.0], [11.0, 0.0], [11.0, 1.0], [10.0, 1.0], [10.0, 0.0]]] }                                                                             | CREATED                | PROVISIONED        | { \"type\": \"Polygon\", \"coordinates\": [[[10.0, 0.0], [11.0, 0.0], [11.0, 1.0], [10.0, 1.0], [10.0, 0.0]]] }                                                                             |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | input      | geometry    | {\"param_name\": \"input\", \"value\": { \"type\": \"MultiPolygon\", \"coordinates\": [[[[12.0, 2.0], [13.0, 2.0], [13.0, 3.0], [12.0, 3.0], [12.0, 2.0]]], [[[10.0, 0.0], [11.0, 0.0], [11.0, 1.0], [10.0, 1.0], [10.0, 0.0]]]] }} | { \"type\": \"MultiPolygon\", \"coordinates\": [[[[12.0, 2.0], [13.0, 2.0], [13.0, 3.0], [12.0, 3.0], [12.0, 2.0]]], [[[10.0, 0.0], [11.0, 0.0], [11.0, 1.0], [10.0, 1.0], [10.0, 0.0]]]] } | CREATED                | PROVISIONED        | { \"type\": \"MultiPolygon\", \"coordinates\": [[[[12.0, 2.0], [13.0, 2.0], [13.0, 3.0], [12.0, 3.0], [12.0, 2.0]]], [[[10.0, 0.0], [11.0, 0.0], [11.0, 1.0], [10.0, 1.0], [10.0, 0.0]]]] } |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | input      | geometry    | {\"param_name\": \"input\", \"value\": { \"type\": \"Feature\", \"geometry\": \"geometry\": { \"type\": \"Point\", \"coordinates\": [-122.126986, 47.639754] }, \"properties\": {\"subType\": \"Circle\", \"radius\": 100}}}        | { \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [-122.126986, 47.639754] }, \"properties\": {\"subType\": \"Circle\", \"radius\": 100}}                      | CREATED                | PROVISIONED        | { \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [-122.126986, 47.639754] }, \"properties\": {\"subType\": \"Circle\", \"radius\": 100}}                      |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | input      | geometry    | {\"param_name\": \"input\", \"value\": { \"type\": \"Feature\", \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[[5,25],[14,25],[14,29],[5,29],[5,25]]]}, \"properties\": {\"subType\": \"Rectangle\"}}}                   | { \"type\": \"Feature\", \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[[5,25],[14,25],[14,29],[5,29],[5,25]]]}, \"properties\": {\"subType\": \"Rectangle\"}}                   | CREATED                | PROVISIONED        | { \"type\": \"Feature\", \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[[5,25],[14,25],[14,29],[5,29],[5,25]]]}, \"properties\": {\"subType\": \"Rectangle\"}}                   |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | input      | geometry    | {\"param_name\": \"input\", \"value\": { \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [1.0, 2.0] }, \"properties\": {}}}                                                                            | { \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [1.0, 2.0] }, \"properties\": {}}                                                                            | CREATED                | PROVISIONED        | { \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [1.0, 2.0] }, \"properties\": {}}                                                                            |
      | com.cytomine.dummy.identity.image              | 1.0.0        | input      | image       | binary                                                                                                                                                                                                                              | binary                                                                                                                                                                                      | CREATED                | PROVISIONED        | binary                                                                                                                                                                                      |


  Scenario Outline: successful partial provisioning of a task run with two input parameters

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/task-runs/{run_id}/input-provisions'

    Given a task has been successfully uploaded
    And this task has "<task_namespace>" and "<task_version>"
    And this task has two input parameters
    And the first parameter is "<param1_name>" of type "<param1_type>" without a validation rule
    And the second parameter is "<param2_name>" of type "<param2_type>" without a validation rule
    And no validation rules are defined for these parameters
    And a task run has been created for this task
    And this task run is attributed an id in UUID format
    And this task run has not been provisioned yet and is therefore in state "<task_run_initial_state>"
    When a user calls the endpoint with JSON "<payload>"
    Then the value "<param1_value>" is saved and associated with parameter "<param1_name>" in the database
    And a input file named "<param1_name>" is created in the task run storage "task-run-"+UUID with content "<param_file_content>"
    And the task run state remains as "<task_run_initial_state>" since not all parameters are provisioned yet
    And the App Engine returns a '200 OK' HTTP response with the updated task run information as JSON payload

    Examples:
      | task_namespace                                    | task_version | param1_name | param1_type | param1_value | param2_name | param2_type | payload                                     | task_run_initial_state | param_file_content |
      | com.cytomine.dummy.arithmetic.integer.addition    | 1.0.0        | a           | integer     | 5            | b           | integer     | [{\"param_name\": \"a\", \"value\": \"5\"}] | CREATED                | 5                  |
      | com.cytomine.dummy.arithmetic.integer.subtraction | 1.0.0        | a           | integer     | 5            | b           | integer     | [{\"param_name\": \"a\", \"value\": \"5\"}] | CREATED                | 5                  |

  Scenario Outline: successful batch provisioning of a task run two parameters one of which one has a validation rule

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/task-runs/{run_id}/input-provisions'

    Given a task has been successfully uploaded
    And this task has "<task_namespace>" and "<task_version>"
    And this task has two input parameters
    And the first parameter is "<param1_name>" of type "<param1_type>" with a validation rule "<param1_validation_rule>"
    And the second parameter is "<param2_name>" of type "<param2_type>" without a validation rule
    And a task run has been created for this task
    And this task run is attributed an id in UUID format
    And this task run has not been provisioned yet and is therefore in state "<task_run_initial_state>"
    When a user calls the endpoint with JSON "<payload>"
    Then the value "<param1_value>" is saved and associated with parameter "<param1_name>" in the database
    And a input file named "<param1_name>" is created in the task run storage "task-run-"+UUID with content "<param1_file_content>"
    And the value "<param2_value>" is saved and associated with parameter "<param2_name>" in the database
    And a input file named "<param2_name>" is created in the task run storage "task-run-"+UUID with content "<param2_file_content>"
    And the task run states changes to "<task_run_new_state>" because the task is now completely provisioned
    And the App Engine returns a '200 OK' HTTP response with the updated task run information as JSON payload

    Examples:
      | task_namespace                                             | task_version | param1_name | param1_type | param1_value | param1_validation_rule | param2_name | param2_type | param2_value | payload                                                                          | task_run_initial_state | task_run_new_state | param1_file_content | param2_file_content |
      | com.cytomine.dummy.arithmetic.integer.addition.constrained | 1.0.0        | a           | integer     | 25           | lt: 53                 | b           | integer     | 30           | [{\"param_name\": \"a\", \"value\": 25}, {\"param_name\": \"b\", \"value\": 30}] | CREATED                | PROVISIONED        | 25                  | 30                  |

  Scenario Outline: failed batch provisioning of a task run with invalid parameter value

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/task-runs/{run_id}/input-provisions'

    Given a task has been successfully uploaded
    And this task has "<task_namespace>" and "<task_version>"
    And this task has two input parameters
    And the first parameter is "<param1_name>" of type "<param1_type>" with a validation rule "<param1_validation_rule>"
    And the second parameter is "<param2_name>" of type "<param2_type>" without a validation rule
    And a task run has been created for this task
    And this task run is attributed an id in UUID format
    And this task run has not been provisioned yet and is therefore in state "<task_run_initial_state>"
    When a user calls the endpoint with JSON "<payload>"
    Then the App Engine returns an "400" bad request error response with "<error_payload>"
    And the task run state remains as "<task_run_initial_state>" since not all parameters are provisioned yet
    And the App Engine does not record "<param1_value>" nor "<param2_value>" in file storage or database

    Examples:
      | task_namespace                                 | task_version | param1_name | param1_type  | param1_validation_rule | param2_name | param2_type  | payload                                                                          | task_run_initial_state | error_payload                                                                                                                                                                                                                                                                                                                    | param1_value | param2_value|
      | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        | a           | integer      | lt: 53                 | b           | integer      | [{\"param_name\": \"a\", \"value\": 75}, {\"param_name\": \"b\", \"value\": 30}] | CREATED                | {\"error_code\": \"APPE-internal-batch-request-error\", \"message\": \"Error(s) occurred during a handling of a batch request.\", \"details\": {\"errors\": [{\"error_code\": \"APPE-internal-request-validation-error\", \"message\": \"value must be less than defined constraint\", \"details\": { \"param_name\": \"a\"}}]}} | 75           | 30          |

  Scenario Outline: failed single parameter provisioning with unknown parameter name

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/task-runs/{run_id}/input-provisions/{parameter}'

    Given a task has been successfully uploaded
    And this task has "<task_namespace>" and "<task_version>"
    And a task run has been created for this task
    And this task run is attributed an id in UUID format
    And this task run has not been provisioned yet and is therefore in state "<task_run_initial_state>"
    But this task has no parameter named "<unknown_param_name>"
    When a user calls the provisioning endpoint with "<payload>" to provision parameter "<unknown_param_name>" with "<unknown_param_value>" of type "<unknown_param_type>"
    Then the App Engine returns an "404" not found error response with "<error_payload>"
    And the task run state remains as "<task_run_initial_state>" since not all parameters are provisioned yet

    Examples:
      | task_namespace                                 | task_version | unknown_param_value                                 | unknown_param_name | unknown_param_type | payload                                                                                     | task_run_initial_state | error_payload                                                                                                                               |
      | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        | 5                                                   | n_unknown          | integer            | {\"value\": 5, \"param_name\": \"n_unknown\"}                                               | CREATED                | {\"error_code\": \"APPE-internal-parameter-not-found\", \"message\": \"parameter not found\", \"details\": {\"param_name\": \"n_unknown\"}} |
      | com.cytomine.dummy.identity.boolean            | 1.0.0        | false                                               | my_input           | boolean            | {\"param_name\": \"my_input\", \"value\": false}                                            | CREATED                | {\"error_code\": \"APPE-internal-parameter-not-found\", \"message\": \"parameter not found\", \"details\": {\"param_name\": \"my_input\"}}  |
      | com.cytomine.dummy.identity.enumeration        | 1.0.0        | A                                                   | my_input           | enumeration        | {\"param_name\": \"my_input\", \"value\": A}                                                | CREATED                | {\"error_code\": \"APPE-internal-parameter-not-found\", \"message\": \"parameter not found\", \"details\": {\"param_name\": \"my_input\"}}  |
      | com.cytomine.dummy.identity.number             | 1.0.0        | 3.14                                                | my_input           | number             | {\"param_name\": \"my_input\", \"value\": 3.14}                                             | CREATED                | {\"error_code\": \"APPE-internal-parameter-not-found\", \"message\": \"parameter not found\", \"details\": {\"param_name\": \"my_input\"}}  |
      | com.cytomine.dummy.identity.string             | 1.0.0        | \"value\"                                           | my_input           | string             | {\"param_name\": \"my_input\", \"value\": \"value\"}                                        | CREATED                | {\"error_code\": \"APPE-internal-parameter-not-found\", \"message\": \"parameter not found\", \"details\": {\"param_name\": \"my_input\"}}  |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | { \"type\": \"Point\", \"coordinates\": [1.0, 1.0]} | my_input           | geometry           | {\"param_name\": \"input\", \"value\": { \"type\": \"Point\", \"coordinates\": [1.0, 1.0]}} | CREATED                | {\"error_code\": \"APPE-internal-parameter-not-found\", \"message\": \"parameter not found\", \"details\": {\"param_name\": \"my_input\"}}  |
      | com.cytomine.dummy.identity.image              | 1.0.0        | binary                                              | my_input           | image              | binary                                                                                      | CREATED                | {\"error_code\": \"APPE-internal-parameter-not-found\", \"message\": \"parameter not found\", \"details\": {\"param_name\": \"my_input\"}}  |


  Scenario Outline: successful re-provisioning of a parameter for a task run

  See "src/main/resources/spec/api/openapi_spec_v0.1.0.yml" file, in particular the paths:
  - '/task-runs/{run_id}/input-provisions'

    Given a task has been successfully uploaded
    And this task has "<task_namespace>" and "<task_version>"
    And this task has at least one input parameter "<param_name>" of type "<param_type>"
    And no validation rules are defined for this parameter
    And a task run has been created and provisioned with parameter "<param_name>" value "<initial_param_value>" for this task
    And this task run is attributed an id in UUID format
    And this task run is in state "<task_run_state>"
    And the file named "<param_name>" in the task run storage "task-run-"+UUID has content "<param_file_initial_content>"
    When a user calls the provisioning endpoint with "<payload>" to provision parameter "<param_name>" with "<new_param_value>" of type "<param_type>"
    Then the value of parameter "<param_name>" is updated to "<new_param_value>" in the database
    And the input file named "<param_name>" is updated in the task run storage "task-run-"+UUID with content "<param_file_new_content>"
    And the task run state remains unchanged and set to "<task_run_state>"
    And the App Engine returns a '200 OK' HTTP response with the updated task run information as JSON payload

    Examples:
      | task_namespace                                 | task_version | param_name | param_type  | initial_param_value                                | new_param_value                                    | payload                                                                                    | task_run_state | param_file_initial_content                         | param_file_new_content                             |
      | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        | a          | integer     | 5                                                  | 10                                                 | {\"param_name\": \"a\", \"value\": 10}                                                     | CREATED        | 5                                                  | 10                                                 |
      | com.cytomine.dummy.arithmetic.integer.addition | 1.0.0        | b          | integer     | 20                                                 | 30                                                 | {\"param_name\": \"b\", \"value\": 30}                                                     | PROVISIONED    | 20                                                 | 30                                                 |
      | com.cytomine.dummy.identity.boolean            | 1.0.0        | input      | boolean     | true                                               | false                                              | {\"param_name\": \"b\", \"value\": false}                                                  | PROVISIONED    | true                                               | false                                              |
      | com.cytomine.dummy.identity.enumeration        | 1.0.0        | input      | enumeration | A                                                  | B                                                  | {\"param_name\": \"b\", \"value\": B }                                                     | PROVISIONED    | A                                                  | B                                                  |
      | com.cytomine.dummy.identity.number             | 1.0.0        | input      | number      | 2.25                                               | 3.14                                               | {\"param_name\": \"b\", \"value\": 3.14}                                                   | PROVISIONED    | 2.25                                               | 3.14                                               |
      | com.cytomine.dummy.identity.string             | 1.0.0        | input      | string      | \"my_value1\"                                      | \"my_value2\"                                      | {\"param_name\": \"b\", \"value\": \"my_value2\"}                                          | PROVISIONED    | \"my_value1\"                                      | \"my_value2\"                                      |
      | com.cytomine.dummy.identity.geometry           | 1.0.0        | input      | geometry    | {\"type\": \"Point\", \"coordinates\": [1.0, 1.0]} | {\"type\": \"Point\", \"coordinates\": [2.0, 2.0]} | {\"param_name\": \"input\", \"value\": {\"type\": \"Point\", \"coordinates\": [2.0, 2.0]}} | PROVISIONED    | {\"type\": \"Point\", \"coordinates\": [1.0, 1.0]} | {\"type\": \"Point\", \"coordinates\": [2.0, 2.0]} |
      | com.cytomine.dummy.identity.image              | 1.0.0        | input      | image       | binary (content 1)                                 | binary (content 2)                                 | binary (content 2)                                                                         | PROVISIONED    | binary (content 1)                                 | binary (content 2)                                 |


  # TODO failed re-provisioning of a task of which the state is not one of {'CREATED', 'PROVISIONED'}