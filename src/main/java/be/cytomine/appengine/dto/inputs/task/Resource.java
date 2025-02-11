package be.cytomine.appengine.dto.inputs.task;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import be.cytomine.appengine.states.TaskRunState;

@Data
@AllArgsConstructor
public class Resource {
    private UUID id;

    private TaskDescription task;

    private TaskRunState state;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    @JsonProperty("last_state_transition_at")
    private Date lastStateTransitionAt;
}
