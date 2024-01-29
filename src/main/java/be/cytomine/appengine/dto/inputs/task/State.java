package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.states.TaskRunState;
import lombok.Data;

@Data
public class State {
    private TaskRunState desired;
}
