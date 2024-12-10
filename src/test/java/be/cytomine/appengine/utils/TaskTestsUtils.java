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

import be.cytomine.appengine.dto.inputs.task.GenericParameterProvision;
import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.bool.BooleanValue;
import be.cytomine.appengine.dto.inputs.task.types.enumeration.EnumerationValue;
import be.cytomine.appengine.dto.inputs.task.types.geometry.GeometryValue;
import be.cytomine.appengine.dto.inputs.task.types.image.ImageValue;
import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerValue;
import be.cytomine.appengine.dto.inputs.task.types.string.StringValue;
import be.cytomine.appengine.dto.inputs.task.types.wsi.WsiValue;
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
                    booleanValue.setParameterName((String) entity.get("param_name"));
                    booleanValue.setTaskRunId(UUID.fromString((String) entity.get("task_run_id")));
                    booleanValue.setValue((boolean) entity.get("value"));
                    parameterValues.add(booleanValue);
                    break;

                case "integer":
                    IntegerValue integerValue = new IntegerValue();
                    integerValue.setParameterName((String) entity.get("param_name"));
                    integerValue.setTaskRunId(UUID.fromString((String) entity.get("task_run_id")));
                    integerValue.setValue((int) entity.get("value"));
                    parameterValues.add(integerValue);
                    break;

                case "string":
                    StringValue stringValue = new StringValue();
                    stringValue.setParameterName((String) entity.get("param_name"));
                    stringValue.setTaskRunId(UUID.fromString((String) entity.get("task_run_id")));
                    stringValue.setValue((String) entity.get("value"));
                    parameterValues.add(stringValue);
                    break;

                case "enumeration":
                    EnumerationValue enumerationValue = new EnumerationValue();
                    enumerationValue.setParameterName((String) entity.get("param_name"));
                    enumerationValue.setTaskRunId(UUID.fromString((String) entity.get("task_run_id")));
                    enumerationValue.setValue((String) entity.get("value"));
                    parameterValues.add(enumerationValue);
                    break;

                case "geometry":
                    GeometryValue geometryValue = new GeometryValue();
                    geometryValue.setParameterName((String) entity.get("param_name"));
                    geometryValue.setTaskRunId(UUID.fromString((String) entity.get("task_run_id")));
                    geometryValue.setValue((String) entity.get("value"));
                    parameterValues.add(geometryValue);
                    break;

                case "image":
                    ImageValue imageValue = new ImageValue();
                    imageValue.setParameterName((String) entity.get("param_name"));
                    imageValue.setTaskRunId(UUID.fromString((String) entity.get("task_run_id")));
                    imageValue.setValue(((String) entity.get("value")).getBytes());
                    parameterValues.add(imageValue);
                    break;

                case "wsi":
                    WsiValue wsiValue = new WsiValue();
                    wsiValue.setParameterName((String) entity.get("param_name"));
                    wsiValue.setTaskRunId(UUID.fromString((String) entity.get("task_run_id")));
                    wsiValue.setValue(((String) entity.get("value")).getBytes());
                    parameterValues.add(wsiValue);
                    break;
            }
        }

        return parameterValues;
    }

    public static GenericParameterProvision createProvision(String parameterName, String type, String value) {
        GenericParameterProvision provision = new GenericParameterProvision();
        provision.setParameterName(parameterName);
        if (type.isEmpty()) {
            provision.setValue(value);
            return provision;
        }

        switch (type) {
            case "BooleanType":
                provision.setType(be.cytomine.appengine.dto.inputs.task.ParameterType.BOOLEAN);
                provision.setValue(Boolean.parseBoolean(value));
                break;
            case "IntegerType":
                provision.setType(be.cytomine.appengine.dto.inputs.task.ParameterType.INTEGER);
                provision.setValue(Integer.parseInt(value));
                break;
            case "NumberType":
                provision.setType(be.cytomine.appengine.dto.inputs.task.ParameterType.NUMBER);
                provision.setValue(Double.parseDouble(value));
                break;
            case "StringType":
                provision.setType(be.cytomine.appengine.dto.inputs.task.ParameterType.STRING);
                provision.setValue(value);
                break;
            case "EnumerationType":
                provision.setType(be.cytomine.appengine.dto.inputs.task.ParameterType.ENUMERATION);
                provision.setValue(value);
                break;
            case "GeometryType":
                provision.setType(be.cytomine.appengine.dto.inputs.task.ParameterType.GEOMETRY);
                provision.setValue(value);
                break;
            case "ImageType":
                provision.setType(be.cytomine.appengine.dto.inputs.task.ParameterType.IMAGE);
                provision.setValue(value.getBytes());
                break;
            case "WsiType":
                provision.setType(be.cytomine.appengine.dto.inputs.task.ParameterType.WSI);
                provision.setValue(value.getBytes());
                break;
        }

        return provision;
    }
}
