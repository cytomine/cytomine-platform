package be.cytomine.appengine.dto.handlers.scheduler;

import lombok.Data;

import be.cytomine.appengine.models.task.Run;

@Data
public class Schedule {
    private Run run;
    // TODO : add resource , constraints and references
}
