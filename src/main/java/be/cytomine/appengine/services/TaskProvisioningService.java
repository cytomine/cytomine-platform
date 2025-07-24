package be.cytomine.appengine.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import be.cytomine.appengine.models.task.Checksum;
import be.cytomine.appengine.repositories.ChecksumRepository;
import be.cytomine.appengine.utils.FileHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.multipart.MultipartFile;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.handlers.scheduler.Schedule;
import be.cytomine.appengine.dto.inputs.task.GenericParameterCollectionItemProvision;
import be.cytomine.appengine.dto.inputs.task.GenericParameterProvision;
import be.cytomine.appengine.dto.inputs.task.Resource;
import be.cytomine.appengine.dto.inputs.task.State;
import be.cytomine.appengine.dto.inputs.task.StateAction;
import be.cytomine.appengine.dto.inputs.task.TaskDescription;
import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.TaskRunResponse;
import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.dto.responses.errors.details.ParameterError;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.exceptions.ProvisioningException;
import be.cytomine.appengine.exceptions.SchedulingException;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageDataEntry;
import be.cytomine.appengine.handlers.StorageDataType;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.CheckTime;
import be.cytomine.appengine.models.Match;
import be.cytomine.appengine.models.task.Parameter;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.models.task.collection.CollectionPersistence;
import be.cytomine.appengine.models.task.collection.CollectionType;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TypePersistenceRepository;
import be.cytomine.appengine.repositories.collection.CollectionPersistenceRepository;
import be.cytomine.appengine.states.TaskRunState;



@Slf4j
@RequiredArgsConstructor
@Service
public class TaskProvisioningService {

    private final TypePersistenceRepository typePersistenceRepository;

    private final CollectionPersistenceRepository collectionPersistenceRepository;

    private final RunRepository runRepository;

    private final StorageHandler fileStorageHandler;

    private final SchedulerHandler schedulerHandler;

    private final TaskService taskService;

    private final ChecksumRepository checksumRepository;

    @Value("${storage.base-path}")
    private String basePath;

    public JsonNode provisionRunParameter(
        String runId,
        String name,
        Object value
    ) throws ProvisioningException {
        log.info("ProvisionParameter: finding associated task run...");
        Run run = getRunIfValid(runId);
        log.info("ProvisionParameter: found");

        log.info("ProvisionParameter: validating provision against parameter type definition...");
        GenericParameterProvision genericParameterProvision = new GenericParameterProvision();

        if (value instanceof JsonNode) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                genericParameterProvision = mapper.treeToValue(
                    (JsonNode) value,
                    GenericParameterProvision.class
                );
            } catch (JsonProcessingException e) {
                log.info("ProvisionParameter: provision is not valid");
                AppEngineError error = ErrorBuilder.build(
                    ErrorCode.INTERNAL_JSON_PROCESSING_ERROR,
                    new ParameterError(name)
                );
                throw new ProvisioningException(error);
            }
        } else if (value instanceof File) {
            genericParameterProvision.setParameterName(name);
            genericParameterProvision.setValue(value);
        }

        genericParameterProvision.setRunId(runId);

        try {
            validateProvisionValuesAgainstTaskType(genericParameterProvision, run);
        } catch (TypeValidationException e) {
            AppEngineError error = ErrorBuilder.build(
                e.getErrorCode(),
                new ParameterError(name)
            );
            throw new ProvisioningException(error);
        }
        log.info("ProvisionParameter: provision is valid");

        JsonNode provision = null;
        if (value instanceof JsonNode) {
            provision = (JsonNode) value;
            log.info("ProvisionParameter: storing provision to storage...");
            saveProvisionInStorage(name, provision, run);
            log.info("ProvisionParameter: stored");
        } else if (value instanceof File) {
            ObjectNode objectNode = (new ObjectMapper()).createObjectNode();
            objectNode.put("param_name", name);
            objectNode.put("value", ((File) value).getAbsolutePath());
            provision = objectNode;
        }

        log.info("ProvisionParameter: saving provision in database...");
        saveInDatabase(name, provision, run);
        log.info("ProvisionParameter: saved");

        changeStateToProvisioned(run);

        return getInputParameterType(name, run)
            .createInputProvisioningEndpointResponse(provision, run);
    }

    private void changeStateToProvisioned(Run run) {
        log.info("ProvisionParameter: Changing run state to PROVISIONED...");
        Set<Parameter> inputParameters = run.getTask()
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .collect(Collectors.toSet());

        List<TypePersistence> persistenceList = typePersistenceRepository
            .findTypePersistenceByRunIdAndParameterTypeAndParameterNameIn(run.getId(),
            ParameterType.INPUT,
            inputParameters.stream().map(Parameter::getName).toList());
        boolean allParametersAreChecked = inputParameters.size() == persistenceList.size();
        if (allParametersAreChecked
            && persistenceList.stream().allMatch(TypePersistence::isProvisioned)) {
            run.setState(TaskRunState.PROVISIONED);
            runRepository.saveAndFlush(run);
            log.info("ProvisionParameter: RUN PROVISIONED");
        } else {
            log.info("ProvisionParameter: RUN NOT PROVISIONED");
        }

    }

    public List<JsonNode> provisionMultipleRunParameters(
        String runId,
        List<JsonNode> provisions
    ) throws ProvisioningException {
        log.info("ProvisionMultipleParameter: finding associated task run...");
        Run run = getRunIfValid(runId);
        log.info("ProvisionMultipleParameter: found");
        log.info("ProvisionMultipleParameter: handling provision list");
        // prepare an error list just in case
        List<AppEngineError> multipleErrors = new ArrayList<>();
        for (JsonNode provision : provisions) {
            GenericParameterProvision genericParameterProvision = new GenericParameterProvision();
            try {
                ObjectMapper mapper = new ObjectMapper();
                genericParameterProvision = mapper.treeToValue(
                    provision,
                    GenericParameterProvision.class
                );
                genericParameterProvision.setRunId(runId);
                log.info(
                    "ProvisionMultipleParameter: "
                    + "validating provision against parameter type definition..."
                );

                validateProvisionValuesAgainstTaskType(genericParameterProvision, run);
            } catch (TypeValidationException e) {
                log.info(
                    "ProvisionMultipleParameter: "
                    + "provision is invalid value validation failed"
                );
                ParameterError parameterError = new ParameterError(
                    genericParameterProvision.getParameterName()
                );
                AppEngineError error = ErrorBuilder.build(e.getErrorCode(), parameterError);
                multipleErrors.add(error);
                continue;
            } catch (JsonProcessingException e) {
                log.info(
                    "ProvisionMultipleParameter: "
                    + "provision is not invalid json processing failed"
                );
                ParameterError parameterError = new ParameterError(
                    genericParameterProvision.getParameterName()
                );
                AppEngineError error = ErrorBuilder.build(
                    ErrorCode.INTERNAL_JSON_PROCESSING_ERROR,
                    parameterError
                );
                multipleErrors.add(error);
                continue;
            }
            log.info("ProvisionMultipleParameter: provision is valid");
        }
        if (!multipleErrors.isEmpty()) {
            AppEngineError error = ErrorBuilder.buildBatchError(multipleErrors);
            throw new ProvisioningException(error);
        }

        List<JsonNode> response = new ArrayList<>();
        for (JsonNode provision : provisions) {
            log.info("ProvisionMultipleParameter: storing provision to storage...");
            String parameterName = provision.get("param_name").asText();
            try {
                saveProvisionInStorage(parameterName, provision, run);
            } catch (ProvisioningException e) {
                multipleErrors.add(e.getError());
                continue;
            }
            log.info("ProvisionMultipleParameter: stored");
            log.info("ProvisionMultipleParameter: saving provision in database...");
            saveInDatabase(parameterName, provision, run);
            log.info("ProvisionMultipleParameter: saved");
            JsonNode responseItem = getInputParameterType(parameterName, run)
                .createInputProvisioningEndpointResponse(provision, run);
            response.add(responseItem);
        }
        if (!multipleErrors.isEmpty()) {
            AppEngineError error = ErrorBuilder.buildBatchError(multipleErrors);
            throw new ProvisioningException(error);
        }

        changeStateToProvisioned(run);

        log.info("ProvisionMultipleParameter: return collection");
        return response;
    }

    @NotNull
    private void saveInDatabase(String parameterName, JsonNode provision, Run run)
        throws ProvisioningException {
        Parameter inputForType = getParameter(parameterName, ParameterType.INPUT, run);

        inputForType
            .getType()
            .persistProvision(provision, run.getId());
    }

    private Type getInputParameterType(String parameterName, Run run) {
        Parameter inputForType = getParameter(parameterName, ParameterType.INPUT, run);

        return inputForType.getType();
    }

    private void saveProvisionInStorage(
        String parameterName,
        JsonNode provision,
        Run run
    ) throws ProvisioningException {
        Parameter inputForType = getParameter(parameterName, ParameterType.INPUT, run);

        Storage runStorage = new Storage("task-run-inputs-" + run.getId());

        try {
            StorageData inputProvisionFileData = inputForType
                .getType()
                .mapToStorageFileData(provision, run);
            for (StorageDataEntry current : inputProvisionFileData.getEntryList()) {
                String filename = current.getName();
                if (!"descriptor.yml".equalsIgnoreCase(filename) && current.getStorageDataType() != StorageDataType.DIRECTORY) {
                    setChecksumCRC32(runStorage.getIdStorage(), calculateFileCRC32(current.getData()), filename);
                }
            }

            fileStorageHandler.saveStorageData(runStorage, inputProvisionFileData);
        } catch (FileStorageException | IOException e) {
            AppEngineError error = ErrorBuilder.buildParamRelatedError(
                ErrorCode.STORAGE_STORING_INPUT_FAILED,
                provision.get("param_name").asText(),
                e.getMessage()
            );
            throw new ProvisioningException(error);
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

    public Parameter getParameter(String parameterName, ParameterType parameterType, Run run) {
        return run
            .getTask()
            .getParameters()
            .stream()
            .filter(param -> param.getName().equalsIgnoreCase(parameterName)
                && param.getParameterType().equals(parameterType))
            .findFirst()
            .orElse(null);
    }

    private void validateProvisionValuesAgainstTaskType(
        GenericParameterProvision provision,
        Run run
    ) throws TypeValidationException {
        Task task = run.getTask();
        Set<Parameter> inputs = task
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .collect(Collectors.toSet());

        boolean inputFound = false;
        for (Parameter parameter : inputs) {
            if (parameter.getName().equalsIgnoreCase(provision.getParameterName())) {
                inputFound = true;
                parameter.getType().validate(provision.getValue());
            }
        }
        if (!inputFound) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_DOES_NOT_EXIST);
        }
    }

    private void validateProvisionValuesAgainstTaskType(
        GenericParameterCollectionItemProvision provision,
        Run run,
        String[] indexesArray
    ) throws TypeValidationException {
        Task task = run.getTask();
        Set<Parameter> inputs = task
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .collect(Collectors.toSet());

        boolean inputFound = false;
        for (Parameter parameter : inputs) {
            if (parameter.getName().equalsIgnoreCase(provision.getParameterName())) {
                inputFound = true;
                if (provision.getValue() instanceof ArrayList<?>) {
                    Type nestedValidationType = validateParents(parameter, indexesArray);
                    nestedValidationType.validate(provision.getValue());
                } else {
                    parameter.getType().validate(provision.getValue());
                }

            }
        }
        if (!inputFound) {
            throw new TypeValidationException(
                "unknown parameter ["
                    + provision.getParameterName()
                    + "], not found in task descriptor"
            );
        }
    }

    private Type validateParents(
        Parameter parameter,
        String[] indexes)
        throws TypeValidationException {
        int validationDepth = indexes.length - 1;
        CollectionType type = (CollectionType) parameter.getType();
        for (int i = 0; i <= validationDepth; i++) {
            if (Objects.isNull(type.getTrackingType())) {
                type.setTrackingType(new CollectionType(type));
                type.setParentType(type.getTrackingType());
            } else {
                if (type.getTrackingType() instanceof CollectionType) {
                    type.setParentType(type.getTrackingType());
                    type.setTrackingType(((CollectionType) type.getTrackingType()).getSubType());
                }
            }
            // validate index against
            int index = Integer.parseInt(indexes[i]);
            if (index > ((CollectionType) type.getParentType()).getMaxSize() - 1
                || index < 0) {
                throw new TypeValidationException("invalid index [{" + index + "}]");
            }
        }
        return type;
    }

    public void retrieveIOZipArchive(
        String runId,
        ParameterType type,
        ServletOutputStream outputStream
    ) throws ProvisioningException, FileStorageException, IOException {
        log.info("Retrieving IO Archive: retrieving...");
        Run run = getRunIfValid(runId);
        TaskRunState state = run.getState();

        if ((type == ParameterType.INPUT && state.equals(TaskRunState.CREATED))
            || (type == ParameterType.OUTPUT && !state.equals(TaskRunState.FINISHED))) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }

        log.info("Retrieving IO Archive: fetching from storage...");
        @SuppressWarnings("checkstyle:lineLength")
        List<TypePersistence> provisions = typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(run.getId(), type);
        if (provisions.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_PROVISIONS_NOT_FOUND);
            throw new ProvisioningException(error);
        }

        log.info("Retrieving IO Archive: zipping...");

        String io = type.equals(ParameterType.INPUT) ? "inputs" : "outputs";
//        Path tempFile = Files.createTempFile(io + "-archive-", runId);
        ZipOutputStream zipOut = new ZipOutputStream(outputStream);
        for (TypePersistence provision : provisions) {
            // check that this type persistence is actually associated with a parameter
            Parameter parameter = getParameter(
                provision.getParameterName(),
                provision.getParameterType(),
                run);
            if (Objects.isNull(parameter)) {
                continue;
            }
            StorageData provisionFileData = fileStorageHandler.readStorageData(
                new StorageData(provision.getParameterName(), "task-run-" + io + "-" + run.getId())
            );

            for (StorageDataEntry current : provisionFileData.getEntryList()) {
                String entryName;
                if (current.getStorageDataType().equals(StorageDataType.FILE)) {
                    entryName = current.getName();
                } else {
                    entryName = current.getName() + "/";
                }

                if (current.getStorageDataType().equals(StorageDataType.FILE)) {
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zipEntry.setMethod(ZipEntry.STORED);
                    zipEntry.setSize(current.getData().length());
                    zipEntry.setCompressedSize(current.getData().length());
                    zipEntry.setCrc(getChecksumCRC32("task-run-" + io + "-" + run.getId(), current.getName()));
                    zipOut.putNextEntry(zipEntry);
                    Files.copy(current.getData().toPath(), zipOut);
                }

                zipOut.closeEntry();
            }

        }
        zipOut.close();

        log.info("Retrieving IO Archive: zipped...");

//        return new StorageData(tempFile.toFile());
    }

    public long getChecksumCRC32(String identifier, String name) throws FileStorageException
    {

        String reference = identifier + "-" + name;
        Checksum crc32 = checksumRepository.findByReference(reference);
        return crc32.getChecksumCRC32();
    }

    public void setChecksumCRC32(String identifier, long checksumCRC32, String name)
    {
        String reference = identifier + "-" + name;
        Checksum crc32 = new Checksum(UUID.randomUUID(), reference, checksumCRC32);
        checksumRepository.save(crc32);
    }

    public List<TaskRunParameterValue> postOutputsZipArchive(
        String runId,
        String secret,
        InputStream outputsInputStream
    ) throws ProvisioningException {
        log.info("Posting Outputs Archive: posting...");
        Run run = getRunIfValid(runId);
        if (!run.getSecret().equals(secret)) {
            AppEngineError error = ErrorBuilder
                .build(ErrorCode.SCHEDULER_UNAUTHENTICATED_OUTPUT_PROVISIONING);
            throw new ProvisioningException(error);
        }
        if (notInOneOfSchedulerManagedStates(run)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }
        Set<Parameter> runTaskOutputs = run
            .getTask()
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.OUTPUT))
            .collect(Collectors.toSet());

        log.info("Posting Outputs Archive: unzipping...");
        try {
            List<TaskRunParameterValue> outputList = processOutputFiles(
                outputsInputStream,
                runTaskOutputs,
                run
            );
            run.setState(TaskRunState.FINISHED);
            runRepository.saveAndFlush(run);
            log.info("Posting Outputs Archive: updated Run state to FINISHED");
            return outputList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<TaskRunParameterValue> processOutputFiles(
        InputStream outputsInputStream,
        Set<Parameter> runTaskOutputs,
        Run run
    ) throws IOException, ProvisioningException {
        // read files from the archive
        try (ZipArchiveInputStream zais = new ZipArchiveInputStream(outputsInputStream)) {
            log.info("Posting Outputs Archive: unzipped");
            List<Parameter> remainingOutputs = new ArrayList<>(runTaskOutputs);
            List<TaskRunParameterValue> taskRunParameterValues = new ArrayList<>();
            List<StorageData> contentsOfZip = new ArrayList<>();
            List<Parameter> remainingUnStoredOutputs = new ArrayList<>(runTaskOutputs);
            boolean remainingOutputsAreCollections = true;
            ZipEntry ze;
            while ((ze = zais.getNextZipEntry()) != null) {
                // look for output matching file name
                Parameter currentOutput = null;
                for (int i = 0; i < remainingOutputs.size(); i++) {
                    currentOutput = remainingOutputs.get(i);
                    if (currentOutput.getName().equals(ze.getName())) { // assuming it's a file
                        remainingOutputs.remove(i);
                        StorageData parameterZipEntryStorageData;
                        String outputName = currentOutput.getName(); //
                        Path filePath = Path.of(basePath, "/", "task-run-outputs-" + run.getId(), outputName);
                        log.info("Posting Outputs Archive: storing {} in storage...", currentOutput);
                        Files.copy(zais, filePath, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Posting Outputs Archive: stored ");
                        parameterZipEntryStorageData = new StorageData(
                            filePath.toFile(),
                            outputName);
                        // calculate CRC32 for all storage data entry and save it in the database
                        log.info("Posting Outputs Archive: calculating CRC32 checksum for zip entry {}"
                            , ze.getName());
                        calculateCRC32Checksum(run, contentsOfZip, parameterZipEntryStorageData);
                        break;
                    }
                    // assuming it's the main directory of a collection parameter
                    if (ze.getName().startsWith(currentOutput.getName() + "/")) {
                        StorageData partialParameterZipEntryStorageData;
                        if (ze.isDirectory()) {
                            partialParameterZipEntryStorageData = new StorageData(ze.getName());
                            contentsOfZip.add(partialParameterZipEntryStorageData);
                            log.info("Posting Outputs Archive: creating directory {} in storage...", ze.getName());
                            Path directoryPath = Path.of(basePath, "/", "task-run-outputs-" + run.getId(), ze.getName());
                            log.info("Posting Outputs Archive: created");
                            Files.createDirectories(directoryPath);
                        } else {
                            Path filePath = Path.of(basePath, "/", "task-run-outputs-" + run.getId(), ze
                                .getName()
                                .replace("/", ""));
                            Files.createDirectories(filePath);
                            log.info("Posting Outputs Archive: storing {} in storage...", ze.getName());
                            Files.copy(zais, filePath, StandardCopyOption.REPLACE_EXISTING);
                            log.info("Posting Outputs Archive: stored ");
                            partialParameterZipEntryStorageData = new StorageData(
                                filePath.toFile(),
                                ze.getName());
                            // calculate CRC32 for all storage data entry and save it in the database
                            log.info("Posting Outputs Archive: calculating CRC32 checksum for zip entry {}"
                                , ze.getName());
                            calculateCRC32Checksum(run, contentsOfZip, partialParameterZipEntryStorageData);
                        }

                        break;
                    }

                    currentOutput = null;
                }

                // there's a file that does not match any output parameter
                if (currentOutput == null) {
                    AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_UNKNOWN_OUTPUT);
                    log.info("Posting Outputs Archive: output invalid (unknown output)");
                    run.setState(TaskRunState.FAILED);
                    runRepository.saveAndFlush(run);
                    log.info("Posting Outputs Archive: updated Run state to FAILED");
                    throw new ProvisioningException(error);
                }

            }

            // check if remaining outputs are all collections
            int numberOfCollections = remainingOutputs
                .stream()
                .filter(parameter -> parameter.getType() instanceof CollectionType)
                .toList()
                .size();
            remainingOutputsAreCollections = numberOfCollections > 0
                && numberOfCollections == remainingOutputs.size();

            if (!remainingOutputs.isEmpty() && !remainingOutputsAreCollections) {
                AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_MISSING_OUTPUTS);
                log.info("Posting Outputs Archive: output invalid (missing outputs)");
                run.setState(TaskRunState.FAILED);
                runRepository.saveAndFlush(run);
                log.info("Posting Outputs Archive: updated Run state to FAILED");
                throw new ProvisioningException(error);
            }

            // a compaction step
            // order by the length of name
            // to make sure deeper files and directories are merged first
            contentsOfZip = contentsOfZip
                .stream()
                .sorted((s1, s2) -> Integer.compare(
                    s2.peek().getName().length(),
                    s1.peek().getName().length())
                )
                .toList(); // now immutable
            List<StorageData> sortedContentsOfZip = new CopyOnWriteArrayList<>(contentsOfZip);
            // merge StorageData objects together
            for (StorageData storageData : sortedContentsOfZip) {
                for (StorageData compared : sortedContentsOfZip) {
                    if (storageData.equals(compared)) {
                        continue;
                    }
                    if (compared
                        .peek()
                        .getName()
                        .startsWith(storageData.peek().getName())) {
                        storageData.merge(compared);
                        sortedContentsOfZip.remove(compared);
                    }
                }
            }

            // prepare an error list just in case
            List<AppEngineError> multipleErrors = new ArrayList<>();

            // processing of files
            for (Parameter currentOutput : remainingUnStoredOutputs) {
                Optional<StorageData> currentOutputStorageDataOptional = sortedContentsOfZip
                    .stream()
                    .filter(s -> s
                    .peek()
                    .getName()
                    .equals(
                    currentOutput.getType() instanceof CollectionType
                    ? currentOutput.getName() + "/" : currentOutput.getName()))
                    .findFirst();
                StorageData currentOutputStorageData = null;
                if (currentOutputStorageDataOptional.isPresent()) {
                    currentOutputStorageData = currentOutputStorageDataOptional.get();
                }
                // read the file
                String outputName = currentOutput.getName();
                // validate files/directories contents and structure
                try {
                    validateFiles(run, currentOutput, currentOutputStorageData);
                } catch (TypeValidationException e) {
                    // todo : delete all outputs of the run in case of validation errors
                    log.info(
                        "ProcessOutputFiles: "
                        + "output provision is invalid value validation failed"
                    );
                    ParameterError parameterError = new ParameterError(outputName);
                    AppEngineError error = ErrorBuilder.build(e.getErrorCode(), parameterError);
                    multipleErrors.add(error);
                    run.setState(TaskRunState.FAILED);
                    runRepository.saveAndFlush(run);
                    log.info("Posting Outputs Archive: updated Run state to FAILED because of invalid output");
                    try (Stream<Path> paths = Files.walk(Paths.get(basePath, "/", "task-run-outputs-" + run.getId()))) {
                        paths.sorted(Comparator.reverseOrder()) // Delete children before parent
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException ignored) {}
                            });
                    } catch (IOException ignored) {}
                    break;
                }
                // saving to the database does not care about the type
                saveOutput(run, currentOutput, currentOutputStorageData);
                // based on parsed type build the response
                taskRunParameterValues.add(currentOutput
                    .getType()
                    .createOutputProvisioningEndpointResponse(
                    currentOutputStorageData,
                    run.getId(),
                    outputName)
                );
            }

            multipleErrors.addAll(checkAfterExecutionMatches(run));

            // throw multiple errors if exist
            if (!multipleErrors.isEmpty()) {
                AppEngineError error = ErrorBuilder.buildBatchError(multipleErrors);
                throw new ProvisioningException(error);
            }

            log.info("Posting Outputs Archive: posted");
            return taskRunParameterValues;
        }
    }

    private void calculateCRC32Checksum(Run run, List<StorageData> contentsOfZip,
                                        StorageData partialParameterZipEntryStorageData)
        throws ProvisioningException
    {
        for (StorageDataEntry current : partialParameterZipEntryStorageData.getEntryList()) {
            try {
                setChecksumCRC32("task-run-outputs-" + run.getId(), calculateFileCRC32(current.getData()), current.getName());
            } catch (IOException e) {
                AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_CRC32_CALC_FAILED);
                throw new ProvisioningException(error);
            }
        }
        contentsOfZip.add(partialParameterZipEntryStorageData);
    }

    private void validateFiles(
        Run run,
        Parameter currentOutput,
        StorageData currentOutputStorageData
    ) throws TypeValidationException {
        log.info("Posting Outputs Archive: "
            + "validating files and directories contents and structure...");
        currentOutput.getType().validateFiles(run, currentOutput, currentOutputStorageData);
        log.info("Posting Outputs Archive: validated finished...");
    }

    private void storeOutputInFileStorage(
        Run run,
        StorageData outputFileData,
        String name
    ) throws ProvisioningException {
        log.info("Posting Outputs Archive: storing in file storage...");
        Storage outputsStorage = new Storage("task-run-outputs-" + run.getId());
        try {
            fileStorageHandler.saveStorageData(outputsStorage, outputFileData);
        } catch (FileStorageException e) {
            run.setState(TaskRunState.FAILED);
            runRepository.saveAndFlush(run);
            log.info("Posting Outputs Archive: updated Run state to FAILED");
            AppEngineError error = ErrorBuilder.buildParamRelatedError(
                ErrorCode.STORAGE_STORING_INPUT_FAILED,
                name,
                e.getMessage()
            );
            throw new ProvisioningException(error);
        }
        log.info("Posting Outputs Archive: stored");
    }

    private void saveOutput(Run run, Parameter currentOutput, StorageData outputValue)
        throws ProvisioningException {
        log.info("Posting Outputs Archive: saving...");
        currentOutput.getType().persistResult(run, currentOutput, outputValue);
        log.info("Posting Outputs Archive: saved...");
    }

    private static boolean notInOneOfSchedulerManagedStates(Run run) {
        return !run.getState().equals(TaskRunState.RUNNING)
            && !run.getState().equals(TaskRunState.PENDING)
            && !run.getState().equals(TaskRunState.QUEUED)
            && !run.getState().equals(TaskRunState.QUEUING);
    }

    public List<TaskRunParameterValue> retrieveRunOutputs(
        String runId
    ) throws ProvisioningException {
        log.info("Retrieving Outputs Json: retrieving...");
        // validate run
        Run run = getRunIfValid(runId);
        if (!run.getState().equals(TaskRunState.FINISHED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }
        // find all the results
        List<TaskRunParameterValue> outputList = buildTaskRunParameterValues(
            run,
            ParameterType.OUTPUT
        );
        log.info("Retrieving Outputs Json: retrieved");
        return outputList;
    }

    public Run getRunIfValid(String runId) throws ProvisioningException {
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }
        // check the state is valid
        return runOptional.get();
    }

    public List<TaskRunParameterValue> retrieveRunInputs(
        String runId
    ) throws ProvisioningException {
        log.info("Retrieving Inputs: retrieving...");
        // validate run
        Run run = getRunIfValid(runId);
        if (run.getState().equals(TaskRunState.CREATED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }
        // find all the results
        List<TaskRunParameterValue> inputList = buildTaskRunParameterValues(
            run,
            ParameterType.INPUT
        );
        log.info("Retrieving Inputs: retrieved");
        return inputList;
    }

    public File retrieveSingleRunIO(
        String runId,
        String parameterName,
        ParameterType type
    ) throws ProvisioningException {
        log.info("Get IO file from storage: searching...");

        String io = type.equals(ParameterType.INPUT) ? "inputs" : "outputs";
        Storage storage = new Storage("task-run-" + io + "-" + runId);
        StorageData data = new StorageData(parameterName, storage.getIdStorage());

        log.info("Get IO file from storage: read file " + parameterName + " from storage...");
        try {
            data = fileStorageHandler.readStorageData(data);
        } catch (FileStorageException e) {
            AppEngineError error = ErrorBuilder.buildParamRelatedError(
                ErrorCode.STORAGE_READING_FILE_FAILED,
                parameterName,
                e.getMessage()
            );
            throw new ProvisioningException(error);
        }

        log.info("Get IO file from storage: done");
        return data.peek().getData();
    }

    public File retrieveSingleRunCollectionItemIO(
        String runId,
        String parameterName,
        ParameterType type,
        String[] indexes
    ) throws ProvisioningException {
        log.info("Get IO file from storage: searching...");

        String io = type.equals(ParameterType.INPUT) ? "inputs" : "outputs";
        Storage storage = new Storage("task-run-" + io + "-" + runId);
        String collectionItem = parameterName
            + "/"
            + Arrays.stream(indexes).sequential().collect(Collectors.joining("/"));
        StorageData data = new StorageData(collectionItem, storage.getIdStorage());

        log.info("Get IO file from storage: read file " + collectionItem + " from storage...");
        try {
            data = fileStorageHandler.readStorageData(data);
        } catch (FileStorageException e) {
            AppEngineError error = ErrorBuilder.buildParamRelatedError(
                ErrorCode.STORAGE_READING_FILE_FAILED,
                parameterName,
                e.getMessage()
            );
            throw new ProvisioningException(error);
        }

        log.info("Get IO file from storage: done");
        return data.peek().getData();
    }

    private List<TaskRunParameterValue> buildTaskRunParameterValues(Run run, ParameterType type)
        throws ProvisioningException {
        List<TaskRunParameterValue> parameterValues = new ArrayList<>();
        @SuppressWarnings("checkstyle:LineLength")
        List<TypePersistence> results = typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(run.getId(), type);
        if (type.equals(ParameterType.INPUT)) {
            Set<Parameter> inputs = run
                .getTask()
                .getParameters()
                .stream()
                .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
                .collect(Collectors.toSet());

            for (TypePersistence result : results) {
                // based on the type of the parameter assign the type
                Optional<Parameter> inputForTypeOptional = inputs
                    .stream()
                    .filter(parameter -> parameter
                    .getName()
                    .equalsIgnoreCase(result.getParameterName()))
                    .findFirst();
                if (inputForTypeOptional.isPresent()) {
                    parameterValues.add(inputForTypeOptional
                        .get()
                        .getType()
                        .createOutputProvisioningEndpointResponse(result));
                }

            }
        } else {
            Set<Parameter> outputs = run
                .getTask()
                .getParameters()
                .stream()
                .filter(parameter -> parameter.getParameterType().equals(ParameterType.OUTPUT))
                .collect(Collectors.toSet());

            for (TypePersistence result : results) {
                // based on the type of the parameter assign the type
                Optional<Parameter> outputForTypeOptional = outputs
                    .stream()
                    .filter(parameter -> parameter
                    .getName()
                    .equalsIgnoreCase(result.getParameterName()))
                    .findFirst();
                if (outputForTypeOptional.isPresent()) {
                    parameterValues
                        .add(outputForTypeOptional
                        .get()
                        .getType()
                        .createOutputProvisioningEndpointResponse(result));
                }

            }
        }

        return parameterValues;
    }

    public StateAction updateRunState(
        String runId,
        State state
    ) throws SchedulingException, ProvisioningException, FileStorageException
    {
        log.info("Update State: validating Run...");
        Run run = getRunIfValid(runId);

        return switch (state.getDesired()) {
            case PROVISIONED -> updateToProvisioned(run);
            case RUNNING -> run(run);
            case FINISHED -> updateToFinished(run);
            // to safeguard against unknown state transition requests
            default -> throw new ProvisioningException(ErrorBuilder.build(ErrorCode.UNKNOWN_STATE));
        };
    }

    private StateAction createStateAction(Run run, TaskRunState state) {
        StateAction action = new StateAction();
        action.setStatus("success");

        TaskDescription description = taskService.makeTaskDescription(run.getTask());
        Resource resource = new Resource(
            run.getId(),
            description,
            state,
            new Date(),
            new Date(),
            new Date()
        );
        action.setResource(resource);

        return action;
    }

    @NotNull
    private StateAction run(Run run) throws ProvisioningException, SchedulingException {
        log.info("Running Task: scheduling...");

        AppEngineError error = null;
        switch (run.getState()) {
            case CREATED:
                error = ErrorBuilder.build(ErrorCode.INTERNAL_NOT_PROVISIONED);
                throw new ProvisioningException(error);
            case PROVISIONED:
                break;
            default:
                error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
                throw new ProvisioningException(error);
        }
        log.info("Running Task: valid run");


        checkBeforeExecutionMatches(run);

        log.info("Running Task: contacting scheduler...");
        Schedule schedule = new Schedule();
        schedule.setRun(run);
        schedulerHandler.schedule(schedule);
        log.info("Running Task: scheduling done");

        // update the final state
        run.setState(TaskRunState.QUEUING);
        runRepository.saveAndFlush(run);
        log.info("Running Task: updated Run state to QUEUING");

        StateAction action = createStateAction(run, TaskRunState.QUEUING);
        log.info("Running Task: scheduled");

        return action;
    }

    private void checkBeforeExecutionMatches(Run run) throws ProvisioningException {
        AppEngineError error;
        List<Match> matches = run.getTask().getMatches().stream().filter(match ->
            match.getCheckTime().equals(CheckTime.BEFORE_EXECUTION)).toList();

        if (!matches.isEmpty()) {
            for (Match match : matches) {
                CollectionPersistence matching = collectionPersistenceRepository
                    .findCollectionPersistenceByParameterNameAndRunIdAndParameterType(match
                    .getMatching()
                    .getName(), run.getId(), ParameterType.INPUT);
                CollectionPersistence matched = collectionPersistenceRepository
                    .findCollectionPersistenceByParameterNameAndRunIdAndParameterType(match
                    .getMatched()
                    .getName(), run.getId(), ParameterType.INPUT);
                // compare size
                if (!Objects.equals(matching.getSize(), matched.getSize())) {
                    error = ErrorBuilder.build(ErrorCode.INTERNAL_NOT_MATCHING_DIFF_SIZE);
                    throw new ProvisioningException(error);
                }
                // map indexes
                for (TypePersistence item : matching.getItems()) {
                    String matchingItemIndex = item
                        .getCollectionIndex()
                        .substring(item.getCollectionIndex().lastIndexOf('['));
                    List<TypePersistence> matchedItemIndexes = matched
                        .getItems()
                        .stream()
                        .filter(typePersistence -> typePersistence
                        .getCollectionIndex()
                        .endsWith(matchingItemIndex))
                        .toList();
                    if (matchedItemIndexes.size() != 1) {
                        error = ErrorBuilder
                            .build(ErrorCode.INTERNAL_NOT_MATCHING_NOT_ALIGNED_INDEXES);
                        throw new ProvisioningException(error);
                    }
                }
            }
        }
    }

    private List<AppEngineError> checkAfterExecutionMatches(Run run) throws ProvisioningException {
        List<AppEngineError> multipleErrors = new ArrayList<>();
        List<Match> matches = run.getTask().getMatches().stream().filter(match ->
            match.getCheckTime().equals(CheckTime.AFTER_EXECUTION)).toList();

        if (!matches.isEmpty()) {
            for (Match match : matches) {
                CollectionPersistence matching = collectionPersistenceRepository
                    .findCollectionPersistenceByParameterNameAndRunId(match
                    .getMatching()
                    .getName(), run.getId());
                CollectionPersistence matched = collectionPersistenceRepository
                    .findCollectionPersistenceByParameterNameAndRunId(match
                    .getMatched()
                    .getName(), run.getId());
                // compare size
                if (!Objects.equals(matching.getSize(), matched.getSize())) {
                    ParameterError parameterError = new ParameterError(matching.getParameterName());
                    AppEngineError error = ErrorBuilder
                        .build(ErrorCode.INTERNAL_NOT_MATCHING_DIFF_SIZE, parameterError);
                    multipleErrors.add(error);
                }
                // map indexes
                for (TypePersistence item : matching.getItems()) {
                    String matchingItemIndex = item
                        .getCollectionIndex()
                        .substring(item.getCollectionIndex().lastIndexOf('['));
                    List<TypePersistence> matchedItemIndexes = matched
                        .getItems()
                        .stream()
                        .filter(typePersistence -> typePersistence
                        .getCollectionIndex()
                        .endsWith(matchingItemIndex))
                        .toList();
                    if (matchedItemIndexes.size() != 1) {
                        ParameterError parameterError = new ParameterError(
                            matching.getParameterName()
                        );
                        AppEngineError error = ErrorBuilder
                            .build(ErrorCode
                            .INTERNAL_NOT_MATCHING_NOT_ALIGNED_INDEXES, parameterError);
                        multipleErrors.add(error);
                    }
                }
            }
        }
        return multipleErrors;
    }

    private StateAction updateToProvisioned(Run run) throws ProvisioningException {
        log.info("Provisioning: update state to PROVISIONED...");
        if (!run.getState().equals(TaskRunState.CREATED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }

        changeStateToProvisioned(run);

        log.info("Provisioning: state updated to PROVISIONED");

        return createStateAction(run, TaskRunState.PROVISIONED);
    }

    private StateAction updateToFinished(Run run) throws ProvisioningException, FileStorageException
    {
        // check the status of Run it should be Queueing
        log.info("Provisioning: processing outputs...");
        if (!run.getState().equals(TaskRunState.QUEUING)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }
        // list all outputs of the task
        Set<Parameter> outputs = run
            .getTask()
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.OUTPUT))
            .collect(Collectors.toSet());
        log.info("Provisioning: reading outputs...");
        List<AppEngineError> multipleErrors = new ArrayList<>();
        for (Parameter parameter : outputs) {
            StorageData provisionFileData = fileStorageHandler.readStorageData(
                new StorageData(parameter.getName(), "task-run-outputs-" + run.getId())
            );
            if (provisionFileData == null || provisionFileData.getEntryList().isEmpty()) {
                AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_MISSING_OUTPUT_FILE_FOR_PARAMETER);
                throw new ProvisioningException(error);
            }

            // validate files
            log.info("Provisioning: validating output files...");
            try {
                validateFiles(run, parameter, provisionFileData);
            } catch (TypeValidationException e) {
                log.info(
                    "Provisioning: "
                        + "output provision is invalid value validation failed"
                );
                ParameterError parameterError = new ParameterError(parameter.getName());
                AppEngineError error = ErrorBuilder.build(e.getErrorCode(), parameterError);
                multipleErrors.add(error);
                continue;
            }
            // store in the database
            log.info("Provisioning: storing output in database...");
            saveOutput(run, parameter, provisionFileData);

            // calculate CRC32 for all storage data entry and save it in the database
            log.info("Provisioning: calculating CRC32 checksum for zip entry {}"
                , parameter.getName());
            for (StorageDataEntry current : provisionFileData.getEntryList()) {
                try {
                    setChecksumCRC32("task-run-outputs-" + run.getId(), calculateFileCRC32(current.getData()), current.getName());
                } catch (IOException e) {
                    AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_CRC32_CALC_FAILED);
                    throw new ProvisioningException(error);
                }
            }

        }

        multipleErrors.addAll(checkAfterExecutionMatches(run));

        // throw multiple errors if exist
        if (!multipleErrors.isEmpty()) {
            log.info("Provisioning: state updated to FAILED");
            run.setState(TaskRunState.FAILED);
            runRepository.saveAndFlush(run);
            AppEngineError error = ErrorBuilder.buildBatchError(multipleErrors);
            throw new ProvisioningException(error);
        }

        log.info("Provisioning: state updated to FINISHED");
        run.setState(TaskRunState.FINISHED);
        runRepository.saveAndFlush(run);
        return createStateAction(run, TaskRunState.FINISHED);
    }

    public TaskRunResponse retrieveRun(String runId) throws ProvisioningException {
        log.info("Retrieving Run: retrieving...");
        Run run = getRunIfValid(runId);
        TaskDescription description = taskService.makeTaskDescription(run.getTask());
        log.info("Retrieving Run: retrieved");
        return new TaskRunResponse(
            UUID.fromString(runId),
            description,
            run.getState(),
            run.getCreatedAt(),
            run.getUpdatedAt(),
            run.getLastStateTransitionAt()
        );
    }

    public JsonNode provisionCollectionItem(
        String runId,
        String name,
        Object value,
        String[] indexesArray)
        throws ProvisioningException, TypeValidationException {
        // get run and make sure it is valid
        log.info("ProvisionCollectionItem: finding associated task run...");
        Run run = getRunIfValid(runId);
        log.info("ProvisionCollectionItem: found");

        // create the collection or get if valid
        Parameter parameter = getParameter(name, ParameterType.INPUT, run);
        if (Objects.isNull(parameter)) {
            log.info("ProvisionCollectionItem: parameter does not exist");
            AppEngineError error = ErrorBuilder.build(
                ErrorCode.INTERNAL_PARAMETER_DOES_NOT_EXIST,
                new ParameterError(name)
            );
            throw new ProvisioningException(error);
        }
        if (!(parameter.getType() instanceof CollectionType)) {
            log.info("ProvisionCollectionItem: parameter is not a collection ");
            AppEngineError error = ErrorBuilder.build(
                ErrorCode.INTERNAL_PARAMETER_TYPE_ERROR,
                new ParameterError(name)
            );
            throw new ProvisioningException(error);
        }

        log.info("ProvisionCollectionItem: preparing generic provision...");

        // prepare item as generic provision
        GenericParameterCollectionItemProvision
            genericParameterProvision = new GenericParameterCollectionItemProvision();

        if (value instanceof JsonNode) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                genericParameterProvision = mapper.treeToValue(
                    (JsonNode) value,
                    GenericParameterCollectionItemProvision.class
                );
                genericParameterProvision.setParameterName(name);
            } catch (JsonProcessingException e) {
                log.info("ProvisionCollectionItem: item provision is not valid");
                AppEngineError error = ErrorBuilder.build(
                    ErrorCode.INTERNAL_JSON_PROCESSING_ERROR,
                    new ParameterError(name)
                );
                throw new ProvisioningException(error);
            }
        } else if (value instanceof File) {
            genericParameterProvision.setParameterName(name);
            genericParameterProvision.setValue(value);
        }

        genericParameterProvision.setRunId(runId);
        log.info("ProvisionCollectionItem: generic provision prepared");

        log.info("ProvisionCollectionItem: validating item...");
        validateProvisionValuesAgainstTaskType(genericParameterProvision, run, indexesArray);
        log.info("ProvisionCollectionItem: item validated");

        JsonNode provision = null;
        if (value instanceof JsonNode) {
            provision = (JsonNode) value;
            ObjectNode objectNode = (new ObjectMapper()).createObjectNode();
            objectNode.put("index", name + "/" + String.join("/", indexesArray));
            objectNode.set("value", provision.get("value"));
            provision = objectNode;
            // store item in storage -> crawl indexes and store
            log.info("ProvisionCollectionItem: storing provision to storage...");
            saveProvisionInStorage(name, provision, run);
            log.info("ProvisionCollectionItem: stored");
        } else if (value instanceof File) {
            ObjectNode objectNode = (new ObjectMapper()).createObjectNode();
            objectNode.put("index", name + "/" + String.join("/", indexesArray));
            objectNode.put("value", ((File) value).getAbsolutePath());
            provision = objectNode;
        }

        // store item in the database
        log.info("ProvisionCollectionItem: validating item...");
        saveInDatabase(name, provision, run);
        log.info("ProvisionCollectionItem: item validated");

        changeStateToProvisioned(run);

        return getInputParameterType(name, run)
            .createInputProvisioningEndpointResponse(provision, run);
    }

    public Path prepareStreaming(
        String runId, String parameterName
    ) throws IOException
    {
        log.info("provisioning streaming: preparing...");
        Storage runStorage = new Storage("task-run-inputs-" + runId);
        Path filePath = Paths.get(basePath, runStorage.getIdStorage(), parameterName);
        Files.createDirectories(filePath.getParent());
        return filePath;
    }

    public Path prepareStreaming(
        String runId,
        String parameterName,
        String[] indexesArray
    ) throws IOException
    {
        log.info("provisioning collection item streaming: preparing...");
        Storage runStorage = new Storage("task-run-inputs-" + runId);
        Path filePath = Paths.get(basePath, runStorage.getIdStorage(), parameterName + "/" + String.join("/", indexesArray));
        Files.createDirectories(filePath.getParent());
        String[] arrayYmlPosition = Arrays.stream(indexesArray)
            .limit(indexesArray.length - 1)
            .toArray(String[]::new);
        String arrayYmlFilePath = parameterName + "/" + (arrayYmlPosition.length > 0 ? String.join("/", arrayYmlPosition) + "/" : "") + "array.yml";
        Path arrayYmlPath = Paths.get(basePath, runStorage.getIdStorage(), "/" + arrayYmlFilePath);
        if (Files.exists(arrayYmlPath)) {
            // update the array.yml metadata file
            log.info("provisioning collection item streaming: updating collection array.yml...");
            String content = FileHelper.read(arrayYmlPath.toFile(), Charset.defaultCharset());
            int size = Integer.parseInt(content.substring(content.indexOf(':') + 1).trim()) + 1;
            String newContent = "size: " + size;
            Files.delete(arrayYmlPath);
            Files.writeString(arrayYmlPath, newContent, Charset.defaultCharset());
            // update CRC32 checksum for it
            long updatedChecksum = calculateFileCRC32(arrayYmlPath.toFile());
            Checksum checksum = checksumRepository.findByReference(runStorage.getIdStorage() + "-" + arrayYmlFilePath);
            checksum.setChecksumCRC32(updatedChecksum);
            checksumRepository.saveAndFlush(checksum);
        } else {
            // create the array.yml metadata file
            log.info("provisioning collection item streaming: creating collection array.yml...");
            String newContent = "size: 1";
            Files.writeString(arrayYmlPath, newContent, Charset.defaultCharset());
            // create CRC32 checksum for it
            setChecksumCRC32(
                runStorage.getIdStorage(),
                calculateFileCRC32(arrayYmlPath.toFile()),
                arrayYmlFilePath);
        }
        return filePath;
    }

    public File streamToStorage(String parameterName, HttpServletRequest request, Path filePath)
        throws ProvisioningException
    {
        log.info("provisioning streaming: streaming...");
        if (!JakartaServletFileUpload.isMultipartContent(request)) {
            log.info("provisioning streaming: not multipart");
            AppEngineError error = ErrorBuilder.build(
                ErrorCode.INTERNAL_NOT_MULTIPART,
                new ParameterError(parameterName)
            );
            throw new ProvisioningException(error);
        }

        FileItemFactory<DiskFileItem> factory = DiskFileItemFactory.builder().get();
        JakartaServletFileUpload<DiskFileItem, FileItemFactory<DiskFileItem>> upload = new JakartaServletFileUpload<>(factory);
        File uploadedFile = new File(filePath.toAbsolutePath().toString());
        ;
        try {
            // Get an iterator for the multipart parts. This avoids parsing the whole request at once.
            FileItemInputIterator iter = upload.getItemIterator(request);

            // Check if there's at least one part in the request
            if (!iter.hasNext()) {
                log.info("provisioning streaming: No file parts found in the request body");
                AppEngineError error = ErrorBuilder.build(
                    ErrorCode.INTERNAL_NO_FILE_PARTS_FOUND,
                    new ParameterError(parameterName)
                );
                throw new ProvisioningException(error);
            }

            // Get the first part. We assume this is the single file we're interested in.
            FileItemInput item = iter.next();

            // Validate that the first part is indeed a file and not a simple form field
            if (item.isFormField()) {
                log.warn("provisioning streaming: Expected a file but the first part is a form field: {}", item.getFieldName());
                AppEngineError error = ErrorBuilder.build(
                    ErrorCode.INTERNAL_NO_FILE_BUT_FORM_FIELD,
                    new ParameterError(parameterName)
                );
                throw new ProvisioningException(error);
            }

            // --- Stream the file content directly to the target File ---
            try (InputStream uploadedStream = item.getInputStream();
                FileOutputStream fos = new FileOutputStream(uploadedFile)) {
                IOUtils.copyLarge(uploadedStream, fos); // Efficiently copy bytes from input to output
                log.info("provisioning streaming: file successfully streamed and saved ");
            }

        } catch (Exception e) {
            log.warn("provisioning streaming: failed to stream input file {}", e.getMessage());
            AppEngineError error = ErrorBuilder.build(
                ErrorCode.INTERNAL_SERVER_ERROR,
                new ParameterError(parameterName)
            );
            throw new ProvisioningException(error);
        }
        return uploadedFile;
    }

    public InputStream prepareStream(HttpServletRequest request)
        throws ProvisioningException
    {
        log.info("provisioning streaming: streaming...");
        if (!JakartaServletFileUpload.isMultipartContent(request)) {
            log.info("provisioning streaming: not multipart");
            AppEngineError error = ErrorBuilder.build(
                ErrorCode.INTERNAL_NOT_MULTIPART
            );
            throw new ProvisioningException(error);
        }

        FileItemFactory<DiskFileItem> factory = DiskFileItemFactory.builder().get();
        JakartaServletFileUpload<DiskFileItem, FileItemFactory<DiskFileItem>> upload = new JakartaServletFileUpload<>(factory);

        InputStream uploadedStream;
        try {
            // Get an iterator for the multipart parts. This avoids parsing the whole request at once.
            FileItemInputIterator iter = upload.getItemIterator(request);

            // Check if there's at least one part in the request
            if (!iter.hasNext()) {
                log.info("provisioning streaming: No file parts found in the request body");
                AppEngineError error = ErrorBuilder.build(
                    ErrorCode.INTERNAL_NO_FILE_PARTS_FOUND
                );
                throw new ProvisioningException(error);
            }

            // Get the first part. We assume this is the single file we're interested in.
            FileItemInput item = iter.next();

            // Validate that the first part is indeed a file and not a simple form field
            if (item.isFormField()) {
                log.warn("provisioning streaming: Expected a file but the first part is a form field: {}", item.getFieldName());
                AppEngineError error = ErrorBuilder.build(
                    ErrorCode.INTERNAL_NO_FILE_BUT_FORM_FIELD
                );
                throw new ProvisioningException(error);
            }

            // --- Stream the file content directly to the target File ---
           uploadedStream = item.getInputStream();

        } catch (Exception e) {
            log.warn("provisioning streaming: failed to stream input file {}", e.getMessage());
            AppEngineError error = ErrorBuilder.build(
                ErrorCode.INTERNAL_SERVER_ERROR
            );
            throw new ProvisioningException(error);
        }
        return uploadedStream;
    }
}
