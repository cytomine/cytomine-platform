package be.cytomine.appengine.integration.cucumber;

import be.cytomine.appengine.AppEngineApplication;
import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.openapi.api.DefaultApi;
import be.cytomine.appengine.openapi.invoker.ApiClient;
import be.cytomine.appengine.openapi.invoker.ApiException;
import be.cytomine.appengine.openapi.invoker.Configuration;
import be.cytomine.appengine.openapi.model.InputParameter;
import be.cytomine.appengine.openapi.model.OutputParameter;
import be.cytomine.appengine.openapi.model.TaskDescription;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.services.TaskService;
import be.cytomine.appengine.exceptions.*;
import be.cytomine.appengine.utils.DescriptorHelper;
import be.cytomine.appengine.utils.TaskTestsUtils;
import be.cytomine.appengine.utils.TestTaskBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@ContextConfiguration(classes = AppEngineApplication.class, loader = SpringBootContextLoader.class)
public class ReadTaskStepDefinitions {

    Logger logger = LoggerFactory.getLogger(TaskService.class);

    @LocalServerPort
    String port;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    DefaultApi appEngineAPI;

    ResponseEntity<String> result;
    private List<TaskDescription> tasks;

    Task persistedTask;
    TaskDescription persistedTaskDescription;
    File persistedDescriptorFile;
    String persistedNamespace;
    String persistedVersion;
    String persistedUUID;
    ApiException persistedException;
    List<InputParameter> persistedInputParameters;
    List<OutputParameter> persistedOutputParameters;
    List<Input> persistedInputs;
    List<Output> persistedOutputs;
    File persistedDescriptorYml;

    @Autowired
    private DefaultApi appEngineApi;

    @Autowired
    StorageHandler fileStorageHandler;

    @Value("${app-engine.api_prefix}")
    private String apiPrefix;

    @Value("${app-engine.api_version}")
    private String apiVersion;

    private String buildAppEngineUrl() {
        return "http://localhost:" + port + apiPrefix + apiVersion;
    }

    @Given("a set of valid tasks has been successfully uploaded")
    public void a_set_of_valid_tasks_has_been_successfully_uploaded() {
        // generate identifiers for two tasks
        // generate identifiers
        taskRepository.save(TestTaskBuilder.buildHardcodedAddInteger());
        taskRepository.save(TestTaskBuilder.buildHardcodedSubtractInteger());
    }

    @When("user calls the endpoint {string} \\(excluding version prefix, e.g. {string}) with HTTP method {string}")
    public void user_calls_the_endpoint_excluding_version_prefix_e_g_with_http_method(String uri, String string2, String method) throws ApiException {
        // use rest app engine client to list tasks
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        tasks = appEngineApi.getTasks();
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

        if (!fileStorageHandler.checkStorageExists(storage)) {
            fileStorageHandler.createStorage(storage);
        }

        // save file using defined storage reference
        persistedDescriptorFile = TestTaskBuilder.getDescriptorFromBundleResource(bundleFilename);
        Assertions.assertNotNull(persistedDescriptorFile);

        try (FileInputStream fis = new FileInputStream(persistedDescriptorFile)) {
            byte[] fileByteArray = new byte[(int) persistedDescriptorFile.length()];
            fileByteArray = fis.readAllBytes();
            StorageData fileData = new StorageData(fileByteArray, "descriptor.yml");
            fileStorageHandler.saveStorageData(storage, fileData);
        }
    }

    @Given("a valid task has a {string}, a {string} has been successfully uploaded")
    public void a_valid_has_a_a_has_been_successfully_uploaded(String namespace, String version) throws FileStorageException, IOException {
        taskRepository.deleteAll();
        String bundleFilename = namespace + "-" + version + ".zip";
        persistedTask = TestTaskBuilder.buildTaskFromResource(bundleFilename);
        taskRepository.save(persistedTask);

        Storage storage = new Storage(persistedTask.getStorageReference());
        if (fileStorageHandler.checkStorageExists(storage)) {
            fileStorageHandler.deleteStorage(storage);
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
        if (fileStorageHandler.checkStorageExists(storage)) {
            fileStorageHandler.deleteStorage(storage);
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
        if (fileStorageHandler.checkStorageExists(storage)) {
            fileStorageHandler.deleteStorage(storage);
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
    public void user_calls_the_endpoint_with_namespace_and_version_http_method_get(String uri, String namespace, String version) throws ApiException {
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        persistedTaskDescription = appEngineApi.getTaskByNamespaceVersion(namespace, version);

    }

    @When("user calls the endpoint {string} with id {string} HTTP method GET")
    public void user_calls_the_endpoint_with_uuid_http_method_get(String uri, String uuid) throws ApiException {
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        persistedTaskDescription = appEngineApi.getTaskByUUID(UUID.fromString(uuid));
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
    public void user_calls_the_endpoint_with_and_http_method_get(String endpoint, String namespace, String version) throws ApiException {
        String endpointUrl = buildAppEngineUrl() + "/tasks/" + namespace + "/" + version + "/inputs";
        ResponseEntity<List<Input>> response = new RestTemplate().exchange(endpointUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<Input>>() {});
        persistedInputs = response.getBody();
    }


    @When("user calls the outputs endpoint {string} with {string} and {string} HTTP method GET")
    public void user_calls_the_outputs_endpoint_with_and_http_method_get(String endpoint, String namespace, String version) throws ApiException {
        String endpointUrl = buildAppEngineUrl() + "/tasks/" + namespace + "/" + version + "/outputs";
        ResponseEntity<List<Output>> response = new RestTemplate().exchange(endpointUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<Output>>() {});
        persistedOutputs = response.getBody();
    }

    @When("user calls the endpoint {string} with {string} HTTP method GET")
    public void user_calls_the_endpoint_with_http_method_get(String endpoint, String uuid) throws ApiException {
        String endpointUrl = buildAppEngineUrl() + "/tasks/" + uuid + "/inputs";
        ResponseEntity<List<Input>> response = new RestTemplate().exchange(endpointUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<Input>>() {});
        persistedInputs = response.getBody();
    }

    @When("user calls the outputs endpoint {string} with {string} HTTP method GET")
    public void user_calls_the_outputs_endpoint_with_http_method_get(String endpoint, String uuid) throws ApiException {
        String endpointUrl = buildAppEngineUrl() + "/tasks/" + uuid + "/outputs";
        ResponseEntity<List<Output>> response = new RestTemplate().exchange(endpointUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<Output>>() {});
        persistedOutputs = response.getBody();
    }

    @When("user calls the download endpoint with {string} and {string} with HTTP method GET")
    public void user_calls_the_download_endpoint_with_and_with_http_method_get(String namespace, String version) throws ApiException {
        // download the descriptor.yml
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        persistedDescriptorYml = appEngineApi.getTaskDescriptorByNamespaceVersion(namespace, version);
    }

    @Given("the task descriptor is stored in the file storage service in storage {string} under filename {string}")
    public void the_task_descriptor_is_stored_in_the_file_storage_service_in_storage_under_filename(String storageReference, String descriptorFileName) throws FileStorageException, IOException {
        // save it in file storage service
        Storage storage = new Storage(persistedTask.getStorageReference());
        Assertions.assertTrue(fileStorageHandler.checkStorageExists(storage));
        StorageData emptyFile = new StorageData(new byte[0]);
        emptyFile.peek().setName("descriptor.yml");
        emptyFile.peek().setStorageId(storage.getIdStorage());
        fileStorageHandler.readStorageData(emptyFile);
        Assertions.assertTrue(emptyFile.peek().getData().length > 0);
    }

    @When("user calls the download endpoint with {string} with HTTP method GET")
    public void user_calls_the_download_endpoint_with_with_http_method_get(String uuid) throws ApiException {
        // download the descriptor.yml
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        persistedDescriptorYml = appEngineApi.getTaskDescriptorByUUID(UUID.fromString(uuid));
    }

    @Then("App Engine retrieves the descriptor file {string} from the file storage")
    public void app_engine_retrieves_the_descriptor_file_from_the_file_storage(String fileName) {
        // make sure descriptor is not null
        Assertions.assertNotNull(persistedDescriptorYml);
    }

    @Then("App Engine sends a {string} OK response with the descriptor file as a binary payload \\(see OpenAPI spec)")
    public void app_engine_sends_a_response_with_the_descriptor_file_as_a_binary_payload_see_open_api_spec(String string) throws IOException {
        Assertions.assertNotNull(persistedDescriptorYml);
        JsonNode descriptorJson = DescriptorHelper.parseDescriptor(Files.readAllBytes(persistedDescriptorYml.toPath()));
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
        this.persistedNamespace = namespace;
        this.persistedVersion = version;
        this.persistedUUID = uuid;
    }

    @When("user calls the fetch endpoint {string} with HTTP method {string}")
    public void user_calls_the_fetch_endpoint_excluding_version_prefix_e_g_with_http_method(String endpoint, String method) {
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        // namespace and version
        try {
            switch (endpoint) {
                case "/task/namespace/version/outputs" ->
                        persistedOutputParameters = appEngineApi.getTaskOutputsByNamespaceVersion(this.persistedNamespace, this.persistedVersion);
                case "/task/id/outputs" ->
                        persistedOutputParameters = appEngineApi.getTaskOutputsByUUID(UUID.fromString(this.persistedUUID));
                case "/task/namespace/version/inputs" ->
                        persistedInputParameters = appEngineApi.getTaskInputsByNamespaceVersion(this.persistedNamespace, this.persistedVersion);
                case "/task/id/inputs" ->
                        persistedInputParameters = appEngineApi.getTaskInputsByUUID(UUID.fromString(this.persistedUUID));
                case "/task/namespace/version" ->
                        persistedTaskDescription = appEngineApi.getTaskByNamespaceVersion(this.persistedNamespace, this.persistedVersion);
                case "/task/id" -> persistedTaskDescription = appEngineApi.getTaskByUUID(UUID.fromString(this.persistedUUID));
                case "/task/namespace/version/descriptor.yml" ->
                        persistedDescriptorYml = appEngineApi.getTaskDescriptorByNamespaceVersion(this.persistedNamespace, this.persistedVersion);
                case "/task/id/descriptor.yml" ->
                        persistedDescriptorYml = appEngineApi.getTaskDescriptorByUUID(UUID.fromString(this.persistedUUID));
            }
        } catch (ApiException e) {
            this.persistedException = e;
        }
    }

    @Then("App Engine sends a {string} HTTP error with a standard error payload containing code {string}")
    public void app_engine_sends_a_http_error(String expResponseCode, String appEngineErrorCode) throws JsonProcessingException {
        // make sure it's a 404 response
        String actualResponseCode = persistedException.getCode() + "";
        Assertions.assertEquals(expResponseCode, actualResponseCode);
        JsonNode jsonPayLoad = new ObjectMapper().readTree(persistedException.getResponseBody());
        // reply with expected error code
        Assertions.assertTrue(jsonPayLoad.get("error_code").textValue().startsWith(appEngineErrorCode));
    }

}
