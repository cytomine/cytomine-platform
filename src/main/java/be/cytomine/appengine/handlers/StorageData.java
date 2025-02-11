package be.cytomine.appengine.handlers;

import lombok.Data;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

@Data
public class StorageData {
    Queue<StorageDataEntry> queue;

    public StorageData(StorageData other) {
        if (other != null && other.getQueue() != null){
            this.queue = new LinkedList<>();
            this.queue.addAll(other.getQueue());
        }

    }


    public StorageData(StorageDataEntry root) {
        queue = new LinkedList<>();
        queue.add(root);
    }
    // useful to create files in StorageData
    public StorageData(byte[] data, String name) {
        StorageDataEntry root = new StorageDataEntry(data , name , StorageDataType.FILE);
        queue = new LinkedList<>();
        queue.add(root);
    }
    // useful to create a directory in StorageData
    public StorageData(String name) {
        StorageDataEntry root = new StorageDataEntry(name);
        root.setStorageDataType(StorageDataType.DIRECTORY);
        queue = new LinkedList<>();
        queue.add(root);
    }

    public StorageData(byte[] fileData) {
        StorageDataEntry root = new StorageDataEntry(fileData);
        queue = new LinkedList<>();
        queue.add(root);
    }

    public StorageData(String parameterName, String storageId) {
        StorageDataEntry root = new StorageDataEntry(parameterName, storageId);
        queue = new LinkedList<>();
        queue.add(root);
    }

    public StorageData() {
        queue = new LinkedList<>();
    }

    public StorageDataEntry peek() {
        return queue.peek();
    }

    public boolean add(StorageDataEntry entry) {
        if(Objects.isNull(entry)) {
            return false; // null values are not allowed in StorageData
        }
        return queue.add(entry);
    }

    public StorageDataEntry poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
    // used to merge two StorageData together just like merging two directories together
    // files and directories with the same name are duplicated .. it's now used in a safe context
    // and duplicates are guaranteed not to exist.
    public boolean merge(StorageData other) {
        if(Objects.isNull(other) || other.isEmpty()) { return false;}
        int sizeBeforeMerge = queue.size();
        queue.addAll(other.getQueue());
        return queue.size() == (sizeBeforeMerge + other.getQueue().size());
    }
}
