package be.cytomine.appengine.handlers.storage.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageDataEntry;
import be.cytomine.appengine.handlers.StorageDataType;
import be.cytomine.appengine.handlers.StorageHandler;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileSystemStorageHandler implements StorageHandler {

    @Value("${storage.base-path}")
    private String basePath;

    public void saveStorageData(
        Storage storage,
        StorageData storageData
    ) throws FileStorageException {
        if (storageData.peek() == null) {
            return;
        }

        for (StorageDataEntry current : storageData.getEntryList()) {
            String filename = current.getName();
            String storageId = storage.getIdStorage();
            // process the node here
            if (current.getStorageDataType() == StorageDataType.FILE) {
                try {
                    Path filePath = Paths.get(basePath, storageId, filename);
                    Files.createDirectories(filePath.getParent());

                    try (InputStream inputStream = new FileInputStream(current.getData())) {
                        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    String error = "Failed to create file " + filename;
                    error += " in storage " + storageId + ": " + e.getMessage();
                    throw new FileStorageException(error);
                }
            }

            if (current.getStorageDataType() == StorageDataType.DIRECTORY) {
                Storage modifiedStorage = new Storage(storageId + current.getName());
                createStorage(modifiedStorage);
            }
        }
    }

    @Override
    public void createStorage(Storage storage) throws FileStorageException {
        String storageId = storage.getIdStorage();

        try {
            Path path = Paths.get(basePath, storageId);
            Files.createDirectories(path);
        } catch (IOException e) {
            String error = "Failed to create storage " + storageId + ": " + e.getMessage();
            throw new FileStorageException(error);
        }
    }

    @Override
    public void deleteStorage(Storage storage) throws FileStorageException {
        String storageId = storage.getIdStorage();

        try {
            Path path = Paths.get(basePath, storageId);
            Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            String error = "Failed to delete storage " + storageId + ": " + e.getMessage();
            throw new FileStorageException(error);
        }
    }

    @Override
    public boolean checkStorageExists(Storage storage) throws FileStorageException {
        return Files.exists(Paths.get(basePath, storage.getIdStorage()));
    }

    @Override
    public boolean checkStorageExists(String idStorage) throws FileStorageException {
        return Files.exists(Paths.get(basePath, idStorage));
    }

    @Override
    public void deleteStorageData(StorageData storageData) throws FileStorageException {
        String fileOrDirName = storageData.peek().getName();
        if (storageData.peek().getStorageDataType() == StorageDataType.FILE) {
            try {
                Path filePath = Paths.get(
                    basePath,
                    storageData.peek().getStorageId(),
                    fileOrDirName
                );
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new FileStorageException("Failed to delete file " + fileOrDirName);
            }
        }

        if (storageData.peek().getStorageDataType() == StorageDataType.DIRECTORY) {
            Storage storage = new Storage(fileOrDirName);
            deleteStorage(storage);
        }
    }

    @Override
    public StorageData readStorageData(StorageData emptyFile) throws FileStorageException {
        StorageDataEntry current = emptyFile.peek();
        String filename = current.getName();
        Path filePath = Paths.get(basePath, current.getStorageId(), filename);
        try {
            Files.walk(filePath).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    current.setData(path.toFile());
                    current.setStorageDataType(StorageDataType.FILE);
                }

                if (Files.isDirectory(path)) {
                    current.setStorageDataType(StorageDataType.DIRECTORY);
                }
            });

            return emptyFile;
        } catch (IOException e) {
            emptyFile.getEntryList().clear();
            throw new FileStorageException("Failed to read file " + filename);
        }
    }
}
