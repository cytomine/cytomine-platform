package be.cytomine.appengine.controllers;

import java.io.File;
import java.util.List;
import java.util.Optional;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import be.cytomine.appengine.dto.inputs.task.TaskDescription;
import be.cytomine.appengine.dto.inputs.task.TaskRun;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.BundleArchiveException;
import be.cytomine.appengine.exceptions.RunTaskServiceException;
import be.cytomine.appengine.exceptions.TaskNotFoundException;
import be.cytomine.appengine.exceptions.TaskServiceException;
import be.cytomine.appengine.exceptions.ValidationException;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.services.TaskService;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "${app-engine.api_prefix}${app-engine.api_version}/")
public class TaskController {

    private final TaskService taskService;

    @PostMapping(path = "tasks")
    public ResponseEntity<?> upload(
        @RequestParam MultipartFile task
    ) throws TaskServiceException, ValidationException, BundleArchiveException {
        log.info("Task Upload POST");
        Optional<TaskDescription> taskDescription = taskService.uploadTask(task);
        log.info("Task Upload POST Ended");
        return ResponseEntity.ok(taskDescription);
    }

    @GetMapping(value = "tasks")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<List<TaskDescription>> findAllTasks() {
        log.info("tasks GET");
        log.info("tasks GET Ended");
        return ResponseEntity.ok(taskService.retrieveTaskDescriptions());
    }

    @GetMapping(value = "tasks/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findTaskById(@PathVariable String id) {
        log.info("tasks/{id} GET");
        Optional<TaskDescription> taskDescription = taskService.retrieveTaskDescription(id);
        log.info("tasks/{id} GET Ended");
        return taskDescription.<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> new ResponseEntity<>(
                ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND),
                HttpStatus.NOT_FOUND
            ));
    }

    @GetMapping(value = "tasks/{namespace}/{version}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findTaskByNamespaceAndVersion(
        @PathVariable String namespace,
        @PathVariable String version
    ) {
        log.info("tasks/{namespace}/{version} GET");
        Optional<TaskDescription> taskDescription = taskService.retrieveTaskDescription(
            namespace,
            version
        );
        log.info("tasks/{namespace}/{version} GET Ended");
        return taskDescription.<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> new ResponseEntity<>(
                ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND),
                HttpStatus.NOT_FOUND
            ));
    }

    @GetMapping(value = "tasks/{id}/inputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findInputsOfTask(@PathVariable String id) {
        log.info("tasks/{id}/inputs GET");
        Optional<Task> task = taskService.findById(id);
        if (task.isEmpty()) {
            log.info("tasks/{id}/inputs GET Ended");
            return new ResponseEntity<>(
                ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND),
                HttpStatus.NOT_FOUND
            );
        }
        log.info("tasks/{id}/inputs GET Ended");
        return ResponseEntity.ok(taskService.makeTaskInputs(task.get()));
    }

    @GetMapping(value = "tasks/{id}/outputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findOutputsOfTask(@PathVariable String id) {
        log.info("tasks/{id}/outputs GET");
        Optional<Task> task = taskService.findById(id);
        if (task.isEmpty()) {
            log.info("tasks/{id}/outputs GET Ended");
            return new ResponseEntity<>(
                ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND),
                HttpStatus.NOT_FOUND
            );
        }
        log.info("tasks/{id}/outputs GET Ended");
        return ResponseEntity.ok(taskService.makeTaskOutputs(task.get()));
    }

    @GetMapping(value = "tasks/{namespace}/{version}/inputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findInputsOfTaskByNamespaceAndVersion(
        @PathVariable String namespace,
        @PathVariable String version
    ) {
        log.info("tasks/{namespace}/{version}/inputs GET");
        Optional<Task> task = taskService.findByNamespaceAndVersion(namespace, version);
        if (task.isEmpty()) {
            log.info("tasks/{namespace}/{version}/inputs GET Ended");
            return new ResponseEntity<>(
                ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND),
                HttpStatus.NOT_FOUND
            );
        }
        log.info("tasks/{namespace}/{version}/inputs GET Ended");
        return ResponseEntity.ok(taskService.makeTaskInputs(task.get()));
    }

    @GetMapping(value = "tasks/{namespace}/{version}/outputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findOutputsOfTaskByNamespaceAndVersion(
        @PathVariable String namespace,
        @PathVariable String version
    ) {
        log.info("tasks/{namespace}/{version}/outputs GET");
        Optional<Task> task = taskService.findByNamespaceAndVersion(namespace, version);
        if (task.isEmpty()) {
            log.info("tasks/{namespace}/{version}/outputs GET Ended");
            return new ResponseEntity<>(
                ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND),
                HttpStatus.NOT_FOUND
            );
        }
        log.info("tasks/{namespace}/{version}/outputs GET Ended");
        return ResponseEntity.ok(taskService.makeTaskOutputs(task.get()));
    }

    @GetMapping(value = "tasks/{namespace}/{version}/descriptor.yml")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findDescriptorOfTaskByNamespaceAndVersion(
        @PathVariable String namespace,
        @PathVariable String version
    ) throws TaskServiceException, TaskNotFoundException {
        log.info("tasks/{namespace}/{version}/descriptor.yml GET");
        StorageData data = taskService.retrieveYmlDescriptor(namespace, version);
        File file = data.peek().getData();

        HttpHeaders headers = new HttpHeaders();
        headers.add(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + file.getName() + "\""
        );
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        log.info("tasks/{namespace}/{version}/descriptor.yml GET Ended");
        return ResponseEntity.ok()
            .headers(headers)
            .body(new FileSystemResource(file));
    }

    @GetMapping(value = "tasks/{id}/descriptor.yml")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findDescriptorOfTaskById(
        @PathVariable String id
    ) throws TaskServiceException, TaskNotFoundException {
        log.info("tasks/{id}/descriptor.yml GET");
        StorageData data = taskService.retrieveYmlDescriptor(id);
        File file = data.peek().getData();

        HttpHeaders headers = new HttpHeaders();
        headers.add(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + file.getName() + "\""
        );
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        log.info("tasks/{id}/descriptor.yml GET Ended");

        return ResponseEntity.ok()
            .headers(headers)
            .body(new FileSystemResource(file));
    }

    @PostMapping(value = "tasks/{namespace}/{version}/runs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> createRunOfTaskByNamespaceAndVersion(
        @PathVariable String namespace,
        @PathVariable String version
    ) throws RunTaskServiceException {
        log.info("tasks/{namespace}/{version}/runs POST");
        TaskRun taskRun = taskService.createRunForTask(namespace, version);
        log.info("tasks/{namespace}/{version}/runs POST Ended");
        return ResponseEntity.ok(taskRun);
    }

    @PostMapping(value = "tasks/{id}/runs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> createRunOfTaskByNamespaceAndVersion(
        @PathVariable String id
    ) throws RunTaskServiceException {
        log.info("tasks/{id}/runs POST");
        TaskRun taskRun = taskService.createRunForTask(id);
        log.info("tasks/{id}/runs POST Ended");
        return ResponseEntity.ok(taskRun);
    }
}
