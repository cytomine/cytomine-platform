package be.cytomine.appengine.exceptions.handlers;

import be.cytomine.appengine.dto.inputs.task.UploadTask;
import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.BundleArchiveException;
import be.cytomine.appengine.exceptions.TaskServiceException;
import be.cytomine.appengine.exceptions.ValidationException;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;

@ControllerAdvice
@Order(0)
public class UploadTaskApiExceptionHandler {

    Logger logger = LoggerFactory.getLogger(UploadTaskApiExceptionHandler.class);

    @Value("${spring.servlet.multipart.max-file-size}")
    private String max;

    @ExceptionHandler({TaskServiceException.class})
    public final ResponseEntity<AppEngineError> handleUploadProcessingExceptionException(TaskServiceException e) {
        logger.info("Internal server error [" + e.getMessage() + "]");
        e.printStackTrace();
        return new ResponseEntity<AppEngineError>(e.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler({ValidationException.class})
    public final ResponseEntity<AppEngineError> handleTaskBundleValidationException(ValidationException e) {
        logger.info("Validation failure [" + e.getError().getMessage() + "]");

        if (internalError(e))
            return new ResponseEntity<AppEngineError>(e.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
        if (violatingRequest(e))
            return new ResponseEntity<AppEngineError>(e.getError(), HttpStatus.CONFLICT);

        return new ResponseEntity<AppEngineError>(e.getError(), HttpStatus.BAD_REQUEST);
    }

    private static boolean containErrorDetails(ValidationException e) {
        return e.getErrors() != null && !e.getErrors().isEmpty();
    }

    private static boolean internalError(ValidationException e) {
        return e.isInternalError() && !e.isIntegrityViolated();
    }

    private static boolean violatingRequest(ValidationException e) {
        return !e.isInternalError() && e.isIntegrityViolated();
    }

    @ExceptionHandler({BundleArchiveException.class})
    public final ResponseEntity<AppEngineError> handleTaskBundleZipException(BundleArchiveException e, WebRequest request) {
        logger.info("Bundle/Archive processing failure [" + e.getMessage() + "]");
        return new ResponseEntity<AppEngineError>(e.getError(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class})
    public final ResponseEntity<AppEngineError> handleUploadSizeExceeded(Exception e, WebRequest request) {
        String message = "Maximum app bundle size of " + max + " exceeded";
        logger.info("Bundle/Archive processing failure [" + message + "]");

        AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_MAX_UPLOAD_SIZE_EXCEEDED);
        return new ResponseEntity<AppEngineError>(error, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    // @ExceptionHandler({IllegalArgumentException.class})
    // public final ResponseEntity<AppEngineError> handleMalformattedBundle(Exception e) {
    //     String message = "Bundle is not formatted correctly";
    //     logger.info("Bundle/Archive processing failure [" + message + "]");

    //     AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_BUNDLE_FORMAT);
    //     return new ResponseEntity<AppEngineError>(error, HttpStatus.BAD_REQUEST);
    // }

    @ExceptionHandler({NullPointerException.class})
    public final ResponseEntity<AppEngineError> handleNullpointerException(Exception e) {
        // TODO temp handler remove later
        String message = "missing properties in descriptor.yml";
        logger.info("Bundle/Archive processing failure [" + message + "]");

        AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_BUNDLE_FORMAT);
        return new ResponseEntity<AppEngineError>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
