package be.cytomine.appengine.integration.cucumber;

import be.cytomine.appengine.AppEngineApplication;
import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerTypeConstraint;
import be.cytomine.appengine.dto.misc.TaskIdentifiers;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.handlers.FileStorageHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.openapi.api.DefaultApi;
import be.cytomine.appengine.openapi.invoker.ApiClient;
import be.cytomine.appengine.openapi.invoker.ApiException;
import be.cytomine.appengine.openapi.invoker.Configuration;
import be.cytomine.appengine.openapi.model.TaskRun;
import be.cytomine.appengine.openapi.model.TaskRunInputProvision;
import be.cytomine.appengine.openapi.model.TaskRunInputProvisionInputBody;
import be.cytomine.appengine.openapi.model.TaskRunInputProvisionInputBodyValue;
import be.cytomine.appengine.repositories.IntegerProvisionRepository;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.states.TaskRunState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.*;

@ContextConfiguration(classes = AppEngineApplication.class, loader = SpringBootContextLoader.class)
public class ProvisionTaskStepDefinitions {

    @LocalServerPort
    String port;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    RunRepository taskRunRepository;

    @Autowired
    IntegerProvisionRepository integerProvisionRepository;

    @Autowired
    private DefaultApi appEngineApi;

    @Autowired
    FileStorageHandler fileStorageHandler;
    Task task;
    TaskRun taskRun;
    Run run;

    ApiException exception;
    private Input inputOne;

    @Value("${app-engine.api_prefix}")
    private String apiPrefix;

    @Value("${app-engine.api_version}")
    private String apiVersion;

    private String buildAppEngineUrl() {
        return "http://localhost:" + port + apiPrefix + apiVersion;
    }

    @Given("a task has been successfully uploaded")
    public void a_task_has_been_successfully_uploaded() {
        taskRepository.deleteAll();
        UUID taskLocalIdentifierForTaskOne = UUID.randomUUID();
        String storageIdentifierForTaskOne = "task-" + taskLocalIdentifierForTaskOne + "-def";
        String imageRegistryCompliantNameForTaskOne = "com/cytomine/app-engine/tasks/toy/add-integers:0.1.0";
        TaskIdentifiers taskIdentifiersForTaskOne = new TaskIdentifiers(taskLocalIdentifierForTaskOne, storageIdentifierForTaskOne, imageRegistryCompliantNameForTaskOne);

        task = new Task();
        task.setIdentifier(taskIdentifiersForTaskOne.getLocalTaskIdentifier());
        task.setStorageReference(taskIdentifiersForTaskOne.getStorageIdentifier());
        task.setName("calculator_addintegers");
        task.setNameShort("add_int");
        task.setDescriptorFile("com.cytomine.app-engine.tasks.toy.add-integers");
        task.setDescription("app to add two numbers");
        // add authors
        Set<Author> authors = new HashSet<>();
        Author a = new Author();
        a.setFirstName("Moh");
        a.setLastName("Altahir");
        a.setOrganization("cytomine");
        a.setEmail("siddig@cytomine.com");
        a.setContact(true);
        authors.add(a);
        task.setAuthors(authors);
        // add inputs

        Set<Input> inputs = new HashSet<>();
        Input num1 = new Input();
        num1.setName("num1");
        num1.setDisplayName("First Number");
        num1.setDescription("First number in sum operation");
        IntegerType inputType1_1 = new IntegerType();
        inputType1_1.setId("integer");
        num1.setType(inputType1_1);

        Input num2 = new Input();
        num2.setName("num2");
        num2.setDisplayName("Second Number");
        num2.setDescription("Second number in sum operation");
        IntegerType inputType1_2 = new IntegerType();
        inputType1_2.setId("integer");
        num2.setType(inputType1_2);

        inputs.add(num1);
        inputs.add(num2);
        task.setInputs(inputs);
        // add outputs for task one
        Set<Output> outputs = new HashSet<>();
        Output output = new Output();
        output.setName("sum");
        output.setDisplayName("Sum");
        output.setDescription("sum of two integers");
        IntegerType outputType = new IntegerType();
        outputType.setId("integer");
        output.setType(outputType);
        outputs.add(output);
        task.setOutputs(outputs);


    }

    @Given("this task has {string} and {string}")
    public void this_task_has_and(String namespace, String version) {
        task.setNamespace(namespace);
        task.setVersion(version);
        taskRepository.save(task);
    }

    @Given("this task has at least one input parameter")
    public void this_task_has_at_least_one_input_parameter() {
        Assertions.assertFalse(task.getInputs().isEmpty());
    }

    @When("user calls the endpoint {string} with HTTP method POST")
    public void user_calls_the_endpoint_excluding_version_prefix_e_g_with_http_method_post(String endpoint) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        try {
            if (endpoint.equalsIgnoreCase("/task/namespace/version/runs")) {
                taskRun = appEngineApi.tasksNamespaceVersionRunsPost(task.getNamespace(), task.getVersion());
            }
            if (endpoint.equalsIgnoreCase("/task/id/runs")) {
                taskRun = appEngineApi.tasksTaskIdRunsPost(task.getIdentifier());
            }
        } catch (ApiException e) {
            e.printStackTrace();
            exception = e;
        }
    }

    @Then("a task run is created on the App Engine")
    public void a_task_run_is_created_on_the_app_engine() {
        List<Run> runs = taskRunRepository.findAll();
        Assertions.assertFalse(runs.isEmpty());
        run = runs.get(0);
        Assertions.assertNotNull(run);
    }

    @Then("this task run is attributed an id in UUID format")
    public void this_task_run_is_attributed_a() {
        Assertions.assertNotNull(run.getId());
    }

    @Then("this task run is attributed the state {string}")
    public void this_task_run_is_attributed_the_state(String state) {
        Assertions.assertEquals(run.getState().toString(), state);
    }

    @Then("a storage for the task run is created in the file service under name {string}+UUID")
    public void a_storage_for_the_task_run_is_created_in_the_file_service_under_name(String template) throws FileStorageException {
        boolean storageExists = fileStorageHandler.checkStorageExists(template + "inputs-" + run.getId().toString());
        Assertions.assertTrue(storageExists);
        storageExists = fileStorageHandler.checkStorageExists(template + "outputs-" + run.getId().toString());
        Assertions.assertTrue(storageExists);
    }

    @Then("the App Engine returns a {string} HTTP response with the updated task run information as JSON payload")
    public void the_app_engine_returns_a_http_response_with_the_updated_task_run_information_as_json_payload(String string) {
        Assertions.assertNull(exception);
    }

    // ONE INPUT PARAMETER TESTS

    @Given("this task has only one input parameter {string} of type {string}")
    public void this_task_has_only_one_input_parameter_of_type(String paramName, String type) {
        task.getInputs().clear();

        Input inputOne = new Input();
        inputOne.setName(paramName);
        inputOne.setDisplayName("First Number");
        inputOne.setDescription("First number in sum operation");
        IntegerType inputType1 = new IntegerType();
        inputType1.setId(type);
        inputOne.setType(inputType1);
        task.getInputs().add(inputOne);


        task = taskRepository.saveAndFlush(task);

    }

    @Given("this parameter has no validation rules")
    public void this_parameter_has_no_validation_rules() {
        // this is assumed from run initialization

    }

    @Given("a task run has been created for this task")
    public void a_task_run_has_been_created_for_this_task() throws FileStorageException {
        run = new Run(UUID.randomUUID(), TaskRunState.CREATED, task);
        run = taskRunRepository.saveAndFlush(run);
        Storage runStorage = new Storage("task-run-inputs-" + run.getId().toString());
        fileStorageHandler.createStorage(runStorage);
        runStorage = new Storage("task-run-outputs-" + run.getId().toString());
        fileStorageHandler.createStorage(runStorage);
    }

    @Given("a task run has been created and provisioned with parameter {string} value {string} for this task")
    public void a_task_run_has_been_created_for_this_task(String parameterName, String initialValue) throws FileStorageException {
        run = new Run(UUID.randomUUID(), TaskRunState.CREATED, task);

        IntegerProvision provision = new IntegerProvision(parameterName, Integer.parseInt(initialValue), run.getId());
        run.getProvisions().add(provision);
        if (parameterName.equals("num2")) {
            IntegerProvision provisionNum1 = new IntegerProvision("num1", 10, run.getId());
            run.getProvisions().add(provisionNum1);
            run.setState(TaskRunState.PROVISIONED);
        }
        run = taskRunRepository.save(run);
        Storage runStorage = new Storage("task-run-inputs-" + run.getId().toString());
        fileStorageHandler.createStorage(runStorage);
    }


    @Given("this task run has not been provisioned yet and is therefore in state {string}")
    public void this_task_run_has_not_been_provisioned_yet_and_is_therefore_in_state(String state) {
        integerProvisionRepository.deleteAll();
        List<IntegerProvision> provisions = integerProvisionRepository.findIntegerProvisionByRunId(run.getId());
        Assertions.assertTrue(provisions.isEmpty());
    }

    @When("a user calls the provisioning endpoint with JSON {string} to provision parameter {string} with {int}")
    public void a_user_calls_the_batch_provisioning_endpoint_put_task_runs_input_provisions_with_json_to_provision_parameter_my_input_with(String payload, String parameterName, int value) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        TaskRunInputProvisionInputBody body = new TaskRunInputProvisionInputBody();
        body.setParamName(parameterName);
        body.setValue(new TaskRunInputProvisionInputBodyValue(value));
        TaskRunInputProvision provision;
        try {
            provision = appEngineApi.taskRunsRunIdInputProvisionsParamNamePut(run.getId(), parameterName, body);
        } catch (ApiException e) {
            exception = e;
        }
    }

    @Then("the value {string} is saved and associated parameter {string} in the database")
    public void the_value_is_saved_and_associated_parameter_in_the_database(String value, String parameterName) {

        IntegerProvision provision = integerProvisionRepository.findIntegerProvisionByParameterNameAndRunId(parameterName, run.getId());
        Assertions.assertNotNull(provision);
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));
    }

    @Then("a input file named {string} is created in the task run storage {string}+UUID with content {string}")
    public void a_input_file_named_is_created_in_the_task_run_storage_with_content(String fileName, String template, String content) throws FileStorageException {
        FileData descriptorMetaData = new FileData(fileName, template + "inputs-" + run.getId().toString());
        FileData descriptor = fileStorageHandler.readFile(descriptorMetaData);
        Assertions.assertNotNull(descriptor);
        String fileContent = new String(descriptor.getFileData(), StandardCharsets.UTF_8); // default encoding assumed
        Assertions.assertTrue(fileContent.equalsIgnoreCase(content));
    }

    @Then("the task run states changes to {string} because the task is now completely provisioned")
    public void the_task_run_states_changes_to_because_the_task_is_now_completely_provisioned(String state) {
        Optional<Run> optionalRun = taskRunRepository.findById(run.getId());
        optionalRun.ifPresent(value -> run = value);
        Assertions.assertEquals(run.getState().toString(), state);
    }

    // PROVISIONING OF TWO PARAMETERS

    @Given("this task has two input parameters")
    public void this_task_has_two_input_parameters() {
        // this will be covered by the definition of the parameters in the next step test
    }

    @Given("the first parameter is {string} of type {string} without a validation rule")
    public void the_first_parameter_is_of_type_without_a_validation_rule(String parameterName, String type) {
        Set<Input> inputs = task.getInputs();
        Input num1 = new Input();
        num1.setName(parameterName);
        num1.setDisplayName("First Number");
        num1.setDescription("First number in sum operation");
        IntegerType inputType1_1 = new IntegerType();
        inputType1_1.setId(type);
        num1.setType(inputType1_1);


        inputs.add(num1);
        task.setInputs(inputs);
    }

    @Given("the second parameter is {string} of type {string} without a validation rule")
    public void the_second_parameter_is_of_type_without_a_validation_rule(String parameterName, String type) {
        Set<Input> inputs = task.getInputs();
        Input num2 = new Input();
        num2.setName(parameterName);
        num2.setDisplayName("Second Number");
        num2.setDescription("Second number in sum operation");
        IntegerType inputType1_2 = new IntegerType();
        inputType1_2.setId(type);
        num2.setType(inputType1_2);

        inputs.add(num2);
        task.setInputs(inputs);
    }

    @Given("no validation rules are defined for these parameters")
    public void no_validation_rules_are_defined_for_these_parameters() {
        // no validation rules were added in the previous step
    }

    @When("a user calls the endpoint with JSON {string}")
    public void a_user_calls_the_endpoint_with_json(String jsonPayload) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode payload = mapper.readTree(jsonPayload);
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        List<TaskRunInputProvisionInputBody> provisionList = new ArrayList<>();
        if (payload.isArray()) {
            for (JsonNode parameter : payload) {
                TaskRunInputProvisionInputBody parameterBody = new TaskRunInputProvisionInputBody();
                parameterBody.setParamName(parameter.get("param_name").textValue());
                parameterBody.setValue(new TaskRunInputProvisionInputBodyValue(parameter.get("value").asInt()));
                provisionList.add(parameterBody);
            }
        }
        List<TaskRunInputProvision> provisions;
        try {
            provisions = appEngineApi.taskRunsRunIdInputProvisionsPut(run.getId(), provisionList);
        } catch (ApiException e) {
            exception = e;
        }
    }

    @Then("the value {string} is saved and associated with parameter {string} in the database")
    public void the_value_is_saved_and_associated_with_parameter_in_the_database(String value, String parameterName) {
        IntegerProvision provision = integerProvisionRepository.findIntegerProvisionByParameterNameAndRunId(parameterName, run.getId());
        Assertions.assertNotNull(provision);
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));
    }


    @Then("the task run state remains as {string} since not all parameters are provisioned yet")
    public void the_task_run_state_remains_as_since_not_all_parameters_are_provisioned_yet(String state) {
        Optional<Run> optionalRun = taskRunRepository.findById(run.getId());
        optionalRun.ifPresent(value -> run = value);
        Assertions.assertEquals(run.getState().toString(), state);
    }


    // ONE VALIDATION RULE

    @Given("the first parameter is {string} of type {string} with a validation rule {string}")
    public void the_first_parameter_is_of_type_with_a_validation_rule(String parameterName, String type, String validationRule) {
        Set<Input> inputs = task.getInputs();
        inputs.removeIf(input -> input.getName().equalsIgnoreCase(parameterName));

        Input num1 = new Input();
        num1.setName(parameterName);
        num1.setDisplayName("First Number");
        num1.setDescription("First number in sum operation");
        IntegerType inputType1_1 = new IntegerType();
        inputType1_1.setId(type);
        String[] ruleSet = validationRule.split(":");
        if (ruleSet[0].equalsIgnoreCase(IntegerTypeConstraint.LOWER_THAN.getStringKey())) {
            inputType1_1.setLt(Integer.parseInt(ruleSet[1].trim()));
        }
        num1.setType(inputType1_1);


        inputs.add(num1);
        task.setInputs(inputs);

    }

    @Then("the value {string} is saved and associated with parameter {string} after passing the validation rule {string}")
    public void the_value_is_saved_and_associated_with_parameter_after_passing_the_validation_rule(String value, String parameterName, String validationRule) {
        IntegerProvision provision = integerProvisionRepository.findIntegerProvisionByParameterNameAndRunId(parameterName, run.getId());
        Assertions.assertNotNull(provision);
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));
    }


    //INVALID PARAMETER VALUE


    @Then("the App Engine returns an {string} bad request error response with {string}")
    public void the_app_engine_returns_an_error_response_with(String response, String errorPayLoad) throws JsonProcessingException {
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(Integer.parseInt(response), exception.getCode());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorJsonNodeFromScenario = mapper.readTree(errorPayLoad);
        JsonNode errorJsonNodeFromServer = mapper.readTree(exception.getResponseBody());
        Assertions.assertEquals(errorJsonNodeFromScenario, errorJsonNodeFromServer);

    }


    @Then("the App Engine does not record {string} nor {string} in file storage or database")
    public void the_app_engine_does_not_record_nor_in_file_storage_or_database(String parameterOneValue, String parameterTwoValue) {
        List<IntegerProvision> provisionList = integerProvisionRepository.findIntegerProvisionByRunId(run.getId());
        Assertions.assertTrue(provisionList.isEmpty());
    }


    // UNKNOWN PARAMETER

    @Given("this task has no parameter named {string}")
    public void this_task_has_no_parameter_named(String parameterName) {
        Set<Input> inputs = task.getInputs();
        inputs.removeIf(input -> input.getName().equalsIgnoreCase(parameterName));
    }

    @Then("the App Engine returns an {string} not found error response with {string}")
    public void the_app_engine_returns_a_not_found_error_response_with(String response, String errorPayLoad) throws JsonProcessingException {
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(Integer.parseInt(response), exception.getCode());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorJsonNodeFromScenario = mapper.readTree(errorPayLoad);
        JsonNode errorJsonNodeFromServer = mapper.readTree(exception.getResponseBody());
        Assertions.assertEquals(errorJsonNodeFromScenario, errorJsonNodeFromServer);
    }


    // RE-PROVISIONING
    @Given("no validation rules are defined for this parameter")
    public void no_validation_rules_are_defined_for_this_parameter() {
        // assumed from previous step
    }

    @Given("this task run is in state {string}")
    public void this_task_run_is_in_state(String state) {
        Assertions.assertEquals(run.getState().toString(), state);
    }

    @Given("the file named {string} in the task run storage {string}+UUID has content {string}")
    public void the_file_named_in_the_task_run_storage_has_content(String fileName, String template, String content) throws FileStorageException {
        FileData parameterFile = new FileData(content.getBytes(StandardCharsets.UTF_8), fileName); // UTF_8 is assumed in tests
        Storage storage = new Storage(template + "inputs-" +run.getId().toString());
        fileStorageHandler.createFile(storage, parameterFile);
    }

    @Given("this task has at least one input parameter {string} of type {string}")
    public void this_task_has_at_least_one_input_parameter_of_type(String paramName, String type) {
        task.getInputs().clear();

        inputOne = new Input();
        inputOne.setName(paramName);
        inputOne.setDisplayName("First Number");
        inputOne.setDescription("First number in sum operation");
        IntegerType inputType1 = new IntegerType();
        inputType1.setId(type);
        inputOne.setType(inputType1);
        task.getInputs().add(inputOne);

        Input inputTwo = new Input();
        inputTwo.setName("num2");
        inputTwo.setDisplayName("Second Number");
        inputTwo.setDescription("Second number in sum operation");
        IntegerType inputType2 = new IntegerType();
        inputType2.setId(type);
        inputTwo.setType(inputType2);
        task.getInputs().add(inputTwo);

        task = taskRepository.saveAndFlush(task);

    }


    @Then("the task run state remains unchanged and set to {string}")
    public void the_task_run_state_remains_unchanged_and_set_to(String state) {
        Optional<Run> optionalRun = taskRunRepository.findById(run.getId());
        optionalRun.ifPresent(value -> run = value);
        Assertions.assertEquals(run.getState().toString(), state);
    }

    @Then("the value of parameter {string} is updated to {string} in the database")
    public void the_value_of_parameter_is_updated_to_in_the_database(String parameterName, String newValue) {
        IntegerProvision provision = integerProvisionRepository.findIntegerProvisionByParameterNameAndRunId(parameterName, run.getId());
        Assertions.assertNotNull(provision);
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));
        Assertions.assertEquals(provision.getValue(), Integer.parseInt(newValue));
    }

    @Then("the input file named {string} is updated in the task run storage {string}+UUID with content {string}")
    public void the_input_file_named_is_updated_in_the_task_run_storage_with_content(String fileName, String template, String content) throws FileStorageException {
        FileData descriptorMetaData = new FileData(fileName, template + "inputs-" + run.getId().toString());
        FileData descriptor = fileStorageHandler.readFile(descriptorMetaData);
        Assertions.assertNotNull(descriptor);
        String fileContent = new String(descriptor.getFileData(), StandardCharsets.UTF_8); // default encoding assumed
        Assertions.assertTrue(fileContent.equalsIgnoreCase(content));
    }
}
