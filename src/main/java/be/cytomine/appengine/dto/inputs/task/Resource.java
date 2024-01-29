package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.states.TaskRunState;
import lombok.AllArgsConstructor;
import lombok.Data;


import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Resource {
    private TaskDescription task;
    private UUID id;
    private TaskRunState state;
    private Date created_at;
    private Date updated_at;
    private Date last_state_transition_at;
}
