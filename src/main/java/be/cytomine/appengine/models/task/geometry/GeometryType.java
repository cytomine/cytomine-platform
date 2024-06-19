package be.cytomine.appengine.models.task.geometry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.geometry.GeometryValue;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.ParseException;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.models.task.ValueType;
import be.cytomine.appengine.repositories.geometry.GeometryPersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonReader;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class GeometryType extends Type {

    public static final List<String> SUPPORTED_TYPES = Arrays.asList(
        "Point",
        "MultiPoint",
        "LineString",
        "MultiLineString",
        "Polygon",
        "MultiPolygon"
    );

    /**
     * Parse a string representation of a geometry to a geometry
     *
     * @param input The string representation of the geometry
     * @return The geometry
     * @throws ParseException
     */
    public static Geometry parse(String input) throws ParseException {
        try {
            GeoJsonReader reader = new GeoJsonReader();
            Geometry geometry = reader.read(input);

            // Parse properties if any
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(input);
            geometry.setUserData(json.get("properties"));

            return geometry;
        } catch(Exception e) {
            throw new ParseException("Error while parsing geometry: " + e.getMessage());
        }
    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (!(valueObject instanceof String)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_TYPE_ERROR);
        }

        Geometry geometry;
        try {
            geometry = parse((String) valueObject);
        } catch (ParseException e) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GEOJSON_PROCESSING_ERROR);
        }

        if (!geometry.isValid()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_GEOJSON);
        }

        if (!SUPPORTED_TYPES.contains(geometry.getGeometryType())) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_UNSUPPORTED_GEOMETRY_TYPE);
        }

        // Validation for Circle and Rectangle
        JSONObject properties = (JSONObject) geometry.getUserData();
        if (properties == null || properties.isEmpty()) {
            return;
        }

        if (!properties.containsKey("subType")) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GEOJSON_SUBTYPE_ERROR);
        }

        String subType = (String) properties.get("subType");
        if (!subType.equals("Circle") && !subType.equals("Rectangle")) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_UNSUPPORTED_GEOMETRY_SUBTYPE);
        }

        if (subType.equals("Circle") && !properties.containsKey("radius")) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_MISSING_RADIUS_ERROR);
        }
    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
        GeometryPersistenceRepository geometryPersistenceRepository = AppEngineApplicationContext.getBean(GeometryPersistenceRepository.class);
        String parameterName = provision.get("param_name").asText();
        String value = provision.get("value").asText();

        GeometryPersistence persistedProvision = geometryPersistenceRepository.findGeometryPersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision == null) {
            persistedProvision = new GeometryPersistence();
            persistedProvision.setValueType(ValueType.GEOMETRY);
            persistedProvision.setParameterType(ParameterType.INPUT);
            persistedProvision.setParameterName(parameterName);
            persistedProvision.setRunId(runId);
            persistedProvision.setValue(value);
            geometryPersistenceRepository.save(persistedProvision);
        } else {
            persistedProvision.setValue(value);
            geometryPersistenceRepository.saveAndFlush(persistedProvision);
        }
    }


    @Override
    public void persistResult(Run run, Output currentOutput, String outputValue) {
        GeometryPersistenceRepository geometryPersistenceRepository = AppEngineApplicationContext.getBean(GeometryPersistenceRepository.class);
        GeometryPersistence result = geometryPersistenceRepository.findGeometryPersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        if (result == null) {
            result = new GeometryPersistence();
            result.setValue(outputValue);
            result.setValueType(ValueType.GEOMETRY);
            result.setParameterType(ParameterType.OUTPUT);
            result.setRunId(run.getId());
            result.setParameterName(currentOutput.getName());
            geometryPersistenceRepository.save(result);
        } else {
            result.setValue(outputValue);
            geometryPersistenceRepository.saveAndFlush(result);
        }
    }

    @Override
    public FileData mapToStorageFileData(JsonNode provision, String charset) {
        String value = provision.get("value").asText();
        String parameterName = provision.get("param_name").asText();
        byte[] inputFileData = value.getBytes(getStorageCharset(charset));

        return new FileData(inputFileData, parameterName);
    }

    @Override
    public JsonNode createTypedParameterResponse(JsonNode provision, Run run) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode provisionedParameter = mapper.createObjectNode();
        provisionedParameter.put("param_name", provision.get("param_name").asText());
        provisionedParameter.put("value", provision.get("value").asText());
        provisionedParameter.put("task_run_id", String.valueOf(run.getId()));

        return provisionedParameter;
    }

    @Override
    public TaskRunParameterValue buildTaskRunParameterValue(String trimmedOutput, UUID id, String outputName) {
        GeometryValue geometryValue = new GeometryValue();
        geometryValue.setTask_run_id(id);
        geometryValue.setValue(trimmedOutput);
        geometryValue.setParam_name(outputName);

        return geometryValue;
    }

    @Override
    public TaskRunParameterValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        GeometryPersistence geometryPersistence = (GeometryPersistence) typePersistence;
        GeometryValue geometryValue = new GeometryValue();
        geometryValue.setTask_run_id(geometryPersistence.getRunId());
        geometryValue.setValue(geometryPersistence.getValue());
        geometryValue.setParam_name(geometryPersistence.getParameterName());

        return geometryValue;
    }
}
