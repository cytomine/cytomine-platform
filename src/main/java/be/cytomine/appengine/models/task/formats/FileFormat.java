package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;
import java.io.File;

public interface FileFormat {
    boolean checkSignature(File file);

    /* Validate extra constraint */
    boolean validate(File file);

    Dimension getDimensions(File file);
}
