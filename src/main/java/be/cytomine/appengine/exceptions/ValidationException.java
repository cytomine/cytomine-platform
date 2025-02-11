package be.cytomine.appengine.exceptions;

import java.util.Set;

import com.networknt.schema.ValidationMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;

@Getter
@EqualsAndHashCode(callSuper = false)
public class ValidationException extends Exception {

    private boolean internalError;

    private Set<ValidationMessage> errors;

    private boolean integrityViolated;

    private AppEngineError error;

    public ValidationException(Exception e) {
        super(e);
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(AppEngineError error) {
        super();
        this.error = error;
    }

    public ValidationException(String message, boolean internalError) {
        super(message);
        this.internalError = internalError;
    }

    public ValidationException(AppEngineError error, boolean internalError) {
        super(error.getMessage());
        this.internalError = internalError;
        this.error = error;
    }

    public ValidationException(String message, boolean internalError, boolean integrityViolated) {
        super(message);
        this.internalError = internalError;
        this.integrityViolated = integrityViolated;
    }

    public ValidationException(
        AppEngineError error,
        boolean internalError,
        boolean integrityViolated
    ) {
        super(error.getMessage());
        this.internalError = internalError;
        this.integrityViolated = integrityViolated;
        this.error = error;
    }

    public ValidationException(String message, Set<ValidationMessage> errors) {
        super(message);
        this.errors = errors;
    }
}
