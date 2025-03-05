package be.cytomine.appengine.utils;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DescriptorHelper {
    public static JsonNode parseDescriptor(File descriptorFile) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try {
            return mapper.readTree(descriptorFile);
        } catch (IOException e) {
            log.error(
                "DescriptorHelper: failed to convert descriptor.yml to json [{}]",
                e.getMessage()
            );
            throw new RuntimeException(e);
        }
    }
}
