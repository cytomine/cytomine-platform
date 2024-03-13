package be.cytomine.appengine.integration.cucumber;

import be.cytomine.appengine.AppEngineApplication;
import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.misc.TaskIdentifiers;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.dto.responses.errors.ErrorDefinitions;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.handlers.FileStorageHandler;
import be.cytomine.appengine.handlers.RegistryHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.repositories.TaskRepository;
import com.cytomine.registry.client.RegistryClient;
import com.cytomine.registry.client.http.resp.CatalogResp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@ContextConfiguration(classes = AppEngineApplication.class, loader = SpringBootContextLoader.class)
public class UploadTaskStepDefinitions {

    Logger logger = LoggerFactory.getLogger(UploadTaskStepDefinitions.class);


    @LocalServerPort
    String port;

    @Autowired
    TaskRepository taskRepository;

    ResponseEntity<String> persistedResponse;

    String taskNameSpace;
    String taskVersion;
    private ClassPathResource archive;
    private Task uploaded;

    @Autowired
    RegistryHandler dockerRegistryHandler;

    @Value("${registry-client.host}")
    private String registry;

    @Autowired
    FileStorageHandler fileStorageHandler;

    @Value("${app-engine.api_prefix}")
    private String apiPrefix;

    @Value("${app-engine.api_version}")
    private String apiVersion;

    private String buildAppEngineUrl() {
        return "http://localhost:" + port + apiPrefix + apiVersion;
    }

    @Given("App Engine is up and running")
    public void app_engine_is_up_and_running() {
        ResponseEntity<String> health = new RestTemplate().exchange("http://localhost:" + port + "/actuator/health", HttpMethod.GET, null, String.class);
        Assertions.assertTrue(health.getStatusCode().is2xxSuccessful());
        taskRepository.deleteAll();
    }

    @Given("File storage service is up and running")
    public void file_storage_service_is_up_and_running() throws FileStorageException {
        // ping the file storage service health endpoint and make sure we get no errors
//      // as long as it responds it means it's up and running
        fileStorageHandler.checkStorageExists("random");
    }

    @Given("Registry service is up and running")
    public void registry_service_is_up_and_running() throws IOException {
        try {
            RegistryClient.delete("registry:5000/com/cytomine/app-engine/tasks/toy/add-integers@sha256:d53ef00848a227ce64ce71cd7cceb7184fd1f116e0202289b26a576cf87dc4cb");
        } catch (Exception e) {
        }
    }

    @Given("a {string} uniquely identified by an {string} and a {string}")
    public void a_uniquely_identified_by_an_and_a(String taskName, String taskNamSpace, String taskVersion) {
        this.taskNameSpace = taskNamSpace;
        this.taskVersion = taskVersion;
    }

    @Given("this {string} identified by an {string} and a {string} is not yet known to the App Engine")
    public void this_identified_by_an_and_a_is_not_yet_known_to_the_app_engine(String taskName, String taskNameSpace, String taskVersion) {
        Task task = taskRepository.findByNamespaceAndVersion(taskNameSpace, taskVersion);
        Assertions.assertNull(task);
    }

    @Given("this {string} is represented by a zip archive containing a task descriptor file and a docker image")
    public void this_is_represented_by_a_zip_archive_containing_a_task_descriptor_file_and_a_docker_image(String string) {
        // make sure valid test archive is in classpath
        archive = new ClassPathResource("/artifacts/test_custom_image_location_task.zip");
        Assertions.assertNotNull(archive);

    }

    @Given("the task descriptor is a YAML file stored in the zip archive named {string} structured following an agreed descriptor schema")
    public void the_task_descriptor_is_a_yaml_file_stored_in_the_zip_archive_named_structured_following_an_agreed_descriptor_schema(String string) {
        // test archive contains valid yml file and following the agreed schema
    }

    @Given("the Docker image stored in the zip archive is represented by a tar file of which the relative path is {string}")
    public void the_docker_image_stored_in_the_zip_archive_is_represented_by_a_tar_file_of_which_the_relative_path_is(String string) {
        // archive contains docker image in tar path
    }

    @Given("{string} is either defined by the field {string} in the task descriptor or, if this field is missing, is {string}")
    public void is_either_defined_by_the_field_in_the_task_descriptor_or_if_this_field_is_missing_is(String string, String string2, String string3) {
        // docker image location is defined in field configuration.image.file in descriptor.yml already in artifacts
    }

    @When("user calls POST on {string} endpoint with the zip archive as a multipart file parameter")
    public void user_calls_post_on_endpoint_with_the_zip_archive_as_a_multipart_file_parameter(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("task", archive);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            persistedResponse = new RestTemplate().postForEntity(buildAppEngineUrl() + "/tasks", request, String.class);
        } catch (HttpClientErrorException.Conflict e) {
            this.logger.error("conflict: " + e.getResponseBodyAsString(), e);
            persistedResponse = new ResponseEntity<String>(e.getResponseBodyAsString(), HttpStatusCode.valueOf(409));
        } catch (HttpClientErrorException.BadRequest e) {
            Assertions.assertEquals("", e.getResponseBodyAsString());
            persistedResponse = new ResponseEntity<String>(e.getResponseBodyAsString(), HttpStatusCode.valueOf(400));
        }
        Assertions.assertNotNull(persistedResponse);
    }

    @Then("App Engine unzip the zip archive")
    public void app_engine_unzip_the_zip_archive() throws JsonProcessingException {
        // failure to unzip result in 400 http code
        Assertions.assertNotEquals(persistedResponse.getStatusCode(), HttpStatusCode.valueOf(400));
        JsonNode jsonPayLoad = new ObjectMapper().readTree(persistedResponse.getBody());
        // doesn't reply with parsing failure
        Assertions.assertFalse(jsonPayLoad.has("error_code"));
    }

    @Then("App Engine successfully validates the task descriptor against the descriptor schema")
    public void app_engine_successfully_validates_the_task_descriptor_against_the_descriptor_schema() throws JsonProcessingException {
        // failure to unzip result in 400 http code
        Assertions.assertFalse(persistedResponse.getStatusCode().is4xxClientError());
        JsonNode jsonPayLoad = new ObjectMapper().readTree(persistedResponse.getBody());
        // doesn't reply with parsing failure
        Assertions.assertFalse(jsonPayLoad.has("error_code"));
    }

    @Then("App Engine successfully validates the docker image by checking that the tar file contains a {string} file")
    public void app_engine_successfully_validates_the_docker_image_by_checking_that_the_tar_file_contains_a_file(String string) throws JsonProcessingException {
        // failure to unzip result in 400 http code
        Assertions.assertFalse(persistedResponse.getStatusCode().is4xxClientError());
        JsonNode jsonPayLoad = new ObjectMapper().readTree(persistedResponse.getBody());
        // doesn't reply with parsing failure
        Assertions.assertFalse(jsonPayLoad.has("error_code"));
    }

    @Then("App Engine creates a unique {string} for referencing the {string}")
    public void app_engine_creates_a_unique_for_referencing_the(String string, String string2) {

        Task uploaded = taskRepository.findByNamespaceAndVersion(taskNameSpace, taskVersion);
        Assertions.assertEquals(UUID.fromString(uploaded.getIdentifier().toString()), uploaded.getIdentifier());
    }

    @Then("App Engine creates a task storage \\(e.g. a bucket reserved for the {string}) in the File Storage service with a unique {string} as follows {string}")
    public void app_engine_creates_a_task_storage_e_g_a_bucket_reserved_for_the_in_the_file_storage_service_with_a_unique_as_follows(String string, String string2, String string3) throws FileStorageException {
        // retrieve from file storage
        String bucketName = uploaded.getStorageReference();
        boolean found = fileStorageHandler.checkStorageExists(bucketName);
        Assertions.assertTrue(found);
    }

    @Then("App Engine stores the task descriptor in the task storage {string} of the File Storage service")
    public void app_engine_stores_the_task_descriptor_in_the_task_storage_of_the_file_storage_service(String string) throws FileStorageException {
        // retrieve from file storage

        String bucket = uploaded.getStorageReference();
        String object = "descriptor.yml";
        FileData descriptorFileData = new FileData(object, bucket);
        FileData descriptor = fileStorageHandler.readFile(descriptorFileData);
        Assertions.assertNotNull(descriptor);
    }


    @Then("App Engine pushes the docker image to the Registry service with an {string} built based on the {string} and {string} \\(replace {string} by {string} in {string}, then append :{string})")
    public void app_engine_pushes_the_docker_image_to_the_registry_service_with_an_built_based_on_the_and_replace_by_in_then_append(String string, String string2, String string3, String string4, String string5, String string6, String string7) {
        // not implemented yet .. assume pushed
    }

    @Then("App Engine stores relevant task metadata \\(a subset of the task descriptor content) in the database associated with the {string}")
    public void app_engine_stores_relevant_task_metadata_a_subset_of_the_task_descriptor_content_in_the_database_associated_with_the(String string) {
        // stores task
        uploaded = taskRepository.findByNamespaceAndVersion(taskNameSpace, taskVersion);
        Assertions.assertNotNull(uploaded);
        Assertions.assertTrue(uploaded.getNamespace().equalsIgnoreCase(taskNameSpace));
        Assertions.assertTrue(uploaded.getVersion().equalsIgnoreCase(taskVersion));

    }

    @Then("App Engine returns an HTTP {string} OK response")
    public void app_engine_returns_an_http_response(String responseCode) {
        Assertions.assertTrue(persistedResponse.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(Integer.parseInt(responseCode))));

    }

    @Then("App Engine cleans up any temporary file created during the process \\(e.g. uploaded zip file, etc)")
    public void app_engine_cleans_up_any_temporary_file_created_during_the_process_e_g_uploaded_zip_file_etc() {
        // tmp files are stored in /tmp/appengine
        final File rootFolder = new File("/tmp/appengine");
        final File multiPartTempFiles = new File("/tmp/appengine/work/Tomcat/localhost/ROOT");
        if (rootFolder.exists() && multiPartTempFiles.exists()) {
            Assertions.assertTrue(multiPartTempFiles.isDirectory());
            Assertions.assertEquals(0, Objects.requireNonNull(multiPartTempFiles.listFiles()).length);
        }

    }

    Task taskOne;

    @Given("this task is already registered by the App Engine")
    public void this_task_is_already_registered_by_the_app_engine() {
        // store task in database
        UUID taskLocalIdentifierForTaskOne = UUID.randomUUID();
        String storageIdentifierForTaskOne = "task-" + taskLocalIdentifierForTaskOne + "-def";
        String imageRegistryCompliantNameForTaskOne = "com/cytomine/app-engine/tasks/toy/add-integers:0.1.0";
        TaskIdentifiers taskIdentifiersForTaskOne = new TaskIdentifiers(taskLocalIdentifierForTaskOne, storageIdentifierForTaskOne, imageRegistryCompliantNameForTaskOne);

        taskOne = new Task();
        taskOne.setIdentifier(taskIdentifiersForTaskOne.getLocalTaskIdentifier());
        taskOne.setStorageReference(taskIdentifiersForTaskOne.getStorageIdentifier());
        taskOne.setName("calculator_addintegers");
        taskOne.setNameShort("must_not_have_changed");
        taskOne.setDescriptorFile("com.cytomine.app-engine.tasks.toy.add-integers");
        taskOne.setNamespace(this.taskNameSpace);
        taskOne.setVersion(this.taskVersion);
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

        taskOne = taskRepository.save(taskOne);
        Assertions.assertNotNull(taskOne);

        // add it to the storage service

        // create bucket
        try {
            boolean bucketExists = fileStorageHandler.checkStorageExists(taskOne.getStorageReference());
            if (!bucketExists) {
                Storage storage = new Storage(taskOne.getStorageReference());
                fileStorageHandler.createStorage(storage);

            }

            // save file using defined storage reference

            ClassPathResource descriptor = new ClassPathResource("/artifacts/descriptorduplicate.yml");
            Assertions.assertNotNull(descriptor);

            File file = descriptor.getFile();
            FileInputStream fileInputStream = new FileInputStream(file);

            Storage storage = new Storage(taskOne.getStorageReference());
            byte[] fileByteArray = new byte[(int) file.length()];
            fileByteArray = fileInputStream.readAllBytes();
            FileData fileData = new FileData(fileByteArray, "descriptor.yml");
            fileStorageHandler.createFile(storage, fileData);


        } catch (IOException e) {

        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }
    }


    @Then("App Engine reads {string} and {string} from the task descriptor")
    public void app_engine_reads_and_from_the_task_descriptor(String namespace, String version) {
        // it's already in the descriptor

    }

    @Then("App Engine returns an HTTP {string} conflict error because this version of the task exists already")
    public void app_engine_returns_an_http_conflict_error_because_this_version_of_the_task_exists_already(String conflictCode) throws JsonProcessingException {
        // failure
        Assertions.assertEquals(persistedResponse.getStatusCode(), HttpStatusCode.valueOf(Integer.parseInt(conflictCode)));
        JsonNode jsonPayLoad = new ObjectMapper().readTree(persistedResponse.getBody());
        // doesn't reply with parsing failure
        Assertions.assertEquals(jsonPayLoad.get("error_code").textValue(), ErrorDefinitions.fromCode(ErrorCode.INTERNAL_TASK_EXISTS).code);
    }

    @Then("App Engine does not create or overwrite the task and related data in the File storage, registry and database services")
    public void app_engine_does_not_create_or_overwrite_the_task_and_related_data_in_the_file_storage_registry_and_database_services() throws FileStorageException, IOException {
        // check app engine doesn't override data in database
        Task uploaded = taskRepository.findByNamespaceAndVersion(taskNameSpace, taskVersion);
        Assertions.assertTrue(uploaded.getNameShort().equalsIgnoreCase("must_not_have_changed"));

        // and storage service
        String object = "descriptor.yml";
        FileData fileData = new FileData(object, taskOne.getStorageReference());
        fileData = fileStorageHandler.readFile(fileData);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode descriptor = mapper.readTree(fileData.getFileData());

        Assertions.assertNotNull(fileData);
        String shortName = descriptor.get("name_short").textValue();
        Assertions.assertTrue(shortName.equalsIgnoreCase("must_not_have_changed"));
        // and registry
        CatalogResp response = RegistryClient.catalog("http://" + registry + ":5000", 2, "");
        boolean repoFound = false;
        String imageName = uploaded.getNamespace().replace(".", "/");
        for (String repository : response.getRepositories()) {
            repoFound = repository.equalsIgnoreCase(imageName);
            break;
        }
    }

    // invalid task bundle tests
    @Given("this task is represented by a zip archive containing a task descriptor file {string} and a docker image at {string}")
    public void this_task_is_represented_by_a_zip_archive_containing_a_task_with_version_descriptor_file_and_a_docker_image_at(String version, String path) {
        // prepare bundles that represent all failure modes based on path and version
        if (path.equalsIgnoreCase("image.tar") && version.equalsIgnoreCase("")) {
            archive = new ClassPathResource("/artifacts/no_version.zip");
            Assertions.assertNotNull(archive);
        }
        if (path.equalsIgnoreCase("image.tar") && version.equalsIgnoreCase("0.1.0")) {
            archive = new ClassPathResource("/artifacts/unexpected_path.zip");
            Assertions.assertNotNull(archive);
        }

        if (path.equalsIgnoreCase("myimage.tar") && version.equalsIgnoreCase("0.1.0")) {
            archive = new ClassPathResource("/artifacts/wrong_name.zip");
            Assertions.assertNotNull(archive);
        }
    }

    String expectedImagePath;

    @Given("the {string} can be retrieved from {string} in the descriptor file or is {string} if the field is missing")
    public void the_can_be_retrieved_from_in_the_descriptor_file_or_is_if_the_field_is_missing(String expectedPath, String config, String image) {
        // reflected by invalid bundles in the artifacts folder in resources
    }

    @Given("the task descriptor is a YAML file stored in the zip archive named {string} which does not abide to the agreed descriptor schema for a {string}")
    public void the_task_descriptor_is_a_yaml_file_stored_in_the_zip_archive_named_which_does_not_abide_to_the_agreed_descriptor_schema_for_a(String descriptor, String reason) {
        // reflected by invalid bundles in the artifacts folder in resources
    }

    @Then("App Engine does not create the {string} and related data in the File storage, registry and database services")
    public void app_engine_does_not_create_the_and_related_data_in_the_file_storage_registry_and_database_services(String namespace) {
        // check database does not contain the task with the given namespace
        // database
        Task task = taskRepository.findByNamespaceAndVersion(taskNameSpace, taskVersion);
        Assertions.assertNull(task);
        // storage is not created because we generate the bucket id based on a generated id

    }

    @Then("App Engine returns an HTTP {string} bad request error because the descriptor is incorrectly structured")
    public void app_engine_returns_an_http_bad_request_error_because_the_descriptor_is_incorrectly_structured(String responseCode) {
        Assertions.assertTrue(persistedResponse.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(400)));
    }

    @Then("App Engine fails to validate the {string} descriptor against the descriptor schema")
    public void app_engine_fails_to_validate_the_task_descriptor_against_the_descriptor_schema(String task) throws JsonProcessingException {
        JsonNode jsonPayLoad = new ObjectMapper().readTree(persistedResponse.getBody());
        if (task.equalsIgnoreCase("Task 1")) {
            Assertions.assertTrue(jsonPayLoad.get("message").textValue().equalsIgnoreCase("schema validation failed for descriptor.yml"));
        }
        if (task.equalsIgnoreCase("Task 2")) {
            Assertions.assertTrue(jsonPayLoad.get("message").textValue().equalsIgnoreCase("image not found in configured place in descriptor and not in the root directory"));
        }

        if (task.equalsIgnoreCase("Task 3")) {
            Assertions.assertTrue(jsonPayLoad.get("message").textValue().equalsIgnoreCase("image not found in configured place in descriptor and not in the root directory"));
        }

    }
}
