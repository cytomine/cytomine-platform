package be.cytomine.appengine.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;

@Data
@EqualsAndHashCode(callSuper = false)
public class TaskServiceException extends Exception {

    private AppEngineError error;

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
