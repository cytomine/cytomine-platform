package be.cytomine.appengine.dto.handlers.filestorage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Storage {
    private String idStorage;

    public Storage() {
    }

    public Storage(String storageIdentifier) {
        this.idStorage = storageIdentifier;
    }
}
