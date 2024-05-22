package be.cytomine.appengine.models.task.enumeration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import be.cytomine.appengine.dto.inputs.task.types.enumeration.EnumerationTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.enumeration.EnumerationValue;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.models.task.ValueType;
import be.cytomine.appengine.repositories.enumeration.EnumerationPersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class EnumerationType extends Type {

    public static final Integer LIMIT = 256;

    public static final String NEW_LINE = System.getProperty("line.separator");

    private List<String> values;

    /**
     * Parse a string representation of a list of string to a list of strings
     * 
     * @param input The string representation of the list
     * @return  The list of strings
    */
    public static List<String> parse(String input) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(input, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setConstraint(EnumerationTypeConstraint constraint, String value) {
        switch (constraint) {
            case VALUES:
                this.setValues(parse(value));
                break;
        }
    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (!(valueObject instanceof String)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_VALIDATION_ERROR);
        }

        String value = (String) valueObject;

        if (value.contains(NEW_LINE)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR);
        }

        if (value.length() > LIMIT) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR);
        }

        if (!values.contains(value)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_VALIDATION_ERROR);
        }
    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
        EnumerationPersistenceRepository enumerationPersistenceRepository = AppEngineApplicationContext.getBean(EnumerationPersistenceRepository.class);
        String parameterName = provision.get("param_name").asText();
        String value = provision.get("value").asText();
        EnumerationPersistence persistedProvision = enumerationPersistenceRepository.findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision == null) {
            persistedProvision = new EnumerationPersistence();
            persistedProvision.setValueType(ValueType.ENUMERATION);
            persistedProvision.setParameterType(ParameterType.INPUT);
            persistedProvision.setParameterName(parameterName);
            persistedProvision.setRunId(runId);
            persistedProvision.setValue(value);
            enumerationPersistenceRepository.save(persistedProvision);
        } else {
            persistedProvision.setValue(value);
            enumerationPersistenceRepository.saveAndFlush(persistedProvision);
        }
    }

    @Override
    public void persistResult(Run run, Output currentOutput, String outputValue) {
        EnumerationPersistenceRepository enumerationPersistenceRepository = AppEngineApplicationContext.getBean(EnumerationPersistenceRepository.class);
        EnumerationPersistence result = enumerationPersistenceRepository.findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        if (result == null) {
            result = new EnumerationPersistence();
            result.setValue(outputValue);
            result.setValueType(ValueType.INTEGER);
            result.setParameterType(ParameterType.OUTPUT);
            result.setRunId(run.getId());
            result.setParameterName(currentOutput.getName());
            enumerationPersistenceRepository.save(result);
        } else {
            result.setValue(outputValue);
            enumerationPersistenceRepository.saveAndFlush(result);
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
        provisionedParameter.put("value", provision.get("value").asInt());
        provisionedParameter.put("task_run_id", String.valueOf(run.getId()));
        return provisionedParameter;
    }

    @Override
    public EnumerationValue buildTaskRunParameterValue(String output, UUID id, String outputName) {
        EnumerationValue enumerationValue = new EnumerationValue();
        enumerationValue.setTask_run_id(id);
        enumerationValue.setValue(output);
        enumerationValue.setParam_name(outputName);
        return enumerationValue;
    }

    @Override
    public EnumerationValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        EnumerationPersistence enumerationPersistence = (EnumerationPersistence) typePersistence;
        EnumerationValue enumerationValue = new EnumerationValue();
        enumerationValue.setTask_run_id(enumerationPersistence.getRunId());
        enumerationValue.setValue(enumerationPersistence.getValue());
        enumerationValue.setParam_name(enumerationPersistence.getParameterName());
        return enumerationValue;
    }
}
