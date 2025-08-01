package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

public class DicomFormat implements FileFormat {

    public static final byte[] SIGNATURE = { (byte) 0x44, (byte) 0x49, (byte) 0x43, (byte) 0x4D };

    @Override
    public boolean checkSignature(File file) {
        if (file.length() < SIGNATURE.length) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileSignature = new byte[SIGNATURE.length];
            int bytesRead = fis.read(fileSignature);

            return bytesRead == SIGNATURE.length && Arrays.equals(fileSignature, SIGNATURE);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean validate(File file) {
        return true;
    }

    @Override
    public Dimension getDimensions(File file) {
        try (DicomInputStream dis = new DicomInputStream(file)) {
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
