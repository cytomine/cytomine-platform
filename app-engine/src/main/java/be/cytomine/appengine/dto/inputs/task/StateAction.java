package be.cytomine.appengine.dto.inputs.task;

import lombok.Data;

@Data
public class StateAction {
    private String status;

    private Resource resource;
}
