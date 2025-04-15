package be.cytomine.appengine.integration.cucumber;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import be.cytomine.appengine.handlers.StorageDataType;
import be.cytomine.appengine.models.task.collection.CollectionPersistence;
import be.cytomine.appengine.repositories.collection.CollectionPersistenceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cucumber.datatable.DataTable;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestClientResponseException;

import be.cytomine.appengine.AppEngineApplication;
import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.inputs.task.GenericParameterProvision;
import be.cytomine.appengine.dto.inputs.task.types.image.ImageTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.number.NumberTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.string.StringTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.wsi.WsiTypeConstraint;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.models.task.bool.BooleanPersistence;
import be.cytomine.appengine.models.task.enumeration.EnumerationPersistence;
import be.cytomine.appengine.models.task.file.FilePersistence;
import be.cytomine.appengine.models.task.geometry.GeometryPersistence;
import be.cytomine.appengine.models.task.image.ImagePersistence;
import be.cytomine.appengine.models.task.image.ImageType;
import be.cytomine.appengine.models.task.integer.IntegerPersistence;
import be.cytomine.appengine.models.task.integer.IntegerType;
import be.cytomine.appengine.models.task.number.NumberPersistence;
import be.cytomine.appengine.models.task.number.NumberType;
import be.cytomine.appengine.models.task.string.StringPersistence;
import be.cytomine.appengine.models.task.string.StringType;
import be.cytomine.appengine.models.task.wsi.WsiPersistence;
import be.cytomine.appengine.models.task.wsi.WsiType;
import be.cytomine.appengine.repositories.TypePersistenceRepository;
import be.cytomine.appengine.repositories.bool.BooleanPersistenceRepository;
import be.cytomine.appengine.repositories.enumeration.EnumerationPersistenceRepository;
import be.cytomine.appengine.repositories.file.FilePersistenceRepository;
import be.cytomine.appengine.repositories.geometry.GeometryPersistenceRepository;
import be.cytomine.appengine.repositories.image.ImagePersistenceRepository;
import be.cytomine.appengine.repositories.integer.IntegerPersistenceRepository;
import be.cytomine.appengine.repositories.number.NumberPersistenceRepository;
import be.cytomine.appengine.repositories.string.StringPersistenceRepository;
import be.cytomine.appengine.repositories.wsi.WsiPersistenceRepository;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.states.TaskRunState;
import be.cytomine.appengine.utils.ApiClient;
import be.cytomine.appengine.utils.FileHelper;
import be.cytomine.appengine.utils.TaskTestsUtils;
import be.cytomine.appengine.utils.TestTaskBuilder;

@ContextConfiguration(classes = AppEngineApplication.class, loader = SpringBootContextLoader.class)
public class ProvisionTaskStepDefinitions {

    @LocalServerPort
    private String port;

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private RunRepository taskRunRepository;

    @Autowired
    private StorageHandler storageHandler;

    @Autowired
    private TaskRepository taskRepository;

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
    private WsiPersistenceRepository wsiPersistenceRepository;

    @Autowired
    private FilePersistenceRepository filePersistenceRepository;

    @Autowired
    private TypePersistenceRepository typePersistenceRepository;

    @Value("${app-engine.api_prefix}")
    private String apiPrefix;

    @Value("${app-engine.api_version}")
    private String apiVersion;

    private RestClientResponseException persistedException;

    private Run persistedRun;

    private Task persistedTask;
    @Autowired
    private CollectionPersistenceRepository collectionPersistenceRepository;

    @Before
    public void setUp() {
        apiClient.setBaseUrl("http://localhost:" + port + apiPrefix + apiVersion);
        apiClient.setPort(port);
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
        persistedTask = taskRepository.saveAndFlush(persistedTask);
    }

    @Given("this task has at least one input parameter")
    public void this_task_has_at_least_one_input_parameter() {
        Set<Parameter> outputs = persistedTask
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.OUTPUT))
            .collect(Collectors.toSet());
        Assertions.assertFalse(outputs.isEmpty());
    }

    @When("user calls the endpoint {string} with HTTP method POST")
    public void user_calls_the_endpoint_excluding_version_prefix_e_g_with_http_method_post(String endpoint) {
        try {
            switch (endpoint) {
                case "/task/namespace/version/runs":
                    apiClient.createTaskRun(persistedTask.getNamespace(), persistedTask.getVersion());
                    break;
                case "/task/id/runs":
                    apiClient.createTaskRun(persistedTask.getIdentifier().toString());
                    break;
                default:
                    throw new RuntimeException("Unknown endpoint: " + endpoint);
            }
        } catch (RestClientResponseException e) {
            persistedException = e;
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
        boolean storageExists = storageHandler.checkStorageExists(template + "inputs-" + persistedRun.getId().toString());
        Assertions.assertTrue(storageExists);

        storageExists = storageHandler.checkStorageExists(template + "outputs-" + persistedRun.getId().toString());
        Assertions.assertTrue(storageExists);
    }

    @Then("the App Engine returns a {string} HTTP response with the updated task run information as JSON payload")
    public void the_app_engine_returns_a_http_response_with_the_updated_task_run_information_as_json_payload(String string) {
        Assertions.assertNull(persistedException);
    }

    // ONE INPUT PARAMETER TESTS

    @Given("this task has only one input parameter {string} of type {string}")
    public void this_task_has_only_one_input_parameter_of_type(String paramName, String type) {
        persistedTask.getParameters().removeIf(input -> !(input.getType().getId().equals(type) && input.getName().equals(paramName) && input.getParameterType().equals(ParameterType.INPUT)));
        persistedTask = taskRepository.saveAndFlush(persistedTask);
        Assertions.assertEquals(1,
            persistedTask.getParameters().stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT)).toList().size());
    }

    @Given("this parameter has no validation rules")
    public void this_parameter_has_no_validation_rules() {
        // this is assumed from run initialization
    }

    @Given("a task run has been created for this task")
    public void a_task_run_has_been_created_for_this_task() throws FileStorageException {
        persistedRun = new Run(UUID.randomUUID(), TaskRunState.CREATED, null);
        persistedRun = taskRunRepository.save(persistedRun);
        persistedTask.setRuns(List.of(persistedRun));
        persistedTask = taskRepository.saveAndFlush(persistedTask);
        persistedTask.setRuns(List.of(persistedRun));
        persistedRun.setTask(persistedTask);
        persistedRun = taskRunRepository.saveAndFlush(persistedRun);
        Storage runStorage = new Storage("task-run-inputs-" + persistedRun.getId().toString());
        storageHandler.createStorage(runStorage);
        runStorage = new Storage("task-run-outputs-" + persistedRun.getId().toString());
        storageHandler.createStorage(runStorage);
    }

    @Given("a task run has been created and provisioned with parameter {string} value {string} for this task")
    public void a_task_run_has_been_created_for_this_task(String parameterName, String initialValue) throws FileStorageException {
        persistedRun = new Run(UUID.randomUUID(), TaskRunState.CREATED, null);

        Parameter input = persistedTask
            .getParameters()
            .stream()
            .filter(i -> i.getName().equals(parameterName) && i.getParameterType().equals(ParameterType.INPUT))
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
            case "WsiType":
                provision = new WsiPersistence();
                provision.setValueType(ValueType.WSI);
                ((WsiPersistence) provision).setValue(initialValue.getBytes());
                break;
            case "FileType":
                provision = new FilePersistence();
                provision.setValueType(ValueType.FILE);
                ((FilePersistence) provision).setValue(initialValue.getBytes());
                break;
            default:
                throw new RuntimeException("Unknown type: " + input.getType().getId());
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
        persistedTask.setRuns(List.of(persistedRun));
        persistedTask = taskRepository.saveAndFlush(persistedTask);
        persistedTask.setRuns(List.of(persistedRun));
        persistedRun.setTask(persistedTask);
        persistedRun = taskRunRepository.saveAndFlush(persistedRun);

        Storage runStorage = new Storage("task-run-inputs-" + persistedRun.getId().toString());
        storageHandler.createStorage(runStorage);
    }

    @Given("this task run has not been provisioned yet and is therefore in state {string}")
    public void this_task_run_has_not_been_provisioned_yet_and_is_therefore_in_state(String state) {
        typePersistenceRepository.deleteAll();
        List<TypePersistence> provisions = typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(persistedRun.getId() , ParameterType.INPUT);
        Assertions.assertTrue(provisions.isEmpty());
    }

    @When("a user calls the provisioning endpoint with {string} to provision parameter {string} with {string} of type {string}")
    public void a_user_calls_the_batch_provisioning_endpoint_put_task_runs_input_provisions_with_json_to_provision_parameter_my_input_with(String payload, String parameterName, String value, String type) {
        Parameter input = persistedTask
            .getParameters()
            .stream()
            .filter(i -> i.getName().equals(parameterName) && i.getParameterType().equals(ParameterType.INPUT))
            .findFirst()
            .orElse(null);

        try {
            apiClient.provisionInput(persistedRun.getId().toString(), parameterName, input == null ? "" : type, value);
        } catch (RestClientResponseException e) {
            persistedException = e;
        } catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Then("the value {string} is saved and associated parameter {string} in the database")
    public void the_value_is_saved_and_associated_parameter_in_the_database(String value, String parameterName) {
        Parameter input = persistedTask
            .getParameters()
            .stream()
            .filter(i -> i.getName().equals(parameterName) && i.getParameterType().equals(ParameterType.INPUT))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(input);

        TypePersistence provision = null;
        switch (input.getType().getClass().getSimpleName()) {
            case "BooleanType":
                provision = booleanProvisionRepository.findBooleanPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            case "IntegerType":
                provision = integerProvisionRepository.findIntegerPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            case "NumberType":
                provision = numberProvisionRepository.findNumberPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            case "StringType":
                provision = stringProvisionRepository.findStringPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            case "EnumerationType":
                provision = enumerationProvisionRepository.findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            case "GeometryType":
                provision = geometryProvisionRepository.findGeometryPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            case "ImageType":
                provision = imageProvisionRepository.findImagePersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            case "WsiType":
                provision = wsiPersistenceRepository.findWsiPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            case "FileType":
                provision = filePersistenceRepository.findFilePersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            case "CollectionType":
                provision = collectionPersistenceRepository.findCollectionPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            default:
                throw new RuntimeException("Unknown type: " + input.getType().getId());
        }

        Assertions.assertNotNull(provision);
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));
    }

    @Then("a input file named {string} is created in the task run storage {string}+UUID with content {string}")
    public void a_input_file_named_is_created_in_the_task_run_storage_with_content(String fileName, String template, String content) throws FileStorageException {
        StorageData descriptorMetaData = new StorageData(fileName, template + "inputs-" + persistedRun.getId().toString());
        StorageData descriptor = storageHandler.readStorageData(descriptorMetaData);
        Assertions.assertNotNull(descriptor);
        if (content.equalsIgnoreCase("directory")){
            Assertions.assertEquals(StorageDataType.DIRECTORY,
                descriptor.peek().getStorageDataType());
            Assertions.assertEquals(4, descriptor.getEntryList().size());
        } else {
            String fileContent = FileHelper.read(descriptor.peek().getData(), StandardCharsets.UTF_8);
            Assertions.assertTrue(fileContent.equalsIgnoreCase(content));
        }

    }

    @Then("the task run states changes to {string} because the task is now completely provisioned")
    public void the_task_run_states_changes_to_because_the_task_is_now_completely_provisioned(String state) {
        Optional<Run> optionalRun = taskRunRepository.findById(persistedRun.getId());
        optionalRun.ifPresent(value -> persistedRun = value);
        Assertions.assertEquals(state,persistedRun.getState().toString());
    }

    // PROVISIONING OF TWO PARAMETERS

    @Given("this task has two input parameters")
    public void this_task_has_two_input_parameters() {
        // this will be covered by the definition of the parameters in the next step test
    }

    @Given("the first parameter is {string} of type {string} without a validation rule")
    public void the_first_parameter_is_of_type_without_a_validation_rule(String parameterName, String type) {
        Assertions.assertTrue(persistedTask.getParameters().stream()
          .anyMatch(input -> input.getType().getId().equals(type) && input.getName().equals(parameterName) && input.getParameterType().equals(ParameterType.INPUT)));
    }

    @Given("the second parameter is {string} of type {string} without a validation rule")
    public void the_second_parameter_is_of_type_without_a_validation_rule(String parameterName, String type) {
        Assertions.assertTrue(persistedTask.getParameters().stream()
          .anyMatch(input -> input.getType().getId().equals(type) && input.getName().equals(parameterName) && input.getParameterType().equals(ParameterType.INPUT)));
    }

    @Given("no validation rules are defined for these parameters")
    public void no_validation_rules_are_defined_for_these_parameters() {
        // no validation rules were added in the previous step
    }

    @When("a user calls the endpoint with JSON {string}")
    public void a_user_calls_the_endpoint_with_json(String jsonPayload) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode payload = mapper.readTree(jsonPayload);

        List<ObjectNode> provisions = new ArrayList<>();
        for (JsonNode parameter : payload) {
            String parameterName = parameter.get("param_name").textValue();
            Parameter input = persistedTask
                .getParameters()
                .stream()
                .filter(i -> i.getName().equals(parameterName) && i.getParameterType().equals(ParameterType.INPUT))
                .findFirst()
                .orElse(null);

            GenericParameterProvision provision = TaskTestsUtils.createProvision(
                parameterName,
                input == null ? "" : input.getType().getId(),
                parameter.get("value").asText()
            );
            provisions.add(mapper.valueToTree(provision));
        }

        try {
            apiClient.provisionMultipleInputs(persistedRun.getId().toString(), provisions);
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
        Optional<Parameter> parameterOptional = persistedTask
            .getParameters()
            .stream()
            .filter(
            input -> ((IntegerType)input.getType()).getId().equals(type)
                && input.getName().equals(parameterName)
                && input.getParameterType().equals(ParameterType.INPUT))
            .findFirst();

        Assertions.assertTrue(parameterOptional.isPresent());

        // adding constraint to the existing unconstrainted parameter
        Parameter parameter = parameterOptional.get();
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
        Set<Parameter> inputs = persistedTask.getParameters().stream().filter(input -> input.getParameterType().equals(ParameterType.INPUT)).collect(Collectors.toSet());
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
    public void the_file_named_in_the_task_run_storage_has_content(String filename, String template, String content) throws FileStorageException {
        StorageData parameterFile = new StorageData(
            FileHelper.write(filename, content.getBytes(StandardCharsets.UTF_8)),
            filename
        );
        Storage storage = new Storage(template + "inputs-" + persistedRun.getId().toString());
        storageHandler.saveStorageData(storage, parameterFile);
    }

    @Given("this task has at least one input parameter {string} of type {string}")
    public void this_task_has_at_least_one_input_parameter_of_type(String paramName, String type) {
        // Check if the set contains an object matching the conditions
        // Will raise an error if the persisted task is not valid with the test
        boolean hasInput = persistedTask
            .getParameters()
            .stream()
            .anyMatch(input -> input.getType().getId().equals(type) && input.getName().equals(paramName) && input.getParameterType().equals(ParameterType.INPUT));

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
        Parameter input = persistedTask
            .getParameters()
            .stream()
            .filter(i -> i.getName().equals(parameterName) && i.getParameterType().equals(ParameterType.INPUT))
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
            case "WsiType":
                provision = wsiPersistenceRepository.findWsiPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
            case "FileType":
                provision = filePersistenceRepository.findFilePersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);
                break;
        }

        Assertions.assertNotNull(provision);
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));
    }

    @Then("the input file named {string} is updated in the task run storage {string}+UUID with content {string}")
    public void the_input_file_named_is_updated_in_the_task_run_storage_with_content(String filename, String template, String content) throws FileStorageException {
        StorageData descriptorMetaData = new StorageData(filename, template + "inputs-" + persistedRun.getId().toString());
        StorageData descriptor = storageHandler.readStorageData(descriptorMetaData);
        Assertions.assertNotNull(descriptor);

        String fileContent = FileHelper.read(descriptor.peek().getData(), StandardCharsets.UTF_8);
        Assertions.assertTrue(fileContent.equalsIgnoreCase(content));
    }

    @When("a user calls the provisioning endpoint with {string} to provision collection {string} with {string} of type {string} in {int}")
    public void aUserCallsTheProvisioningEndpointWithToProvisionCollectionWithOfType(String payload, String parameterName, String value, String type, int index)
        throws JsonProcessingException
    {

        Parameter input = persistedTask
            .getParameters()
            .stream()
            .filter(i -> i.getName().equals(parameterName) && i.getParameterType().equals(ParameterType.INPUT))
            .findFirst()
            .orElse(null);

        try {
            apiClient.provisionInputPart(persistedRun.getId().toString(), parameterName, input == null ? "" : type, value, index);
        } catch (RestClientResponseException e) {
            persistedException = e;
        }

    }

    @Then("the value {string} is saved and associated collection {string} in the database")
    public void theValueIsSavedAndAssociatedCollectionInTheDatabase(String value, String parameterName)
    {

        Parameter input = persistedTask
            .getParameters()
            .stream()
            .filter(i -> i.getName().equals(parameterName) && i.getParameterType().equals(ParameterType.INPUT))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(input);

        CollectionPersistence provision = collectionPersistenceRepository.findCollectionPersistenceByParameterNameAndRunIdAndParameterType(parameterName, persistedRun.getId(), ParameterType.INPUT);

        Assertions.assertNotNull(provision);
        Assertions.assertEquals(1, provision.getItems().size());
        Assertions.assertTrue(provision.getParameterName().equalsIgnoreCase(parameterName));

    }

    @And("a input file named {string} is created in the task run storage {string}+UUID within {string}")
    public void aInputFileNamedIsCreatedInTheTaskRunStorageUUIDWithinWithContent(String index, String template, String parameterName)
        throws FileStorageException
    {
        StorageData descriptorMetaData = new StorageData(parameterName, template + "inputs-" + persistedRun.getId().toString());
        StorageData descriptor = storageHandler.readStorageData(descriptorMetaData);
        Assertions.assertNotNull(descriptor);
        Assertions.assertEquals(StorageDataType.DIRECTORY, descriptor.peek().getStorageDataType());
        Assertions.assertEquals(3, descriptor.getEntryList().size());
        Assertions.assertTrue(descriptor.getEntryList().stream().anyMatch(entry -> entry.getName().equalsIgnoreCase(parameterName+"/"+index)));

    }


    // Constraint validations

    @Given("the parameter {string} has validation rules")
    public void parameterHasValidationRules(String parameterName) {
        Parameter input = persistedTask
            .getParameters()
            .stream()
            .filter(i -> i.getName().equals(parameterName) && i.getParameterType().equals(ParameterType.INPUT))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(input);

        Map<String, Function<Type, Boolean>> typeConstraintCheckers = Map.of(
            "integer", t -> Arrays.stream(IntegerTypeConstraint.values()).anyMatch(((IntegerType) t)::hasConstraint),
            "number", t -> Arrays.stream(NumberTypeConstraint.values()).anyMatch(((NumberType) t)::hasConstraint),
            "string", t -> Arrays.stream(StringTypeConstraint.values()).anyMatch(((StringType) t)::hasConstraint),
            "image", t -> Arrays.stream(ImageTypeConstraint.values()).anyMatch(((ImageType) t)::hasConstraint),
            "wsi", t -> Arrays.stream(WsiTypeConstraint.values()).anyMatch(((WsiType) t)::hasConstraint)
        );

        Type type = input.getType();
        Boolean hasValidationRules = typeConstraintCheckers.getOrDefault(type.getId(), t -> {
            throw new RuntimeException("Unknown type: " + t.getId());
        }).apply(type);

        Assertions.assertTrue(hasValidationRules);
    }

    // Multiple different types

    @When("a user calls the provisioning endpoint for provisioning all the parameters")
    public void provisionTaskParameters(DataTable table) {
        List<Map<String, String>> parameters = table.asMaps(String.class, String.class);

        for (Map<String, String> param : parameters) {
            String name = param.get("parameter_name");
            String type = param.get("parameter_type");
            String value = param.get("parameter_value");

            try {
                apiClient.provisionInput(persistedRun.getId().toString(), name, type, value);
            } catch (RestClientResponseException e) {
                Assertions.fail("Provisioning '" + name + "' failed: " + e.getMessage());
            } catch (JsonProcessingException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
