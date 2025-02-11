package be.cytomine.appengine.dto.inputs.task;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

import be.cytomine.appengine.states.TaskRunState;

@Data
@AllArgsConstructor
public class TaskRun {
    private UUID id;

    private TaskDescription task;

    private TaskRunState state;
}
