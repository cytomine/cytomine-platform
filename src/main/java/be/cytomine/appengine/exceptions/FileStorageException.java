package be.cytomine.appengine.exceptions;

public class FileStorageException extends Exception {

    public FileStorageException(Exception e) {
        super(e);
    }

    public FileStorageException(String message) {
        super(message);
    }
}
