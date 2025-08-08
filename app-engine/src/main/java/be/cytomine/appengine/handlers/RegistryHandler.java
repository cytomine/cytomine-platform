package be.cytomine.appengine.handlers;

import java.io.InputStream;

import be.cytomine.appengine.dto.handlers.registry.DockerImage;
import be.cytomine.appengine.exceptions.RegistryException;

public interface RegistryHandler {
    void pushImage(InputStream imageInputStream, String imageName) throws RegistryException;

    void deleteImage(String imageName) throws RegistryException;
}
