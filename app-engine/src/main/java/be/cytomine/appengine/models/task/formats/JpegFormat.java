package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class JpegFormat implements FileFormat {

    public static final byte[] SIGNATURE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

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
    public Dimension getDimensions(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            return new Dimension(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean validate(File file) {
        return true;
    }
}
