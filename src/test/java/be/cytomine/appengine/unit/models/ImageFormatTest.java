package be.cytomine.appengine.unit.models;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import be.cytomine.appengine.models.task.image.ImageFormat;
import be.cytomine.appengine.models.task.image.formats.JPEGFormat;
import be.cytomine.appengine.models.task.image.formats.PNGFormat;
import be.cytomine.appengine.models.task.image.formats.TIFFFormat;

public class ImageFormatTest {

    private static final int EXPECTED_WIDTH = 680;
    private static final int EXPECTED_HEIGHT = 512;

    private static Map<String, byte[]> images;

    @BeforeAll
    public static void setUp() throws IOException {
        images = new HashMap<>();
        images.put("PNG", loadImage("src/test/resources/artifacts/images/image.png"));
        images.put("JPEG", loadImage("src/test/resources/artifacts/images/image.jpg"));
        images.put("TIFF", loadImage("src/test/resources/artifacts/images/image.tif"));
    }

    @AfterAll
    public static void tearDown() {
        images.clear();
    }

    private static byte[] loadImage(String imagePath) throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get(imagePath))) {
            return is.readAllBytes();
        }
    }

    private static Stream<Arguments> streamImageFormat() {
        return Stream.of(
            Arguments.of(new PNGFormat(), "PNG"),
            Arguments.of(new JPEGFormat(), "JPEG"),
            Arguments.of(new TIFFFormat(), "TIFF")
        );
    }

    @ParameterizedTest
    @MethodSource("streamImageFormat")
    public void testCheckSignature(ImageFormat format, String formatKey) {
        byte[] image = images.get(formatKey);
        Assertions.assertTrue(format.checkSignature(image), "Image signature should be valid.");
    }

    @ParameterizedTest
    @MethodSource("streamImageFormat")
    public void testGetDimensions(ImageFormat format, String formatKey) {
        byte[] image = images.get(formatKey);
        Dimension dimension = format.getDimensions(image);
        Assertions.assertEquals(EXPECTED_WIDTH, dimension.getWidth(), "Width should be " + EXPECTED_WIDTH + " pixels.");
        Assertions.assertEquals(EXPECTED_HEIGHT, dimension.getHeight(), "Height should be " + EXPECTED_HEIGHT + " pixels.");
    }

    @ParameterizedTest
    @MethodSource("streamImageFormat")
    public void testValidate(ImageFormat format, String formatKey) {
        byte[] image = images.get(formatKey);
        Assertions.assertTrue(format.validate(image), "Validation should return true.");
    }
}
