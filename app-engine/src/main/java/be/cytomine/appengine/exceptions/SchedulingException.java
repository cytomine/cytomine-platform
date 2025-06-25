package be.cytomine.appengine.exceptions;

public class SchedulingException extends Exception {

    public SchedulingException(Exception e) {
        super(e);
    }

    public SchedulingException(String message) {
        super(message);
    }
}
