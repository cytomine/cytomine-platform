package be.cytomine.appengine.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import be.cytomine.appengine.dto.responses.errors.details.ParameterError;
import be.cytomine.appengine.exceptions.ProvisioningException;
import be.cytomine.appengine.handlers.StorageDataType;
import be.cytomine.appengine.handlers.StorageStringEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItemFactory;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.inputs.task.TaskAuthor;
import be.cytomine.appengine.dto.inputs.task.TaskDescription;
import be.cytomine.appengine.dto.inputs.task.TaskInput;
import be.cytomine.appengine.dto.inputs.task.TaskInputFactory;
import be.cytomine.appengine.dto.inputs.task.TaskOutput;
import be.cytomine.appengine.dto.inputs.task.TaskOutputFactory;
import be.cytomine.appengine.dto.inputs.task.TaskRun;
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
import be.cytomine.appengine.models.CheckTime;
import be.cytomine.appengine.models.Match;
import be.cytomine.appengine.models.task.Author;
import be.cytomine.appengine.models.task.Parameter;
import be.cytomine.appengine.models.task.ParameterType;
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
    public Optional<TaskDescription> uploadTask(HttpServletRequest request)
        throws BundleArchiveException, TaskServiceException, ValidationException
    {

        // prepare for streaming
        log.info("UploadTask: preparing streaming...");
        InputStream inputStream = prepareStream(request);
        String descriptorFileYmlContent = "";
        JsonNode descriptorFileAsJson = null;
        log.info("UploadTask: Task identifiers generated ");
        UUID taskLocalIdentifier = UUID.randomUUID();
        String storageIdentifier = "task-" + taskLocalIdentifier + "-def";
        Storage storage = new Storage(storageIdentifier);
        String imageRegistryCompliantName = null;
        log.info("UploadTask: building archive...");
//        if (archiveUtils.isZip(inputStream)) {
            log.info("UploadTask: extracting descriptor and Docker image from archive...");


            boolean descriptorFile = true;
            boolean dockerImageFile = true;

            try (ZipArchiveInputStream zais = new ZipArchiveInputStream(inputStream)) {
                ZipEntry entry;

                while ((entry = zais.getNextZipEntry()) != null) {
                    String entryName = entry.getName();

                    if (entryName.toLowerCase().matches("descriptor\\.(yml|yaml)")) {
                        descriptorFileYmlContent = IOUtils.toString(zais, StandardCharsets.UTF_8);
                        log.info("UploadTask: Descriptor file read into memory");
                        try {
                            log.info("UploadTask: validating descriptor file...");
                            descriptorFileAsJson = new ObjectMapper(new YAMLFactory()).readTree(descriptorFileYmlContent);
                            taskValidationService.validateDescriptorFile(descriptorFileAsJson);
                            taskValidationService.checkIsNotDuplicate(descriptorFileAsJson);
                            log.info("UploadTask: Descriptor file validated");
                        } catch (ValidationException e) {
                            log.info("UploadTask: Descriptor file not valid");
                            if (imageRegistryCompliantName == null) {
                                throw e;
                            } else {
                                try {
                                    log.info("UploadTask: deleting image registry...");
                                    registryHandler.deleteImage(imageRegistryCompliantName);
                                    log.info("UploadTask: image deleted");
                                } catch (RegistryException ex) {
                                    log.debug("UploadTask: failed to delete image from registry");
                                }
                            }
                        }
                        log.info("UploadTask: Descriptor file extracted");
                    }

                    if (entryName.endsWith(".tar")) {
                        String fullName = entryName
                            .substring(0, entryName.length() - 4);
                        String namespace = fullName
                            .substring(0, fullName.indexOf("-"))
                            .replace('.' , '/');
                        String version = fullName
                            .substring(fullName.indexOf('-') + 1);
                        imageRegistryCompliantName = namespace + ":" + version;
                        try {
                            log.info("UploadTask: pushing docker image to registry...");
                            registryHandler.pushImage(zais, imageRegistryCompliantName);
                            log.info("UploadTask: Docker image pushed");
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
                        }
                    }

                }
            } catch (IOException e) {
                log.error("UploadTask: Failed to extract files from archive: " + imageRegistryCompliantName, e);
                throw new BundleArchiveException(ErrorBuilder.build(ErrorCode.INTERNAL_DESCRIPTOR_EXTRACTION_FAILED)); // pick a general error
            }

            if (!descriptorFile) {
                log.error("UploadTask: Descriptor file not found in archive: " + imageRegistryCompliantName);
                throw new BundleArchiveException(ErrorBuilder.build(ErrorCode.INTERNAL_DESCRIPTOR_NOT_IN_DEFAULT_LOCATION));
            }

            if (!dockerImageFile) {
                log.error("UploadTask: Docker image not found in archive");
                throw new BundleArchiveException(ErrorBuilder.build(ErrorCode.INTERNAL_DOCKER_IMAGE_TAR_NOT_FOUND));
            }
//        } else {
//            log.error("UploadTask: Unknown bundle archive format");
//            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_UNKNOWN_BUNDLE_ARCHIVE_FORAMT);
//            throw new BundleArchiveException(error);
//        }



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
                storage,
                new StorageData(new StorageStringEntry(descriptorFileYmlContent, "descriptor.yml", StorageDataType.FILE))
            );
            log.info("UploadTask: descriptor.yml is stored in storage");
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
        // save task info
        Task task = new Task();
        task.setIdentifier(taskLocalIdentifier);
        task.setStorageReference(storageIdentifier);
        task.setImageName(imageRegistryCompliantName);
        task.setName(descriptorFileAsJson.get("name").textValue());
        task.setNameShort(descriptorFileAsJson
            .get("name_short")
            .textValue());
        task.setDescriptorFile(
            descriptorFileAsJson.get("namespace").textValue());
        task.setNamespace(descriptorFileAsJson.get("namespace").textValue());
        task.setVersion(descriptorFileAsJson.get("version").textValue());
        task.setInputFolder(
            descriptorFileAsJson
            .get("configuration")
            .get("input_folder")
            .textValue());
        task.setOutputFolder(
            descriptorFileAsJson
            .get("configuration")
            .get("output_folder")
            .textValue());

        // resources
        JsonNode resources =
            descriptorFileAsJson.get("configuration").get("resources");

        if (!Objects.nonNull(resources)) {
            task.setRam(defaultRam);
            task.setCpus(defaultCpus);
        } else {
            task.setRam(resources.path("ram").asText(defaultRam));
            task.setCpus(resources.path("cpus").asInt(defaultCpus));
            task.setGpus(resources.path("gpus").asInt(0));
        }

        task.setAuthors(getAuthors(descriptorFileAsJson));
        task.setParameters(getParameters(descriptorFileAsJson));
        task.setMatches(getMatches(descriptorFileAsJson, task.getParameters()));

        log.info("UploadTask: saving task...");
        taskRepository.save(task);
        log.info("UploadTask: task saved");

        return Optional.of(makeTaskDescription(task));
    }

    private List<Match> getMatches(JsonNode descriptor, Set<Parameter> parameters) {
        log.info("UploadTask: looking for matches...");
        JsonNode inputsNode = descriptor.get("inputs");
        JsonNode outputsNode = descriptor.get("outputs");
        List<Match> matches = new ArrayList<>();

        // Process dependencies for inputs
        processDependencies(inputsNode, parameters, matches);

        // Process dependencies for outputs
        processDependencies(outputsNode, parameters, matches);

        log.info("UploadTask: matches processed successfully");
        return matches;
    }

    private void processDependencies(
        JsonNode node,
        Set<Parameter> parameters,
        List<Match> matches) {
        if (node != null && node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                JsonNode value = node.get(key);
                parameters.stream()
                    .filter(parameter -> parameter.getName().equals(key))
                    .findFirst()
                    .ifPresent(parameter -> processParameterDependencies(
                    parameter,
                    value,
                    parameters,
                    matches));
            }
        }
    }


    private void processParameterDependencies(
        Parameter param,
        JsonNode value,
        Set<Parameter> parameters,
        List<Match> matches) {
        JsonNode dependencies = value.get("dependencies");
        if (dependencies != null && dependencies.isObject()) {
            JsonNode matching = dependencies.get("matching");
            if (matching != null && matching.isArray()) {
                for (JsonNode element : matching) {
                    String text = element.textValue();
                    int slashIndex = text.indexOf("/");
                    if (slashIndex == -1) {
                        continue; // skip unexpected format
                    }

                    String matchingType = text.substring(0, slashIndex);
                    String matchingName = text.substring(slashIndex + 1);

                    parameters.stream()
                        .filter(p -> p.getName()
                        .equals(matchingName) && p.getParameterType()
                        .equals(ParameterType.from(matchingType)))
                        .findFirst()
                        .ifPresent(other -> {
                            // set check time relative to execution
                            CheckTime when = CheckTime.UNDEFINED;
                            boolean bothInputs = param
                                .getParameterType()
                                .equals(ParameterType.INPUT)
                                && matchingType.equalsIgnoreCase("inputs");
                            if (bothInputs) {
                                when = CheckTime.BEFORE_EXECUTION;
                            }
                            boolean bothOutputs = param
                                .getParameterType()
                                .equals(ParameterType.OUTPUT)
                                && matchingType.equalsIgnoreCase("outputs");
                            boolean crossMatch = (param
                                .getParameterType()
                                .equals(ParameterType.INPUT)
                                && matchingType.equalsIgnoreCase("outputs"))
                                || param.getParameterType().equals(ParameterType.OUTPUT)
                                && matchingType.equalsIgnoreCase("inputs");
                            if (bothOutputs || crossMatch) {
                                when = CheckTime.AFTER_EXECUTION;
                            }

                            matches.add(new Match(param, other, when));
                        });

                }
            }
        }
    }

    private Set<Parameter> getParameters(JsonNode descriptor) {
        log.info("UploadTask: getting inputs...");
        Set<Parameter> parameters = new HashSet<>();
        JsonNode inputsNode = descriptor.get("inputs");

        log.info("UploadTask: getting outputs...");
        JsonNode outputsNode = descriptor.get("outputs");

        if (!inputsNode.isObject() && !outputsNode.isObject()) {
            return new HashSet<>();
        }

        if (inputsNode.isObject()) {
            Iterator<String> fieldNames = inputsNode.fieldNames();
            while (fieldNames.hasNext()) {
                String inputKey = fieldNames.next();
                JsonNode inputValue = inputsNode.get(inputKey);

                Parameter input = new Parameter();
                input.setName(inputKey);
                input.setDisplayName(inputValue.get("display_name").textValue());
                input.setDescription(inputValue.get("description").textValue());
                // use type factory to generate the correct type
                input.setType(TypeFactory.createType(inputValue, charset));
                input.setParameterType(ParameterType.INPUT);
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

                parameters.add(input);
            }
        }

        log.info("UploadTask: successful input parameters");

        Iterator<String> outputFieldNames = outputsNode.fieldNames();
        while (outputFieldNames.hasNext()) {
            String outputKey = outputFieldNames.next();
            JsonNode outputValue = outputsNode.get(outputKey);

            Parameter output = new Parameter();
            output.setName(outputKey);
            output.setDisplayName(outputValue.get("display_name").textValue());
            output.setDescription(outputValue.get("description").textValue());
            output.setParameterType(ParameterType.OUTPUT);
            // use type factory to generate the correct type
            output.setType(TypeFactory.createType(outputValue, charset));

            JsonNode dependencies = outputValue.get("dependencies");
            if (dependencies != null && dependencies.isObject()) {
                JsonNode derivedFrom = dependencies.get("derived_from");
                String inputName = derivedFrom.textValue().substring("inputs/".length());
                parameters.stream()
                    .filter(parameter -> parameter.getName().equals(inputName)
                        && parameter.getParameterType().equals(ParameterType.INPUT))
                    .findFirst().ifPresent(output::setDerivedFrom);
            }

            parameters.add(output);
        }

        log.info("UploadTask: successful output parameters ");
        return parameters;
    }

    private Set<Author> getAuthors(JsonNode descriptor) {
        log.info("UploadTask: getting authors...");
        Set<Author> authors = new HashSet<>();
        JsonNode authorNode = descriptor.get("authors");
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
        task
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .forEach(parameter -> inputs.add(TaskInputFactory.createTaskInput(parameter)));
        return inputs;
    }

    public List<TaskOutput> makeTaskOutputs(Task task) {
        List<TaskOutput> outputs = new ArrayList<>();
        task
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.OUTPUT))
            .forEach(parameter -> outputs.add(TaskOutputFactory.createTaskOutput(parameter)));

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
        Set<Parameter> taskInputParameters = task
            .getParameters()
            .stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .collect(Collectors.toSet());

        if (taskInputParameters.isEmpty()) {
            throw new RunTaskServiceException("task {"
                + namespace + ":" + version + "} has no inputs");
        }
        log.info("tasks/{namespace}/{version}/runs: retrieved task...");
        Run run = new Run(taskRunID, TaskRunState.CREATED, task, String.valueOf(UUID.randomUUID()));
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
        Set<Parameter> taskInputParameters = task.get()
            .getParameters()
            .stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .collect(Collectors.toSet());

        if (taskInputParameters.isEmpty()) {
            throw new RunTaskServiceException("task {" + taskId + "} has no inputs");
        }
        log.info("tasks/{namespace}/{version}/runs : retrieved task...");
        Run run = new Run(taskRunID, TaskRunState.CREATED, task.get(), LocalDateTime.now());
        run.setSecret(String.valueOf(UUID.randomUUID()));
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

    public InputStream prepareStream(HttpServletRequest request)
        throws TaskServiceException
    {
        log.info("UploadTask streaming: streaming...");
        if (!JakartaServletFileUpload.isMultipartContent(request)) {
            log.info("UploadTask streaming: not multipart");
            AppEngineError error = ErrorBuilder.build(
                ErrorCode.INTERNAL_NOT_MULTIPART);
            throw new TaskServiceException(error);
        }

        FileItemFactory<DiskFileItem> factory = DiskFileItemFactory.builder().get();
        JakartaServletFileUpload<DiskFileItem, FileItemFactory<DiskFileItem>> upload = new JakartaServletFileUpload<>(factory);

        try {
            // Get an iterator for the multipart parts. This avoids parsing the whole request at once.
            FileItemInputIterator iter = upload.getItemIterator(request);

            // Check if there's at least one part in the request
            if (!iter.hasNext()) {
                log.info("UploadTask streaming: No file parts found in the request body");
                AppEngineError error = ErrorBuilder.build(
                    ErrorCode.INTERNAL_NO_FILE_PARTS_FOUND
                );
                throw new TaskServiceException(error);
            }

            // Get the first part. We assume this is the single file we're interested in.
            FileItemInput item = iter.next();

            // Validate that the first part is indeed a file and not a simple form field
            if (item.isFormField()) {
                log.warn("UploadTask streaming: Expected a file but the first part is a form field: {}", item.getFieldName());
                AppEngineError error = ErrorBuilder.build(
                    ErrorCode.INTERNAL_NO_FILE_BUT_FORM_FIELD
                );
                throw new TaskServiceException(error);
            }

            // --- Stream the file content directly to the target File ---
            InputStream uploadedStream = item.getInputStream();
            log.info("UploadTask streaming: stream ready");

            return uploadedStream;
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("provisioning streaming: failed to stream input file {}", e.getMessage());
            AppEngineError error = ErrorBuilder.build(
                ErrorCode.INTERNAL_SERVER_ERROR
            );
            throw new TaskServiceException(error);
        }
    }
}
