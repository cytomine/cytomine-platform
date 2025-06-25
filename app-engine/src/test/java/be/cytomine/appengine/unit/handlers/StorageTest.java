package be.cytomine.appengine.unit.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageDataEntry;
import be.cytomine.appengine.handlers.StorageDataType;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.handlers.storage.impl.FileSystemStorageHandler;
import be.cytomine.appengine.utils.FileHelper;

public class StorageTest {

    private static StorageHandler storageHandler;

    private static String basePath;

    @BeforeAll
    public static void init() {
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource("application.yml"));
        Properties properties = yamlFactory.getObject();
        assert properties != null;
        String property = properties.getProperty("storage.base-path");
        basePath = property.substring(property.indexOf(':'), property.lastIndexOf('}'));
        storageHandler = new FileSystemStorageHandler(basePath);
    }

    @AfterAll
    public static void cleanUp() throws IOException {
        Path dirPath = Paths.get(basePath + "/main");
        Files.walk(dirPath)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    @DisplayName("Testing successful directory storage")
    public void successfulDirectoryStorageDataSave() throws FileStorageException {
        // creating directory
        Storage dir = new Storage("main");
        storageHandler.createStorage(dir);

        Path filePath = Paths.get(basePath, dir.getIdStorage());
        Assertions.assertTrue(Files.exists(filePath));
    }

    @Test
    @DisplayName("Testing successful file storage")
    public void successfulFileStorageDataSave() throws IOException, FileStorageException {
        // Storing a primitive type integer file
        int value = 200;
        String valueString = new ObjectMapper().writeValueAsString(value);
        byte[] valueBytes = valueString.getBytes(StandardCharsets.UTF_8);

        StorageData integerStorageData = new StorageData(FileHelper.write("a", valueBytes), "a");
        Storage dir = new Storage("main");
        storageHandler.createStorage(dir);
        storageHandler.saveStorageData(dir, integerStorageData);

        Path filePath = Paths.get(basePath, dir.getIdStorage(), "a");
        Assertions.assertTrue(Files.exists(filePath));
    }

    @Test
    @DisplayName("Testing successful nested directory structure")
    public void successfulNestDirectoriesStorageDataSave()
        throws IOException, FileStorageException {
        // Storing nested directories
        int value = 200;
        String valueString = new ObjectMapper().writeValueAsString(value);
        byte[] valueBytes = valueString.getBytes(StandardCharsets.UTF_8);

        StorageData nestedDirectory = new StorageData();
        StorageDataEntry mainDirectory = new StorageDataEntry(
            "folder",
            StorageDataType.DIRECTORY
        );
        StorageDataEntry subDirectory = new StorageDataEntry(
            "folder/subdir",
            StorageDataType.DIRECTORY
        );
        String randomText = "This is a random text";
        StorageDataEntry randomTextFile = new StorageDataEntry(
            FileHelper.write("random", randomText.getBytes(StandardCharsets.UTF_8)),
            "folder/subdir/random",
            StorageDataType.FILE
        );
        StorageDataEntry integerTextFile = new StorageDataEntry(
            FileHelper.write("integer", valueBytes),
            "integer",
            StorageDataType.FILE
        );

        nestedDirectory.add(integerTextFile);
        nestedDirectory.add(mainDirectory);
        nestedDirectory.add(subDirectory);
        nestedDirectory.add(randomTextFile);

        Storage dir = new Storage("main");
        storageHandler.saveStorageData(dir, nestedDirectory);

        Path integerPath = Paths.get(basePath + "/main/integer");
        Path folderPath = Paths.get(basePath + "/main/folder");
        Path subdirPath = Paths.get(basePath + "/main/folder/subdir");
        Path randomPath = Paths.get(basePath + "/main/folder/subdir/random");

        Assertions.assertTrue(Files.exists(integerPath));
        Assertions.assertTrue(Files.exists(folderPath));
        Assertions.assertTrue(Files.exists(subdirPath));
        Assertions.assertTrue(Files.exists(randomPath));
    }

    @Test
    @DisplayName("Testing successful nested directory structure merger")
    public void successfulNestDirectoriesStorageDataMergeSave()
        throws IOException, FileStorageException {
        // Storing nested directories
        int value = 200;
        String valueString = new ObjectMapper().writeValueAsString(value);
        byte[] valueBytes = valueString.getBytes(StandardCharsets.UTF_8);

        StorageData nestedDirectory = new StorageData();
        StorageDataEntry mainDirectory = new StorageDataEntry(
            "folder",
            StorageDataType.DIRECTORY
        );
        StorageDataEntry subDirectory = new StorageDataEntry(
            "folder/subdir",
            StorageDataType.DIRECTORY
        );
        String randomText = "This is a random text";
        StorageDataEntry randomTextFile = new StorageDataEntry(
            FileHelper.write("random", randomText.getBytes(StandardCharsets.UTF_8)),
            "folder/subdir/random",
            StorageDataType.FILE
        );
        StorageDataEntry integerTextFile = new StorageDataEntry(
            FileHelper.write("integer", valueBytes),
            "integer",
            StorageDataType.FILE
        );

        nestedDirectory.add(integerTextFile);
        nestedDirectory.add(mainDirectory);
        nestedDirectory.add(subDirectory);
        nestedDirectory.add(randomTextFile);

        StorageData secondNestedDirectory = new StorageData();
        String orderedText = "This is an ordered text";
        StorageDataEntry orderedTextFile = new StorageDataEntry(
            FileHelper.write("ordered", orderedText.getBytes(StandardCharsets.UTF_8)),
            "folder/subdir/ordered",
            StorageDataType.FILE
        );

        secondNestedDirectory.add(orderedTextFile);

        // merge
        nestedDirectory.merge(secondNestedDirectory);

        Storage dir = new Storage("main");
        storageHandler.saveStorageData(dir, nestedDirectory);

        Path integerPath = Paths.get(basePath + "/main/integer");
        Path folderPath = Paths.get(basePath + "/main/folder");
        Path subdirPath = Paths.get(basePath + "/main/folder/subdir");
        Path randomPath = Paths.get(basePath + "/main/folder/subdir/random");
        Path orderedPath = Paths.get(basePath + "/main/folder/subdir/ordered");

        Assertions.assertTrue(Files.exists(integerPath));
        Assertions.assertTrue(Files.exists(folderPath));
        Assertions.assertTrue(Files.exists(subdirPath));
        Assertions.assertTrue(Files.exists(randomPath));
        Assertions.assertTrue(Files.exists(orderedPath));
    }
}
