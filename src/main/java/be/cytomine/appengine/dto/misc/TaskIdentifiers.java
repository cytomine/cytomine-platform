package be.cytomine.appengine.dto.misc;

import lombok.Getter;

import java.util.UUID;

@Getter
public class TaskIdentifiers {

    private final String storageIdentifier;
    private final String imageRegistryCompliantName;
    private final UUID localTaskIdentifier;
    public TaskIdentifiers(UUID localTaskIdentifier, String storageIdentifier, String imageRegistryCompliantName) {
        super();
        this.storageIdentifier = storageIdentifier;
        this.imageRegistryCompliantName = imageRegistryCompliantName;
        this.localTaskIdentifier = localTaskIdentifier;
    }

    @Override
    public String toString() {
        return "Ids [" +
                "storage ='" + storageIdentifier + '\'' +
                ", image ='" + imageRegistryCompliantName + '\'' +
                ", ID =" + localTaskIdentifier +
                ']';
    }
}
