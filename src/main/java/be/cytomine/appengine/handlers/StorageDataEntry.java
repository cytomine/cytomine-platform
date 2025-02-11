package be.cytomine.appengine.handlers;

import lombok.Data;


@Data
public class StorageDataEntry {

    private byte[] data;
    private String name;
    private String storageId;
    private StorageDataType storageDataType;

    public StorageDataEntry(byte[] data, String name, String storageId, StorageDataType storageDataType) {
        this.data = data;
        this.name = name;
        this.storageId = storageId;
        this.storageDataType = storageDataType;
    }

    public StorageDataEntry(byte[] data) {
        this.data = data;
    }

    public StorageDataEntry(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    public StorageDataEntry(byte[] data, String name, StorageDataType storageDataType) {
        this.data = data;
        this.name = name;
        this.storageDataType = storageDataType;
    }

    public StorageDataEntry( String name, StorageDataType storageDataType) {
        this.name = name;
        this.storageDataType = storageDataType;
    }

    public StorageDataEntry(String name) {
        this.name = name;
        this.storageDataType = StorageDataType.DIRECTORY;
    }

    public StorageDataEntry(String name, String storageId) {
        this.name = name;
        this.storageId = storageId;
    }
}
