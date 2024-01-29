package be.cytomine.appengine.handlers.storage.impl;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.handlers.FileStorageHandler;
import be.cytomine.appengine.handlers.FileData;
import org.springframework.stereotype.Component;

@Component public class DefaultStorageHandler implements FileStorageHandler {
    @Override
    public void createStorage(Storage storage) throws FileStorageException {
    }

    @Override
    public void deleteStorage(Storage storage) throws FileStorageException {

    }

    @Override
    public void createFile(Storage storage, FileData file) throws FileStorageException {
    }

    @Override
    public boolean checkStorageExists(Storage storage) throws FileStorageException {
        return false;
    }

    @Override
    public boolean checkStorageExists(String idStorage) throws FileStorageException {
        return false;
    }

    @Override
    public void deleteFile(FileData file) throws FileStorageException {

    }

    @Override
    public FileData readFile(FileData emptyFile) throws FileStorageException {
        return null;
    }
}
