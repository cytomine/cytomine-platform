package be.cytomine.appengine.exceptions;


import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProvisioningException extends Exception {

    AppEngineError error;

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
