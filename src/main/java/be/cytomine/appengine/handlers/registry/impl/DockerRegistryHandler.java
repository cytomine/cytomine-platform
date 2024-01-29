package be.cytomine.appengine.handlers.registry.impl;

import be.cytomine.appengine.dto.handlers.registry.DockerImage;
import be.cytomine.appengine.exceptions.RegistryException;
import be.cytomine.appengine.handlers.RegistryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import com.cytomine.registry.client.RegistryClient;

public class DockerRegistryHandler implements RegistryHandler {

    private String password;
    private String user;
    private boolean authenticated;
    private String port;
    private String host;
    private String scheme;
    Logger logger = LoggerFactory.getLogger(DockerRegistryHandler.class);

    public DockerRegistryHandler(String registryHost, String registryPort, String registryScheme, boolean authenticated, String registryUsername, String registryPassword) throws IOException {
        RegistryClient.config(registryScheme, registryHost, registryPort);
        if (authenticated)
            RegistryClient.authenticate(registryUsername, registryPassword);
        logger.info("Image Registry Handler : Docker Registry initialized");
    }

    @Override
    public boolean checkImage(DockerImage image) throws RegistryException {
        // TODO implement image validation by registry if exists
        return false;
    }

    @Override
    public void pushImage(DockerImage image) throws RegistryException {
        logger.info("Image Registry Handler : pushing image...");
        byte[] imageTarData = image.getDockerImageData();
        InputStream imageTarDataInputStream = new ByteArrayInputStream(imageTarData);
        String imageNameWithRegistry = image.getImageName();
        try {
            RegistryClient.push(imageTarDataInputStream, imageNameWithRegistry);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RegistryException("Docker Registry Handler : failed to push image to registry");
        }
        logger.info("Image Registry Handler : image pushed");

    }


}
