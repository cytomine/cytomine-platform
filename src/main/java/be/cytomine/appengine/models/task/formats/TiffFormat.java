package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;

public class TiffFormat implements FileFormat {

    public static final byte[] LE_SIGNATURE = {
            (byte) 0x49, (byte) 0x49, (byte) 0x2A, (byte) 0x00
    };

    public static final byte[] BE_SIGNATURE = {
            (byte) 0x4D, (byte) 0x4D, (byte) 0x00, (byte) 0x2A
    };

    @Override
    public boolean checkSignature(byte[] file) {
        if (file.length < LE_SIGNATURE.length && file.length < BE_SIGNATURE.length) {
            return false;
        }

        // Check little-endian signature
        if (Arrays.equals(Arrays.copyOf(file, LE_SIGNATURE.length), LE_SIGNATURE)) {
            return true;
        }

        // Check big-endian signature
        if (Arrays.equals(Arrays.copyOf(file, BE_SIGNATURE.length), BE_SIGNATURE)) {
            return true;
        }

        return false;
    }

    @Override
    public Dimension getDimensions(byte[] file) {
        TiffImageParser parser = new TiffImageParser();
        try {
            return parser.getImageSize(file);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean validate(byte[] file) {
        return isPlanar(file);
    }

    public boolean isPlanar(byte[] file) {
        TiffImageParser parser = new TiffImageParser();
        TiffImageMetadata metadata = null;

        try {
            metadata = (TiffImageMetadata) parser.getMetadata(file);
        } catch (IOException e) {
            return false;
        }

        List<TiffField> fields = metadata.getAllFields();

        TiffField samplesPerPixel = fields
                .stream()
                .filter(field -> field.getTagInfo().name.equals("SamplesPerPixel"))
                .findFirst()
                .orElse(null);

        TiffField tileWidth = fields
                .stream()
                .filter(field -> field.getTagInfo().name.equals("TileWidth"))
                .findFirst()
                .orElse(null);

        try {
            boolean isRGB = samplesPerPixel.getIntValue() == 3;
            boolean isTiled = tileWidth != null && tileWidth.getIntValue() != 0;
            return isRGB && !isTiled;
        } catch (ImagingException e) {
            return false;
        }
    }
}
