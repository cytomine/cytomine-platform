package be.cytomine.appengine.dto.inputs.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskInput {
    private String id;

    @JsonProperty(value = "default")
    private String defaultValue; // this matches a java reserved keyword

    private String name;

    @JsonProperty(value = "display_name")
    private String displayName;

    private String description;

    private boolean optional;

    private TaskParameterType type;
}
