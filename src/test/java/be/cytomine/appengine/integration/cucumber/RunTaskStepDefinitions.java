package be.cytomine.appengine.integration.cucumber;

import be.cytomine.appengine.AppEngineApplication;
import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerValue;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.exceptions.SchedulingException;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.handlers.FileStorageHandler;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.models.task.integer.IntegerPersistence;
import be.cytomine.appengine.openapi.api.DefaultApi;
import be.cytomine.appengine.openapi.invoker.ApiException;
import be.cytomine.appengine.openapi.model.*;
import be.cytomine.appengine.repositories.integer.IntegerPersistenceRepository;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.services.RunService;
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
import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ContextConfiguration(classes = AppEngineApplication.class, loader = SpringBootContextLoader.class)
public class RunTaskStepDefinitions {

    @LocalServerPort
    private String port;

    @Autowired
    private DefaultApi appEngineApi;

    @Autowired
    private FileStorageHandler fileStorageHandler;

    @Autowired
    private IntegerPersistenceRepository integerPersistenceRepository;

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private RunService runService;

    @Autowired
    private SchedulerHandler schedulerHandler;

    @Autowired
    private TaskRepository taskRepository;

    @Value("${app-engine.api_prefix}")
    private String apiPrefix;

    @Value("${app-engine.api_version}")
    private String apiVersion;

    @Value("${storage.input.charset}")
    private String charset;

    private ApiException exception;

    private RestClientResponseException persistedException;

    private Run persistedRun;
    private TaskRun persistedTaskRun;
    private TaskRunStateActionSuccess persistedResponse;
    private List<TaskRunParameterValue> outputs;

    private File inputsArchive;
    private File outputsArchive;
    private File persistedZipFile;
    private FileData param1FileData;
    private FileData param2FileData;
    private FileData outputFileData;

    @NotNull
    private static String removeWhitespacesFromPath(File file) {
        String absolutePath = file.getAbsolutePath();
        // this is just because when there's whitespace in the path it can't be resolved correctly
        boolean pathContainsWhitespace = absolutePath.contains(" ");
        String formattedPath = "";
        if (pathContainsWhitespace) {
            String[] pathComponents = absolutePath.split("/");
            for (String component : pathComponents) {
                if (component.contains(" "))
                    component = "'" + component + "'";
                if (component.equalsIgnoreCase(""))
                    formattedPath += component;
                else
                    formattedPath += "/" + component;
                if (component.endsWith(".zip"))
                    formattedPath = formattedPath.replace(".", "s.");
            }
        }
        return formattedPath;
    }

    private String buildAppEngineUrl() {
        return "http://localhost:" + port + apiPrefix + apiVersion;
    }

    private void createStorage(String uuid) throws FileStorageException {
        Storage inputStorage = new Storage("task-run-inputs-" + uuid);
        if (!fileStorageHandler.checkStorageExists(inputStorage)) {
            fileStorageHandler.createStorage(inputStorage);
        }

        Storage outputStorage = new Storage("task-run-outputs-" + uuid);
        if (!fileStorageHandler.checkStorageExists(outputStorage)) {
            fileStorageHandler.createStorage(outputStorage);
        }
    }

    private Charset getStorageCharset(String charset) {
        return switch (charset.toUpperCase()) {
            case "US_ASCII" -> StandardCharsets.US_ASCII;
            case "ISO_8859_1" -> StandardCharsets.ISO_8859_1;
            case "UTF_16LE" -> StandardCharsets.UTF_16LE;
            case "UTF_16BE" -> StandardCharsets.UTF_16BE;
            case "UTF_16" -> StandardCharsets.UTF_16;
            default -> StandardCharsets.UTF_8;
        };
    }

    @Given("Scheduler is up and running")
    public void scheduler_is_up_and_running() throws SchedulingException {
        schedulerHandler.alive();
    }

    @Given("a task run exists with identifier {string}")
    public void a_task_run_exists_with_identifier(String uuid) throws FileStorageException {
        runRepository.deleteAll();
        Task task = TestTaskBuilder.buildHardcodedAddInteger(UUID.fromString(uuid));
        task = taskRepository.save(task);
        persistedRun = new Run(UUID.fromString(uuid), null, task);
        createStorage(uuid);
    }

    @Given("the task run is in state {string}")
    public void the_task_run_is_in_state(String state) {
        persistedRun.setState(TaskRunState.valueOf(state));
        persistedRun = runRepository.save(persistedRun);
    }

    @When("user calls the endpoint with {string} HTTP method GET")
    public void user_calls_the_endpoint_with_http_method_get(String uuid) throws ApiException {
        persistedTaskRun = appEngineApi.getTaskRun(UUID.fromString(uuid));
    }

    @Then("App Engine sends a {string} OK response with a payload containing task run information \\(see OpenAPI spec)")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_task_run_information_see_open_api_spec(String string) {
        Assertions.assertNotNull(persistedTaskRun);
    }

    @Then("the retrieved task run information matches the expected details")
    public void the_retrieved_task_run_information_matches_the_expected_details() {
        Assertions.assertEquals(persistedTaskRun.getId(), persistedRun.getId());
    }

    @Then("the task run state remains as {string}")
    public void the_task_run_state_remains_as(String state) {
        Assertions.assertEquals(persistedTaskRun.getState(), be.cytomine.appengine.openapi.model.TaskRunState.valueOf(state));
    }

    // successful fetch of task run inputs archive in a launched task run
    @Given("the task run {string} has input parameters: {string} of type {string} with value {string} and {string} of type {string} with value {string}")
    public void the_task_run_has_input_parameters_of_type_with_value_and_of_type_with_value(String runId, String name1, String type1, String value1, String name2, String type2, String value2) throws ApiException, FileStorageException {
        String endpointUrl = buildAppEngineUrl() + "/task-runs/" + persistedRun.getId() + "/input-provisions";

        ObjectMapper mapper = new ObjectMapper();
        List<ObjectNode> provisions = new ArrayList<>();
        provisions.add(mapper.valueToTree(TaskTestsUtils.createProvision(name1, type1, value1)));
        provisions.add(mapper.valueToTree(TaskTestsUtils.createProvision(name2, type2, value2)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<ObjectNode>> entity = new HttpEntity<>(provisions, headers);

        new RestTemplate().exchange(endpointUrl, HttpMethod.PUT, entity, JsonNode.class);

        // save inputs in storage
        Storage storage = new Storage("task-run-inputs-" + runId);
        fileStorageHandler.createStorage(storage);
        FileData parameterFile = new FileData(value1.getBytes(StandardCharsets.UTF_8), name1); // UTF_8 is assumed in tests
        fileStorageHandler.createFile(storage, parameterFile);
        parameterFile = new FileData(value2.getBytes(StandardCharsets.UTF_8), name2); // UTF_8 is assumed in tests
        fileStorageHandler.createFile(storage, parameterFile);
    }

    @When("user calls the endpoint to fetch inputs archive with {string} HTTP method GET")
    public void user_calls_the_endpoint_to_fetch_inputs_archive_with_http_method_get(String runId) {
        String endpointUrl = buildAppEngineUrl() + "/task-runs/" + runId + "/inputs.zip";

        try {
            ResponseEntity<byte[]> response = new RestTemplate().exchange(endpointUrl, HttpMethod.GET, null, byte[].class);
            // Write the byte array to a file
            inputsArchive = File.createTempFile("inputs", ".zip");
            try (FileOutputStream fos = new FileOutputStream(inputsArchive)) {
                fos.write(response.getBody());
            }
        } catch (RestClientResponseException e) {
            persistedException = e;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @When("user calls the endpoint to fetch outputs archive with {string} HTTP method GET")
    public void user_calls_the_endpoint_to_fetch_outputs_archive_with_http_method_get(String runId) {
        String endpointUrl = buildAppEngineUrl() + "/task-runs/" + runId + "/outputs.zip";

        try {
            ResponseEntity<File> response = new RestTemplate().exchange(endpointUrl, HttpMethod.GET, null, new ParameterizedTypeReference<File>() {});
            outputsArchive = response.getBody();
        } catch (RestClientResponseException e) {
            persistedException = e;
        }
    }

    @Then("App Engine sends a {string} response with a payload containing the inputs archive")
    public void app_engine_sends_a_response_with_a_payload_containing_the_inputs_archive(String string) {
        Assertions.assertNotNull(inputsArchive);
    }

    @Then("the archive contains files named {string} and {string}")
    public void the_archive_contains_files_named_and(String param1, String param2) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(inputsArchive))) {
            boolean param1Found = false, param2Found = false;
            ZipEntry file;
            while ((file = zis.getNextEntry()) != null) {
                if (file.getName().equalsIgnoreCase(param1)) {
                    param1Found = true;
                    param1FileData = new FileData(zis.readAllBytes(), file.getName());
                }
                if (file.getName().equalsIgnoreCase(param2)) {
                    param2Found = true;
                    param2FileData = new FileData(zis.readAllBytes(), file.getName());
                }
            }
            Assertions.assertTrue(param1Found);
            Assertions.assertTrue(param2Found);
        }
    }

    @Then("the content of file {string} is {string}")
    public void the_content_of_file_is(String paramName, String paramValue) {
        int fileValue;
        int testValue;
        if (paramName.equalsIgnoreCase("a")) {
            testValue = Integer.parseInt(paramValue);
            fileValue = Integer.parseInt(new String(param1FileData.getFileData()));
            Assertions.assertEquals(fileValue, testValue);
        }
        if (paramName.equalsIgnoreCase("b")) {
            testValue = Integer.parseInt(paramValue);
            fileValue = Integer.parseInt(new String(param2FileData.getFileData()));
            Assertions.assertEquals(fileValue, testValue);
        }
    }

    // unsuccessful fetch of task run inputs archive in a created task run
    @Then("App Engine sends a {string} forbidden response with a payload containing the error message \\(see OpenAPI spec) and code {string}")
    public void app_engine_sends_a_forbidden_response_with_a_payload_containing_the_error_message_see_open_api_spec_and_code(String ResponseCode, String errorCode) throws JsonProcessingException {
        Assertions.assertEquals(Integer.parseInt(ResponseCode), persistedException.getStatusCode().value());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorJsonNodeFromServer = mapper.readTree(persistedException.getResponseBodyAsString());
        Assertions.assertEquals(errorCode, errorJsonNodeFromServer.get("error_code").textValue());
    }

    // successful fetch of task run outputs archive in a finished task run
    @Given("the task run {string} has output parameters: {string} of type {string} with value {int}")
    public void the_task_run_has_output_parameters_of_type_with_value_and_of_type_with_value(String runId, String name, String type, Integer value) throws FileStorageException, IOException, ApiException {
        // Outputs
        integerPersistenceRepository.deleteAll();
        IntegerPersistence result = new IntegerPersistence();
        // name, String.valueOf(value), persistedRun.getId()
        result.setParameterName(name);
        result.setParameterType(ParameterType.OUTPUT);
        result.setRunId(persistedRun.getId());
        result.setValue(value);
        result.setValueType(ValueType.INTEGER);
        result = integerPersistenceRepository.save(result);
        Assertions.assertNotNull(result);

        if (runId.startsWith("0000")) {
            Storage storage = new Storage("task-run-outputs-" + runId);
            fileStorageHandler.createStorage(storage);
            Storage outputsStorage = new Storage("task-run-outputs-" + persistedRun.getId());
            String valueString = String.valueOf(value);
            byte[] inputFileData = valueString.getBytes(getStorageCharset(charset));
            FileData outputFileData = new FileData(inputFileData, name);

            fileStorageHandler.createFile(outputsStorage, outputFileData);
        }
    }

    @When("user calls the endpoint to fetch with {string} HTTP method GET")
    public void user_calls_the_endpoint_to_fetch_with_http_method_get(String runId) {
        try {
            outputsArchive = appEngineApi.getTaskRunOutputsInArchive(UUID.fromString(runId));
        } catch (ApiException e) {
            e.printStackTrace();
            exception = e;
        }
    }

    @Then("App Engine sends a {string} response with a payload containing the outputs archive")
    public void app_engine_sends_a_response_with_a_payload_containing_the_outputs_archive(String string) {
        Assertions.assertNotNull(outputsArchive);
    }

    @Then("the archive contains files named {string}")
    public void the_archive_contains_files_named(String outputName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(outputsArchive))) {
            boolean outputFound = false;
            ZipEntry file;
            while ((file = zis.getNextEntry()) != null) {
                if (file.getName().equalsIgnoreCase(outputName)) {
                    outputFound = true;
                    outputFileData = new FileData(zis.readAllBytes(), file.getName());
                }

            }
            Assertions.assertTrue(outputFound);
        }
    }

    @Then("the content of output file {string} is {string}")
    public void the_content_of_output_file_is(String outputName, String outputValue) {
        int fileValue = Integer.parseInt(new String(outputFileData.getFileData()));
        int testValue = Integer.parseInt(outputValue);

        Assertions.assertEquals(fileValue, testValue);
    }

    // successful fetch of task run outputs in JSON format for a finished task run

    @When("user calls the endpoint to fetch outputs json with {string} HTTP method GET")
    public void user_calls_the_endpoint_to_fetch_outputs_json_with_http_method_get(String runId) {
        String endpointUrl = buildAppEngineUrl() + "/task-runs/" + runId + "/outputs";

        try {
            ResponseEntity<String> response = new RestTemplate().getForEntity(endpointUrl, String.class);
            outputs = TaskTestsUtils.convertTo(response.getBody());
        } catch (RestClientResponseException e) {
            persistedException = e;
        }
    }

    @Then("App Engine sends a {string} response with a payload containing task run outputs in JSON format")
    public void app_engine_sends_a_response_with_a_payload_containing_task_run_outputs_in_json_format(String string) {
        Assertions.assertFalse(outputs.isEmpty());
    }

    @Then("the payload contains the output {string} and their corresponding value {int}")
    public void the_payload_contains_the_output_and_their_corresponding_value(String output, Integer value) {
        IntegerValue outputParameter = (IntegerValue) outputs.get(0);
        Assertions.assertEquals(outputParameter.getParameterName(), output);
        Assertions.assertEquals(outputParameter.getValue(), value);
    }

    // unsuccessful run request of a task which has not been provisioned
    @Given("a task run {string} has successfully been created for a task")
    public void a_task_run_has_successfully_been_created_for_a_task(String runId) {
        runRepository.deleteAll();
        taskRepository.deleteAll();
        Task task = TestTaskBuilder.buildHardcodedAddInteger();
        task = taskRepository.save(task);
        persistedRun = new Run(UUID.fromString(runId), null, task);
    }

    @Given("this task run has not been successfully provisioned yet and is therefore in state {string}")
    public void this_task_run_has_not_been_successfully_provisioned_yet_and_is_therefore_in_state(String state) {
        persistedRun.setState(TaskRunState.valueOf(state));
        persistedRun = runRepository.save(persistedRun);
    }

    @When("When user calls the endpoint to run task with HTTP method POST")
    public void when_user_calls_the_endpoint_to_run_task_with_http_method_post() {
        TaskRunStateAction taskRunStateAction = new TaskRunStateAction();
        taskRunStateAction.desired(new TaskRunStateActionAllOfDesired("RUNNING"));
        try {
            persistedResponse = appEngineApi.performStateActionAgainstTaskRun(persistedRun.getId(), taskRunStateAction);
        } catch (ApiException e) {
            e.printStackTrace();
            exception = e;
        }
    }

    @Then("App Engine sends a {string} Forbidden response with a payload containing the error message \\(see OpenAPI spec) and code {string}")
    public void app_engine_sends_a_response_with_a_payload_containing_the_error_message_see_open_api_spec_and_code(String ResponseCode, String errorCode) throws JsonProcessingException {
        Assertions.assertEquals(Integer.parseInt(ResponseCode), exception.getCode());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorJsonNodeFromServer = mapper.readTree(exception.getResponseBody());
        Assertions.assertEquals(errorCode, errorJsonNodeFromServer.get("error_code").textValue());
    }

    @Then("this task run remains in state {string}")
    public void this_task_run_remains_in_state(String state) {
        Optional<Run> runOptional = runRepository.findById(persistedRun.getId());
        runOptional.ifPresent(value -> persistedRun = value);
        Assertions.assertNotNull(persistedRun);
        Assertions.assertEquals(persistedRun.getState(), TaskRunState.CREATED);
    }

    @Then("App Engine does not initiate the process of executing this task run")
    public void app_engine_does_not_initiate_the_process_of_executing_this_task_run() {
        // TODO : How?
    }

    // unsuccessful run request for a task that was already launched

    @Given("App Engine has already received a run request for this task run which is therefore not in state {string}")
    public void app_engine_has_already_received_a_run_request_for_this_task_run_which_is_therefore_not_in_state(String excludedStates) {
        persistedRun.setState(TaskRunState.RUNNING);
        runRepository.saveAndFlush(persistedRun);
    }

    @Then("this task run state progress is not affected by the request")
    public void this_task_run_state_progress_is_not_affected_by_the_request() {
        Optional<Run> oprionalRun = runRepository.findById(persistedRun.getId());
        Assertions.assertEquals(TaskRunState.RUNNING, oprionalRun.get().getState());
    }

    @Then("App Engine does not re-initiate the process of executing this task run")
    public void app_engine_does_not_re_initiate_the_process_of_executing_this_task_run() {
        // TODO : How?
    }

    // unsuccessful upload of task run outputs as an invalid zip file in running state
    @Given("the task run is in state {string} or {string}")
    public void the_task_run_is_in_state_or(String state1, String state2) {
        persistedRun.setState(TaskRunState.RUNNING);
        persistedRun = runService.update(persistedRun);
        Assertions.assertEquals(persistedRun.getState(), TaskRunState.valueOf(state1));
    }

    @Given("the task run has an output parameter {string}")
    public void the_task_run_has_an_output_parameter(String output) {
        Set<Output> outputs = persistedRun.getTask().getOutputs();
        boolean found = false;
        for (Output runOutput : outputs) {
            if (runOutput.getName().equalsIgnoreCase(output)) {
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found);
    }

    @Given("a zip file is used which does not contain a file named {string}")
    public void a_zip_file_is_used_which_does_not_contain_a_file_named(String output) throws IOException {
        ClassPathResource invalidOutputArchiveResource = new ClassPathResource("/artifacts/invalid_output.zip");
        Assertions.assertNotNull(invalidOutputArchiveResource);
        boolean found = false;
        persistedZipFile = invalidOutputArchiveResource.getFile();
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(persistedZipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().equalsIgnoreCase(output)) {
                    found = true;
                    break;
                }
            }
        }
        Assertions.assertFalse(found);
    }

    @When("user calls the endpoint to post outputs with {string} HTTP method POST and the zip file as a binary payload")
    public void user_calls_the_endpoint_to_post_outputs_with_http_method_post_and_the_zip_file_as_a_binary_payload(String runId) {
        try {
            appEngineApi.uploadTaskRunOutputs(UUID.fromString(runId), persistedZipFile);
        } catch (ApiException e) {
            e.printStackTrace();
            exception = e;
        }
    }

    @Then("App Engine sends a {string} Bad Request response with a payload containing the error message \\(see OpenAPI spec) and code {string}")
    public void app_engine_sends_a_bad_request_response_with_a_payload_containing_the_error_message_see_open_api_spec_and_code(String responseCode, String errorCode) throws JsonProcessingException {
        Assertions.assertEquals(Integer.parseInt(responseCode), exception.getCode());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorJsonNodeFromServer = mapper.readTree(exception.getResponseBody());
        Assertions.assertEquals(errorCode, errorJsonNodeFromServer.get("error_code").textValue());
    }

    // unsuccessful upload of task run outputs as a valid zip file in a non-running non-pending non-queuing non-queued state state
    @Given("the task run is not in state {string} or {string} or {string} or {string}")
    public void the_task_run_is_not_in_state_or(String state1, String state2, String state3, String state4) {
        persistedRun.setState(TaskRunState.PROVISIONED);
        persistedRun = runService.update(persistedRun);

        Assertions.assertNotEquals(persistedRun.getState(), TaskRunState.valueOf(state1));
        Assertions.assertNotEquals(persistedRun.getState(), TaskRunState.valueOf(state2));
        Assertions.assertNotEquals(persistedRun.getState(), TaskRunState.valueOf(state3));
        Assertions.assertNotEquals(persistedRun.getState(), TaskRunState.valueOf(state4));
    }

    @When("user calls the endpoint to post outputs with {string} HTTP method POST and a valid outputs zip file")
    public void user_calls_the_endpoint_to_post_outputs_with_http_method_post_and_a_valid_outputs_zip_file(String runId) throws IOException {
        ClassPathResource validOutputArchiveResource = new ClassPathResource("/artifacts/" + runId + "-sum.zip");
        Assertions.assertNotNull(validOutputArchiveResource);
        persistedZipFile = validOutputArchiveResource.getFile();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("outputs", new FileSystemResource(persistedZipFile));

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        String endpointUrl = buildAppEngineUrl() + "/task-runs/" + runId + "/outputs.zip";
        try {
            ResponseEntity<List<TaskRunParameterValue>> response = new RestTemplate().exchange(endpointUrl, HttpMethod.POST, entity, new ParameterizedTypeReference<List<TaskRunParameterValue>>() {});
            outputs = response.getBody();
        } catch (RestClientResponseException e) {
            persistedException = e;
        }
    }

    // successful upload of task run outputs as a valid zip file in running or pending state
    @Given("a valid zip file containing one file named {string} and contains {string}")
    public void a_valid_zip_file_containing_one_file_named_and_contains(String fileName, String value) {
        // assumed as already satisfied in the archive
    }

    @Then("App Engine sends a {string} OK response with a payload containing task run outputs in JSON format")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_task_run_outputs_in_json_format(String string) {
        Assertions.assertNotNull(outputs);
        Assertions.assertEquals(outputs.size(), 1);
    }

    @Then("the payload contains the output parameters and their corresponding values")
    public void the_payload_contains_the_output_parameters_and_their_corresponding_values() {
        Assertions.assertEquals(outputs.get(0).getParameterName(), "sum");
    }

    // successful run request for a provisioned task run
    @Given("this task run has been successfully provisioned and is therefore in state {string}")
    public void this_task_run_has_been_successfully_provisioned_and_is_therefore_in_state(String provisionedState) throws FileStorageException {
        // save in the database
        integerPersistenceRepository.deleteAll();
        IntegerPersistence provisionInputA = new IntegerPersistence();
        provisionInputA.setValueType(ValueType.INTEGER);
        provisionInputA.setValue(250);
        provisionInputA.setParameterName("a");
        provisionInputA.setParameterType(ParameterType.INPUT);
        provisionInputA.setRunId(persistedRun.getId());
        integerPersistenceRepository.save(provisionInputA);

        IntegerPersistence provisionInputB = new IntegerPersistence();
        provisionInputB.setValueType(ValueType.INTEGER);
        provisionInputB.setValue(250);
        provisionInputB.setParameterName("b");
        provisionInputB.setParameterType(ParameterType.INPUT);
        provisionInputB.setRunId(persistedRun.getId());
        integerPersistenceRepository.save(provisionInputB);

        // store in storage
        Storage runStorage = new Storage("task-run-inputs-" + provisionInputA.getRunId());
        fileStorageHandler.createStorage(runStorage);

        String value = String.valueOf(provisionInputA.getValue());
        byte[] inputFileData = value.getBytes(getStorageCharset(charset));
        FileData inputProvisionFileData = new FileData(inputFileData, provisionInputA.getParameterName());
        fileStorageHandler.createFile(runStorage, inputProvisionFileData);

        value = String.valueOf(provisionInputB.getValue());
        inputFileData = value.getBytes(getStorageCharset(charset));
        inputProvisionFileData = new FileData(inputFileData, provisionInputB.getParameterName());
        fileStorageHandler.createFile(runStorage, inputProvisionFileData);

        persistedRun.setState(TaskRunState.PROVISIONED);
        persistedRun = runRepository.saveAndFlush(persistedRun);
        Assertions.assertEquals(persistedRun.getState(), TaskRunState.PROVISIONED);
    }

    @Then("App Engine sends a {string} OK response with a payload containing the success message \\(see OpenAPI spec)")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_the_success_message_see_open_api_spec(String responseCode) {
        Assertions.assertNotNull(persistedResponse);
    }

    @Then("App Engine moves the task run to a state different from {string}")
    public void app_engine_moves_the_task_run_to_a_state_different_from(String states) {
        String[] stateArray = states.split(",");
        Optional<Run> runOptional = runRepository.findById(persistedRun.getId());
        runOptional.ifPresent(value -> persistedRun = value);
        Assertions.assertNotEquals(persistedRun.getState(), TaskRunState.valueOf(stateArray[0]));
        Assertions.assertNotEquals(persistedRun.getState(), TaskRunState.valueOf(stateArray[1]));
    }

    @Then("App Engine initiates the process of executing the task run")
    public void app_engine_initiates_the_process_of_executing_the_task_run() {
        // TODO : How?
    }
}
