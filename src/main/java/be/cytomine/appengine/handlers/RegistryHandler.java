package be.cytomine.appengine.handlers;

import be.cytomine.appengine.dto.handlers.registry.DockerImage;
import be.cytomine.appengine.exceptions.RegistryException;

public interface RegistryHandler {
    boolean checkImage(DockerImage image) throws RegistryException;

    void pushImage(DockerImage image) throws RegistryException;
}
