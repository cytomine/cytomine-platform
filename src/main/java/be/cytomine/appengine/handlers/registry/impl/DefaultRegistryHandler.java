package be.cytomine.appengine.handlers.registry.impl;

import be.cytomine.appengine.dto.handlers.registry.DockerImage;
import be.cytomine.appengine.exceptions.RegistryException;
import be.cytomine.appengine.handlers.RegistryHandler;

public class DefaultRegistryHandler implements RegistryHandler {
    @Override
    public boolean checkImage(DockerImage image) throws RegistryException {
        // TODO is the check actually a possible outcome of <pushImage> operation?
        return false;
    }

    @Override
    public void pushImage(DockerImage image) throws RegistryException {

    }
}
