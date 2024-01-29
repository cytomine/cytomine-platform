package be.cytomine.appengine.dto.inputs.task;


import be.cytomine.appengine.states.TaskRunState;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;


@Data
@AllArgsConstructor
public class TaskRun {
    TaskDescription task;
    private UUID id;
    private TaskRunState state;
}
