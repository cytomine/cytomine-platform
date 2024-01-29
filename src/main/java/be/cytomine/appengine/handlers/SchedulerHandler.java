package be.cytomine.appengine.handlers;


import be.cytomine.appengine.dto.handlers.scheduler.Schedule;
import be.cytomine.appengine.exceptions.SchedulingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public interface SchedulerHandler {

    public Schedule schedule (Schedule schedule) throws SchedulingException;

    void alive() throws SchedulingException;

    public void monitor() throws SchedulingException;
}
