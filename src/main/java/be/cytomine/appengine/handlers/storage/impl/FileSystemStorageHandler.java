package be.cytomine.appengine.handlers.storage.impl;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.handlers.FileStorageHandler;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class FileSystemStorageHandler implements FileStorageHandler {

    @Value("${storage.base-path}")
    private String basePath;

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
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            String error = "Failed to delete storage " + storageId + ": " + e.getMessage();
            throw new FileStorageException(error);
        }
    }

    @Override
    public void createFile(Storage storage, FileData file) throws FileStorageException {
        String filename = file.getFileName();
        String storageId = storage.getIdStorage();

        try {
            Path filePath = Paths.get(basePath, storageId, filename);
            Files.write(filePath, file.getFileData());
        } catch (IOException e) {
            String error = "Failed to create file " + filename;
            error += " in storage " + storageId + ": " + e.getMessage();
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
    public void deleteFile(FileData file) throws FileStorageException {
        String filename = file.getFileName();

        try {
            Path filePath = Paths.get(basePath, filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file " + filename);
        }
    }

    @Override
    public FileData readFile(FileData emptyFile) throws FileStorageException {
        String filename = emptyFile.getFileName();

        try {
            Path filePath = Paths.get(basePath, filename);
            byte[] data = Files.readAllBytes(filePath);
            emptyFile.setFileData(data);

            return emptyFile;
        } catch (IOException e) {
            throw new FileStorageException("Failed to read file " + filename);
        }
    }
}
