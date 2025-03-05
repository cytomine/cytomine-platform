package be.cytomine.appengine.integration.cucumber;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestClientResponseException;

import be.cytomine.appengine.AppEngineApplication;
import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.inputs.task.TaskDescription;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.exceptions.*;
import be.cytomine.appengine.utils.ApiClient;
import be.cytomine.appengine.utils.DescriptorHelper;
import be.cytomine.appengine.utils.TaskTestsUtils;
import be.cytomine.appengine.utils.TestTaskBuilder;

@ContextConfiguration(classes = AppEngineApplication.class, loader = SpringBootContextLoader.class)
public class ReadTaskStepDefinitions {

    @LocalServerPort
    private String port;

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private StorageHandler storageHandler;

    @Value("${app-engine.api_prefix}")
    private String apiPrefix;

    @Value("${app-engine.api_version}")
    private String apiVersion;

    private String persistedNamespace;

    private String persistedVersion;

    private String persistedUUID;

    private List<Input> persistedInputs;

    private List<Output> persistedOutputs;

    private File persistedDescriptor;

    private Task persistedTask;

    private TaskDescription persistedTaskDescription;

    private List<TaskDescription> tasks;

    private RestClientResponseException persistedException;

    @Before
    public void setUp() {
        apiClient.setBaseUrl("http://localhost:" + port + apiPrefix + apiVersion);
        apiClient.setPort(port);
    }

    @Given("a set of valid tasks has been successfully uploaded")
    public void a_set_of_valid_tasks_has_been_successfully_uploaded() {
        // generate identifiers for two tasks
        taskRepository.save(TestTaskBuilder.buildHardcodedAddInteger());
        taskRepository.save(TestTaskBuilder.buildHardcodedSubtractInteger());
    }

    @When("user calls the endpoint {string} \\(excluding version prefix, e.g. {string}) with HTTP method {string}")
    public void user_calls_the_endpoint_excluding_version_prefix_e_g_with_http_method(String uri, String string2, String method) {
        tasks = apiClient.getTasks();
    }

    @Then("App Engine retrieves relevant data from the database")
    public void app_engine_retrieves_relevant_data_from_the_database() {
        // response should contain a list of two task description
        Assertions.assertNotNull(tasks);
        Assertions.assertEquals(tasks.size(), 2);
    }

    @Then("App Engine sends a {string} OK response with a payload containing the descriptions of the available tasks as a JSON payload \\(see OpenAPI spec)")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_the_descriptions_of_the_available_tasks_as_a_json_payload_see_open_api_spec(String string) {
        // the response should be OK 200
        for (TaskDescription description : tasks) {
            Assertions.assertNotNull(description.getDescription());
            Assertions.assertNotNull(description.getNamespace());
            Assertions.assertNotNull(description.getVersion());
            Assertions.assertNotNull(description.getName());
            Assertions.assertNotNull(description.getAuthors());
            Assertions.assertFalse(description.getAuthors().isEmpty());
        }
    }

    private void createDescriptorInStorage(String bundleFilename, Task task) throws FileStorageException, IOException {
        // save it in file storage service
        Storage storage = new Storage(task.getStorageReference());
        if (!storageHandler.checkStorageExists(storage)) {
            storageHandler.createStorage(storage);
        }

        // save file using defined storage reference
        persistedDescriptor = TestTaskBuilder.getDescriptorFromBundleResource(bundleFilename);
        Assertions.assertNotNull(persistedDescriptor);

        StorageData fileData = new StorageData(persistedDescriptor, "descriptor.yml");
        storageHandler.saveStorageData(storage, fileData);
    }

    @Given("a valid task has a {string}, a {string} has been successfully uploaded")
    public void a_valid_has_a_a_has_been_successfully_uploaded(String namespace, String version) throws FileStorageException, IOException {
        taskRepository.deleteAll();
        String bundleFilename = namespace + "-" + version + ".zip";
        persistedTask = TestTaskBuilder.buildTaskFromResource(bundleFilename);
        taskRepository.save(persistedTask);

        Storage storage = new Storage(persistedTask.getStorageReference());
        if (storageHandler.checkStorageExists(storage)) {
            storageHandler.deleteStorage(storage);
        }
        createDescriptorInStorage(bundleFilename, persistedTask);
    }

    @Given("a valid task has a {string} has been successfully uploaded")
    public void a_valid_has_a_has_been_successfully_uploaded(String uuid) throws FileStorageException, IOException {
        taskRepository.deleteAll();
        String bundleFilename = "com.cytomine.dummy.arithmetic.integer.subtraction-1.0.0.zip";
        persistedTask = TestTaskBuilder.buildTaskFromResource(bundleFilename, UUID.fromString(uuid));
        taskRepository.save(persistedTask);

        // clean storage
        Storage storage = new Storage(persistedTask.getStorageReference());
        if (storageHandler.checkStorageExists(storage)) {
            storageHandler.deleteStorage(storage);
        }
        createDescriptorInStorage(bundleFilename, persistedTask);
    }

    @Given("a valid task has a {string}, a {string} and {string} has been successfully uploaded")
    public void a_valid_task_with_namespace_and_version_and_uuid_successfully_uploaded(String namespace, String version, String uuid) throws FileStorageException, IOException  {
        taskRepository.deleteAll();
        String bundleFilename = namespace + "-" + version + ".zip";
        persistedTask = TestTaskBuilder.buildTaskFromResource(bundleFilename, UUID.fromString(uuid));
        taskRepository.save(persistedTask);

        // clean storage
        Storage storage = new Storage(persistedTask.getStorageReference());
        if (storageHandler.checkStorageExists(storage)) {
            storageHandler.deleteStorage(storage);
        }
        createDescriptorInStorage(bundleFilename, persistedTask);
    }

    @Then("App Engine retrieves task with {string}, a {string}  from the database")
    public void app_engine_retrieves_task_data_from_the_database(String namespace, String version) {
    }

    @Then("App Engine retrieves task inputs with {string}, a {string}  from the database")
    public void app_engine_retrieves_task_inputs_data_from_the_database(String namespace, String version) {
        Assertions.assertNotNull(persistedInputs);
        Assertions.assertFalse(persistedInputs.isEmpty());
    }

    @Then("App Engine retrieves task with {string} from the database")
    public void app_engine_retrieves_task_data_from_the_database(String uuid) {
    }

    @Then("App Engine retrieves task inputs with {string} from the database")
    public void app_engine_retrieves_task_inputs_data_from_the_database(String uuid) {
        // a Input Parameters is received
        Assertions.assertNotNull(persistedInputs);
        Assertions.assertFalse(persistedInputs.isEmpty());
        Assertions.assertEquals(persistedInputs.size(), 2);
    }

    @When("user calls the endpoint {string} with {string} and {string} with HTTP method GET")
    public void user_calls_the_endpoint_with_namespace_and_version_http_method_get(String uri, String namespace, String version) {
        persistedTaskDescription = apiClient.getTask(namespace, version);
    }

    @When("user calls the endpoint {string} with id {string} HTTP method GET")
    public void user_calls_the_endpoint_with_uuid_http_method_get(String uri, String uuid) {
        persistedTaskDescription = apiClient.getTask(uuid);
    }

    @Then("App Engine sends a {string} OK response with a payload containing the task description as a JSON payload \\(see OpenAPI spec)")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_the_task_description_as_a_json_payload_see_open_api_spec(String string) {
        Assertions.assertNotNull(persistedTaskDescription);
        Assertions.assertEquals(persistedTaskDescription.getNamespace(), persistedTask.getNamespace());
        Assertions.assertEquals(persistedTaskDescription.getVersion(), persistedTask.getVersion());
        Assertions.assertEquals(persistedTaskDescription.getName(), persistedTask.getName());
        Assertions.assertEquals(persistedTaskDescription.getAuthors().size(), persistedTask.getAuthors().size());
    }

    @Then("App Engine sends a {string} OK response with a payload containing the task inputs as a JSON payload \\(see OpenAPI spec)")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_the_task_inputs_as_a_json_payload_see_open_api_spec(String string) {
        Assertions.assertNotNull(persistedInputs);
        Assertions.assertTrue(TaskTestsUtils.areSetEquals(persistedTask.getInputs(), persistedInputs));
    }

    @When("user calls the endpoint {string} with {string} and {string} HTTP method GET")
    public void user_calls_the_endpoint_with_and_http_method_get(String endpoint, String namespace, String version) {
        persistedInputs = apiClient.getInputs(namespace, version);
    }

    @When("user calls the outputs endpoint {string} with {string} and {string} HTTP method GET")
    public void user_calls_the_outputs_endpoint_with_and_http_method_get(String endpoint, String namespace, String version) {
        persistedOutputs = apiClient.getOutputs(namespace, version);
    }

    @When("user calls the endpoint {string} with {string} HTTP method GET")
    public void user_calls_the_endpoint_with_http_method_get(String endpoint, String uuid) {
        persistedInputs = apiClient.getInputs(uuid);
    }

    @When("user calls the outputs endpoint {string} with {string} HTTP method GET")
    public void user_calls_the_outputs_endpoint_with_http_method_get(String endpoint, String uuid) {
        persistedOutputs = apiClient.getOutputs(uuid);
    }

    @When("user calls the download endpoint with {string} and {string} with HTTP method GET")
    public void user_calls_the_download_endpoint_with_and_with_http_method_get(String namespace, String version) {
        persistedDescriptor = apiClient.getTaskDescriptor(namespace, version);
    }

    @Given("the task descriptor is stored in the file storage service in storage {string} under filename {string}")
    public void the_task_descriptor_is_stored_in_the_file_storage_service_in_storage_under_filename(String storageReference, String descriptorFileName) throws FileStorageException, IOException {
        // save it in file storage service
        Storage storage = new Storage(persistedTask.getStorageReference());
        Assertions.assertTrue(storageHandler.checkStorageExists(storage));

        File tempFile = Files.createTempFile(descriptorFileName, null).toFile();
        tempFile.deleteOnExit();

        StorageData emptyFile = new StorageData(tempFile, descriptorFileName);
        emptyFile.peek().setName("descriptor.yml");
        emptyFile.peek().setStorageId(storage.getIdStorage());
        storageHandler.readStorageData(emptyFile);
        Assertions.assertTrue(Files.size(emptyFile.peek().getData().toPath()) > 0);
    }

    @When("user calls the download endpoint with {string} with HTTP method GET")
    public void user_calls_the_download_endpoint_with_with_http_method_get(String uuid) {
        persistedDescriptor = apiClient.getTaskDescriptor(uuid);
    }

    @Then("App Engine retrieves the descriptor file {string} from the file storage")
    public void app_engine_retrieves_the_descriptor_file_from_the_file_storage(String fileName) {
        // make sure descriptor is not null
        Assertions.assertNotNull(persistedDescriptor);
    }

    @Then("App Engine sends a {string} OK response with the descriptor file as a binary payload \\(see OpenAPI spec)")
    public void app_engine_sends_a_response_with_the_descriptor_file_as_a_binary_payload_see_open_api_spec(String string) throws IOException {
        Assertions.assertNotNull(persistedDescriptor);
        JsonNode descriptorJson = DescriptorHelper.parseDescriptor(persistedDescriptor);
        Assertions.assertTrue(descriptorJson.has("namespace"));
        Assertions.assertTrue(descriptorJson.has("version"));
        Assertions.assertEquals(persistedTask.getNamespace(), descriptorJson.get("namespace").textValue());
        Assertions.assertEquals(persistedTask.getVersion(), descriptorJson.get("version").textValue());
    }

    @Then("App Engine retrieves task outputs with {string}, a {string}  from the database")
    public void app_engine_retrieves_task_outputs_with_a_from_the_database(String namespace, String version) {
        Assertions.assertNotNull(persistedOutputs);
        Assertions.assertTrue(TaskTestsUtils.areSetEquals(persistedTask.getOutputs(), persistedOutputs));
    }

    @Then("App Engine sends a {string} OK response with a payload containing the task outputs as a JSON payload \\(see OpenAPI spec)")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_the_task_outputs_as_a_json_payload_see_open_api_spec(String string) {
        Assertions.assertNotNull(persistedOutputs);
        Assertions.assertTrue(TaskTestsUtils.areSetEquals(persistedTask.getOutputs(), persistedOutputs));
    }

    @Then("App Engine retrieves task outputs with {string} from the database")
    public void app_engine_retrieves_task_outputs_with_from_the_database(String string) {
        Assertions.assertNotNull(persistedOutputs);
        Assertions.assertTrue(TaskTestsUtils.areSetEquals(persistedTask.getOutputs(), persistedOutputs));
    }

    @Given("a task unknown to the App Engine has a {string} and a {string} and a {string}")
    public void a_task_unknown_to_the_app_engine_has_a_and_a_and_a(String namespace, String version, String uuid) {
        // just make sure database is empty and doesn't contain the referenced tasks
        taskRepository.deleteAll();
        persistedNamespace = namespace;
        persistedVersion = version;
        persistedUUID = uuid;
    }

    @When("user calls the fetch endpoint {string} with HTTP method {string}")
    public void user_calls_the_fetch_endpoint_excluding_version_prefix_e_g_with_http_method(String endpoint, String method) {
        // call the correct endpoint based on URI

        try {
            switch (endpoint) {
                case "/task/namespace/version/outputs":
                    apiClient.getTaskOutputs(persistedNamespace, persistedVersion);
                    break;
                case "/task/id/outputs":
                    apiClient.getTaskOutputs(persistedUUID);
                    break;
                case "/task/namespace/version/inputs":
                    apiClient.getTaskInputs(persistedNamespace, persistedVersion);
                    break;
                case "/task/id/inputs":
                    apiClient.getTaskInputs(persistedUUID);
                    break;
                case "/task/namespace/version":
                    apiClient.getTask(persistedNamespace, persistedVersion);
                    break;
                case "/task/id":
                    apiClient.getTask(persistedUUID);
                    break;
                case "/task/namespace/version/descriptor.yml":
                    apiClient.getTaskDescriptor(persistedNamespace, persistedVersion);
                    break;
                case "/task/id/descriptor.yml":
                    apiClient.getTaskDescriptor(persistedUUID);
                    break;
                default:
                    throw new RuntimeException("Unknown endpoint");
            }
        } catch (RestClientResponseException e) {
            persistedException = e;
        }
    }

    @Then("App Engine sends a {string} HTTP error with a standard error payload containing code {string}")
    public void app_engine_sends_a_http_error(String expectedStatusCode, String appEngineErrorCode) throws JsonProcessingException {
        // make sure it's a 404 response
        String actualStatusCode = String.valueOf(persistedException.getStatusCode().value());
        Assertions.assertEquals(expectedStatusCode, actualStatusCode);

        // reply with expected error code
        JsonNode jsonPayLoad = new ObjectMapper().readTree(persistedException.getResponseBodyAsString());
        Assertions.assertTrue(jsonPayLoad.get("error_code").textValue().startsWith(appEngineErrorCode));
    }
}
