package be.cytomine.appengine.handlers;

import java.io.File;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StorageDataEntry
{

    private File data;

    private String name;

    private String storageId;

    private StorageDataType storageDataType;


    public StorageDataEntry(File data)
    {
        this.data = data;
    }

    public StorageDataEntry(File data, String name)
    {
        this.data = data;
        this.name = name;
    }

    public StorageDataEntry(File data, String name, StorageDataType storageDataType)
    {
        this.data = data;
        this.name = name;
        this.storageDataType = storageDataType;
    }

    public StorageDataEntry(String name, StorageDataType storageDataType)
    {
        this.name = name;
        this.storageDataType = storageDataType;
    }

    public StorageDataEntry(String name)
    {
        this.name = name;
        this.storageDataType = StorageDataType.DIRECTORY;
    }

    public StorageDataEntry(String name, String storageId)
    {
        this.name = name;
        this.storageId = storageId;
    }

    public StorageDataEntry(File data, String parameterName, String storageId, StorageDataType storageDataType) {
        this.data = data;
        this.name = parameterName;
        this.storageId = storageId;
        this.storageDataType = storageDataType;
    }
}
