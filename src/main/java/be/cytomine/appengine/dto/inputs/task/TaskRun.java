package be.cytomine.appengine.dto.inputs.task;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.states.TaskRunState;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskRun {
    private UUID id;

    private TaskDescription task;

    private TaskRunState state;
}
