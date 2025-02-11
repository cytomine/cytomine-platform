package be.cytomine.appengine.handlers.registry.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.cytomine.registry.client.RegistryClient;
import lombok.extern.slf4j.Slf4j;

import be.cytomine.appengine.dto.handlers.registry.DockerImage;
import be.cytomine.appengine.exceptions.RegistryException;
import be.cytomine.appengine.handlers.RegistryHandler;

@Slf4j
public class DockerRegistryHandler implements RegistryHandler {

    public DockerRegistryHandler(
        String registryHost,
        String registryPort,
        String registryScheme,
        boolean authenticated,
        String registryUsername,
        String registryPassword
    ) throws IOException {
        RegistryClient.config(registryScheme, registryHost, registryPort);
        if (authenticated) {
            RegistryClient.authenticate(registryUsername, registryPassword);
        }

        log.info("Docker Registry Handler: initialised");
    }

    @Override
    public boolean checkImage(DockerImage image) throws RegistryException {
        return false;
    }

    @Override
    public void pushImage(DockerImage image) throws RegistryException {
        log.info("Docker Registry Handler: pushing image...");

        String imageName = image.getImageName();
        File imageData = image.getImageData();

        try (InputStream inputStream = new FileInputStream(imageData)) {
            RegistryClient.push(inputStream, imageName);
            log.info("Docker Registry Handler: image pushed");
        } catch (FileNotFoundException e) {
            log.error("Image data file not found: {}", imageData.getAbsolutePath(), e);
            throw new RegistryException("Docker Registry Handler: image data file not found");
        } catch (IOException e) {
            log.error("Error reading image data from file: {}", imageData.getAbsolutePath(), e);
            throw new RegistryException("Docker Registry Handler: failed to read image data");
        } catch (Exception e) {
            log.error("Failed to push image: {}", imageName, e);
            throw new RegistryException("Docker Registry Handler: failed to push the image to registry");
        }
    }
}
