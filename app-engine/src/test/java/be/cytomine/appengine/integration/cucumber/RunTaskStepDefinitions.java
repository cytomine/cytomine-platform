package be.cytomine.appengine.integration.cucumber;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import be.cytomine.appengine.repositories.ChecksumRepository;
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
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestClientResponseException;

import be.cytomine.appengine.AppEngineApplication;
import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.inputs.task.StateAction;
import be.cytomine.appengine.dto.inputs.task.TaskRun;
import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerValue;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.exceptions.SchedulingException;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.models.task.integer.IntegerPersistence;
import be.cytomine.appengine.repositories.integer.IntegerPersistenceRepository;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.services.RunService;
import be.cytomine.appengine.states.TaskRunState;
import be.cytomine.appengine.utils.ApiClient;
import be.cytomine.appengine.utils.ClusterClient;
import be.cytomine.appengine.utils.FileHelper;
import be.cytomine.appengine.utils.TaskTestsUtils;
import be.cytomine.appengine.utils.TestTaskBuilder;

@ContextConfiguration(classes = AppEngineApplication.class, loader = SpringBootContextLoader.class)
public class RunTaskStepDefinitions {

    @LocalServerPort
    private String port;

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ClusterClient clusterClient;

    @Autowired
    private StorageHandler storageHandler;

    @Autowired
    private IntegerPersistenceRepository integerPersistenceRepository;

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private ChecksumRepository checksumRepository;

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

    private String secret;

    private RestClientResponseException persistedException;

    private ResponseEntity<StateAction> persistedRunResponse;

    private List<TaskRunParameterValue> outputs;

    private File inputsArchive;

    private File outputsArchive;

    private File persistedZipFile;

    private Run persistedRun;

    private Task persistedTask;

    private TaskRun persistedTaskRun;

    private StorageData param1FileData;

    private StorageData param2FileData;

    private StorageData outputFileData;

    private Task task;

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

    private void createStorage(String uuid) throws FileStorageException {
        Storage inputStorage = new Storage("task-run-inputs-" + uuid);
        if (!storageHandler.checkStorageExists(inputStorage)) {
            storageHandler.createStorage(inputStorage);
        }

        Storage outputStorage = new Storage("task-run-outputs-" + uuid);
        if (!storageHandler.checkStorageExists(outputStorage)) {
            storageHandler.createStorage(outputStorage);
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

    @Before
    public void setUp() {
        apiClient.setBaseUrl("http://localhost:" + port + apiPrefix + apiVersion);
        apiClient.setPort(port);
    }

    @Given("Scheduler is up and running")
    public void scheduler_is_up_and_running() throws SchedulingException {
        schedulerHandler.alive();
    }

    @Given("a task run exists with identifier {string}")
    public void a_task_run_exists_with_identifier(String uuid) throws FileStorageException {
        runRepository.deleteAll();
        task = TestTaskBuilder.buildHardcodedAddInteger(UUID.fromString(uuid));
        secret = UUID.randomUUID().toString();
        persistedRun = new Run(UUID.fromString(uuid), null, null , secret);
        persistedRun.setTask(task);
        persistedRun = runRepository.save(persistedRun);

        createStorage(uuid);
    }

    @Given("the task run is in state {string}")
    public void the_task_run_is_in_state(String state) {
        persistedRun.setState(TaskRunState.valueOf(state));
        persistedRun = runRepository.saveAndFlush(persistedRun);
    }

    @When("user calls the endpoint with {string} HTTP method GET")
    public void user_calls_the_endpoint_with_http_method_get(String uuid) {
        persistedTaskRun = apiClient.getTaskRun(uuid);
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
        Assertions.assertEquals(persistedTaskRun.getState().toString(), state);
    }

    // successful fetch of task run inputs archive in a launched task run
    @Given("the task run {string} has input parameters: {string} of type {string} with value {string} and {string} of type {string} with value {string}")
    public void the_task_run_has_input_parameters_of_type_with_value_and_of_type_with_value(String runId, String name1, String type1, String value1, String name2, String type2, String value2) throws FileStorageException,
        JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        List<ObjectNode> provisions = new ArrayList<>();
        provisions.add(mapper.valueToTree(TaskTestsUtils.createProvision(name1, type1, value1)));
        provisions.add(mapper.valueToTree(TaskTestsUtils.createProvision(name2, type2, value2)));

        apiClient.provisionMultipleInputs(persistedRun.getId().toString(), provisions);

        // save inputs in storage
        Storage storage = new Storage("task-run-inputs-" + runId);
        storageHandler.createStorage(storage);
        StorageData parameterFile = new StorageData(
            FileHelper.write(name1, value1.getBytes(StandardCharsets.UTF_8)),
            name1
        );
        storageHandler.saveStorageData(storage, parameterFile);
        parameterFile = new StorageData(
            FileHelper.write(name2, value2.getBytes(StandardCharsets.UTF_8)),
            name2
        );
        storageHandler.saveStorageData(storage, parameterFile);
    }

    @When("user calls the endpoint to fetch inputs archive with {string} HTTP method GET")
    public void user_calls_the_endpoint_to_fetch_inputs_archive_with_http_method_get(String runId) {
        try {
            inputsArchive = apiClient.getTaskRunInputsArchive(runId);
        } catch (RestClientResponseException e) {
            persistedException = e;
        }
    }

    @When("user calls the endpoint to fetch outputs archive with {string} HTTP method GET")
    public void user_calls_the_endpoint_to_fetch_outputs_archive_with_http_method_get(String runId) {
        try {
            outputsArchive = apiClient.getTaskRunOutputsArchive(runId);
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
            boolean param1Found = false;
            boolean param2Found = false;
            ZipEntry file;

            while ((file = zis.getNextEntry()) != null) {
                if (file.getName().equalsIgnoreCase(param1)) {
                    param1Found = true;

                    File tempFile = File.createTempFile("extracted_", "_" + file.getName());
                    tempFile.deleteOnExit();
                    Files.copy(zis, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    param1FileData = new StorageData(tempFile, file.getName());
                }
                if (file.getName().equalsIgnoreCase(param2)) {
                    param2Found = true;

                    File tempFile = File.createTempFile("extracted_", "_" + file.getName());
                    tempFile.deleteOnExit();
                    Files.copy(zis, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    param2FileData = new StorageData(tempFile, file.getName());
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
            fileValue = Integer.parseInt(FileHelper.read(
                param1FileData.peek().getData(),
                StandardCharsets.UTF_8
            ));
            Assertions.assertEquals(fileValue, testValue);
        }
        if (paramName.equalsIgnoreCase("b")) {
            testValue = Integer.parseInt(paramValue);
            fileValue = Integer.parseInt(FileHelper.read(
                param2FileData.peek().getData(),
                StandardCharsets.UTF_8
            ));
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
    public void the_task_run_has_output_parameters_of_type_with_value_and_of_type_with_value(String runId, String name, String type, Integer value) throws FileStorageException, IOException {
        // Outputs
        integerPersistenceRepository.deleteAll();
        checksumRepository.deleteAll();
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
            storageHandler.createStorage(storage);
            Storage outputsStorage = new Storage("task-run-outputs-" + persistedRun.getId());
            String valueString = String.valueOf(value);
            byte[] inputFileData = valueString.getBytes(getStorageCharset(charset));
            StorageData outputFileData = new StorageData(
                FileHelper.write(name, inputFileData),
                name
            );

            storageHandler.saveStorageData(outputsStorage, outputFileData);

            // calculate CRC32
            String reference = "task-run-outputs-" + runId + "-" + name;
            Checksum crc32 = new Checksum(UUID.randomUUID(), reference, calculateFileCRC32(outputFileData.peek().getData()));
            checksumRepository.save(crc32);
        }
    }

    public long calculateFileCRC32(File file) throws IOException {
        java.util.zip.Checksum crc32 = new CRC32();
        // Use try-with-resources to ensure the input stream is closed automatically
        // BufferedInputStream is used for efficient reading in chunks
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192]; // Define a buffer size (e.g., 8KB)
            int bytesRead;

            // Read the file chunk by chunk and update the CRC32 checksum
            while ((bytesRead = is.read(buffer)) != -1) {
                crc32.update(buffer, 0, bytesRead);
            }
        }
        return crc32.getValue(); // Return the final CRC32 value
    }

    @When("user calls the endpoint to fetch with {string} HTTP method GET")
    public void user_calls_the_endpoint_to_fetch_with_http_method_get(String runId) {
        try {
            outputsArchive = apiClient.getTaskRunOutputsArchive(runId);
        } catch (RestClientResponseException e) {
            persistedException = e;
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

                    File tempFile = File.createTempFile("extracted_", "_" + file.getName());
                    tempFile.deleteOnExit();
                    Files.copy(zis, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    outputFileData = new StorageData(tempFile, file.getName());
                }
            }

            Assertions.assertTrue(outputFound);
        }
    }

    @Then("the content of output file {string} is {string}")
    public void the_content_of_output_file_is(String outputName, String outputValue) {
        String value = FileHelper.read(outputFileData.peek().getData(), StandardCharsets.UTF_8);
        int fileValue = Integer.parseInt(value);
        int testValue = Integer.parseInt(outputValue);

        Assertions.assertEquals(fileValue, testValue);
    }

    // successful fetch of task run outputs in JSON format for a finished task run

    @When("user calls the endpoint to fetch outputs json with {string} HTTP method GET")
    public void user_calls_the_endpoint_to_fetch_outputs_json_with_http_method_get(String runId) {
        try {
            outputs = apiClient.getTaskRunOutputs(runId);
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
        persistedRun = new Run(UUID.fromString(runId), null, null);
        persistedRun.setTask(task);
        persistedRun = runRepository.save(persistedRun);
        taskRepository.save(task);
        persistedRun = runRepository.findById(persistedRun.getId()).get();

    }

    @Given("this task run has not been successfully provisioned yet and is therefore in state {string}")
    public void this_task_run_has_not_been_successfully_provisioned_yet_and_is_therefore_in_state(String state) {
        persistedRun.setState(TaskRunState.valueOf(state));
        persistedRun = runRepository.saveAndFlush(persistedRun);
    }

    @When("When user calls the endpoint to run task with HTTP method POST")
    public void when_user_calls_the_endpoint_to_run_task_with_http_method_post() {
        try {
            persistedRunResponse = apiClient.updateState(persistedRun.getId().toString(), TaskRunState.RUNNING);
        } catch (RestClientResponseException e) {
            persistedException = e;
        }
    }

    @Then("App Engine sends a {string} Forbidden response with a payload containing the error message \\(see OpenAPI spec) and code {string}")
    public void app_engine_sends_a_response_with_a_payload_containing_the_error_message_see_open_api_spec_and_code(String ResponseCode, String errorCode) throws JsonProcessingException {
        Assertions.assertEquals(Integer.parseInt(ResponseCode), persistedException.getStatusCode().value());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorJsonNodeFromServer = mapper.readTree(persistedException.getResponseBodyAsString());
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
        Set<Parameter> outputs = persistedRun
            .getTask()
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.OUTPUT))
            .collect(Collectors.toSet());
        boolean found = false;
        for (Parameter runOutput : outputs) {
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
            outputs = apiClient.postTaskRunOutputsArchive(runId, secret, persistedZipFile);
        } catch (RestClientResponseException e) {
            persistedException = e;
        }
    }

    @Then("App Engine sends a {string} Bad Request response with a payload containing the error message \\(see OpenAPI spec) and code {string}")
    public void app_engine_sends_a_bad_request_response_with_a_payload_containing_the_error_message_see_open_api_spec_and_code(String responseCode, String errorCode) throws JsonProcessingException {
        Assertions.assertTrue(persistedException.getStatusCode().is4xxClientError());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorJsonNodeFromServer = mapper.readTree(persistedException.getResponseBodyAsString());
        Assertions.assertEquals(errorCode, errorJsonNodeFromServer.get("error_code").textValue());
    }

    // unsuccessful upload of task run outputs as a valid zip file in a non-running non-pending non-queuing non-queued state state
    @Given("the task run is not in one of the following state")
    public void checkTaskRunNotInState(DataTable table) {
        List<String> allowedStates = table.asList(String.class);

        persistedRun.setState(TaskRunState.PROVISIONED);
        persistedRun = runService.update(persistedRun);

        Assertions.assertTrue(!allowedStates.contains(persistedRun.getState().toString()));
    }

    @When("user calls the endpoint to post outputs with {string} HTTP method POST and a valid outputs zip file")
    public void user_calls_the_endpoint_to_post_outputs_with_http_method_post_and_a_valid_outputs_zip_file(String runId) throws IOException {
        ClassPathResource validOutputArchiveResource = new ClassPathResource("/artifacts/" + runId + "-sum.zip");
        Assertions.assertNotNull(validOutputArchiveResource);
        persistedZipFile = validOutputArchiveResource.getFile();

        try {
            outputs = apiClient.postTaskRunOutputsArchive(runId, secret, persistedZipFile);
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
        storageHandler.createStorage(runStorage);

        String value = String.valueOf(provisionInputA.getValue());
        byte[] inputFileData = value.getBytes(getStorageCharset(charset));
        StorageData inputProvisionFileData = new StorageData(
            FileHelper.write(provisionInputA.getParameterName(), inputFileData),
            provisionInputA.getParameterName()
        );
        storageHandler.saveStorageData(runStorage, inputProvisionFileData);

        value = String.valueOf(provisionInputB.getValue());
        inputFileData = value.getBytes(getStorageCharset(charset));
        inputProvisionFileData = new StorageData(
            FileHelper.write(provisionInputB.getParameterName(), inputFileData),
            provisionInputB.getParameterName()
        );
        storageHandler.saveStorageData(runStorage, inputProvisionFileData);

        persistedRun.setState(TaskRunState.PROVISIONED);
        persistedRun = runRepository.saveAndFlush(persistedRun);
        Assertions.assertEquals(TaskRunState.PROVISIONED, persistedRun.getState());
    }

    @Then("App Engine sends a {string} OK response with a payload containing the success message \\(see OpenAPI spec)")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_the_success_message_see_open_api_spec(String responseCode) {
        Assertions.assertNotNull(persistedRunResponse);
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

    @Given("a task with {string} and {string} has been uploaded")
    public void uploadTask(String namespace, String version) {
        taskRepository.deleteAll();

        String bundleFilename = namespace + "-" + version + ".zip";
        persistedTask = TestTaskBuilder.buildTaskFromResource(bundleFilename);
    }

    @And("The task is assigned {string} RAM, {string} CPUs, and {string} GPUs")
    public void setTaskResources(String ram, String cpus, String gpus) {
        persistedTask.setRam(ram);
        persistedTask.setCpus(Integer.parseInt(cpus));
        persistedTask.setGpus(Integer.parseInt(gpus));
    }

    @And("the task has requested {string} RAM, {string} CPUs, and {string} GPUs")
    public void checkResourceRequested(String ram, String cpus, String gpus) {
        Assertions.assertEquals(ram, persistedTask.getRam());
        Assertions.assertEquals(Integer.parseInt(cpus), persistedTask.getCpus());
        Assertions.assertEquals(Integer.parseInt(gpus), persistedTask.getGpus());
    }

    @And("a task run has been created with {string}")
    public void createTaskRun(String uuid) {
        persistedRun = new Run(UUID.fromString(uuid), TaskRunState.CREATED, persistedTask);
        persistedRun = runRepository.save(persistedRun);

    }

    @And("a user provisioned all the parameters")
    public void provisionInputs(DataTable table) {
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

    @Then("the cluster has allocated {string} RAM, {string} CPUs, and {string} GPUs as requested.")
    public void checkResourceAllocation(String ram, String cpus, String gpus) {
        String uuid = persistedTask.getName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "") + "-" + persistedRun.getId();
        Map<String, String> resources = clusterClient.getAllocatedResources(uuid);

        Assertions.assertEquals(ram, resources.get("ram"));
        Assertions.assertEquals(cpus, resources.get("cpu"));
        Assertions.assertEquals(gpus, resources.get("gpu"));
    }
}
