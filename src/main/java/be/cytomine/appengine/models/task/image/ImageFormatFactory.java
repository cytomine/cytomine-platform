package be.cytomine.appengine.models.task.image;

import java.util.HashMap;
import java.util.Map;

import be.cytomine.appengine.models.task.formats.FileFormat;
import be.cytomine.appengine.models.task.formats.GenericFormat;
import be.cytomine.appengine.models.task.formats.JpegFormat;
import be.cytomine.appengine.models.task.formats.PngFormat;
import be.cytomine.appengine.models.task.formats.TiffFormat;

public class ImageFormatFactory {
    private static final Map<String, FileFormat> formats = new HashMap<>();

    static {
        formats.put("JPEG", new JpegFormat());
        formats.put("PNG", new PngFormat());
        formats.put("TIFF", new TiffFormat());
    }

    public static FileFormat getFormat(String format) {
        return formats.get(format.toUpperCase());
    }

    public static FileFormat getGenericFormat() {
        return new GenericFormat();
    }
}
