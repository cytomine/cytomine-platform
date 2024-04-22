package be.cytomine.appengine.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;


public class DescriptorHelper {
  public static JsonNode parseDescriptor(File descriptorFile) {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    JsonNode map;
    try {
      map = mapper.readTree(descriptorFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return map;
  }
}
