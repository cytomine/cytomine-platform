package be.cytomine.appengine.dto.inputs.task;

import lombok.Data;

import be.cytomine.appengine.states.TaskRunState;

@Data
public class State {
    private TaskRunState desired;
}
