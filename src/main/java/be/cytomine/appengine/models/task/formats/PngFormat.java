package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PngFormat implements FileFormat {

    public static final byte[] SIGNATURE = {
        (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
        (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };

    @Override
    public boolean checkSignature(byte[] file) {
        if (file.length < SIGNATURE.length) {
            return false;
        }

        return Arrays.equals(Arrays.copyOf(file, SIGNATURE.length), SIGNATURE);
    }

    @Override
    public Dimension getDimensions(byte[] file) {
        byte[] ihdrChunk = new byte[8];
        System.arraycopy(file, 8, ihdrChunk, 0, 8);

        String ihdr = new String(ihdrChunk, 4, 4);
        if (!ihdr.equals("IHDR")) {
            throw new IllegalArgumentException("IHDR chunk not found at the expected position");
        }

        // Width and height start after the IHDR type
        byte[] widthBytes = new byte[4];
        byte[] heightBytes = new byte[4];
        System.arraycopy(file, 16, widthBytes, 0, 4);
        System.arraycopy(file, 20, heightBytes, 0, 4);

        Integer width = ByteBuffer.wrap(widthBytes).getInt();
        Integer height = ByteBuffer.wrap(heightBytes).getInt();

        return new Dimension(width, height);
    }

    @Override
    public boolean validate(byte[] file) {
        return true;
    }
}
