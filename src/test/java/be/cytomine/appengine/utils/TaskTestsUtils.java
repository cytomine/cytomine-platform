package be.cytomine.appengine.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.bool.BooleanValue;
import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerValue;
import be.cytomine.appengine.models.BaseEntity;

public class TaskTestsUtils {

    private static <E extends BaseEntity> List<String> getName(List<E> entities) {
        return entities
                .stream()
                .map(entity -> {
                    try {
                        return (String) entity.getClass().getMethod("getName").invoke(entity);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public static <E extends BaseEntity> boolean areSetEquals(Set<E> expected, List<E> actual) {
        if (actual.size() != expected.size()) {
            return false;
        }

        List<String> expectedNames = getName(new ArrayList<>(expected));
        List<String> actualNames = getName(actual);

        return expectedNames.containsAll(actualNames) && actualNames.containsAll(expectedNames);
    }

    public static List<TaskRunParameterValue> convertTo(String body) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> values = new ArrayList<>();
        try {
            values = objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        List<TaskRunParameterValue> parameterValues = new ArrayList<>();
        for (Map<String, Object> entity : values) {
            Object value = entity.get("value");
            switch (value.getClass().getSimpleName().toLowerCase()) {
                case "boolean":
                    BooleanValue booleanValue = new BooleanValue();
                    booleanValue.setParam_name((String) entity.get("param_name"));
                    booleanValue.setTask_run_id(UUID.fromString((String) entity.get("task_run_id")));
                    booleanValue.setValue((boolean) entity.get("value"));
                    parameterValues.add(booleanValue);
                    break;

                case "integer":
                    IntegerValue integerValue = new IntegerValue();
                    integerValue.setParam_name((String) entity.get("param_name"));
                    integerValue.setTask_run_id(UUID.fromString((String) entity.get("task_run_id")));
                    integerValue.setValue((int) entity.get("value"));
                    parameterValues.add(integerValue);
                    break;
            }
        }

        return parameterValues;
    }
}
