package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.exceptions.BundleArchiveException;
import be.cytomine.appengine.services.TaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Setter
@Getter
@NoArgsConstructor
public class UploadTaskArchive {

    Logger logger = LoggerFactory.getLogger(TaskService.class);

    private byte[] descriptorFile;
    private byte[] dockerImage;
    private JsonNode descriptorFileAsJson;

    public UploadTaskArchive(byte[] discriptorFile, byte[] dockerImage) throws BundleArchiveException {
        this.descriptorFile = discriptorFile;
        this.dockerImage = dockerImage;
        descriptorFileAsJson = convertFromYamlToJson();
    }

    private JsonNode convertFromYamlToJson() throws BundleArchiveException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode map;
        try {
            map = mapper.readTree(descriptorFile);
        } catch (IOException e) {
            logger.info("UploadTask : failed to convert descriptor.yml to json [" + e.getMessage() + "]");
            throw new BundleArchiveException(e);
        }
        return map;
    }


}
