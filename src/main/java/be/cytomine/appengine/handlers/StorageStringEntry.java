package be.cytomine.appengine.handlers;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class StorageStringEntry extends StorageDataEntry {

    private String dataAsString;

    public StorageStringEntry(String data, String name, StorageDataType storageDataType) {
        this.dataAsString = data;
        super.setName(name);
        super.setStorageDataType(storageDataType);
    }

}
