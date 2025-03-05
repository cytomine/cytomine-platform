package be.cytomine.appengine.dto.inputs.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskOutput {
    private String id;

    @JsonProperty(value = "default")
    private int defaultValue; // this matches a reserved keyword

    private String name;

    @JsonProperty(value = "display_name")
    private String displayName;

    private String description;

    private boolean optional;

    private TaskParameterType type;

    @JsonProperty(value = "derived_from")
    private String derivedFrom;
}
