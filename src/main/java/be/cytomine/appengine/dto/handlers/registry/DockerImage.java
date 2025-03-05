package be.cytomine.appengine.dto.handlers.registry;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class DockerImage {
    private File imageData;
    private String imageName;
}
