package be.cytomine.appengine.exceptions;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class TaskServiceException extends Exception {

    AppEngineError error;

    public TaskServiceException(AppEngineError error) {
        super();
        this.error = error;
    }

    public TaskServiceException(Exception e) {
        super(e);
    }

    public TaskServiceException(String message) {
        super(message);
    }
}
