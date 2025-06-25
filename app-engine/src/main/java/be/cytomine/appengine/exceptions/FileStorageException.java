package be.cytomine.appengine.exceptions;

import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.dto.responses.errors.ErrorDefinitions;

public class FileStorageException extends Exception {

    private ErrorCode errorCode;

    public FileStorageException(Exception e) {
        super(e);
    }

    public FileStorageException(ErrorCode e) {
        super(ErrorDefinitions.fromCode(e).getMessage());
        errorCode = e;
    }

    public FileStorageException(String message) {
        super(message);
    }
}
