package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class GenericFormat implements FileFormat {

    @Override
    public boolean checkSignature(byte[] file) {
        return true;
    }

    @Override
    public boolean validate(byte[] file) {
        return true;
    }

    @Override
    public Dimension getDimensions(byte[] file) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(file)) {
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                throw new IOException("Unable to read image");
            }

            return new Dimension(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            return null;
        }
    }
}
