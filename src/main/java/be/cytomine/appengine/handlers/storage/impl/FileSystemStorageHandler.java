package be.cytomine.appengine.handlers.storage.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

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
                    String identifier = getIdentifier(storageId);
                    current.setChecksumCRC32(UUID.fromString(identifier), calculateFileCRC32(current.getData()));
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

    private static String getIdentifier(String storageId)
    {
        String identifier = "";
        if(storageId.startsWith("task-") && storageId.endsWith("-def")) { // task storage
            identifier = storageId.replace("task-", "");
            identifier = identifier.replace("-def" , "");
        } else if(storageId.startsWith("task-run-inputs-")) { // inputs
            identifier = storageId.replace("task-run-inputs-", "");
        } else if(storageId.startsWith("task-run-outputs-")) { // outputs
            identifier = storageId.replace("task-run-outputs-", "");
        }
        return identifier;
    }

    private long calculateFileCRC32(File file) throws IOException {
        Checksum crc32 = new CRC32();
        // Use try-with-resources to ensure the input stream is closed automatically
        // BufferedInputStream is used for efficient reading in chunks
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192]; // Define a buffer size (e.g., 8KB)
            int bytesRead;

            // Read the file chunk by chunk and update the CRC32 checksum
            while ((bytesRead = is.read(buffer)) != -1) {
                crc32.update(buffer, 0, bytesRead);
            }
        }
        return crc32.getValue(); // Return the final CRC32 value
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
        emptyFile.getEntryList().clear();
        String filename = current.getName();
        Path filePath = Paths.get(basePath, current.getStorageId(), filename);
        AtomicBoolean currentUsed = new AtomicBoolean(false);
        try {
            Files.walk(filePath).forEach(path -> {
                StorageDataEntry entry;
                if (currentUsed.get()) {
                    entry = new StorageDataEntry();
                } else {
                    entry = current;
                }
                if (Files.isRegularFile(path)) {
                    entry.setData(path.toFile());
                    String fromStorageId = path
                        .toString()
                        .substring(path.toString().indexOf(current.getStorageId()));
                    String subTreeFileName = fromStorageId
                        .substring(current.getStorageId().length() + 1);
                    if (!subTreeFileName.equalsIgnoreCase(filename)) {
                        entry.setName(subTreeFileName);
                    }
                    entry.setStorageDataType(StorageDataType.FILE);
                    emptyFile.getEntryList().add(entry);
                }

                if (Files.isDirectory(path)) {
                    entry.setStorageDataType(StorageDataType.DIRECTORY);
                    String fromStorageId = path
                        .toString()
                        .substring(path.toString().indexOf(current.getStorageId()));
                    String subTreeFileName = fromStorageId
                        .substring(current.getStorageId().length() + 1);
                    if (!subTreeFileName.equalsIgnoreCase(filename)) {
                        entry.setName(subTreeFileName);
                    }
                    emptyFile.getEntryList().add(entry);
                }
                currentUsed.set(true);
            });

            return emptyFile;
        } catch (IOException e) {
            emptyFile.getEntryList().clear();
            throw new FileStorageException("Failed to read file " + filename);
        }
    }
}
