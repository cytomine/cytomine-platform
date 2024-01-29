package be.cytomine.appengine.dto.handlers.scheduler;

import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import lombok.Data;

import java.util.UUID;

@Data
public class Schedule {
    private Run run;
    // TODO : add resource , constraints and references
}
