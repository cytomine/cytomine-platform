package be.cytomine.appengine.handlers.scheduler.impl;

import be.cytomine.appengine.dto.handlers.scheduler.Schedule;
import be.cytomine.appengine.exceptions.SchedulingException;
import be.cytomine.appengine.handlers.SchedulerHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DefaultScheduler implements SchedulerHandler {

    public DefaultScheduler() {

    }

    @Override
    public Schedule schedule(Schedule schedule) throws SchedulingException {
        return null;
    }

    @Override
    public void alive() throws SchedulingException{

    }

    @Override
    public void monitor() throws SchedulingException {

    }
}
