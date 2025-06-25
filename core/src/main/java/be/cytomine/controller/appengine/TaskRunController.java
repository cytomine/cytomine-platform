package be.cytomine.controller.appengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import be.cytomine.service.appengine.TaskRunService;

@ConditionalOnExpression("${application.appEngine.enabled: false}")
@RequiredArgsConstructor
@RequestMapping("/api/app-engine")
@RestController
public class TaskRunController {

    private final TaskRunService taskRunService;

    @PostMapping("/project/{project}/tasks/{task}/runs")
    public ResponseEntity<String> add(
        @PathVariable Long project,
        @PathVariable UUID task,
        @RequestBody JsonNode body
    ) {
        return taskRunService.addTaskRun(project, "tasks/" + task + "/runs", body);
    }

    @PostMapping("/project/{project}/tasks/{namespace}/{version}/runs")
    public ResponseEntity<String> add(
        @PathVariable Long project,
        @PathVariable String namespace,
        @PathVariable String version,
        @RequestBody JsonNode body
    ) {
        String uri = "tasks/" + namespace + "/" + version + "/runs";
        return taskRunService.addTaskRun(project, uri, body);
    }

    @GetMapping("/project/{project}/task-runs/{task}")
    public ResponseEntity<String> get(
        @PathVariable Long project,
        @PathVariable UUID task
    ) {
        return taskRunService.getTaskRun(project, task);
    }

    @PutMapping("/project/{project}/task-runs/{task}/input-provisions")
    public ResponseEntity<String> batchProvision(
        @PathVariable Long project,
        @PathVariable UUID task,
        @RequestBody List<JsonNode> body
    ) {
        return taskRunService.batchProvisionTaskRun(body, project, task);
    }

    @PutMapping(
        value = "/project/{project}/task-runs/{task}/input-provisions/{parameter_name}",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> provision(
        @PathVariable Long project,
        @PathVariable UUID task,
        @PathVariable("parameter_name") String parameterName,
        @RequestBody JsonNode json
    ) {
        return taskRunService.provisionTaskRun(json, project, task, parameterName);
    }

    @PutMapping(
        value = "/project/{project}/task-runs/{task}/input-provisions/{parameter_name}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> provision(
            @PathVariable Long project,
            @PathVariable UUID task,
            @PathVariable("parameter_name") String parameterName,
            @RequestParam MultipartFile file
    ) {
        return taskRunService.provisionBinaryData(file, project, task, parameterName);
    }

    @PostMapping("/project/{project}/task-runs/{task}/state-actions")
    public ResponseEntity<String> stateAction(
        @PathVariable Long project,
        @PathVariable UUID task,
        @RequestBody JsonNode body
    ) {
        return taskRunService.postStateAction(body, project, task);
    }

    @GetMapping("/project/{project}/task-runs/{task}/inputs")
    public ResponseEntity<String> getInputs(
        @PathVariable Long project,
        @PathVariable UUID task
    ) {
        return taskRunService.getInputs(project, task);
    }

    @GetMapping("/project/{project}/task-runs/{task}/input/{parameter_name}")
    public void getTaskRunInputParameter(
            @PathVariable Long project,
            @PathVariable UUID task,
            @PathVariable("parameter_name") String parameterName,
            HttpServletResponse response
    ) {
        File file = taskRunService.getTaskRunIOParameter(project, task, parameterName, "input");
        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + parameterName + "\"");
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            response.setHeader(HttpHeaders.PRAGMA, "no-cache");
            response.setHeader(HttpHeaders.EXPIRES, "0");

            is.transferTo(os);
            os.flush();
            file.delete();
        } catch (IOException e) {
            throw new RuntimeException("Error while streaming the input parameter", e);
        }
    }

    @GetMapping("/project/{project}/task-runs/{task}/outputs")
    public ResponseEntity<String> getOutputs(
        @PathVariable Long project,
        @PathVariable UUID task
    ) {
        return taskRunService.getOutputs(project, task);
    }

    @GetMapping("/project/{project}/task-runs/{task}/output/{parameter_name}")
    public void getTaskRunOutputParameter(
            @PathVariable Long project,
            @PathVariable UUID task,
            @PathVariable("parameter_name") String parameterName,
            HttpServletResponse response
    ) {
        File file = taskRunService.getTaskRunIOParameter(project, task, parameterName, "output");
        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + parameterName + "\"");
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            response.setHeader(HttpHeaders.PRAGMA, "no-cache");
            response.setHeader(HttpHeaders.EXPIRES, "0");

            is.transferTo(os);
            os.flush();
            file.delete();
        } catch (IOException e) {
            throw new RuntimeException("Error while streaming the output parameter", e);
        }
    }
}
