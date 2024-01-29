package be.cytomine.appengine.exceptions;

public class RunTaskServiceException extends Exception {

    public RunTaskServiceException(Exception e) {
        super(e);
    }

    public RunTaskServiceException(String message) {
        super(message);
    }
}
