package be.cytomine.appengine.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.dto.responses.errors.ErrorDefinitions;

@Data
@EqualsAndHashCode(callSuper = false)
public class TypeValidationException extends Exception {

    private AppEngineError error;

    private ErrorCode errorCode;

    public TypeValidationException(Exception e) {
        super(e);
    }

    public TypeValidationException(ErrorCode e) {
        super(ErrorDefinitions.fromCode(e).getMessage());
        errorCode = e;
    }

    public TypeValidationException(AppEngineError error) {
        super();
        this.error = error;
    }

    public TypeValidationException(String message) {
        super(message);
    }
}
