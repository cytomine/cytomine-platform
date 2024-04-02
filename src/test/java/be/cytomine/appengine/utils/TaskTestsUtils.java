package be.cytomine.appengine.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import be.cytomine.appengine.models.BaseEntity;
import be.cytomine.appengine.openapi.model.AbstractOpenApiSchema;

public class TaskTestsUtils {


  public static <T> JsonNode backToJson(T object) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(object, JsonNode.class);
  }

  public static <F extends AbstractOpenApiSchema, P extends BaseEntity> void checkParametersSetsMatch(List<F> actualParameters, Set<P> expectedParameters) {
    // convert to json to avoid issue with OutputParameter actual instance type
    Assertions.assertEquals(actualParameters.size(), expectedParameters.size());
    Map<String, JsonNode> fetchedOutputs = new HashMap<>();
    for (F output : actualParameters) {
      JsonNode outputAsJsonNode = backToJson(output.getActualInstance());
      fetchedOutputs.put(outputAsJsonNode.get("name").textValue(), outputAsJsonNode);
    }

    for (P output : expectedParameters) {
      try {  // need reflection because there is no common Parameter interface above Output and Input to reach getName
        Method getNameMethod = output.getClass().getMethod("getName");
        String name = (String) getNameMethod.invoke(output);
        Assertions.assertTrue(fetchedOutputs.containsKey(name));
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
}
}
