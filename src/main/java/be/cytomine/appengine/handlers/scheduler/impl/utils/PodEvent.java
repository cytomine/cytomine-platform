package be.cytomine.appengine.handlers.scheduler.impl.utils;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class PodEvent extends ApplicationEvent {

    private Map<String, String> labels;

    public PodEvent(Object source, Map<String, String> labels) {
        super(source);

        this.labels = labels;
    }

}
