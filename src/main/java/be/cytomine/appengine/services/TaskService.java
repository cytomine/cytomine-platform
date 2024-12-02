package be.cytomine.appengine.services;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.handlers.registry.DockerImage;
import be.cytomine.appengine.dto.inputs.task.*;
import be.cytomine.appengine.dto.misc.TaskIdentifiers;
import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.*;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.handlers.FileStorageHandler;
import be.cytomine.appengine.handlers.RegistryHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.models.task.integer.IntegerType;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.states.TaskRunState;
import be.cytomine.appengine.utils.ArchiveUtils;


import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
public class TaskService {

    Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final RunRepository runRepository;
    private final FileStorageHandler fileStorageHandler;
    private final RegistryHandler registryHandler;
    private final TaskValidationService taskValidationService;
    private final ArchiveUtils archiveUtils;


    public TaskService(TaskRepository taskRepository, RunRepository runRepository, FileStorageHandler fileStorageHandler, RegistryHandler registryHandler, TaskValidationService taskValidationService, ArchiveUtils archiveUtils) {
        this.taskRepository = taskRepository;
        this.runRepository = runRepository;
        this.fileStorageHandler = fileStorageHandler;
        this.registryHandler = registryHandler;
        this.taskValidationService = taskValidationService;
        this.archiveUtils = archiveUtils;
    }

    @Transactional
    public Optional<TaskDescription> uploadTask(MultipartFile taskArchive) throws TaskServiceException, ValidationException, BundleArchiveException {

        logger.info("UploadTask : building archive...");
        UploadTaskArchive uploadTaskArchive = archiveUtils.readArchive(taskArchive);
        logger.info("UploadTask : Archive is built");
        validateTaskBundle(uploadTaskArchive);
        logger.info("UploadTask : Archive validated");

        // generate identifiers
        TaskIdentifiers taskIdentifiers = generateTaskIdentifiers(uploadTaskArchive);
        logger.info("UploadTask : Task identifiers generated " + taskIdentifiers);
        // create storage
        Storage storage = new Storage(taskIdentifiers.getStorageIdentifier());
        try {
            fileStorageHandler.createStorage(storage);
            logger.info("UploadTask : Storage is created for task");
        } catch (FileStorageException e) {
            logger.error("UploadTask : failed to create storage [" + e.getMessage() + "]");
            AppEngineError error = ErrorBuilder.build(ErrorCode.STORAGE_CREATING_STORAGE_FAILED);
            throw new TaskServiceException(error);
        }

        // create file
        try {
            fileStorageHandler.createFile(storage, new FileData(uploadTaskArchive.getDescriptorFile(), "descriptor.yml"));
            logger.info("UploadTask : descriptor.yml is stored in object storage");
        } catch (FileStorageException e) {
            try {
                logger.info("UploadTask : failed to store descriptor.yml, attempting deleting storage...");
                fileStorageHandler.deleteStorage(storage);
                logger.info("UploadTask : storage deleted");

            } catch (FileStorageException ex) {
                logger.error("UploadTask : file storage service is failing [" + ex.getMessage() + "]");
                AppEngineError error = ErrorBuilder.build(ErrorCode.STORAGE_STORING_TASK_DEFINITION_FAILED);
                throw new TaskServiceException(error);
            }
            return Optional.empty();
        }
        // push image
        try {
            logger.info("UploadTask : pushing task image...");
            DockerImage image = new DockerImage(uploadTaskArchive.getDockerImage() , taskIdentifiers.getImageRegistryCompliantName());
            registryHandler.pushImage(image);
            logger.info("UploadTask : image pushed to registry");

        } catch (RegistryException e) {
            try {
                logger.debug("UploadTask : failed to push image to registry, attempting to delete storage...");
                fileStorageHandler.deleteStorage(storage);
                logger.info("UploadTask : storage deleted");

            } catch (FileStorageException ex) {
                logger.error("UploadTask : file storage service is failing [" + ex.getMessage() + "]");
                AppEngineError error = ErrorBuilder.build(ErrorCode.REGISTRY_PUSHING_TASK_IMAGE_FAILED);
                throw new TaskServiceException(error);
            }
        }

        // save task info
        Task task = new Task();
        task.setIdentifier(taskIdentifiers.getLocalTaskIdentifier());
        task.setStorageReference(taskIdentifiers.getStorageIdentifier());
        task.setImageName(taskIdentifiers.getImageRegistryCompliantName());
        task.setName(uploadTaskArchive.getDescriptorFileAsJson().get("name").textValue());
        task.setNameShort(uploadTaskArchive.getDescriptorFileAsJson().get("name_short").textValue());
        task.setDescriptorFile(uploadTaskArchive.getDescriptorFileAsJson().get("namespace").textValue());
        task.setNamespace(uploadTaskArchive.getDescriptorFileAsJson().get("namespace").textValue());
        task.setVersion(uploadTaskArchive.getDescriptorFileAsJson().get("version").textValue());
        task.setInputFolder(uploadTaskArchive.getDescriptorFileAsJson().get("configuration").get("input_folder").textValue());
        task.setOutputFolder(uploadTaskArchive.getDescriptorFileAsJson().get("configuration").get("output_folder").textValue());

        if (uploadTaskArchive.getDescriptorFileAsJson().get("description") != null)
            task.setDescription(uploadTaskArchive.getDescriptorFileAsJson().get("description").textValue());

        task.setAuthors(getAuthors(uploadTaskArchive));
        task.setInputs(getInputs(uploadTaskArchive));
        task.setOutputs(getOutputs(uploadTaskArchive));

        logger.info("UploadTask : saving task...");
        taskRepository.save(task);
        logger.info("UploadTask : task saved");
        // TODO : if save failed a delete request should be sent to both storage and registry
        return Optional.of(makeTaskDescription(task));
    }

    private Set<Input> getInputs(UploadTaskArchive uploadTaskArchive) {
        logger.info("UploadTask : getting inputs...");
        Set<Input> inputs = new HashSet<>();
        JsonNode inputsNode = uploadTaskArchive.getDescriptorFileAsJson().get("inputs");
        if (inputsNode.isObject()) {
            Iterator<String> fieldNames = inputsNode.fieldNames();
            while (fieldNames.hasNext()) {
                String inputKey = fieldNames.next();
                JsonNode inputValue = inputsNode.get(inputKey);

                Input input = new Input();
                input.setName(inputKey);
                input.setDisplayName(inputValue.get("display_name").textValue());
                input.setDescription(inputValue.get("description").textValue());
                // use type factory to generate the correct type
                input.setType(TypeFactory.createType(inputValue));

                // Set default value
                JsonNode defaultNode = inputValue.get("default");
                if (defaultNode != null) {
                    switch (defaultNode.getNodeType()) {
                        case STRING:
                            input.setDefaultValue(defaultNode.textValue());
                            break;
                        case BOOLEAN:
                            input.setDefaultValue(Boolean.toString(defaultNode.booleanValue()));
                            break;
                        case NUMBER:
                            input.setDefaultValue(defaultNode.numberValue().toString());
                            break;
                        default:
                            input.setDefaultValue(defaultNode.toString());
                            break;
                    }
                }

                inputs.add(input);
            }
        }
        logger.info("UploadTask : successful inputs ");
        return inputs;
    }

    private Set<Output> getOutputs(UploadTaskArchive uploadTaskArchive) {
        logger.info("UploadTask : getting outputs...");
        Set<Output> outputs = new HashSet<>();
        JsonNode outputsNode = uploadTaskArchive.getDescriptorFileAsJson().get("outputs");
        if (outputsNode.isObject()) {
            Iterator<String> fieldNames = outputsNode.fieldNames();
            while (fieldNames.hasNext()) {
                String outputKey = fieldNames.next();
                JsonNode inputValue = outputsNode.get(outputKey);

                Output output = new Output();
                output.setName(outputKey);
                output.setDisplayName(inputValue.get("display_name").textValue());
                output.setDescription(inputValue.get("description").textValue());
                // use type factory to generate the correct type
                output.setType(TypeFactory.createType(inputValue));

                outputs.add(output);

            }
        }
        logger.info("UploadTask : successful outputs ");
        return outputs;
    }

    private Set<Author> getAuthors(UploadTaskArchive uploadTaskArchive) {
        logger.info("UploadTask : getting authors...");
        Set<Author> authors = new HashSet<>();
        JsonNode authorNode = uploadTaskArchive.getDescriptorFileAsJson().get("authors");
        if (authorNode.isArray()) {
            for (JsonNode author : authorNode) {
                Author a = new Author();
                a.setFirstName(author.get("first_name").textValue());
                a.setLastName(author.get("last_name").textValue());
                a.setOrganization(author.get("organization").textValue());
                a.setEmail(author.get("email").textValue());
                a.setContact(author.get("is_contact").asBoolean());
                authors.add(a);
            }
        }
        logger.info("UploadTask : successful authors ");
        return authors;
    }

    private void validateTaskBundle(UploadTaskArchive uploadTaskArchive) throws ValidationException, BundleArchiveException {
        taskValidationService.validateDescriptorFile(uploadTaskArchive);
        taskValidationService.checkIsNotDuplicate(uploadTaskArchive);
        taskValidationService.validateImage(uploadTaskArchive);
    }

    private TaskIdentifiers generateTaskIdentifiers(UploadTaskArchive uploadTaskArchive) {
        UUID taskLocalIdentifier = UUID.randomUUID();
        String storageIdentifier = "task-" + taskLocalIdentifier + "-def";
        String imageIdentifierFromDescriptor = uploadTaskArchive.getDescriptorFileAsJson().get("namespace").textValue();
        String version = uploadTaskArchive.getDescriptorFileAsJson().get("version").textValue();
        String imageRegistryCompliantName = imageIdentifierFromDescriptor.replace(".", "/") + ":" + version;

        return new TaskIdentifiers(taskLocalIdentifier, storageIdentifier, imageRegistryCompliantName);
    }

    public FileData retrieveYmlDescriptor(String namespace, String version) throws TaskServiceException, TaskNotFoundException {
        logger.info("Storage : retrieving descriptor.yml...");
        Task task = taskRepository.findByNamespaceAndVersion(namespace, version);
        if (task == null)
            throw new TaskNotFoundException("task not found");

        FileData file = new FileData("descriptor.yml", task.getStorageReference());
        try {
            file = fileStorageHandler.readFile(file);
        } catch (FileStorageException ex) {
            logger.debug("Storage : failed to get file from storage [" + ex.getMessage() + "]");
            throw new TaskServiceException(ex);
        }
        return file;
    }

    public FileData retrieveYmlDescriptor(String id) throws TaskServiceException, TaskNotFoundException {
        logger.info("Storage : retrieving descriptor.yml...");
        Optional<Task> task = taskRepository.findById(UUID.fromString(id));
        if (task.isEmpty())
            throw new TaskNotFoundException("task not found");
        FileData file = new FileData("descriptor.yml", task.get().getStorageReference());
        try {
            file = fileStorageHandler.readFile(file);
        } catch (FileStorageException ex) {
            logger.debug("Storage : failed to get file from storage [" + ex.getMessage() + "]");
            throw new TaskServiceException(ex);
        }
        return file;
    }

    public Optional<TaskDescription> retrieveTaskDescription(String id) {
        Optional<Task> task = findById(id);
        return task.map(this::makeTaskDescription);
    }

    public Optional<TaskDescription> retrieveTaskDescription(String namespace, String version) {
        Optional<Task> task = findByNamespaceAndVersion(namespace, version);
        return task.map(this::makeTaskDescription);
    }

    public List<TaskDescription> retrieveTaskDescriptions() {
        List<Task> tasks = findAll();
        List<TaskDescription> taskDescriptions = new ArrayList<>();
        for (Task task : tasks) {
            TaskDescription taskDescription = makeTaskDescription(task);
            taskDescriptions.add(taskDescription);
        }
        return taskDescriptions;
    }

    public TaskDescription makeTaskDescription(Task task) {
        TaskDescription taskDescription = new TaskDescription(task.getIdentifier(), task.getName(), task.getNamespace(), task.getVersion(), task.getDescription());
        Set<TaskAuthor> descriptionAuthors = new HashSet<>();
        for (Author author : task.getAuthors()) {
            TaskAuthor taskAuthor = new TaskAuthor(author.getFirstName(), author.getLastName(), author.getOrganization(), author.getEmail(), author.isContact());
            descriptionAuthors.add(taskAuthor);
        }
        taskDescription.setAuthors(descriptionAuthors);
        return taskDescription;
    }

    public List<TaskInput> makeTaskInputs(Task task) {
        List<TaskInput> inputs = new ArrayList<>();
        for (Input input : task.getInputs()) {
            inputs.add(TaskInputFactory.createTaskInput(input));
        }
        return inputs;
    }


    public List<TaskOutput> makeTaskOutputs(Task task) {
        List<TaskOutput> outputs = new ArrayList<>();
        for (Output output : task.getOutputs()) {
            outputs.add(TaskOutputFactory.createTaskOutput(output));
        }
        return outputs;
    }


    public List<Task> findAll() {
        logger.info("tasks : retrieving tasks...");
        List<Task> taskList = taskRepository.findAll();
        logger.info("tasks : retrieved tasks");
        return taskList;
    }

    public Optional<Task> findById(String id) {
        logger.info("Data : retrieving task...");
        Optional<Task> task = taskRepository.findById(UUID.fromString(id));
        logger.info("Data : retrieved task");
        return task;
    }

    public Optional<Task> findByNamespaceAndVersion(String namespace, String version) {
        logger.info("tasks/{namespace}/{version} : retrieving task...");
        Task task = taskRepository.findByNamespaceAndVersion(namespace, version);
        logger.info("tasks/{namespace}/{version} : retrieved task...");
        return Optional.ofNullable(task);
    }

    @Transactional
    public TaskRun createRunForTask(String namespace, String version) throws RunTaskServiceException {
        logger.info("tasks/{namespace}/{version}/runs : creating run...");
        // find associated task
        logger.info("tasks/{namespace}/{version}/runs : retrieving associated task...");
        Task task = taskRepository.findByNamespaceAndVersion(namespace, version);

        // update task to have a new task run
        UUID taskRunID = UUID.randomUUID();
        if (task == null) {
            throw new RunTaskServiceException("task {" + namespace + ":" + version + "} not found to associate with this run");
        }
        if (task.getInputs().isEmpty()) {
            throw new RunTaskServiceException("task {" + namespace + ":" + version + "} has no inputs");
        }
        logger.info("tasks/{namespace}/{version}/runs : retrieved task...");
        Run run = new Run(taskRunID, TaskRunState.CREATED, task);
        runRepository.saveAndFlush(run);
        // create a storage for the inputs and outputs
        createRunStorages(taskRunID);
        // build response dto
        logger.info("tasks/{id}/runs : run created...");
        return new TaskRun(makeTaskDescription(task), taskRunID, TaskRunState.CREATED);
    }

    @Transactional
    public TaskRun createRunForTask(String taskId) throws RunTaskServiceException {
        logger.info("tasks/{id}/runs : creating run...");
        // find associated task
        logger.info("tasks/{namespace}/{version}/runs : retrieving associated task...");
        Optional<Task> task = taskRepository.findById(UUID.fromString(taskId));
        // update task to have a new task run
        UUID taskRunID = UUID.randomUUID();
        if (task.isEmpty()) {
            throw new RunTaskServiceException("task {" + taskId + "} not found to associate with this run");
        }
        if (task.get().getInputs().isEmpty()) {
            throw new RunTaskServiceException("task {" + taskId + "} has no inputs");
        }
        logger.info("tasks/{namespace}/{version}/runs : retrieved task...");
        Run run = new Run(taskRunID, TaskRunState.CREATED, task.get() , LocalDateTime.now());
        runRepository.saveAndFlush(run);
        // create a storage for the inputs and outputs
        createRunStorages(taskRunID);
        // build response dto
        logger.info("tasks/{id}/runs : run created...");
        return new TaskRun(makeTaskDescription(task.get()), taskRunID, TaskRunState.CREATED);
    }

    private void createRunStorages(UUID taskRunID) throws RunTaskServiceException {
        String inputStorageIdentifier = "task-run-inputs-" + taskRunID.toString();
        String outputsStorageIdentifier = "task-run-outputs-" + taskRunID.toString();
        Storage inputStorage = new Storage(inputStorageIdentifier);
        Storage outputStorage = new Storage(outputsStorageIdentifier);
        try {
            fileStorageHandler.createStorage(inputStorage);
            fileStorageHandler.createStorage(outputStorage);
            logger.info("tasks/{namespace}/{version}/runs : Storage is created for task");
        } catch (FileStorageException e) {
            logger.error("tasks/{namespace}/{version}/runs : failed to create storage [" + e.getMessage() + "]");
            throw new RunTaskServiceException(e);
        }
    }
}
