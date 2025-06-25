package be.cytomine.appengine.exceptions.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.TaskNotFoundException;

@Slf4j
@ControllerAdvice
@Order(0)
public class TaskApiExceptionHandler {

    @ExceptionHandler({ TaskNotFoundException.class })
    public final ResponseEntity<AppEngineError> handleTaskNotFound(TaskNotFoundException e) {
        log.error("Internal server error [{}]", e.getMessage(), e);

        return new ResponseEntity<>(
            ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND),
            HttpStatus.NOT_FOUND
        );
    }
}
