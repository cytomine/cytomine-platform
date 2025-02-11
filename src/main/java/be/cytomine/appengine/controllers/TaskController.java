package be.cytomine.appengine.controllers;

import be.cytomine.appengine.dto.inputs.task.*;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.*;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.services.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping(path = "${app-engine.api_prefix}${app-engine.api_version}/")
public class TaskController {

    Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping(path = "tasks")
    public ResponseEntity<?> upload(@RequestParam("task") MultipartFile task) throws TaskServiceException, ValidationException, BundleArchiveException {
        logger.info("Task Upload POST");
        Optional<TaskDescription> taskDescription = taskService.uploadTask(task);
        logger.info("Task Upload POST Ended");
        return ResponseEntity.ok(taskDescription);
    }

    @GetMapping(value = "tasks")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<List<TaskDescription>> findAllTasks() {
        logger.info("tasks GET");
        logger.info("tasks GET Ended");
        return ResponseEntity.ok(taskService.retrieveTaskDescriptions());
    }

    @GetMapping(value = "tasks/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findTaskById(@PathVariable String id) {
        logger.info("tasks/{id} GET");
        Optional<TaskDescription> taskDescription = taskService.retrieveTaskDescription(id);
        logger.info("tasks/{id} GET Ended");
        return taskDescription.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND), HttpStatus.NOT_FOUND));
    }

    @GetMapping(value = "tasks/{namespace}/{version}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findTaskByNamespaceAndVersion(@PathVariable String namespace, @PathVariable String version) {
        logger.info("tasks/{namespace}/{version} GET");
        Optional<TaskDescription> taskDescription = taskService.retrieveTaskDescription(namespace, version);
        logger.info("tasks/{namespace}/{version} GET Ended");
        return taskDescription.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND), HttpStatus.NOT_FOUND));
    }

    @GetMapping(value = "tasks/{id}/inputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findInputsOfTask(@PathVariable String id) {
        logger.info("tasks/{id}/inputs GET");
        Optional<Task> task = taskService.findById(id);
        if (task.isEmpty()) {
            logger.info("tasks/{id}/inputs GET Ended");
            return new ResponseEntity<>(ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND), HttpStatus.NOT_FOUND);
        }
        logger.info("tasks/{id}/inputs GET Ended");
        return ResponseEntity.ok(taskService.makeTaskInputs(task.get()));
    }

    @GetMapping(value = "tasks/{id}/outputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findOutputsOfTask(@PathVariable String id) {
        logger.info("tasks/{id}/outputs GET");
        Optional<Task> task = taskService.findById(id);
        if (task.isEmpty()) {
            logger.info("tasks/{id}/outputs GET Ended");
            return new ResponseEntity<>(ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND), HttpStatus.NOT_FOUND);
        }
        logger.info("tasks/{id}/outputs GET Ended");
        return ResponseEntity.ok(taskService.makeTaskOutputs(task.get()));
    }

    @GetMapping(value = "tasks/{namespace}/{version}/inputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findInputsOfTaskByNamespaceAndVersion(@PathVariable String namespace, @PathVariable String version) {
        logger.info("tasks/{namespace}/{version}/inputs GET");
        Optional<Task> task = taskService.findByNamespaceAndVersion(namespace, version);
        if (task.isEmpty()) {
            logger.info("tasks/{namespace}/{version}/inputs GET Ended");
            return new ResponseEntity<>(ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND), HttpStatus.NOT_FOUND);
        }
        logger.info("tasks/{namespace}/{version}/inputs GET Ended");
        return ResponseEntity.ok(taskService.makeTaskInputs(task.get()));
    }

    @GetMapping(value = "tasks/{namespace}/{version}/outputs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findOutputsOfTaskByNamespaceAndVersion(@PathVariable String namespace, @PathVariable String version) {
        logger.info("tasks/{namespace}/{version}/outputs GET");
        Optional<Task> task = taskService.findByNamespaceAndVersion(namespace, version);
        if (task.isEmpty()) {
            logger.info("tasks/{namespace}/{version}/outputs GET Ended");
            return new ResponseEntity<>(ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND), HttpStatus.NOT_FOUND);
        }
        logger.info("tasks/{namespace}/{version}/outputs GET Ended");
        return ResponseEntity.ok(taskService.makeTaskOutputs(task.get()));
    }

    @GetMapping(value = "tasks/{namespace}/{version}/descriptor.yml")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findDescriptorOfTaskByNamespaceAndVersion(@PathVariable String namespace, @PathVariable String version) throws TaskServiceException {
        logger.info("tasks/{namespace}/{version}/descriptor.yml GET");
        try {
            StorageData file = taskService.retrieveYmlDescriptor(namespace, version);
            logger.info("tasks/{namespace}/{version}/descriptor.yml GET Ended");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<>(file.peek().getData(), headers, HttpStatus.OK);
        } catch (TaskNotFoundException e) {
            logger.info("tasks/{namespace}/{version}/descriptor.yml GET Ended");
            return new ResponseEntity<>(ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "tasks/{id}/descriptor.yml")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> findDescriptorOfTaskById(@PathVariable String id) throws TaskServiceException {
        logger.info("tasks/{namespace}/{version}/descriptor.yml GET");
        try {
            StorageData file = taskService.retrieveYmlDescriptor(id);
            logger.info("tasks/{namespace}/{version}/descriptor.yml GET Ended");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<>(file.peek().getData(), headers, HttpStatus.OK);
        } catch (TaskNotFoundException e) {
            logger.info("tasks/{namespace}/{version}/descriptor.yml GET Ended");
            return new ResponseEntity<>(ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND), HttpStatus.NOT_FOUND);
        }
    }


    @PostMapping(value = "tasks/{namespace}/{version}/runs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> createRunOfTaskByNamespaceAndVersion(@PathVariable String namespace, @PathVariable String version) throws RunTaskServiceException {
        logger.info("tasks/{namespace}/{version}/runs POST");
        TaskRun taskRun = taskService.createRunForTask(namespace, version);
        logger.info("tasks/{namespace}/{version}/runs POST Ended");
        return ResponseEntity.ok(taskRun);
    }

    @PostMapping(value = "tasks/{id}/runs")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<?> createRunOfTaskByNamespaceAndVersion(@PathVariable String id) throws RunTaskServiceException {
        logger.info("tasks/{id}/runs POST");
        TaskRun taskRun = taskService.createRunForTask(id);
        logger.info("tasks/{id}/runs POST Ended");
        return ResponseEntity.ok(taskRun);
    }

}
