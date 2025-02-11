package be.cytomine.appengine.dto.misc;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TaskIdentifiers {

    private final UUID localTaskIdentifier;

    private final String storageIdentifier;

    private final String imageRegistryCompliantName;

    @Override
    public String toString() {
        return new StringBuilder("Ids [")
            .append("storage='").append(storageIdentifier).append('\'')
            .append(", image='").append(imageRegistryCompliantName).append('\'')
            .append(", ID=").append(localTaskIdentifier)
            .append(']')
            .toString();
    }
}
