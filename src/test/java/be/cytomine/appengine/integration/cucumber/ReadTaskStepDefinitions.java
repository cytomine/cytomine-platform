package be.cytomine.appengine.integration.cucumber;

import be.cytomine.appengine.AppEngineApplication;
import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.misc.TaskIdentifiers;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.handlers.FileStorageHandler;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    private ClassPathResource descriptor;
    private List<TaskDescription> tasks;

    @Autowired
    private DefaultApi appEngineApi;

    @Autowired
    FileStorageHandler fileStorageHandler;

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
        UUID taskLocalIdentifierForTaskOne = UUID.randomUUID();
        String storageIdentifierForTaskOne = "task-" + taskLocalIdentifierForTaskOne + "-def";
        String imageRegistryCompliantNameForTaskOne = "com/cytomine/app-engine/tasks/toy/add-integers:0.1.0";
        TaskIdentifiers taskIdentifiersForTaskOne = new TaskIdentifiers(taskLocalIdentifierForTaskOne, storageIdentifierForTaskOne, imageRegistryCompliantNameForTaskOne);

        UUID taskLocalIdentifierForTaskTwo = UUID.randomUUID();
        String storageIdentifierForTaskTwo = "task-" + taskLocalIdentifierForTaskTwo + "-def";
        String imageRegistryCompliantNameForTaskTwo = "com/cytomine/app-engine/tasks/toy/add-integers:0.1.1";
        TaskIdentifiers taskIdentifiersForTaskTwo = new TaskIdentifiers(taskLocalIdentifierForTaskTwo, storageIdentifierForTaskTwo, imageRegistryCompliantNameForTaskTwo);
        // store two tasks in the database
        taskRepository.deleteAll();
        Task taskOne = new Task();
        taskOne.setIdentifier(taskIdentifiersForTaskOne.getLocalTaskIdentifier());
        taskOne.setStorageReference(taskIdentifiersForTaskOne.getStorageIdentifier());
        taskOne.setName("calculator_addintegers");
        taskOne.setNameShort("add_int");
        taskOne.setDescriptorFile("com.cytomine.app-engine.tasks.toy.add-integers");
        taskOne.setNamespace("com.cytomine.app-engine.tasks.toy.add-integers");
        taskOne.setVersion("0.1.0");
        taskOne.setDescription("app to add two numbers");
        // add authors
        Set<Author> authors = new HashSet<>();
        Author a = new Author();
        a.setFirstName("Siddig");
        a.setLastName("Hamed");
        a.setOrganization("cytomine");
        a.setEmail("siddig@cytomine.com");
        a.setContact(true);
        authors.add(a);
        taskOne.setAuthors(authors);
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
        taskOne.setInputs(inputs);
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
        taskOne.setOutputs(outputs);
        // task two
        Task taskTwo = new Task();
        taskTwo.setIdentifier(taskIdentifiersForTaskTwo.getLocalTaskIdentifier());
        taskTwo.setStorageReference(taskIdentifiersForTaskTwo.getStorageIdentifier());
        taskTwo.setName("calculator_addintegers");
        taskTwo.setNameShort("add_int");
        taskTwo.setDescriptorFile("com.cytomine.app-engine.tasks.toy.add-integers");
        taskTwo.setNamespace("com.cytomine.app-engine.tasks.toy.add-integers");
        taskTwo.setVersion("0.1.1");
        taskTwo.setDescription("app to add two numbers");
        // add authors
        Set<Author> authors2 = new HashSet<>();
        Author a2 = new Author();
        a2.setFirstName("John");
        a2.setLastName("Doe");
        a2.setOrganization("cytomine");
        a2.setEmail("siddig@cytomine.com");
        a2.setContact(true);
        authors2.add(a2);
        taskTwo.setAuthors(authors2);
        // add inputs
        Set<Input> inputs2 = new HashSet<>();
        Input num1_2 = new Input();
        num1_2.setName("num1_2");
        num1_2.setDisplayName("First Number");
        num1_2.setDescription("First number in sum operation");
        IntegerType inputType2_1 = new IntegerType();

        inputType2_1.setId("integer");
        num1_2.setType(inputType2_1);

        Input num2_2 = new Input();
        num2_2.setName("num2_2");
        num2_2.setDisplayName("Second Number");
        num2_2.setDescription("Second number in sum operation");
        IntegerType inputType2_2 = new IntegerType();
        inputType2_2.setId("integer");
        num2_2.setType(inputType2_2);

        inputs2.add(num1_2);
        inputs2.add(num2_2);
        taskTwo.setInputs(inputs2);
        // add outputs for task one
        Set<Output> outputs_2 = new HashSet<>();
        Output output_2 = new Output();
        output_2.setName("sum");
        output_2.setDisplayName("Sum");
        output_2.setDescription("sum of two integers");
        IntegerType outputType_2 = new IntegerType();
        outputType_2.setId("integer");
        output_2.setType(outputType_2);
        outputs_2.add(output_2);
        taskTwo.setOutputs(outputs_2);

        taskRepository.save(taskOne);
        taskRepository.save(taskTwo);
    }

    @When("user calls the endpoint {string} \\(excluding version prefix, e.g. {string}) with HTTP method {string}")
    public void user_calls_the_endpoint_excluding_version_prefix_e_g_with_http_method(String uri, String string2, String method) throws ApiException {
        // use rest app engine client to list tasks
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        tasks = appEngineApi.tasksGet();

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

    Task taskOne;

    @Given("a valid {string} has a {string}, a {string} has been successfully uploaded")
    public void a_valid_has_a_a_has_been_successfully_uploaded(String task, String namespace, String version) {
        taskRepository.deleteAll();
        UUID taskLocalIdentifierForTaskOne = UUID.randomUUID();
        String storageIdentifierForTaskOne = "task-" + taskLocalIdentifierForTaskOne + "-def";
        String imageRegistryCompliantNameForTaskOne = "com/cytomine/app-engine/tasks/toy/add-integers:0.1.0";
        TaskIdentifiers taskIdentifiersForTaskOne = new TaskIdentifiers(taskLocalIdentifierForTaskOne, storageIdentifierForTaskOne, imageRegistryCompliantNameForTaskOne);

        taskOne = new Task();
        taskOne.setIdentifier(taskIdentifiersForTaskOne.getLocalTaskIdentifier());
        taskOne.setStorageReference(taskIdentifiersForTaskOne.getStorageIdentifier());
        taskOne.setName("calculator_addintegers");
        taskOne.setNameShort("add_int");
        taskOne.setDescriptorFile("com.cytomine.app-engine.tasks.toy.add-integers");
        taskOne.setNamespace(namespace);
        taskOne.setVersion(version);
        taskOne.setDescription("app to add two numbers");
        // add authors
        Set<Author> authors = new HashSet<>();
        Author a = new Author();
        a.setFirstName("Siddig");
        a.setLastName("Hamed");
        a.setOrganization("cytomine");
        a.setEmail("siddig@cytomine.com");
        a.setContact(true);
        authors.add(a);
        taskOne.setAuthors(authors);
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
        taskOne.setInputs(inputs);
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
        taskOne.setOutputs(outputs);

        taskRepository.save(taskOne);

    }


    @Given("a valid {string} has a {string} has been successfully uploaded")
    public void a_valid_has_a_has_been_successfully_uploaded(String task, String uuid) {
        taskRepository.deleteAll();
        UUID taskLocalIdentifierForTaskOne = UUID.fromString(uuid);
        String storageIdentifierForTaskOne = "task-" + taskLocalIdentifierForTaskOne + "-def";
        String imageRegistryCompliantNameForTaskOne = "com/cytomine/app-engine/tasks/toy/add-integers:0.1.0";
        TaskIdentifiers taskIdentifiersForTaskOne = new TaskIdentifiers(taskLocalIdentifierForTaskOne, storageIdentifierForTaskOne, imageRegistryCompliantNameForTaskOne);

        taskOne = new Task();
        taskOne.setIdentifier(taskIdentifiersForTaskOne.getLocalTaskIdentifier());
        taskOne.setStorageReference(taskIdentifiersForTaskOne.getStorageIdentifier());
        taskOne.setName("calculator_addintegers");
        taskOne.setNameShort("add_int");
        taskOne.setDescriptorFile("com.cytomine.app-engine.tasks.toy.add-integers");
        taskOne.setNamespace("com.cytomine.app-engine.tasks.toy.add-integers");
        taskOne.setVersion("0.1.0");
        taskOne.setDescription("app to add two numbers");
        // add authors
        Set<Author> authors = new HashSet<>();
        Author a = new Author();
        a.setFirstName("Moh");
        a.setLastName("Altahir");
        a.setOrganization("cytomine");
        a.setEmail("siddig@cytomine.com");
        a.setContact(true);
        authors.add(a);
        taskOne.setAuthors(authors);
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
        taskOne.setInputs(inputs);
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
        taskOne.setOutputs(outputs);

        taskRepository.save(taskOne);

    }

    @Given("a valid {string} has a {string}, a {string} and {string} has been successfully uploaded")
    public void a_valid_task_with_namespace_and_version_and_uuid_successfully_uploaded(String task, String namespace, String version, String uuid) {
        taskRepository.deleteAll();
        UUID taskLocalIdentifierForTaskOne = UUID.fromString(uuid);
        String storageIdentifierForTaskOne = "task-" + taskLocalIdentifierForTaskOne + "-def";
        String imageRegistryCompliantNameForTaskOne = "com/cytomine/app-engine/tasks/toy/add-integers:0.1.0";
        TaskIdentifiers taskIdentifiersForTaskOne = new TaskIdentifiers(taskLocalIdentifierForTaskOne, storageIdentifierForTaskOne, imageRegistryCompliantNameForTaskOne);

        taskOne = new Task();
        taskOne.setIdentifier(taskIdentifiersForTaskOne.getLocalTaskIdentifier());
        taskOne.setStorageReference(taskIdentifiersForTaskOne.getStorageIdentifier());
        taskOne.setName("calculator_addintegers");
        taskOne.setNameShort("add_int");
        taskOne.setDescriptorFile("com.cytomine.app-engine.tasks.toy.add-integers");
        taskOne.setNamespace(namespace);
        taskOne.setVersion(version);
        taskOne.setDescription("app to add two numbers");
        // add authors
        Set<Author> authors = new HashSet<>();
        Author a = new Author();
        a.setFirstName("Moh");
        a.setLastName("Altahir");
        a.setOrganization("cytomine");
        a.setEmail("siddig@cytomine.com");
        a.setContact(true);
        authors.add(a);
        taskOne.setAuthors(authors);
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
        taskOne.setInputs(inputs);
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
        taskOne.setOutputs(outputs);

        taskRepository.save(taskOne);

    }

    @Then("App Engine retrieves task with {string}, a {string}  from the database")
    public void app_engine_retrieves_task_data_from_the_database(String nameSpace, String version) {
        // a TaskDescription is received
        Assertions.assertNotNull(description.getDescription());
        Assertions.assertNotNull(description.getNamespace());
        Assertions.assertNotNull(description.getVersion());
        Assertions.assertNotNull(description.getName());
        Assertions.assertNotNull(description.getAuthors());
        Assertions.assertFalse(description.getAuthors().isEmpty());
    }

    @Then("App Engine retrieves task inputs with {string}, a {string}  from the database")
    public void app_engine_retrieves_task_inputs_data_from_the_database(String nameSpace, String version) {
        // check inputs
        Assertions.assertNotNull(inputParameters);
        Assertions.assertFalse(inputParameters.isEmpty());
        Assertions.assertEquals(inputParameters.size(), 2);
    }

    @Then("App Engine retrieves task with {string} from the database")
    public void app_engine_retrieves_task_data_from_the_database(String uuid) {
        // a TaskDescription is received
        Assertions.assertNotNull(description.getDescription());
        Assertions.assertNotNull(description.getNamespace());
        Assertions.assertNotNull(description.getVersion());
        Assertions.assertNotNull(description.getName());
        Assertions.assertNotNull(description.getAuthors());
        Assertions.assertFalse(description.getAuthors().isEmpty());
    }

    @Then("App Engine retrieves task inputs with {string} from the database")
    public void app_engine_retrieves_task_inputs_data_from_the_database(String uuid) {
        // a Input Parameters is received
        Assertions.assertNotNull(inputParameters);
        Assertions.assertFalse(inputParameters.isEmpty());
        Assertions.assertEquals(inputParameters.size(), 2);
    }

    TaskDescription description;

    @When("user calls the endpoint {string} with {string} and {string} with HTTP method GET")
    public void user_calls_the_endpoint_with_namespace_and_version_http_method_get(String uri, String namespace, String version) throws ApiException {
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        description = appEngineApi.tasksNamespaceVersionGet(namespace, version);

    }

    @When("user calls the endpoint {string} with id {string} HTTP method GET")
    public void user_calls_the_endpoint_with_uuid_http_method_get(String uri, String uuid) throws ApiException {
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        description = appEngineApi.tasksIdGet(UUID.fromString(uuid));


    }

    @Then("App Engine sends a {string} OK response with a payload containing the task description as a JSON payload \\(see OpenAPI spec)")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_the_task_description_as_a_json_payload_see_open_api_spec(String string) {
        Assertions.assertNotNull(description);
    }

    @Then("App Engine sends a {string} OK response with a payload containing the task inputs as a JSON payload \\(see OpenAPI spec)")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_the_task_inputs_as_a_json_payload_see_open_api_spec(String string) {
        Assertions.assertNotNull(inputParameters);
    }

    List<InputParameter> inputParameters;

    @When("user calls the endpoint {string} with {string} and {string} HTTP method GET")
    public void user_calls_the_endpoint_with_and_http_method_get(String endpoint, String namespace, String version) throws ApiException {
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        inputParameters = appEngineApi.tasksNamespaceVersionInputsGet(namespace, version);
    }

    List<OutputParameter> outputParameters;

    @When("user calls the outputs endpoint {string} with {string} and {string} HTTP method GET")
    public void user_calls_the_outputs_endpoint_with_and_http_method_get(String endpoint, String namespace, String version) throws ApiException {
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        outputParameters = appEngineApi.tasksNamespaceVersionOutputsGet(namespace, version);
    }

    @When("user calls the endpoint {string} with {string} HTTP method GET")
    public void user_calls_the_endpoint_with_http_method_get(String endpoint, String uuid) throws ApiException {
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        inputParameters = appEngineApi.tasksIdInputsGet(UUID.fromString(uuid));
    }

    @When("user calls the outputs endpoint {string} with {string} HTTP method GET")
    public void user_calls_the_outputs_endpoint_with_http_method_get(String endpoint, String uuid) throws ApiException {
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        outputParameters = appEngineApi.tasksIdOutputsGet(UUID.fromString(uuid));
    }

    File descriptorYml;

    @When("user calls the download endpoint with {string} and {string} with HTTP method GET")
    public void user_calls_the_download_endpoint_with_and_with_http_method_get(String namespace, String version) throws ApiException {
        // download the descriptor.yml
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        descriptorYml = appEngineApi.tasksNamespaceVersionDescriptorYmlGet(namespace, version);
    }

    @Given("the task descriptor is stored in the file storage service in storage {string} under filename {string}")
    public void the_task_descriptor_is_stored_in_the_file_storage_service_in_storage_under_filename(String storageReference, String descriptorFileName) throws FileStorageException, IOException {
        // save it in file storage service
        Storage storage = new Storage(storageReference);
        boolean bucketExists = fileStorageHandler.checkStorageExists(storage);
        if (!bucketExists) {
            fileStorageHandler.createStorage(storage);
        }
        // save file using defined storage reference
        descriptor = new ClassPathResource("/artifacts/descriptor.yml");
        Assertions.assertNotNull(descriptor);

        File file = descriptor.getFile();
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileByteArray = new byte[(int) file.length()];
        fileByteArray = fileInputStream.readAllBytes();
        FileData fileData = new FileData(fileByteArray, descriptorFileName);
        fileStorageHandler.createFile(storage, fileData);
    }

    @When("user calls the download endpoint with {string} with HTTP method GET")
    public void user_calls_the_download_endpoint_with_with_http_method_get(String uuid) throws ApiException {
        // download the descriptor.yml
        // call the correct endpoint based on URI
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(buildAppEngineUrl());
        appEngineApi = new DefaultApi(defaultClient);
        descriptorYml = appEngineApi.tasksIdDescriptorYmlGet(UUID.fromString(uuid));
    }

    @Then("App Engine retrieves the descriptor file {string} from the file storage")
    public void app_engine_retrieves_the_descriptor_file_from_the_file_storage(String fileName) {
        // make sure descriptor is not null
        Assertions.assertNotNull(descriptorYml);
        // TODO : maybe validate the schema to make sure it's a valid file but this is not the test here
    }

    @Then("App Engine sends a {string} OK response with the descriptor file as a binary payload \\(see OpenAPI spec)")
    public void app_engine_sends_a_response_with_the_descriptor_file_as_a_binary_payload_see_open_api_spec(String string) {
        Assertions.assertNotNull(descriptorYml);
    }

    @Then("App Engine retrieves task outputs with {string}, a {string}  from the database")
    public void app_engine_retrieves_task_outputs_with_a_from_the_database(String name, String string2) {
        Assertions.assertNotNull(outputParameters);
    }

    @Then("App Engine sends a {string} OK response with a payload containing the task outputs as a JSON payload \\(see OpenAPI spec)")
    public void app_engine_sends_a_ok_response_with_a_payload_containing_the_task_outputs_as_a_json_payload_see_open_api_spec(String string) {
        Assertions.assertNotNull(outputParameters);
    }

    @Then("App Engine retrieves task outputs with {string} from the database")
    public void app_engine_retrieves_task_outputs_with_from_the_database(String string) {
        Assertions.assertNotNull(outputParameters);
    }

    // TODO : unknown task
    String namespace;
    String version;
    String uuid_id;
    ApiException exception;

    @Given("a task unknown to the App Engine has a {string} and a {string} and a {string}")
    public void a_task_unknown_to_the_app_engine_has_a_and_a_and_a(String namespace, String version, String uuid) {
        // just make sure database is empty and doesn't contain the referenced tasks
        taskRepository.deleteAll();
        this.namespace = namespace;
        this.version = version;
        this.uuid_id = uuid;


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
                        outputParameters = appEngineApi.tasksNamespaceVersionOutputsGet(this.namespace, this.version);
                case "/task/id/outputs" ->
                        outputParameters = appEngineApi.tasksIdOutputsGet(UUID.fromString(this.uuid_id));
                case "/task/namespace/version/inputs" ->
                        inputParameters = appEngineApi.tasksNamespaceVersionInputsGet(this.namespace, this.version);
                case "/task/id/inputs" ->
                        inputParameters = appEngineApi.tasksIdInputsGet(UUID.fromString(this.uuid_id));
                case "/task/namespace/version" ->
                        description = appEngineApi.tasksNamespaceVersionGet(this.namespace, this.version);
                case "/task/id" -> description = appEngineApi.tasksIdGet(UUID.fromString(this.uuid_id));
                case "/task/namespace/version/descriptor.yml" ->
                        descriptorYml = appEngineApi.tasksNamespaceVersionDescriptorYmlGet(this.namespace, this.version);
                case "/task/id/descriptor.yml" ->
                        descriptorYml = appEngineApi.tasksIdDescriptorYmlGet(UUID.fromString(this.uuid_id));
            }
        } catch (ApiException e) {
            this.exception = e;
        }
    }

    @Then("App Engine sends a {string} HTTP error with a standard error payload containing code {string}")
    public void app_engine_sends_a_http_error(String expResponseCode, String appEngineErrorCode) throws JsonProcessingException {
        // make sure it's a 404 response
        String actualResponseCode = exception.getCode() + "";
        Assertions.assertEquals(expResponseCode, actualResponseCode);
        JsonNode jsonPayLoad = new ObjectMapper().readTree(exception.getResponseBody());
        // reply with expected error code
        Assertions.assertTrue(jsonPayLoad.get("error_code").textValue().startsWith(appEngineErrorCode));
    }

}
