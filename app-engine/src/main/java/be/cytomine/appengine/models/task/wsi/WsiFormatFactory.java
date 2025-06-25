package be.cytomine.appengine.models.task.wsi;

import java.util.HashMap;
import java.util.Map;

import be.cytomine.appengine.models.task.formats.DicomFormat;
import be.cytomine.appengine.models.task.formats.FileFormat;

public class WsiFormatFactory {
    private static final Map<String, FileFormat> formats = new HashMap<>();

    static {
        formats.put("DICOM", new DicomFormat());
    }

    public static FileFormat getFormat(String format) {
        return formats.get(format.toUpperCase());
    }

    public static FileFormat getGenericFormat() {
        return new DicomFormat();
    }
}
