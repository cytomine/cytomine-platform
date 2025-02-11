package be.cytomine.appengine.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProvisioningException extends Exception {

    private AppEngineError error;

    public ProvisioningException(Exception e) {
        super(e);
    }

    public ProvisioningException(AppEngineError error) {
        super();
        this.error = error;
    }

    public ProvisioningException(String message) {
        super(message);
    }
}
