package be.cytomine.appengine.exceptions.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.ProvisioningException;
import be.cytomine.appengine.exceptions.RunTaskServiceException;

@Slf4j
@ControllerAdvice
@Order(0)
public class ProvisionTaskApiExceptionHandler {
    @ExceptionHandler({ RunTaskServiceException.class })
    public final ResponseEntity<AppEngineError> handleRunProcessingException(Exception e) {
        AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_TASK_NOT_FOUND);
        log.info("bad request 404 error [{}]", e.getMessage());
        return new ResponseEntity<AppEngineError>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ ProvisioningException.class })
    public final ResponseEntity<AppEngineError> handleProvisioningException(
        ProvisioningException e
    ) {
        String runNotFoundErrorMessage = "APPE-internal-run-not-found-error";
        String parameterNotFoundErrorMessage = "APPE-internal-parameter-not-found";

        if (
            e.getError().getErrorCode().equalsIgnoreCase(runNotFoundErrorMessage)
            || e.getError().getErrorCode().equalsIgnoreCase(parameterNotFoundErrorMessage)
        ) {
            log.info("not found 404 error [{}]", e.getMessage());
            return new ResponseEntity<AppEngineError>(e.getError(), HttpStatus.NOT_FOUND);

        }

        String provisionsNotFound = "APPE-internal-task-run-provisions-not-found";
        String invalidRunState = "APPE-internal-task-run-state-error";
        if (
            e.getError().getErrorCode().equalsIgnoreCase(provisionsNotFound)
            || e.getError().getErrorCode().equalsIgnoreCase(invalidRunState)
        ) {
            log.info("forbidden 403 error [{}]", e.getMessage());
            return new ResponseEntity<AppEngineError>(e.getError(), HttpStatus.FORBIDDEN);
        }

        log.info("bad request 400 error [{}]", e.getMessage());
        return new ResponseEntity<AppEngineError>(e.getError(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class })
    public final ResponseEntity<AppEngineError> handleProvisioningException(
        HttpMessageNotReadableException e
    ) {
        AppEngineError error = ErrorBuilder.build(ErrorCode.UNKNOWN_STATE);
        log.info("bad request 400 error [{}]", e.getMessage());
        return new ResponseEntity<AppEngineError>(error, HttpStatus.BAD_REQUEST);
    }
}
