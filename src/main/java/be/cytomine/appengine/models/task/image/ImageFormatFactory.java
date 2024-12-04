package be.cytomine.appengine.models.task.image;

import java.util.HashMap;
import java.util.Map;

import be.cytomine.appengine.models.task.image.formats.GenericFormat;
import be.cytomine.appengine.models.task.image.formats.JPEGFormat;
import be.cytomine.appengine.models.task.image.formats.PNGFormat;
import be.cytomine.appengine.models.task.image.formats.TIFFFormat;

public class ImageFormatFactory {
    private static final Map<String, ImageFormat> formats = new HashMap<>();

    static {
        formats.put("JPEG", new JPEGFormat());
        formats.put("PNG", new PNGFormat());
        formats.put("TIFF", new TIFFFormat());
    }

    public static ImageFormat getFormat(String format) {
        return formats.get(format.toUpperCase());
    }

    public static ImageFormat getGenericFormat() {
        return new GenericFormat();
    }
}
