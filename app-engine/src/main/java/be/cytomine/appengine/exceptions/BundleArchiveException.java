package be.cytomine.appengine.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;

@Data
@EqualsAndHashCode(callSuper = false)
public class BundleArchiveException extends Exception {

    AppEngineError error;

    public BundleArchiveException(AppEngineError error) {

        super();
        this.error = error;
    }

    public BundleArchiveException(Exception e) {
        super(e);
    }

    public BundleArchiveException(String message) {
        super(message);
    }
}
