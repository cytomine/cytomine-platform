package be.cytomine.appengine.dto.inputs.task;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import be.cytomine.appengine.states.TaskRunState;

@Data
@AllArgsConstructor
public class TaskRunResponse {
    private UUID id;

    private TaskDescription task;

    private TaskRunState state;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("last_state_transition_at")
    private LocalDateTime lastStateTransitionAt;
}
