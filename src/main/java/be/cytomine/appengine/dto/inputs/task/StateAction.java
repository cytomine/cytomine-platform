package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.states.TaskRunState;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
public class StateAction {
    private String status;
    private Resource resource;
}


