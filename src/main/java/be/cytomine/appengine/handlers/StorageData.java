package be.cytomine.appengine.handlers;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import lombok.Data;

@Data
public class StorageData {
    List<StorageDataEntry> entryList;

    public StorageData(StorageData other) {
        if (other != null && other.getEntryList() != null) {
            this.entryList = new LinkedList<>();
            this.entryList.addAll(other.getEntryList());
        }
    }

    public StorageData(StorageDataEntry root) {
        entryList = new LinkedList<>();
        entryList.add(root);
    }

    // useful to create files in StorageData
    public StorageData(File data, String name) {
        StorageDataEntry root = new StorageDataEntry(data, name, StorageDataType.FILE);
        entryList = new LinkedList<>();
        entryList.add(root);
    }

    // useful to create a directory in StorageData
    public StorageData(String name) {
        StorageDataEntry root = new StorageDataEntry(name);
        root.setStorageDataType(StorageDataType.DIRECTORY);
        entryList = new LinkedList<>();
        entryList.add(root);
    }

    public StorageData(File fileData) {
        StorageDataEntry root = new StorageDataEntry(fileData);
        entryList = new LinkedList<>();
        entryList.add(root);
    }

    public StorageData(String parameterName, String storageId) {
        StorageDataEntry root = new StorageDataEntry(parameterName, storageId);
        entryList = new LinkedList<>();
        entryList.add(root);
    }

    public StorageData() {
        entryList = new LinkedList<>();
    }

    public StorageDataEntry peek() {
        return entryList.stream().findFirst().orElse(null);
    }

    public boolean add(StorageDataEntry entry) {
        if (Objects.isNull(entry)) {
            return false; // null values are not allowed in StorageData
        }
        return entryList.add(entry);
    }

    public boolean isEmpty() {
        return entryList.isEmpty();
    }

    public boolean merge(StorageData other) {
        if (Objects.isNull(other) || other.isEmpty()) {
            return false;
        }

        int sizeBeforeMerge = entryList.size();
        entryList.addAll(other.getEntryList());

        return entryList.size() == (sizeBeforeMerge + other.getEntryList().size());
    }
}
