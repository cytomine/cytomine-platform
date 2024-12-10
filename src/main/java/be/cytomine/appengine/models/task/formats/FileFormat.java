package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;

public interface FileFormat {
    boolean checkSignature(byte[] file);

    /* Validate extra constraint */
    boolean validate(byte[] file);

    Dimension getDimensions(byte[] file);
}
