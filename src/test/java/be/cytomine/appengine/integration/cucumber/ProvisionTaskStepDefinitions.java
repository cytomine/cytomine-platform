package be.cytomine.appengine.integration.cucumber;

import be.cytomine.appengine.AppEngineApplication;
import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.inputs.task.GenericParameterProvision;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.handlers.FileStorageHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.models.task.bool.BooleanPersistence;
import be.cytomine.appengine.models.task.bool.BooleanType;
import be.cytomine.appengine.models.task.enumeration.EnumerationPersistence;
import be.cytomine.appengine.models.task.enumeration.EnumerationType;
import be.cytomine.appengine.models.task.geometry.GeometryPersistence;
import be.cytomine.appengine.models.task.geometry.GeometryType;
import be.cytomine.appengine.models.task.image.ImagePersistence;
import be.cytomine.appengine.models.task.image.ImageType;
import be.cytomine.appengine.models.task.integer.IntegerPersistence;
import be.cytomine.appengine.models.task.integer.IntegerType;
import be.cytomine.appengine.models.task.number.NumberPersistence;
import be.cytomine.appengine.models.task.number.NumberType;
import be.cytomine.appengine.models.task.string.StringPersistence;
import be.cytomine.appengine.models.task.string.StringType;
import be.cytomine.appengine.openapi.api.DefaultApi;
import be.cytomine.appengine.openapi.invoker.ApiClient;
import be.cytomine.appengine.openapi.invoker.ApiException;
import be.cytomine.appengine.openapi.invoker.Configuration;
import be.cytomine.appengine.openapi.model.TaskRun;
import be.cytomine.appengine.repositories.TypePersistenceRepository;
import be.cytomine.appengine.repositories.bool.BooleanPersistenceRepository;
import be.cytomine.appengine.repositories.enumeration.EnumerationPersistenceRepository;
import be.cytomine.appengine.repositories.geometry.GeometryPersistenceRepository;
import be.cytomine.appengine.repositories.image.ImagePersistenceRepository;
import be.cytomine.appengine.repositories.integer.IntegerPersistenceRepository;
import be.cytomine.appengine.repositories.number.NumberPersistenceRepository;
import be.cytomine.appengine.repositories.string.StringPersistenceRepository;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.states.TaskRunState;
import be.cytomine.appengine.utils.TaskTestsUtils;
import be.cytomine.appengine.utils.TestTaskBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@ContextConfiguration(classes = AppEngineApplication.class, loader = SpringBootContextLoader.class)
public class ProvisionTaskStepDefinitions {

    @LocalServerPort
    private String port;

    @Autowired
    private DefaultApi appEngineApi;

    @Autowired
    private FileStorageHandler fileStorageHandler;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RunRepository taskRunRepository;

    @Autowired
    private BooleanPersistenceRepository booleanProvisionRepository;

    @Autowired
    private EnumerationPersistenceRepository enumerationProvisionRepository;

    @Autowired
    private GeometryPersistenceRepository geometryProvisionRepository;

    @Autowired
    private IntegerPersistenceRepository integerProvisionRepository;

    @Autowired
    private NumberPersistenceRepository numberProvisionRepository;

    @Autowired
    private StringPersistenceRepository stringProvisionRepository;

    @Autowired
    private ImagePersistenceRepository imageProvisionRepository;

    @Autowired
    private TypePersistenceRepository typePersistenceRepository;

    @Value("${app-engine.api_prefix}")
    private String apiPrefix;

    @Value("${app-engine.api_version}")
    private String apiVersion;

    private ApiException exception;
    private RestClientResponseException persistedException;

    private Run persistedRun;
    private Task persistedTask;
    private TaskRun taskRun;

    private String buildAppEngineUrl() {
        return "http://localhost:" + port + apiPrefix + apiVersion;
    }

    @Given("a task has been successfully uploaded")
    public void a_task_has_been_successfully_uploaded() {
        taskRepository.deleteAll();
        persistedTask = TestTaskBuilder.buildHardcodedAddInteger();
    }

    @Given("this task has {string} and {string}")
    public void this_task_has_and(String namespace, String version) {
        String bundleFilename = namespace + "-" + version + ".zip";
        persistedTask = TestTaskBuilder.buildTaskFromResource(bundleFilename);
        persistedTask = taskRepository.save(persistedTask);
    }

    @Given("this task has at least one input parameter")
    public void this_task_has_at_least_one_input_parameter() {
        Assertions.assertFalse(persistedTask.getInputs().isEmpty());
    }

    @When("user calls the endpoint {string} with HTTP method POST")
    public void user_calls_the_endpoint_excluding_version_prefix_e_g_with_http_method_post(String endpoint) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        try {
            if (endpoint.equalsIgnoreCase("/task/namespace/version/runs")) {
                taskRun = appEngineApi.createTaskRunByNamespaceVersion(persistedTask.getNamespace(), persistedTask.getVersion());
            }
            if (endpoint.equalsIgnoreCase("/task/id/runs")) {
                taskRun = appEngineApi.createTaskRunByUUID(persistedTask.getIdentifier());
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
        persistedRun = runs.get(0);
        Assertions.assertNotNull(persistedRun);
    }

    @Then("this task run is attributed an id in UUID format")
    public void this_task_run_is_attributed_a() {
        Assertions.assertNotNull(persistedRun.getId());
    }

    @Then("this task run is attributed the state {string}")
    public void this_task_run_is_attributed_the_state(String state) {
        Assertions.assertEquals(persistedRun.getState().toString(), state);
    }

    @Then("a storage for the task run is created in the file service under name {string}+UUID")
    public void a_storage_for_the_task_run_is_created_in_the_file_service_under_name(String template) throws FileStorageException {
        boolean storageExists = fileStorageHandler.checkStorageExists(template + "inputs-" + persistedRun.getId().toString());
        Assertions.assertTrue(storageExists);
        storageExists = fileStorageHandler.checkStorageExists(template + "outputs-" + persistedRun.getId().toString());
        Assertions.assertTrue(storageExists);
    }

    @Then("the App Engine returns a {string} HTTP response with the updated task run information as JSON payload")
    public void the_app_engine_returns_a_http_response_with_the_updated_task_run_information_as_json_payload(String string) {
        Assertions.assertNull(exception);
    }

    // ONE INPUT PARAMETER TESTS

    @Given("this task has only one input parameter {string} of type {string}")
    public void this_task_has_only_one_input_parameter_of_type(String paramName, String type) {
        persistedTask.getInputs().removeIf(input -> {
            switch (input.getType().getClass().getSimpleName()) {
                case "BooleanType":
                    return !(((BooleanType) input.getType()).getId().equals(type) && input.getName().equals(paramName));
                case "IntegerType":
                    return !(((IntegerType) input.getType()).getId().equals(type) && input.getName().equals(paramName));
                case "NumberType":
                    return !(((NumberType) input.getType()).getId().equals(type) && input.getName().equals(paramName));
                case "StringType":
                    return !(((StringType) input.getType()).getId().equals(type) && input.getName().equals(paramName));
                case "EnumerationType":
                    return !(((EnumerationType) input.getType()).getId().equals(type) && input.getName().equals(paramName));
                case "GeometryType":
                    return !(((GeometryType) input.getType()).getId().equals(type) && input.getName().equals(paramName));
                case "ImageType":
                    return !(((ImageType) input.getType()).getId().equals(type) && input.getName().equals(paramName));
                default:
                    return false;
            }
        });
        persistedTask = taskRepository.saveAndFlush(persistedTask);
        Assertions.assertEquals(persistedTask.getInputs().size(), 1);
    }

    @Given("this parameter has no validation rules")
    public void this_parameter_has_no_validation_rules() {
        // this is assumed from run initialization
    }

    @Given("a task run has been created for this task")
    public void a_task_run_has_been_created_for_this_task() throws FileStorageException {
        persistedRun = new Run(UUID.randomUUID(), TaskRunState.CREATED, persistedTask);
        persistedRun = taskRunRepository.saveAndFlush(persistedRun);
        Storage runStorage = new Storage("task-run-inputs-" + persistedRun.getId().toString());
        fileStorageHandler.createStorage(runStorage);
        runStorage = new Storage("task-run-outputs-" + persistedRun.getId().toString());
        fileStorageHandler.createStorage(runStorage);
    }

    @Given("a task run has been created and provisioned with parameter {string} value {string} for this task")
    public void a_task_run_has_been_created_for_this_task(String parameterName, String initialValue) throws FileStorageException {
        persistedRun = new Run(UUID.randomUUID(), TaskRunState.CREATED, persistedTask);

        Input input = persistedTask
                .getInputs()
                .stream()
                .filter(i -> i.getName().equals(parameterName))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(input);

        TypePersistence provision = new TypePersistence();
        switch (input.getType().getClass().getSimpleName()) {
            case "BooleanType":
                provision = new BooleanPersistence();
                provision.setValueType(ValueType.BOOLEAN);
                ((BooleanPersistence) provision).setValue(Boolean.parseBoolean(initialValue));
                break;
            case "IntegerType":
                provision = new IntegerPersistence();
                provision.setValueType(ValueType.INTEGER);
                ((IntegerPersistence) provision).setValue(Integer.parseInt(initialValue));
                break;
            case "NumberType":
                provision = new NumberPersistence();
                provision.setValueType(ValueType.NUMBER);
                ((NumberPersistence) provision).setValue(Double.parseDouble(initialValue));
                break;
            case "StringType":
                provision = new StringPersistence();
                provision.setValueType(ValueType.STRING);
                ((StringPersistence) provision).setValue(initialValue);
                break;
            case "EnumerationType":
                provision = new EnumerationPersistence();
                provision.setValueType(ValueType.ENUMERATION);
                ((EnumerationPersistence) provision).setValue(initialValue);
                break;
            case "GeometryType":
                provision = new GeometryPersistence();
                provision.setValueType(ValueType.GEOMETRY);
                ((GeometryPersistence) provision).setValue(initialValue);
                break;
            case "ImageType":
                provision = new ImagePersistence();
                provision.setValueType(ValueType.IMAGE);
                ((ImagePersistence) provision).setValue(initialValue.getBytes());
                break;
        }

        provision.setRunId(persistedRun.getId());
        provision.setParameterName(parameterName);
        provision.setParameterType(ParameterType.INPUT);
        persistedRun.getProvisions().add(provision);

        switch (parameterName) {
            case "b":
                IntegerPersistence provisionInputA = new IntegerPersistence();
                provisionInputA.setRunId(persistedRun.getId());
                provisionInputA.setValueType(ValueType.INTEGER);
                provisionInputA.setValue(10);
                provisionInputA.setParameterName("a");
                provisionInputA.setParameterType(ParameterType.INPUT);
                //"a", String.valueOf(10), run.getId()
                persistedRun.getProvisions().add(provisionInputA);
                persistedRun.setState(TaskRunState.PROVISIONED);
                break;

            case "input":
            persistedRun.setState(TaskRunState.PROVISIONED);
                break;
        }

        persistedRun = taskRunRepository.save(persistedRun);
        Storage runStorage = new Storage("task-run-inputs-" + persistedRun.getId().toString());
        fileStorageHandler.createStorage(runStorage);
    }

    @Given("this task run has not been provisioned yet and is therefore in state {string}")
    public void this_task_run_has_not_been_provisioned_yet_and_is_therefore_in_state(String state) {
        typePersistenceRepository.deleteAll();
        List<TypePersistence> provisions = typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(persistedRun.getId() , ParameterType.INPUT);
        Assertions.assertTrue(provisions.isEmpty());
    }

    @When("a user calls the provisioning endpoint with {string} to provision parameter {string} with {string} of type {string}")
    public void a_user_calls_the_batch_provisioning_endpoint_put_task_runs_input_provisions_with_json_to_provision_parameter_my_input_with(String payload, String parameterName, String value, String type) {
        String endpointUrl = buildAppEngineUrl() + "/task-runs/" + persistedRun.getId() + "/input-provisions/" + parameterName;
        Input input = persistedTask
                .getInputs()
                .stream()
                .filter(i -> i.getName().equals(parameterName))
                .findFirst()
                .orElse(null);

        HttpEntity<?> entity = null;
        if (type.equals("image")) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(value.getBytes()) {
                @Override
                public String getFilename() {
                    return "file.txt";
                }
            };
            body.add("file", fileResource);

            entity = new HttpEntity<>(body, headers);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonNode = mapper.valueToTree(
                TaskTestsUtils.createProvision(parameterName, input == null ? "" : input.getType().getClass().getSimpleName(), value)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            entity = new HttpEntity<>(jsonNode, headers);
        }

        try {
            new RestTemplate().exchange(endpointUrl, HttpMethod.PUT, entity, JsonNode.class);
        } catch (RestClientResponseException e) {
            persistedException = e;
        }
    }

    @Then("the value {string} is saved and associated parameter {string} in the database")
    public void the_value_is_saved_and_associated_parameter_in_the_database(String value, String parameterName) {
        Input input = persistedTask
                .getInputs()
                .stream()
                .filter(i -> i.getName().equals(parameterName))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(input);

        TypePersistence provision = null;
        switch (input.getType().getClass().getSimpleName()) {
            case "BooleanType":
                provision = booleanProvisionRepository.findBooleanPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId() , ParameterType.INPUT);
                break;
            case "IntegerType":
                provision = integerProvisionRepository.findIntegerPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId() , ParameterType.INPUT);
                break;
            case "NumberType":
                provision = numberProvisionRepository.findNumberPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId() , ParameterType.INPUT);
                break;
            case "StringType":
                provision = stringProvisionRepository.findStringPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId() , ParameterType.INPUT);
                break;
            case "EnumerationType":
                provision = enumerationProvisionRepository.findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId() , ParameterType.INPUT);
                break;
            case "GeometryType":
                provision = geometryProvisionRepository.findGeometryPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId() , ParameterType.INPUT);
                break;
            case "ImageType":
                provision = imageProvisionRepository.findImagePersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId() , ParameterType.INPUT);
                break;
        }

        Assertions.assertNotNull(provision);
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));
    }

    @Then("a input file named {string} is created in the task run storage {string}+UUID with content {string}")
    public void a_input_file_named_is_created_in_the_task_run_storage_with_content(String fileName, String template, String content) throws FileStorageException {
        FileData descriptorMetaData = new FileData(fileName, template + "inputs-" + persistedRun.getId().toString());
        FileData descriptor = fileStorageHandler.readFile(descriptorMetaData);
        Assertions.assertNotNull(descriptor);
        String fileContent = new String(descriptor.getFileData(), StandardCharsets.UTF_8); // default encoding assumed
        Assertions.assertTrue(fileContent.equalsIgnoreCase(content));
    }

    @Then("the task run states changes to {string} because the task is now completely provisioned")
    public void the_task_run_states_changes_to_because_the_task_is_now_completely_provisioned(String state) {
        Optional<Run> optionalRun = taskRunRepository.findById(persistedRun.getId());
        optionalRun.ifPresent(value -> persistedRun = value);
        Assertions.assertEquals(persistedRun.getState().toString(), state);
    }

    // PROVISIONING OF TWO PARAMETERS

    @Given("this task has two input parameters")
    public void this_task_has_two_input_parameters() {
        // this will be covered by the definition of the parameters in the next step test
    }

    @Given("the first parameter is {string} of type {string} without a validation rule")
    public void the_first_parameter_is_of_type_without_a_validation_rule(String parameterName, String type) {
        Assertions.assertTrue(persistedTask.getInputs().stream()
          .anyMatch(input -> ((IntegerType)input.getType()).getId().equals(type) && input.getName().equals(parameterName)));
    }

    @Given("the second parameter is {string} of type {string} without a validation rule")
    public void the_second_parameter_is_of_type_without_a_validation_rule(String parameterName, String type) {
        Assertions.assertTrue(persistedTask.getInputs().stream()
          .anyMatch(input -> ((IntegerType)input.getType()).getId().equals(type) && input.getName().equals(parameterName)));
    }

    @Given("no validation rules are defined for these parameters")
    public void no_validation_rules_are_defined_for_these_parameters() {
        // no validation rules were added in the previous step
    }

    @When("a user calls the endpoint with JSON {string}")
    public void a_user_calls_the_endpoint_with_json(String jsonPayload) throws JsonProcessingException {
        String endpointUrl = buildAppEngineUrl() + "/task-runs/" + persistedRun.getId() + "/input-provisions";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode payload = mapper.readTree(jsonPayload);

        List<ObjectNode> provisions = new ArrayList<>();
        for (JsonNode parameter : payload) {
            String parameterName = parameter.get("param_name").textValue();
            Input input = persistedTask
                    .getInputs()
                    .stream()
                    .filter(i -> i.getName().equals(parameterName))
                    .findFirst()
                    .orElse(null);

            GenericParameterProvision provision = TaskTestsUtils.createProvision(
                parameterName,
                input == null ? "" : input.getType().getClass().getSimpleName(),
                parameter.get("value").asText()
            );
            provisions.add(mapper.valueToTree(provision));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<ObjectNode>> entity = new HttpEntity<>(provisions, headers);

        try {
            new RestTemplate().exchange(endpointUrl, HttpMethod.PUT, entity, JsonNode.class);
        } catch (RestClientResponseException e) {
            persistedException = e;
        }
    }

    @Then("the value {string} is saved and associated with parameter {string} in the database")
    public void the_value_is_saved_and_associated_with_parameter_in_the_database(String value, String parameterName) {
        IntegerPersistence provision = integerProvisionRepository.findIntegerPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId() , ParameterType.INPUT);
        Assertions.assertNotNull(provision);
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));
    }


    @Then("the task run state remains as {string} since not all parameters are provisioned yet")
    public void the_task_run_state_remains_as_since_not_all_parameters_are_provisioned_yet(String state) {
        Optional<Run> optionalRun = taskRunRepository.findById(persistedRun.getId());
        optionalRun.ifPresent(value -> persistedRun = value);
        Assertions.assertEquals(persistedRun.getState().toString(), state);
    }


    // ONE VALIDATION RULE

    @Given("the first parameter is {string} of type {string} with a validation rule {string}")
    public void the_first_parameter_is_of_type_with_a_validation_rule(String parameterName, String type, String validationRule) {
        Optional<Input> parameterOptional = persistedTask.getInputs().stream().filter(input -> ((IntegerType)input.getType()).getId().equals(type) && input.getName().equals(parameterName)).findFirst();

        Assertions.assertTrue(parameterOptional.isPresent());

        // adding constraint to the existing unconstrainted parameter
        Input parameter = parameterOptional.get();
        String[] ruleSet = validationRule.split(":");
        switch (ruleSet[0].trim()) {
            case "lt":
                ((IntegerType)parameter.getType()).setLt(Integer.parseInt(ruleSet[1].trim()));
                break;
            default:
                break;
        }
    }

    @Then("the value {string} is saved and associated with parameter {string} after passing the validation rule {string}")
    public void the_value_is_saved_and_associated_with_parameter_after_passing_the_validation_rule(String value, String parameterName, String validationRule) {
        TypePersistence provision = integerProvisionRepository.findIntegerPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId() , ParameterType.INPUT);
        Assertions.assertNotNull(provision);
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));
    }

    //INVALID PARAMETER VALUE

    @Then("the App Engine returns an {string} bad request error response with {string}")
    public void the_app_engine_returns_an_error_response_with(String response, String errorPayLoad) throws JsonProcessingException {
        Assertions.assertNotNull(persistedException);
        Assertions.assertEquals(Integer.parseInt(response), persistedException.getStatusCode().value());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorJsonNodeFromScenario = mapper.readTree(errorPayLoad);
        JsonNode errorJsonNodeFromServer = mapper.readTree(persistedException.getResponseBodyAsString());
        Assertions.assertEquals(errorJsonNodeFromScenario, errorJsonNodeFromServer);
    }

    @Then("the App Engine does not record {string} nor {string} in file storage or database")
    public void the_app_engine_does_not_record_nor_in_file_storage_or_database(String parameterOneValue, String parameterTwoValue) {
        List<IntegerPersistence> provisionList = integerProvisionRepository.findIntegerPersistenceByRunIdAndParameterType(persistedRun.getId() , ParameterType.INPUT);
        Assertions.assertTrue(provisionList.isEmpty());
    }

    // UNKNOWN PARAMETER

    @Given("this task has no parameter named {string}")
    public void this_task_has_no_parameter_named(String parameterName) {
        Set<Input> inputs = persistedTask.getInputs();
        inputs.removeIf(input -> input.getName().equalsIgnoreCase(parameterName));
    }

    @Then("the App Engine returns an {string} not found error response with {string}")
    public void the_app_engine_returns_a_not_found_error_response_with(String response, String errorPayLoad) throws JsonProcessingException {
        Assertions.assertNotNull(persistedException);
        Assertions.assertEquals(Integer.parseInt(response), persistedException.getStatusCode().value());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorJsonNodeFromScenario = mapper.readTree(errorPayLoad);
        JsonNode errorJsonNodeFromServer = mapper.readTree(persistedException.getResponseBodyAsString());
        Assertions.assertEquals(errorJsonNodeFromScenario, errorJsonNodeFromServer);
    }

    // RE-PROVISIONING

    @Given("no validation rules are defined for this parameter")
    public void no_validation_rules_are_defined_for_this_parameter() {
        // assumed from previous step
    }

    @Given("this task run is in state {string}")
    public void this_task_run_is_in_state(String state) {
        Assertions.assertEquals(persistedRun.getState().toString(), state);
    }

    @Given("the file named {string} in the task run storage {string}+UUID has content {string}")
    public void the_file_named_in_the_task_run_storage_has_content(String fileName, String template, String content) throws FileStorageException {
        FileData parameterFile = new FileData(content.getBytes(StandardCharsets.UTF_8), fileName); // UTF_8 is assumed in tests
        Storage storage = new Storage(template + "inputs-" + persistedRun.getId().toString());
        fileStorageHandler.createFile(storage, parameterFile);
    }

    @Given("this task has at least one input parameter {string} of type {string}")
    public void this_task_has_at_least_one_input_parameter_of_type(String paramName, String type) {
        // Check if the set contains an object matching the conditions
        // Will raise an error if the persisted task is not valid with the test
        boolean hasInput = persistedTask
                .getInputs()
                .stream()
                .anyMatch(input -> {
                    switch (type) {
                        case "boolean":
                            return ((BooleanType) input.getType()).getId().equals(type) && input.getName().equals(paramName);
                        case "integer":
                            return ((IntegerType) input.getType()).getId().equals(type) && input.getName().equals(paramName);
                        case "number":
                            return ((NumberType) input.getType()).getId().equals(type) && input.getName().equals(paramName);
                        case "string":
                            return ((StringType) input.getType()).getId().equals(type) && input.getName().equals(paramName);
                        case "enumeration":
                            return ((EnumerationType) input.getType()).getId().equals(type) && input.getName().equals(paramName);
                        case "geometry":
                            return ((GeometryType) input.getType()).getId().equals(type) && input.getName().equals(paramName);
                        case "image":
                            return ((ImageType) input.getType()).getId().equals(type) && input.getName().equals(paramName);
                        default:
                            return false;
                    }
                });

        Assertions.assertTrue(hasInput);
    }

    @Then("the task run state remains unchanged and set to {string}")
    public void the_task_run_state_remains_unchanged_and_set_to(String state) {
        Optional<Run> optionalRun = taskRunRepository.findById(persistedRun.getId());
        optionalRun.ifPresent(value -> persistedRun = value);
        Assertions.assertEquals(persistedRun.getState().toString(), state);
    }

    @Then("the value of parameter {string} is updated to {string} in the database")
    public void the_value_of_parameter_is_updated_to_in_the_database(String parameterName, String newValue) {
        Input input = persistedTask
                .getInputs()
                .stream()
                .filter(i -> i.getName().equals(parameterName))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(input);

        TypePersistence provision = null;
        switch (input.getType().getClass().getSimpleName()) {
            case "BooleanType":
                provision = booleanProvisionRepository.findBooleanPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                Assertions.assertEquals(Boolean.parseBoolean(newValue), ((BooleanPersistence) provision).isValue());
                break;
            case "IntegerType":
                provision = integerProvisionRepository.findIntegerPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                Assertions.assertEquals(Integer.parseInt(newValue), ((IntegerPersistence) provision).getValue());
                break;
            case "NumberType":
                provision = numberProvisionRepository.findNumberPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                Assertions.assertEquals(Double.parseDouble(newValue), ((NumberPersistence) provision).getValue());
                break;
            case "StringType":
                provision = stringProvisionRepository.findStringPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                Assertions.assertEquals(newValue, ((StringPersistence) provision).getValue());
                break;
            case "EnumerationType":
                provision = enumerationProvisionRepository.findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                Assertions.assertEquals(newValue, ((EnumerationPersistence) provision).getValue());
                break;
            case "GeometryType":
                provision = geometryProvisionRepository.findGeometryPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                Assertions.assertEquals(newValue, ((GeometryPersistence) provision).getValue());
                break;
            case "ImageType":
                provision = imageProvisionRepository.findImagePersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
        }

        Assertions.assertNotNull(provision);
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));
    }

    @Then("the input file named {string} is updated in the task run storage {string}+UUID with content {string}")
    public void the_input_file_named_is_updated_in_the_task_run_storage_with_content(String fileName, String template, String content) throws FileStorageException {
        FileData descriptorMetaData = new FileData(fileName, template + "inputs-" + persistedRun.getId().toString());
        FileData descriptor = fileStorageHandler.readFile(descriptorMetaData);
        Assertions.assertNotNull(descriptor);
        String fileContent = new String(descriptor.getFileData(), StandardCharsets.UTF_8); // default encoding assumed
        Assertions.assertTrue(fileContent.equalsIgnoreCase(content));
    }
}
