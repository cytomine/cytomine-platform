package be.cytomine.appengine.exceptions;

public class TaskNotFoundException extends Exception {

    public TaskNotFoundException(Exception e) {
        super(e);
    }

    public TaskNotFoundException(String message) {
        super(message);
    }
}
