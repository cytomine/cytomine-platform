package be.cytomine.appengine.dto.inputs.task;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import be.cytomine.appengine.utils.DescriptorHelper;

@Setter
@Getter
@NoArgsConstructor
public class UploadTaskArchive {

    private byte[] descriptorFile;

    private File dockerImage;

    private JsonNode descriptorFileAsJson;

    public UploadTaskArchive(byte[] descriptorFile, File dockerImage) {
        this.descriptorFile = descriptorFile;
        this.dockerImage = dockerImage;
        this.descriptorFileAsJson = DescriptorHelper.parseDescriptor(descriptorFile);
    }
}
