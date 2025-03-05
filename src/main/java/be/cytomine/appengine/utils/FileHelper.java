package be.cytomine.appengine.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class FileHelper {
    public static String read(File file, Charset charset) {
        try {
            return Files.readString(file.toPath(), charset).trim();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        }
    }

    public static File write(String filename, byte[] content) {
        try {
            File data = Files.createTempFile(filename, null).toFile();
            try (FileOutputStream fos = new FileOutputStream(data)) {
                fos.write(content);
            }
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write content to file", e);
        }
    }
}
