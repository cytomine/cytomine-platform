package be.cytomine.appengine.exceptions.handlers;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultApiExceptionHandler {
    Logger logger = LoggerFactory.getLogger(ProvisionTaskApiExceptionHandler.class);

    @ExceptionHandler({Exception.class})
    public final ResponseEntity<AppEngineError> handleException(Exception e) {
        logger.info("bad request 500 error");
        e.printStackTrace();
        AppEngineError error = ErrorBuilder.buildServerError(e);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
