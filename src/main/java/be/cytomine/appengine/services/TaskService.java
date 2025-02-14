package be.cytomine.appengine.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.handlers.registry.DockerImage;
import be.cytomine.appengine.dto.inputs.task.TaskAuthor;
import be.cytomine.appengine.dto.inputs.task.TaskDescription;
import be.cytomine.appengine.dto.inputs.task.TaskInput;
import be.cytomine.appengine.dto.inputs.task.TaskInputFactory;
import be.cytomine.appengine.dto.inputs.task.TaskOutput;
import be.cytomine.appengine.dto.inputs.task.TaskOutputFactory;
import be.cytomine.appengine.dto.inputs.task.TaskRun;
import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.dto.misc.TaskIdentifiers;
import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.BundleArchiveException;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.exceptions.RegistryException;
import be.cytomine.appengine.exceptions.RunTaskServiceException;
import be.cytomine.appengine.exceptions.TaskNotFoundException;
import be.cytomine.appengine.exceptions.TaskServiceException;
import be.cytomine.appengine.exceptions.ValidationException;
import be.cytomine.appengine.handlers.RegistryHandler;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.Author;
import be.cytomine.appengine.models.task.Input;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.models.task.TypeFactory;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.states.TaskRunState;
import be.cytomine.appengine.utils.ArchiveUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    private final RunRepository runRepository;

    private final StorageHandler fileStorageHandler;

    private final RegistryHandler registryHandler;

    private final TaskValidationService taskValidationService;

    private final ArchiveUtils archiveUtils;

    @Value("${storage.input.charset}")
    private String charset;

    @Value("${scheduler.task-resources.ram}")
    private String defaultRam;

    @Value("${scheduler.task-resources.cpus}")
    private int defaultCpus;

    @Transactional
    public Optional<TaskDescription> uploadTask(MultipartFile taskArchive)
        throws BundleArchiveException, TaskServiceException, ValidationException {

        log.info("UploadTask: building archive...");
        UploadTaskArchive uploadTaskArchive = archiveUtils.readArchive(taskArchive);
        log.info("UploadTask: Archive is built");
        validateTaskBundle(uploadTaskArchive);
        log.info("UploadTask: Archive validated");

        TaskIdentifiers taskIdentifiers = generateTaskIdentifiers(uploadTaskArchive);
        log.info("UploadTask: Task identifiers generated {}", taskIdentifiers);

        Storage storage = new Storage(taskIdentifiers.getStorageIdentifier());
        try {
            fileStorageHandler.createStorage(storage);
            log.info("UploadTask: Storage is created for task");
        } catch (FileStorageException e) {
            log.error("UploadTask: failed to create storage [{}]", e.getMessage());
            AppEngineError error = ErrorBuilder.build(ErrorCode.STORAGE_CREATING_STORAGE_FAILED);
            throw new TaskServiceException(error);
        }

        try {
            fileStorageHandler.saveStorageData(
                storage, new StorageData(uploadTaskArchive.getDescriptorFile(), "descriptor.yml"));
            log.info("UploadTask: descriptor.yml is stored in object storage");
        } catch (FileStorageException e) {
            try {
                log.info("UploadTask: failed to store descriptor.yml");
                log.info("UploadTask: attempting deleting storage...");
                fileStorageHandler.deleteStorage(storage);
                log.info("UploadTask: storage deleted");
            } catch (FileStorageException ex) {
                log.error("UploadTask: file storage service is failing [{}]", ex.getMessage());
                AppEngineError error = ErrorBuilder
                    .build(ErrorCode.STORAGE_STORING_TASK_DEFINITION_FAILED);
                throw new TaskServiceException(error);
            }
            return Optional.empty();
        }

        log.info("UploadTask: pushing task image...");
        DockerImage image =
            new DockerImage(
            uploadTaskArchive.getDockerImage(),
            taskIdentifiers.getImageRegistryCompliantName());
        try {
            registryHandler.pushImage(image);
        } catch (RegistryException e) {
            try {
                log.debug("UploadTask: failed to push image to registry");
                log.debug("UploadTask: attempting to delete storage...");
                fileStorageHandler.deleteStorage(storage);
                log.info("UploadTask: storage deleted");
            } catch (FileStorageException ex) {
                log.error("UploadTask: file storage service is failing [{}]", ex.getMessage());
                AppEngineError error = ErrorBuilder
                    .build(ErrorCode.REGISTRY_PUSHING_TASK_IMAGE_FAILED);
                throw new TaskServiceException(error);
            }
        } finally {
            uploadTaskArchive.getDockerImage().delete();
        }
        log.info("UploadTask: image pushed to registry");

        // save task info
        Task task = new Task();
        task.setIdentifier(taskIdentifiers.getLocalTaskIdentifier());
        task.setStorageReference(taskIdentifiers.getStorageIdentifier());
        task.setImageName(taskIdentifiers.getImageRegistryCompliantName());
        task.setName(uploadTaskArchive.getDescriptorFileAsJson().get("name").textValue());
        task.setNameShort(uploadTaskArchive
            .getDescriptorFileAsJson()
            .get("name_short")
            .textValue());
        task.setDescriptorFile(
            uploadTaskArchive.getDescriptorFileAsJson().get("namespace").textValue());
        task.setNamespace(uploadTaskArchive.getDescriptorFileAsJson().get("namespace").textValue());
        task.setVersion(uploadTaskArchive.getDescriptorFileAsJson().get("version").textValue());
        task.setInputFolder(
            uploadTaskArchive
            .getDescriptorFileAsJson()
            .get("configuration")
            .get("input_folder")
            .textValue());
        task.setOutputFolder(
            uploadTaskArchive
            .getDescriptorFileAsJson()
            .get("configuration")
            .get("output_folder")
            .textValue());

        // resources
        JsonNode resources =
            uploadTaskArchive.getDescriptorFileAsJson().get("configuration").get("resources");

        if (!Objects.nonNull(resources)) {
            task.setRam(defaultRam);
            task.setCpus(defaultCpus);
        } else {
            task.setRam(resources.path("ram").asText(defaultRam));
            task.setCpus(resources.path("cpus").asInt(defaultCpus));
            task.setGpus(resources.path("gpus").asInt(0));
        }

        task.setAuthors(getAuthors(uploadTaskArchive));
        task.setInputs(getInputs(uploadTaskArchive));
        task.setOutputs(getOutputs(uploadTaskArchive, task.getInputs()));

        log.info("UploadTask: saving task...");
        taskRepository.save(task);
        log.info("UploadTask: task saved");

        return Optional.of(makeTaskDescription(task));
    }

    private Set<Input> getInputs(UploadTaskArchive uploadTaskArchive) {
        log.info("UploadTask: getting inputs...");
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
                input.setType(TypeFactory.createType(inputValue, charset));

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
        log.info("UploadTask: successful inputs ");
        return inputs;
    }

    private Set<Output> getOutputs(UploadTaskArchive uploadTaskArchive, Set<Input> inputs) {
        log.info("UploadTask: getting outputs...");

        JsonNode outputsNode = uploadTaskArchive.getDescriptorFileAsJson().get("outputs");
        if (!outputsNode.isObject()) {
            return new HashSet<>();
        }

        Set<Output> outputs = new HashSet<>();
        Iterator<String> fieldNames = outputsNode.fieldNames();
        while (fieldNames.hasNext()) {
            String outputKey = fieldNames.next();
            JsonNode inputValue = outputsNode.get(outputKey);

            Output output = new Output();
            output.setName(outputKey);
            output.setDisplayName(inputValue.get("display_name").textValue());
            output.setDescription(inputValue.get("description").textValue());
            // use type factory to generate the correct type
            output.setType(TypeFactory.createType(inputValue, charset));

            JsonNode dependencies = inputValue.get("dependencies");
            if (dependencies != null && dependencies.isObject()) {
                JsonNode derivedFrom = dependencies.get("derived_from");
                String inputName = derivedFrom.textValue().substring("inputs/".length());
                inputs.stream()
                .filter(input -> input.getName().equals(inputName))
                .findFirst().ifPresent(output::setDerivedFrom);
            }

            outputs.add(output);
        }

        log.info("UploadTask: successful outputs ");
        return outputs;
    }

    private Set<Author> getAuthors(UploadTaskArchive uploadTaskArchive) {
        log.info("UploadTask: getting authors...");
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
        log.info("UploadTask: successful authors ");
        return authors;
    }

    private void validateTaskBundle(UploadTaskArchive uploadTaskArchive)
        throws ValidationException {
        taskValidationService.validateDescriptorFile(uploadTaskArchive);
        taskValidationService.checkIsNotDuplicate(uploadTaskArchive);
        taskValidationService.validateImage(uploadTaskArchive);
    }

    private TaskIdentifiers generateTaskIdentifiers(
        UploadTaskArchive uploadTaskArchive) {
        UUID taskLocalIdentifier = UUID.randomUUID();
        String storageIdentifier = "task-" + taskLocalIdentifier + "-def";
        String imageIdentifierFromDescriptor =
            uploadTaskArchive.getDescriptorFileAsJson().get("namespace").textValue();
        String version = uploadTaskArchive.getDescriptorFileAsJson().get("version").textValue();
        String imageRegistryCompliantName = imageIdentifierFromDescriptor.replace(".", "/");
        imageRegistryCompliantName += ":" + version;

        return new TaskIdentifiers(taskLocalIdentifier,
            storageIdentifier,
            imageRegistryCompliantName);
    }

    public StorageData retrieveYmlDescriptor(String namespace, String version)
        throws TaskServiceException, TaskNotFoundException {
        log.info("Storage : retrieving descriptor.yml...");
        Task task = taskRepository.findByNamespaceAndVersion(namespace, version);
        if (task == null) {
            throw new TaskNotFoundException("task not found");
        }

        StorageData file = new StorageData("descriptor.yml", task.getStorageReference());
        try {
            file = fileStorageHandler.readStorageData(file);
        } catch (FileStorageException ex) {
            log.debug("Storage: failed to get file from storage [{}]", ex.getMessage());
            throw new TaskServiceException(ex);
        }
        return file;
    }

    public StorageData retrieveYmlDescriptor(String id)
        throws TaskServiceException, TaskNotFoundException {
        log.info("Storage : retrieving descriptor.yml...");
        Optional<Task> task = taskRepository.findById(UUID.fromString(id));
        if (task.isEmpty()) {
            throw new TaskNotFoundException("task not found");
        }
        StorageData file = new StorageData("descriptor.yml", task.get().getStorageReference());
        try {
            file = fileStorageHandler.readStorageData(file);
        } catch (FileStorageException ex) {
            log.debug("Storage: failed to get file from storage [{}]", ex.getMessage());
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
        TaskDescription taskDescription =
            new TaskDescription(
            task.getIdentifier(),
            task.getName(),
            task.getNamespace(),
            task.getVersion(),
            task.getDescription());
        Set<TaskAuthor> descriptionAuthors = new HashSet<>();
        for (Author author : task.getAuthors()) {
            TaskAuthor taskAuthor =
                new TaskAuthor(
                author.getFirstName(),
                author.getLastName(),
                author.getOrganization(),
                author.getEmail(),
                author.isContact());
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
        log.info("tasks: retrieving tasks...");
        List<Task> taskList = taskRepository.findAll();
        log.info("tasks: retrieved tasks");
        return taskList;
    }

    public Optional<Task> findById(String id) {
        log.info("Data: retrieving task...");
        Optional<Task> task = taskRepository.findById(UUID.fromString(id));
        log.info("Data: retrieved task");
        return task;
    }

    public Optional<Task> findByNamespaceAndVersion(String namespace, String version) {
        log.info("tasks/{namespace}/{version}: retrieving task...");
        Task task = taskRepository.findByNamespaceAndVersion(namespace, version);
        log.info("tasks/{namespace}/{version}: retrieved task...");
        return Optional.ofNullable(task);
    }

    @Transactional
    public TaskRun createRunForTask(String namespace, String version)
        throws RunTaskServiceException {
        log.info("tasks/{namespace}/{version}/runs: creating run...");
        // find associated task
        log.info("tasks/{namespace}/{version}/runs: retrieving associated task...");
        Task task = taskRepository.findByNamespaceAndVersion(namespace, version);

        // update task to have a new task run
        UUID taskRunID = UUID.randomUUID();
        if (task == null) {
            throw new RunTaskServiceException(
                "task {" + namespace + ":" + version + "} not found to associate with this run");
        }
        if (task.getInputs().isEmpty()) {
            throw new RunTaskServiceException("task {"
                + namespace + ":" + version + "} has no inputs");
        }
        log.info("tasks/{namespace}/{version}/runs: retrieved task...");
        Run run = new Run(taskRunID, TaskRunState.CREATED, task);
        runRepository.saveAndFlush(run);
        // create a storage for the inputs and outputs
        createRunStorages(taskRunID);
        // build response dto
        log.info("tasks/{id}/runs: run created...");
        return new TaskRun(taskRunID, makeTaskDescription(task), TaskRunState.CREATED);
    }

    @Transactional
    public TaskRun createRunForTask(String taskId) throws RunTaskServiceException {
        log.info("tasks/{id}/runs : creating run...");
        // find associated task
        log.info("tasks/{namespace}/{version}/runs : retrieving associated task...");
        Optional<Task> task = taskRepository.findById(UUID.fromString(taskId));
        // update task to have a new task run
        UUID taskRunID = UUID.randomUUID();
        if (task.isEmpty()) {
            throw new RunTaskServiceException(
                "task {" + taskId + "} not found to associate with this run");
        }
        if (task.get().getInputs().isEmpty()) {
            throw new RunTaskServiceException("task {" + taskId + "} has no inputs");
        }
        log.info("tasks/{namespace}/{version}/runs : retrieved task...");
        Run run = new Run(taskRunID, TaskRunState.CREATED, task.get(), LocalDateTime.now());
        runRepository.saveAndFlush(run);
        // create a storage for the inputs and outputs
        createRunStorages(taskRunID);
        // build response dto
        log.info("tasks/{id}/runs : run created...");
        return new TaskRun(taskRunID, makeTaskDescription(task.get()), TaskRunState.CREATED);
    }

    private void createRunStorages(UUID taskRunID) throws RunTaskServiceException {
        String inputStorageIdentifier = "task-run-inputs-" + taskRunID;
        String outputsStorageIdentifier = "task-run-outputs-" + taskRunID;
        Storage inputStorage = new Storage(inputStorageIdentifier);
        Storage outputStorage = new Storage(outputsStorageIdentifier);
        try {
            fileStorageHandler.createStorage(inputStorage);
            fileStorageHandler.createStorage(outputStorage);
            log.info("tasks/{namespace}/{version}/runs: Storage is created for task");
        } catch (FileStorageException e) {
            log.error("tasks/{namespace}/{version}/runs: failed to create storage [{}]",
                e.getMessage());
            throw new RunTaskServiceException(e);
        }
    }
}
