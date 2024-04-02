package be.cytomine.appengine.services;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.handlers.scheduler.Schedule;
import be.cytomine.appengine.dto.inputs.task.*;
import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.dto.responses.errors.details.ParameterError;
import be.cytomine.appengine.exceptions.*;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.handlers.FileStorageHandler;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.repositories.ProvisionRepository;
import be.cytomine.appengine.repositories.ResultRepository;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.states.TaskRunState;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Service
@Slf4j
public class TaskProvisioningService {
    private final TaskRepository taskRepository;

    Logger logger = LoggerFactory.getLogger(TaskProvisioningService.class);

    private final ProvisionRepository provisionRepository;

    private final ResultRepository resultRepository;
    private final RunRepository runRepository;
    private final FileStorageHandler fileStorageHandler;
    private final TaskValidationService taskValidationService; // TODO move validation to validation service

    private SchedulerHandler schedulerHandler;
    @Value("${storage.input.charset}")
    private String charset;


    public TaskProvisioningService(SchedulerHandler schedulerHandler, ProvisionRepository provisionRepository, RunRepository runRepository, FileStorageHandler fileStorageHandler, TaskValidationService taskValidationService, ResultRepository resultRepository,
                                   TaskRepository taskRepository) {
        this.runRepository = runRepository;
        this.fileStorageHandler = fileStorageHandler;
        this.taskValidationService = taskValidationService;
        this.provisionRepository = provisionRepository;
        this.resultRepository = resultRepository;
        this.schedulerHandler = schedulerHandler;
        this.taskRepository = taskRepository;
    }

    public GenericParameterRunProvision provisionRunParameter(GenericParameterProvision provision) throws ProvisioningException {
        logger.info("ProvisionParameter : finding associated task run...");
        Run run = getRunOfProvision(provision);
        logger.info("ProvisionParameter : found");
        logger.info("ProvisionParameter : validating provision against parameter type definition...");
        try {
            validateProvisionValuesAgainstTaskType(provision, run);
        } catch (TypeValidationException e) {
            ParameterError parameterError = new ParameterError(provision.getParameterName());
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_PARAMETER_DOES_NOT_EXIST, parameterError);
            throw new ProvisioningException(error);
        }
        logger.info("ProvisionParameter : provision is valid");
        logger.info("ProvisionParameter : storing provision to storage...");
        saveProvisionInStorage(provision);
        logger.info("ProvisionParameter : stored");
        logger.info("ProvisionParameter : saving provision in database...");
        saveInDatabase(provision, run);
        logger.info("ProvisionParameter : saved");
        if (run.getTask().getInputs().size() == 1) {
            changeStateToProvisioned(run);
        }
        return new GenericParameterRunProvision(provision.getParameterName(), provision.getValue(), provision.getRunId().toString());

    }

    private void changeStateToProvisioned(Run run) {
        logger.info("ProvisionParameter : Changing run state to PROVISIONED...");
        run.setState(TaskRunState.PROVISIONED);
        runRepository.saveAndFlush(run);
        logger.info("ProvisionParameter : RUN PROVISIONED");
    }

    public List<GenericParameterRunProvision> provisionMultipleRunParameters(String runId, List<GenericParameterProvision> provisions) throws ProvisioningException {
        List<GenericParameterRunProvision> response = new ArrayList<>();
        logger.info("ProvisionMultipleParameter : finding associated task run...");
        Run run = getRunOfProvision(runId);
        logger.info("ProvisionMultipleParameter : found");
        logger.info("ProvisionMultipleParameter : handling provision list");
        // prepare error list just in case
        List<AppEngineError> multipleErrors = new ArrayList<>();
        for (GenericParameterProvision provision : provisions) {
            provision.setRunId(runId);
            logger.info("ProvisionMultipleParameter : validating provision against parameter type definition...");
            try {
                validateProvisionValuesAgainstTaskType(provision, run);
            } catch (TypeValidationException e) {
                logger.info("ProvisionMultipleParameter : provision is invalid");
                ParameterError parameterError = new ParameterError(provision.getParameterName());
                AppEngineError error = ErrorBuilder.build(e.getErrorCode(), parameterError);
                multipleErrors.add(error);
                continue;
            }
            logger.info("ProvisionMultipleParameter : provision is valid");
        }
        if (!multipleErrors.isEmpty()) {
            AppEngineError error = ErrorBuilder.buildBatchError(multipleErrors);
            throw new ProvisioningException(error);
        }

        for (GenericParameterProvision provision : provisions) {
            logger.info("ProvisionMultipleParameter : storing provision to storage...");
            try {
                saveProvisionInStorage(provision);
            } catch (ProvisioningException e) {
                multipleErrors.add(e.getError());
                continue;
            }
            logger.info("ProvisionMultipleParameter : stored");
            logger.info("ProvisionMultipleParameter : saving provision in database...");
            saveInDatabase(provision, run);
            logger.info("ProvisionMultipleParameter : saved");
            response.add(new GenericParameterRunProvision(provision.getParameterName(), String.valueOf(provision.getValue()), provision.getRunId().toString()));
        }
        if (!multipleErrors.isEmpty()) {
            AppEngineError error = ErrorBuilder.buildBatchError(multipleErrors);
            throw new ProvisioningException(error);
        }
        if (provisions.size() == run.getTask().getInputs().size()) {
            changeStateToProvisioned(run);
        }
        logger.info("ProvisionMultipleParameter : return collection");
        return response;
    }

    @NotNull
    private void saveInDatabase(GenericParameterProvision provision, Run run) {
        Provision persistedProvision = provisionRepository.findProvisionByParameterNameAndRunId(provision.getParameterName(), run.getId());
        if (persistedProvision == null) {
            persistedProvision = new Provision(provision.getParameterName(), provision.getValue(), run.getId());
            provisionRepository.save(persistedProvision);
        } else {
            persistedProvision.setValue(provision.getValue());
            provisionRepository.saveAndFlush(persistedProvision);
        }
    }

    private void saveProvisionInStorage(GenericParameterProvision provision) throws ProvisioningException {
        Storage runStorage = new Storage("task-run-inputs-" + provision.getRunId());
        String value = String.valueOf(provision.getValue());
        byte[] inputFileData = value.getBytes(getStorageCharset(charset));
        FileData inputProvisionFileData = new FileData(inputFileData, provision.getParameterName());
        try {
            fileStorageHandler.createFile(runStorage, inputProvisionFileData);
        } catch (FileStorageException e) {
            AppEngineError error = ErrorBuilder.buildParamRelatedError(ErrorCode.STORAGE_STORING_INPUT_FAILED, provision.getParameterName(), e.getMessage());
            throw new ProvisioningException(error);

        }
    }

    private static void validateProvisionValuesAgainstTaskType(GenericParameterProvision provision, Run run) throws TypeValidationException {
        Task task = run.getTask();
        Set<Input> inputs = task.getInputs();
        boolean inputFound = false;
        for (Input input : inputs) {
            if (input.getName().equalsIgnoreCase(provision.getParameterName())) {
                // input found
                inputFound = true;
                // determine input type
                if (input.getType() instanceof IntegerType type) {
                    type.validate(Integer.parseInt(provision.getValue()));
                }
                // TODO : add other types here
            }
        }
        if (!inputFound) {
            throw new TypeValidationException("unknown parameter [" + provision.getParameterName() + "], not found in task descriptor");
        }
    }

    @NotNull
    private Run getRunOfProvision(GenericParameterProvision provision) throws ProvisioningException {
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(provision.getRunId()));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }
        return runOptional.get();
    }

    @NotNull
    private Run getRunOfProvision(String runId) throws ProvisioningException {
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }

        return runOptional.get();
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


    public FileData retrieveInputsZipArchive(String runId) throws ProvisioningException, FileStorageException, IOException {
        logger.info("Retrieving Inputs Archive : retrieving...");
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }

        Run run = runOptional.get();
        if (run.getState().equals(TaskRunState.CREATED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }

        logger.info("Retrieving Inputs Archive : fetching from storage...");
        List<Provision> provisions = provisionRepository.findProvisionByRunId(runOptional.get().getId());
        if (provisions.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_PROVISIONS_NOT_FOUND);
            throw new ProvisioningException(error);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        logger.info("Retrieving Inputs Archive : zipping...");
        ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream);
        for (Provision provision : provisions) {
            FileData provisionFileData = fileStorageHandler.readFile(new FileData(provision.getParameterName(), "task-run-inputs-" + runOptional.get().getId()));
            ZipEntry zipEntry = new ZipEntry(provision.getParameterName());
            zipOut.putNextEntry(zipEntry);
            zipOut.write(provisionFileData.getFileData());
            zipOut.closeEntry();
        }
        zipOut.close();
        byteArrayOutputStream.close();
        logger.info("Retrieving Inputs Archive : zipped...");
        return new FileData(byteArrayOutputStream.toByteArray());
    }

    public FileData retrieveOutputsZipArchive(String runId) throws ProvisioningException, FileStorageException, IOException {
        logger.info("Retrieving Outputs Archive : retrieving...");
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }

        Run run = runOptional.get();
        if (!run.getState().equals(TaskRunState.FINISHED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }

        // fetch results from storage
        List<Result> results = resultRepository.findResultByRunId(runOptional.get().getId());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        logger.info("Retrieving Outputs Archive : zipping...");
        ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream);
        for (Result result : results) {
            FileData provision = fileStorageHandler.readFile(new FileData(result.getParameterName(), "task-run-outputs-" + runOptional.get().getId()));
            ZipEntry zipEntry = new ZipEntry(result.getParameterName());
            zipOut.putNextEntry(zipEntry);
            zipOut.write(provision.getFileData());
            zipOut.closeEntry();
        }
        zipOut.close();
        byteArrayOutputStream.close();
        logger.info("Retrieving Outputs Archive : zipped...");
        return new FileData(byteArrayOutputStream.toByteArray());
    }

    public List<TaskRunOutput> postOutputsZipArchive(String runId, MultipartFile outputs) throws ProvisioningException {
        logger.info("Posting Outputs Archive : posting...");
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }
        Run run = runOptional.get();
        if (notInOneOfSchedulerManagedStates(run)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }
        Set<Output> runTaskOutputs = run.getTask().getOutputs();
        new ArrayList<>();
        logger.info("Posting Outputs Archive : unzipping...");
        try {
            List<TaskRunOutput> outputList = processOutputFiles(outputs, runTaskOutputs, run, runOptional);
            run.setState(TaskRunState.FINISHED);
            runRepository.saveAndFlush(run);
            logger.info("Posting Outputs Archive : updated Run state to FINISHED");
            return outputList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<TaskRunOutput> processOutputFiles(MultipartFile outputs, Set<Output> runTaskOutputs, Run run, Optional<Run> runOptional) throws IOException, ProvisioningException {
        // read files from the archive
        try (ZipArchiveInputStream multiPartFileZipInputStream = new ZipArchiveInputStream(outputs.getInputStream())) {
            logger.info("Posting Outputs Archive : unzipped");
            List<Output> remainingOutputs = new ArrayList<>(runTaskOutputs);
            List<TaskRunOutput> taskRunOutputs = new ArrayList<>();

            ZipEntry ze;
            while ((ze = multiPartFileZipInputStream.getNextZipEntry()) != null) {
                // look for output matching file name
                Output currentOutput = null;
                for (int i = 0; i < remainingOutputs.size(); i++) {
                    currentOutput = remainingOutputs.get(i);
                    if (currentOutput.getName().equals(ze.getName())) {
                        remainingOutputs.remove(i);
                        break;
                    }
                }

                // there's a file that do not match any output parameter
                if (currentOutput == null) {
                    AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_UNKNOWN_OUTPUT);
                    logger.info("Posting Outputs Archive : output invalid (unknown output)");
                    run.setState(TaskRunState.FAILED);
                    runRepository.saveAndFlush(run);
                    logger.info("Posting Outputs Archive : updated Run state to FAILED");
                    throw new ProvisioningException(error);
                }

                // read file
                // TODO make this more generic to support multiple types
                String outputName = currentOutput.getName();
                byte[] rawOutput = multiPartFileZipInputStream.readNBytes((int) ze.getSize());
                String output = new String(rawOutput, getStorageCharset(charset));
                String trimmedOutput = output.trim();
                // saving to database does not care about the type
                saveOutput(runOptional, outputName, trimmedOutput);
                // saving to the storage does not care about the type
                storeOutputInFileStorage(run, runOptional, trimmedOutput, outputName);
                // based on parsed type build the response
                String type = currentOutput.getType().getId();
                TaskRunOutput taskRunOutput = new TaskRunOutput(type, trimmedOutput, run.getId(), outputName);
                taskRunOutputs.add(taskRunOutput);


            }

            if (!remainingOutputs.isEmpty()) {
                AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_MISSING_OUTPUTS);
                logger.info("Posting Outputs Archive : output invalid (missing outputs)");
                run.setState(TaskRunState.FAILED);
                runRepository.saveAndFlush(run);
                logger.info("Posting Outputs Archive : updated Run state to FAILED");
                throw new ProvisioningException(error);
            }

            logger.info("Posting Outputs Archive : posted");
            return taskRunOutputs;
        }
    }

    private void storeOutputInFileStorage(Run run, Optional<Run> runOptional, String outputValue, String name) throws ProvisioningException {
        logger.info("Posting Outputs Archive : storing in file storage...");
        Storage outputsStorage = new Storage("task-run-outputs-" + runOptional.get().getId());
        byte[] inputFileData = outputValue.getBytes(getStorageCharset(charset));
        FileData outputFileData = new FileData(inputFileData, name);
        try {
            fileStorageHandler.createFile(outputsStorage, outputFileData);
        } catch (FileStorageException e) {
            run.setState(TaskRunState.FAILED);
            runRepository.saveAndFlush(run);
            logger.info("Posting Outputs Archive : updated Run state to FAILED");
            AppEngineError error = ErrorBuilder.buildParamRelatedError(ErrorCode.STORAGE_STORING_INPUT_FAILED, name, e.getMessage());
            throw new ProvisioningException(error);
        }
        logger.info("Posting Outputs Archive : stored");
    }

    private void saveOutput(Optional<Run> runOptional, String name, String outputValue) {
        logger.info("Posting Outputs Archive : saving...");
        Result result = resultRepository.findResultByParameterNameAndRunId(name, runOptional.get().getId());
        if (result == null) {
            result = new Result(name, outputValue, runOptional.get().getId());
            resultRepository.save(result);
        } else {
            result.setValue(outputValue);
            resultRepository.saveAndFlush(result);
        }
        logger.info("Posting Outputs Archive : saved...");
    }

    private static boolean notInOneOfSchedulerManagedStates(Run run) {
        return !run.getState().equals(TaskRunState.RUNNING) && !run.getState().equals(TaskRunState.PENDING) && !run.getState().equals(TaskRunState.QUEUED) && !run.getState().equals(TaskRunState.QUEUING);
    }

    public List<TaskRunOutput> retrieveRunOutputs(String runId) throws ProvisioningException {
        logger.info("Retrieving Outputs Json : retrieving...");
        List<TaskRunOutput> outputList = new ArrayList<>();
        // validate run
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }
        // check the state is valid
        Run run = runOptional.get();
        if (!run.getState().equals(TaskRunState.FINISHED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }
        // find all the results
        List<Result> results = resultRepository.findResultByRunId(UUID.fromString(runId));
        for (Result result : results) {
            // based on the type of the parameter assign the type
            Set<Output> outputs = run.getTask().getOutputs();
            Output outputForType = outputs.stream().filter(output -> output.getName().equalsIgnoreCase(result.getParameterName())).findFirst().get();
            String type = outputForType.getType().getId();
            TaskRunOutput output = new TaskRunOutput(type, result.getValue(), result.getRunId(), result.getParameterName());
            outputList.add(output);
        }
        logger.info("Retrieving Outputs Json : retrieved");
        return outputList;
    }

    public List<TaskRunOutput> retrieveRunInputs(String runId) throws ProvisioningException {
        logger.info("Retrieving Inputs : retrieving...");
        List<TaskRunOutput> outputList = new ArrayList<>();
        // validate run
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }
        Run run = runOptional.get();
        if (run.getState().equals(TaskRunState.CREATED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }
        // find all the results
        List<Provision> results = provisionRepository.findProvisionByRunId(UUID.fromString(runId));
        for (Provision result : results) {
            // based on the type of the parameter assign the type
            Set<Output> outputs = run.getTask().getOutputs();
            Output outputForType = outputs.stream().filter(output -> output.getName().equalsIgnoreCase(result.getParameterName())).findFirst().get();
            String type = outputForType.getType().getId();
            TaskRunOutput output = new TaskRunOutput(type, result.getValue(), result.getRunId(), result.getParameterName());
            outputList.add(output);
        }
        logger.info("Retrieving Inputs : retrieved");
        return outputList;
    }


    public StateAction updateRunState(String runId, State state) throws SchedulingException, ProvisioningException {
        if (state.getDesired().equals(TaskRunState.RUNNING))
            return run(runId);
        // TODO : handle other state transitions here
        // to safeguard against unknown state transition requests
        AppEngineError error = ErrorBuilder.build(ErrorCode.UKNOWN_STATE);
        throw new ProvisioningException(error);
    }

    @NotNull
    private StateAction run(String runId) throws ProvisioningException, SchedulingException {
        logger.info("Running Task : scheduling...");
        logger.info("Running Task : validating Run...");
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }
        // check run state is valid for run
        Run run = runOptional.get();
        if (!run.getState().equals(TaskRunState.PROVISIONED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_NOT_PROVISIONED);
            throw new ProvisioningException(error);
        }
        logger.info("Running Task : valid run");
        logger.info("Running Task : contacting scheduler...");
        Schedule schedule = new Schedule();
        schedule.setRun(run);
        schedulerHandler.schedule(schedule);
        logger.info("Running Task : scheduling done");
        // update the final state

        run.setState(TaskRunState.QUEUING);
        runRepository.saveAndFlush(run);
        logger.info("Running Task : updated Run state to QUEUING");
        // return response
        StateAction action = new StateAction();

        action.setStatus("successful");

        TaskDescription description = makeTaskDescription(run.getTask());
        Resource resource = new Resource(description, run.getId(), TaskRunState.QUEUING, new Date(), new Date(), new Date());
        action.setResource(resource);
        logger.info("Running Task : scheduled");
        return action;
    }

    public TaskDescription makeTaskDescription(Task task) {
        TaskDescription taskDescription = new TaskDescription(task.getName(), task.getNamespace(), task.getVersion(), task.getDescription());
        Set<TaskAuthor> descriptionAuthors = new HashSet<>();
        for (Author author : task.getAuthors()) {
            TaskAuthor taskAuthor = new TaskAuthor(author.getFirstName(), author.getLastName(), author.getOrganization(), author.getEmail(), author.isContact());
            descriptionAuthors.add(taskAuthor);
        }
        taskDescription.setAuthors(descriptionAuthors);
        return taskDescription;
    }

    public TaskRunResponse retrieveRun(String runId) throws ProvisioningException {
        logger.info("Retrieving Run : retrieving...");
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }
        Run run = runOptional.get();
        TaskDescription description = makeTaskDescription(run.getTask());
        logger.info("Retrieving Run : retrieved");
        return new TaskRunResponse(description, UUID.fromString(runId), run.getState(), run.getCreated_at(), run.getUpdated_at(), run.getLast_state_transition_at());
    }
}
