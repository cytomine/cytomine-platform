package be.cytomine.appengine.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import be.cytomine.appengine.models.task.Parameter;
import be.cytomine.appengine.models.task.collection.CollectionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.handlers.scheduler.Schedule;
import be.cytomine.appengine.dto.inputs.task.GenericParameterProvision;
import be.cytomine.appengine.dto.inputs.task.Resource;
import be.cytomine.appengine.dto.inputs.task.State;
import be.cytomine.appengine.dto.inputs.task.StateAction;
import be.cytomine.appengine.dto.inputs.task.TaskAuthor;
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
import be.cytomine.appengine.models.task.Author;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TypePersistenceRepository;
import be.cytomine.appengine.states.TaskRunState;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskProvisioningService {

    private final TypePersistenceRepository typePersistenceRepository;

    private final RunRepository runRepository;

    private final StorageHandler fileStorageHandler;

    private final SchedulerHandler schedulerHandler;

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
        } else if (value instanceof File) {
            ObjectNode objectNode = (new ObjectMapper()).createObjectNode();
            objectNode.put("param_name", name);
            objectNode.put("value", ((File) value).getAbsolutePath());
            provision = objectNode;
        }

        log.info("ProvisionParameter: storing provision to storage...");
        saveProvisionInStorage(name, provision, run);
        log.info("ProvisionParameter: stored");

        log.info("ProvisionParameter: saving provision in database...");
        saveInDatabase(name, provision, run);
        log.info("ProvisionParameter: saved");

        Set<Parameter> inputParameters = run.getTask()
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .collect(Collectors.toSet());

        if (inputParameters.size() == 1) {
            changeStateToProvisioned(run);
        }

        return getInputParameterType(name, run).createTypedParameterResponse(provision, run);
    }

    private void changeStateToProvisioned(Run run) {
        log.info("ProvisionParameter: Changing run state to PROVISIONED...");
        run.setState(TaskRunState.PROVISIONED);
        runRepository.saveAndFlush(run);
        log.info("ProvisionParameter: RUN PROVISIONED");
    }

    public List<JsonNode> provisionMultipleRunParameters(
        String runId,
        List<JsonNode> provisions
    ) throws ProvisioningException {
        log.info("ProvisionMultipleParameter: finding associated task run...");
        Run run = getRunIfValid(runId);
        log.info("ProvisionMultipleParameter: found");
        log.info("ProvisionMultipleParameter: handling provision list");
        // prepare error list just in case
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
                .createTypedParameterResponse(provision, run);
            response.add(responseItem);
        }
        if (!multipleErrors.isEmpty()) {
            AppEngineError error = ErrorBuilder.buildBatchError(multipleErrors);
            throw new ProvisioningException(error);
        }

        Set<Parameter> inputParameters = run.getTask()
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .collect(Collectors.toSet());

        if (provisions.size() == inputParameters.size()) {
            changeStateToProvisioned(run);
        }
        log.info("ProvisionMultipleParameter: return collection");
        return response;
    }

    @NotNull
    private void saveInDatabase(String parameterName, JsonNode provision, Run run)
        throws ProvisioningException
    {
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
            StorageData inputProvisionFileData = inputForType.getType().mapToStorageFileData(provision);
            fileStorageHandler.saveStorageData(runStorage, inputProvisionFileData);
        } catch (FileStorageException e) {
            AppEngineError error = ErrorBuilder.buildParamRelatedError(
                ErrorCode.STORAGE_STORING_INPUT_FAILED,
                provision.get("param_name").asText(),
                e.getMessage()
            );
            throw new ProvisioningException(error);
        }
    }

    private static Parameter getParameter(String parameterName, ParameterType parameterType, Run run)
    {
        return run
            .getTask()
            .getParameters()
            .stream()
            .filter(param -> param.getName().equalsIgnoreCase(parameterName)
                && param.getParameterType().equals(parameterType))
            .findFirst()
            .orElse(null);
    }

    private static void validateProvisionValuesAgainstTaskType(
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
            throw new TypeValidationException(
                "unknown parameter ["
                + provision.getParameterName()
                + "], not found in task descriptor"
                );
        }
    }

    public StorageData retrieveIOZipArchive(
        String runId,
        ParameterType type
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
        Path tempFile = Files.createTempFile(io + "-archive-", runId);
        ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(tempFile));
        for (TypePersistence provision : provisions) {
            // check that this type persistence is actually associated with a parameter
            Parameter parameter = getParameter(provision.getParameterName(), provision.getParameterType(), run);
            if (Objects.isNull(parameter)) {
                continue;
            }
            StorageData provisionFileData = fileStorageHandler.readStorageData(
                new StorageData(provision.getParameterName(), "task-run-" + io + "-" + run.getId())
            );

            for (StorageDataEntry current : provisionFileData.getEntryList()) {
                String entryName ;
                if (current.getStorageDataType().equals(StorageDataType.FILE)) {
                    entryName = current.getName();
                } else {
                    entryName = current.getName() + "/";
                }
                ZipEntry zipEntry = new ZipEntry(entryName);
                zipOut.putNextEntry(zipEntry);

                if (current.getStorageDataType().equals(StorageDataType.FILE)) {
                    Files.copy(current.getData().toPath(), zipOut);
                }

                zipOut.closeEntry();
            }

        }
        zipOut.close();

        log.info("Retrieving IO Archive: zipped...");
        return new StorageData(tempFile.toFile());
    }

    public List<TaskRunParameterValue> postOutputsZipArchive(
        String runId,
        String secret,
        MultipartFile outputs
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

        new ArrayList<>();
        log.info("Posting Outputs Archive: unzipping...");
        try {
            List<TaskRunParameterValue> outputList = processOutputFiles(
                outputs,
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

    // This fuction should return a JsonNode object to give more freedom
    // to the type implementer to return complex types
    private List<TaskRunParameterValue> processOutputFiles(
        MultipartFile outputs,
        Set<Parameter> runTaskOutputs,
        Run run
    ) throws IOException, ProvisioningException {
        // read files from the archive
        try (ZipArchiveInputStream zais = new ZipArchiveInputStream(outputs.getInputStream())) {
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
                        String outputName = currentOutput.getName();
                        Path tempFile = Files.createTempFile(outputName, null);
                        Files.copy(zais, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        parameterZipEntryStorageData = new StorageData(tempFile.toFile(), outputName);
                        contentsOfZip.add(parameterZipEntryStorageData);
                        break;
                    }
                    // assuming it's the main directory of a collection parameter
                    if (ze.getName().startsWith(currentOutput.getName()+"/")) {
                        StorageData partialParameterZipEntryStorageData;
                        if (ze.isDirectory()) {
                            partialParameterZipEntryStorageData = new StorageData(ze.getName());
                            contentsOfZip.add(partialParameterZipEntryStorageData);
                        } else {
                            Path tempFile = Files.createTempFile(ze.getName().replace("/" , ""), null);
                            Files.copy(zais, tempFile, StandardCopyOption.REPLACE_EXISTING);
                            partialParameterZipEntryStorageData = new StorageData(tempFile.toFile(), ze.getName());
                            contentsOfZip.add(partialParameterZipEntryStorageData);
                        }

                        break;
                    }

                    //
                    currentOutput = null;
                }


                // there's a file that do not match any output parameter
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
            remainingOutputsAreCollections = numberOfCollections > 0 && numberOfCollections == remainingOutputs.size();

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

            // prepare error list just in case
            List<AppEngineError> multipleErrors = new ArrayList<>();

            // processing of files
            for (Parameter currentOutput : remainingUnStoredOutputs) {
                Optional<StorageData> currentOutputStorageDataOptional = sortedContentsOfZip
                    .stream()
                    .filter(s -> s
                    .peek()
                    .getName()
                    .equals(currentOutput.getType() instanceof CollectionType? currentOutput.getName()+"/":currentOutput.getName()))
                    .findFirst();
                StorageData currentOutputStorageData = null;
                if (currentOutputStorageDataOptional.isPresent()) {
                    currentOutputStorageData = currentOutputStorageDataOptional.get();
                }
                // read file
                String outputName = currentOutput.getName();
                // validate files/directories contents and structure
                try {
                    validateFiles(run, currentOutput, currentOutputStorageData);
                } catch (TypeValidationException e) {
                    log.info(
                        "ProcessOutputFiles: "
                        + "output provision is invalid value validation failed"
                    );
                    ParameterError parameterError = new ParameterError(outputName);
                    AppEngineError error = ErrorBuilder.build(e.getErrorCode(), parameterError);
                    multipleErrors.add(error);
                    continue;
                }
                // saving to database does not care about the type
                saveOutput(run, currentOutput, currentOutputStorageData);
                // saving to the storage does not care about the type
                storeOutputInFileStorage(run, currentOutputStorageData, outputName);
                // based on parsed type build the response
                taskRunParameterValues.add(currentOutput.getType().buildTaskRunParameterValue(
                    currentOutputStorageData,
                    run.getId(),
                    outputName)
                );
            }

            // throw multiple errors if exist
            if (!multipleErrors.isEmpty()) {
                AppEngineError error = ErrorBuilder.buildBatchError(multipleErrors);
                throw new ProvisioningException(error);
            }

            log.info("Posting Outputs Archive: posted");
            return taskRunParameterValues;
        }
    }

    private void validateFiles(Run run, Parameter currentOutput, StorageData currentOutputStorageData)
        throws TypeValidationException {
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
        throws ProvisioningException
    {
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

    private Run getRunIfValid(String runId) throws ProvisioningException {
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

    private List<TaskRunParameterValue> buildTaskRunParameterValues(Run run, ParameterType type)
        throws ProvisioningException
    {
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
                    .filter(parameter -> parameter.getName().equalsIgnoreCase(result.getParameterName()))
                    .findFirst();
                if (inputForTypeOptional.isPresent()) {
                    parameterValues.add(inputForTypeOptional.get().getType().buildTaskRunParameterValue(result));
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
                    .filter(parameter -> parameter.getName().equalsIgnoreCase(result.getParameterName()))
                    .findFirst();
                if (outputForTypeOptional.isPresent()) {
                    parameterValues.add(outputForTypeOptional.get().getType().buildTaskRunParameterValue(result));
                }

            }
        }

        return parameterValues;
    }

    public StateAction updateRunState(
        String runId,
        State state
    ) throws SchedulingException, ProvisioningException {
        log.info("Update State: validating Run...");
        Run run = getRunIfValid(runId);

        return switch (state.getDesired()) {
            case PROVISIONED -> updateToProvisioned(run);
            case RUNNING -> run(run);
            // to safeguard against unknown state transition requests
            default -> throw new ProvisioningException(ErrorBuilder.build(ErrorCode.UNKNOWN_STATE));
        };
    }

    private StateAction createStateAction(Run run, TaskRunState state) {
        StateAction action = new StateAction();
        action.setStatus("success");

        TaskDescription description = makeTaskDescription(run.getTask());
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

    public TaskDescription makeTaskDescription(Task task) {
        TaskDescription taskDescription = new TaskDescription(
            task.getIdentifier(),
            task.getName(),
            task.getNamespace(),
            task.getVersion(),
            task.getDescription()
        );
        Set<TaskAuthor> descriptionAuthors = new HashSet<>();
        for (Author author : task.getAuthors()) {
            TaskAuthor taskAuthor = new TaskAuthor(
                author.getFirstName(),
                author.getLastName(),
                author.getOrganization(),
                author.getEmail(),
                author.isContact()
            );
            descriptionAuthors.add(taskAuthor);
        }
        taskDescription.setAuthors(descriptionAuthors);
        return taskDescription;
    }

    public TaskRunResponse retrieveRun(String runId) throws ProvisioningException {
        log.info("Retrieving Run: retrieving...");
        Run run = getRunIfValid(runId);
        TaskDescription description = makeTaskDescription(run.getTask());
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
}
