package be.cytomine.appengine.dto.handlers.registry;

import lombok.Data;

@Data
public class DockerImage {

    private byte[] dockerImageData;
    private String imageName;

    public DockerImage(byte[] dockerImageData) {
        this.dockerImageData = dockerImageData;
    }

    public DockerImage(byte[] dockerImageData, String imageName) {
        this.dockerImageData = dockerImageData;
        this.imageName = imageName;
    }
}
