package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class JpegFormat implements FileFormat {

    public static final byte[] SIGNATURE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

    @Override
    public boolean checkSignature(byte[] file) {
        if (file.length < SIGNATURE.length) {
            return false;
        }

        return Arrays.equals(Arrays.copyOf(file, SIGNATURE.length), SIGNATURE);
    }

    @Override
    public Dimension getDimensions(byte[] file) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file));
            return new Dimension(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean validate(byte[] file) {
        return true;
    }
}
