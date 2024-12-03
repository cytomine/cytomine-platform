package be.cytomine.appengine.exceptions;

public class ParseException extends Exception {

    public ParseException(Exception e) {
        super(e);
    }

    public ParseException(String message) {
        super(message);
    }
}
