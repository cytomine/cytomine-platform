package be.cytomine.appengine.controllers;

import be.cytomine.appengine.dto.inputs.task.*;
import be.cytomine.appengine.exceptions.*;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.services.TaskProvisioningService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping(path = "${app-engine.api_prefix}${app-engine.api_version}/")
public class TaskRunController {

    Logger logger = LoggerFactory.getLogger(TaskRunController.class);
    private final TaskProvisioningService taskRunService;

    public TaskRunController(TaskProvisioningService taskRunService) {
        this.taskRunService = taskRunService;
    }

    @PutMapping(value = "/task-runs/{run_id}/input-provisions/{param_name}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> provisionJson(@PathVariable String run_id, @PathVariable String param_name, @RequestBody JsonNode provision) throws ProvisioningException {
        logger.info("/task-runs/{run_id}/input-provisions/{param_name} JSON PUT");
        JsonNode provisioned = taskRunService.provisionRunParameter(provision, run_id);
        logger.info("/task-runs/{run_id}/input-provisions/{param_name} JSON PUT Ended");
        return ResponseEntity.ok(provisioned);
    }

    @PutMapping(value = "/task-runs/{run_id}/input-provisions/{param_name}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> provisionData(
        @PathVariable("run_id") String runId,
        @PathVariable("param_name") String parameterName,
        @RequestParam("file") MultipartFile file
    ) throws IOException, ProvisioningException {
        logger.info("/task-runs/{run_id}/input-provisions/{param_name} File PUT");

        JsonNode provisioned = taskRunService.provisionRunParameter(parameterName, runId, file.getBytes());
        logger.info("/task-runs/{run_id}/input-provisions/{param_name} File PUT Ended");

        return ResponseEntity.ok(provisioned);
    }

    @PutMapping(value = "/task-runs/{run_id}/input-provisions")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> provisionMultiple(@PathVariable String run_id, @RequestBody List<JsonNode> provisions) throws ProvisioningException {
        logger.info("/task-runs/{run_id}/input-provisions PUT");
        List<JsonNode> provisionedList = taskRunService.provisionMultipleRunParameters(run_id, provisions);
        logger.info("/task-runs/{run_id}/input-provisions PUT Ended");
        return ResponseEntity.ok(provisionedList);
    }

    @GetMapping(value = "/task-runs/{run_id}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getRun(@PathVariable String run_id) throws ProvisioningException, IOException, FileStorageException {
        logger.info("/task-runs/{run_id} GET");
        TaskRunResponse run = taskRunService.retrieveRun(run_id);
        logger.info("/task-runs/{run_id} GET Ended");
        return new ResponseEntity<>(run, HttpStatus.OK);
    }

    @GetMapping(value = "/task-runs/{run_id}/inputs.zip")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getInputProvisionsArchives(@PathVariable String run_id) throws ProvisioningException, IOException, FileStorageException {
        logger.info("/task-runs/{run_id}/inputs.zip GET");
        FileData file = taskRunService.retrieveInputsZipArchive(run_id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        logger.info("/task-runs/{run_id}/inputs.zip GET Ended");
        return new ResponseEntity<>(file.getFileData(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/task-runs/{run_id}/outputs.zip")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getOutputsProvisionsArchives(@PathVariable String run_id) throws ProvisioningException, IOException, FileStorageException {
        logger.info("/task-runs/{run_id}/outputs.zip GET");
        FileData file = taskRunService.retrieveOutputsZipArchive(run_id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        logger.info("/task-runs/{run_id}/outputs.zip GET Ended");
        return new ResponseEntity<>(file.getFileData(), headers, HttpStatus.OK);
    }


    @GetMapping(value = "/task-runs/{run_id}/inputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getRuninputsList(@PathVariable String run_id) throws ProvisioningException, IOException, FileStorageException {
        logger.info("/task-runs/{run_id}/inputs GET");
        List<TaskRunParameterValue> outputs = taskRunService.retrieveRunInputs(run_id);
        logger.info("/task-runs/{run_id}/inputs GET Ended");
        return new ResponseEntity<>(outputs, HttpStatus.OK);
    }

    @GetMapping(value = "/task-runs/{run_id}/input/{parameter_name}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getInputRunParameter(
        @PathVariable("run_id") String runId,
        @PathVariable("parameter_name") String parameterName
    ) throws ProvisioningException {
        logger.info("/task-runs/{run_id}/input/{parameter_name} GET");
        byte[] input = taskRunService.retrieveSingleRunIO(runId, parameterName, ParameterType.INPUT);
        logger.info("/task-runs/{run_id}/input/{parameter_name} Ended");
        return new ResponseEntity<>(input, HttpStatus.OK);
    }

    @GetMapping(value = "/task-runs/{run_id}/output/{parameter_name}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getOutputRunParameter(
        @PathVariable("run_id") String runId,
        @PathVariable("parameter_name") String parameterName
    ) throws ProvisioningException {
        logger.info("/task-runs/{run_id}/output/{parameter_name} GET");
        byte[] output = taskRunService.retrieveSingleRunIO(runId, parameterName, ParameterType.OUTPUT);
        logger.info("/task-runs/{run_id}/output/{parameter_name} Ended");
        return new ResponseEntity<>(output, HttpStatus.OK);
    }

    @GetMapping(value = "/task-runs/{run_id}/outputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getRunOutputsList(@PathVariable String run_id) throws ProvisioningException, IOException, FileStorageException {
        logger.info("/task-runs/{run_id}/outputs GET");
        List<TaskRunParameterValue> outputs = taskRunService.retrieveRunOutputs(run_id);
        logger.info("/task-runs/{run_id}/outputs GET Ended");
        return new ResponseEntity<>(outputs, HttpStatus.OK);
    }

    @PostMapping(value = "/task-runs/{run_id}/outputs.zip")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getInputProvisionsArchives(@PathVariable String run_id, @RequestParam("outputs") MultipartFile outputs) throws ProvisioningException {
        logger.info("/task-runs/{run_id}/outputs.zip POST");
        List<TaskRunParameterValue> taskOutputs = taskRunService.postOutputsZipArchive(run_id, outputs);
        logger.info("/task-runs/{run_id}/outputs.zip POST Ended");
        return new ResponseEntity<>(taskOutputs, HttpStatus.OK);
    }

    @PostMapping(value = "/task-runs/{run_id}/state-actions")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> updateState(@PathVariable String run_id, @RequestBody State state) throws ProvisioningException, SchedulingException {
        logger.info("/task-runs/{run_id}/state_actions POST");
        StateAction stateAction = taskRunService.updateRunState(run_id, state);
        logger.info("/task-runs/{run_id}/state_actions POST Ended");
        return new ResponseEntity<>(stateAction, HttpStatus.OK);
    }
}
