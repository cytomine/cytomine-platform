package be.cytomine.appengine.models.task.image;

import java.awt.Dimension;

public interface ImageFormat {
    boolean checkSignature(byte[] file);

    /* Validate extra constraint */
    boolean validate(byte[] file);

    Dimension getDimensions(byte[] file);
}
