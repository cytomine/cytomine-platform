package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

public class DicomFormat implements FileFormat {

    public static final byte[] SIGNATURE = { (byte) 0x44, (byte) 0x49, (byte) 0x43, (byte) 0x4D };

    @Override
    public boolean checkSignature(byte[] file) {
        if (file.length < SIGNATURE.length) {
            return false;
        }

        return Arrays.equals(Arrays.copyOf(file, SIGNATURE.length), SIGNATURE);
    }

    @Override
    public boolean validate(byte[] file) {
        return true;
    }

    @Override
    public Dimension getDimensions(byte[] file) {
        ByteArrayInputStream bis = new ByteArrayInputStream(file);
        try (DicomInputStream dis = new DicomInputStream(bis)) {
            Attributes attributes = dis.readDataset();

            int width = attributes.getInt(Tag.Columns, -1);
            int height = attributes.getInt(Tag.Rows, -1);
            if (width == -1 || height == -1) {
                return null;
            }

            return new Dimension(width, height);
        } catch (IOException e) {
            return null;
        }
    }
}
