package be.cytomine.appengine.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.dto.responses.errors.ErrorDefinitions;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProvisioningException extends Exception {

    private AppEngineError error;
    private ErrorCode errorCode;

    public ProvisioningException(Exception e) {
        super(e);
    }

    public ProvisioningException(AppEngineError error) {
        super();
        this.error = error;
    }

    public ProvisioningException(ErrorCode e) {
        super(ErrorDefinitions.fromCode(e).getMessage());
        errorCode = e;
    }

    public ProvisioningException(String message) {
        super(message);
    }
}
