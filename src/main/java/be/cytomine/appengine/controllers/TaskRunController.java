package be.cytomine.appengine.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import be.cytomine.appengine.dto.inputs.task.State;
import be.cytomine.appengine.dto.inputs.task.StateAction;
import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.TaskRunResponse;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.exceptions.ProvisioningException;
import be.cytomine.appengine.exceptions.SchedulingException;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.services.TaskProvisioningService;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "${app-engine.api_prefix}${app-engine.api_version}/")
public class TaskRunController {

    private final TaskProvisioningService taskRunService;

    @PutMapping(
        value = "/task-runs/{run_id}/input-provisions/{param_name}",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> provisionJson(
        @PathVariable("run_id") String runId,
        @PathVariable("param_name") String parameterName,
        @RequestBody JsonNode provision
    ) throws ProvisioningException {
        log.info("/task-runs/{run_id}/input-provisions/{param_name} JSON PUT");
        JsonNode provisioned = taskRunService.provisionRunParameter(
            runId,
            parameterName,
            provision
        );
        log.info("/task-runs/{run_id}/input-provisions/{param_name} JSON PUT Ended");

        return ResponseEntity.ok(provisioned);
    }

    @PutMapping(
        value = "/task-runs/{run_id}/input-provisions/{param_name}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> provisionData(
        @PathVariable("run_id") String runId,
        @PathVariable("param_name") String parameterName,
        @RequestParam MultipartFile file
    ) throws IOException, ProvisioningException {
        log.info("/task-runs/{run_id}/input-provisions/{param_name} File PUT");

        Path data = Files.createTempFile(parameterName, null);
        file.transferTo(data);

        JsonNode provisioned = taskRunService.provisionRunParameter(
            runId,
            parameterName,
            data.toFile()
        );
        log.info("/task-runs/{run_id}/input-provisions/{param_name} File PUT Ended");

        return ResponseEntity.ok(provisioned);
    }

    @PutMapping(value = "/task-runs/{run_id}/input-provisions")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> provisionMultiple(
        @PathVariable("run_id") String runId,
        @RequestBody List<JsonNode> provisions
    ) throws ProvisioningException {
        log.info("/task-runs/{run_id}/input-provisions PUT");
        List<JsonNode> provisionedList = taskRunService.provisionMultipleRunParameters(
            runId,
            provisions
        );
        log.info("/task-runs/{run_id}/input-provisions PUT Ended");
        return ResponseEntity.ok(provisionedList);
    }

    @GetMapping(value = "/task-runs/{run_id}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getRun(
        @PathVariable("run_id") String runId
    ) throws ProvisioningException {
        log.info("/task-runs/{run_id} GET");
        TaskRunResponse run = taskRunService.retrieveRun(runId);
        log.info("/task-runs/{run_id} GET Ended");
        return new ResponseEntity<>(run, HttpStatus.OK);
    }

    @GetMapping(value = "/task-runs/{run_id}/inputs.zip")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getInputProvisionsArchives(
        @PathVariable("run_id") String runId
    ) throws ProvisioningException, IOException, FileStorageException {
        log.info("/task-runs/{run_id}/inputs.zip GET");
        StorageData data = taskRunService.retrieveIOZipArchive(runId, ParameterType.INPUT);
        File file = data.peek().getData();

        HttpHeaders headers = new HttpHeaders();
        headers.add(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + file.getName() + "\""
        );
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        log.info("/task-runs/{run_id}/inputs.zip GET Ended");
        return ResponseEntity.ok()
            .headers(headers)
            .body(new FileSystemResource(file));
    }

    @GetMapping(value = "/task-runs/{run_id}/inputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getRunInputsList(
        @PathVariable("run_id") String runId
    ) throws ProvisioningException {
        log.info("/task-runs/{run_id}/inputs GET");
        List<TaskRunParameterValue> outputs = taskRunService.retrieveRunInputs(runId);
        log.info("/task-runs/{run_id}/inputs GET Ended");
        return new ResponseEntity<>(outputs, HttpStatus.OK);
    }

    @GetMapping(value = "/task-runs/{run_id}/input/{parameter_name}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getInputRunParameter(
        @PathVariable("run_id") String runId,
        @PathVariable("parameter_name") String parameterName
    ) throws ProvisioningException {
        log.info("/task-runs/{run_id}/input/{parameter_name} GET");
        File input = taskRunService.retrieveSingleRunIO(
            runId,
            parameterName,
            ParameterType.INPUT
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + input.getName() + "\""
        );
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        log.info("/task-runs/{run_id}/input/{parameter_name} Ended");

        return ResponseEntity.ok()
            .headers(headers)
            .body(new FileSystemResource(input));
    }

    @GetMapping(value = "/task-runs/{run_id}/outputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getRunOutputsList(
        @PathVariable("run_id") String runId
    ) throws ProvisioningException {
        log.info("/task-runs/{run_id}/outputs GET");
        List<TaskRunParameterValue> outputs = taskRunService.retrieveRunOutputs(runId);
        log.info("/task-runs/{run_id}/outputs GET Ended");
        return new ResponseEntity<>(outputs, HttpStatus.OK);
    }

    @GetMapping(value = "/task-runs/{run_id}/output/{parameter_name}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getOutputRunParameter(
        @PathVariable("run_id") String runId,
        @PathVariable("parameter_name") String parameterName
    ) throws ProvisioningException {
        log.info("/task-runs/{run_id}/output/{parameter_name} GET");
        File output = taskRunService.retrieveSingleRunIO(
            runId,
            parameterName,
            ParameterType.OUTPUT
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + output.getName() + "\""
        );
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        log.info("/task-runs/{run_id}/output/{parameter_name} Ended");

        return ResponseEntity.ok()
            .headers(headers)
            .body(new FileSystemResource(output));
    }

    @GetMapping(value = "/task-runs/{run_id}/outputs.zip")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> getOutputsProvisionsArchives(
        @PathVariable("run_id") String runId
    ) throws ProvisioningException, IOException, FileStorageException {
        log.info("/task-runs/{run_id}/outputs.zip GET");
        StorageData data = taskRunService.retrieveIOZipArchive(runId, ParameterType.OUTPUT);
        File file = data.peek().getData();

        HttpHeaders headers = new HttpHeaders();
        headers.add(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + file.getName() + "\""
        );
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        log.info("/task-runs/{run_id}/outputs.zip GET Ended");
        return ResponseEntity.ok()
            .headers(headers)
            .body(new FileSystemResource(file));
    }

    @PostMapping(value = "/task-runs/{run_id}/{secret}/outputs.zip")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> postOutputsProvisionsArchives(
        @PathVariable("run_id") String runId,
        @PathVariable String secret,
        @RequestParam MultipartFile outputs
    ) throws ProvisioningException {
        log.info("/task-runs/{run_id}/outputs.zip POST");
        List<TaskRunParameterValue> taskOutputs = taskRunService.postOutputsZipArchive(
            runId,
            secret,
            outputs);
        log.info("/task-runs/{run_id}/outputs.zip POST Ended");
        return new ResponseEntity<>(taskOutputs, HttpStatus.OK);
    }

    @PostMapping(value = "/task-runs/{run_id}/state-actions")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> updateState(
        @PathVariable("run_id") String runId,
        @RequestBody State state
    ) throws ProvisioningException, SchedulingException {
        log.info("/task-runs/{run_id}/state_actions POST");
        StateAction stateAction = taskRunService.updateRunState(runId, state);
        log.info("/task-runs/{run_id}/state_actions POST Ended");
        return new ResponseEntity<>(stateAction, HttpStatus.OK);
    }
}
