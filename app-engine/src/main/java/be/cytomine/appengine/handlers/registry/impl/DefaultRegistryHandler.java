package be.cytomine.appengine.handlers.registry.impl;

import java.io.InputStream;

import be.cytomine.appengine.exceptions.RegistryException;
import be.cytomine.appengine.handlers.RegistryHandler;

public class DefaultRegistryHandler implements RegistryHandler {

    @Override
    public void pushImage(InputStream imageInputStream, String imageName) throws RegistryException {}

    @Override
    public void deleteImage(String imageName) throws RegistryException{}
}
