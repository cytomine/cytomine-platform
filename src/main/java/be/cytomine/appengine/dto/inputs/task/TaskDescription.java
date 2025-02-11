package be.cytomine.appengine.dto.inputs.task;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskDescription {
    @JsonProperty(defaultValue = "")
    private UUID id;

    @JsonProperty(defaultValue = "")
    private String name;

    @JsonProperty(defaultValue = "")
    private String namespace;

    @JsonProperty(defaultValue = "")
    private String version;

    @JsonProperty(defaultValue = "")
    private String description;

    private Set<TaskAuthor> authors;

    public TaskDescription(
        UUID id,
        String name,
        String namespace,
        String version,
        String description
    ) {
        this.id = id;
        this.name = name;
        this.namespace = namespace;
        this.version = version;
        this.description = description == null ? "" : description;
    }
}
