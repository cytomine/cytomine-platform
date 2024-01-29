package be.cytomine.appengine.handlers;

import lombok.Data;

@Data
public class FileData {

    private byte[] fileData;
    private String fileName;
    private String storageId;

    public FileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public FileData(byte[] fileData, String fileName) {
        this.fileData = fileData;
        this.fileName = fileName;
    }

    public FileData(String fileName, String storageId) {
        this.fileName = fileName;
        this.storageId = storageId;
    }
}
