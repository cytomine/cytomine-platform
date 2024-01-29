package be.cytomine.appengine.dto.inputs.task;


import be.cytomine.appengine.states.TaskRunState;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@AllArgsConstructor
public class TaskRunResponse {
    TaskDescription task;
    private UUID id;
    private TaskRunState state;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private LocalDateTime last_state_transition_at;
}
