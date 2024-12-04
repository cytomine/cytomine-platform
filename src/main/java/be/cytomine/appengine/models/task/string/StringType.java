package be.cytomine.appengine.models.task.string;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.string.StringTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.string.StringValue;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.models.task.ValueType;
import be.cytomine.appengine.repositories.string.StringPersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class StringType extends Type {

    private Integer minLength = 0;

    @Column(nullable = true)
    private Integer maxLength;

    public void setConstraint(StringTypeConstraint constraint, Integer value) {
        switch (constraint) {
            case MIN_LENGTH:
                this.setMinLength(value);
                break;
            case MAX_LENGTH:
                this.setMaxLength(value);
                break;
        }
    }

    public boolean hasConstraint(StringTypeConstraint constraint) {
        return switch (constraint) {
            case MIN_LENGTH -> this.minLength != null;
            case MAX_LENGTH -> this.maxLength != null;
            default -> false;
        };
    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (valueObject == null) {
            return;
        }

        String value = (String) valueObject;

        if (this.hasConstraint(StringTypeConstraint.MIN_LENGTH) && value.length() < this.getMinLength()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR);
        }

        if (this.hasConstraint(StringTypeConstraint.MAX_LENGTH) && value.length() > this.getMaxLength()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GEQ_VALIDATION_ERROR);
        }
    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
        StringPersistenceRepository stringPersistenceRepository = AppEngineApplicationContext.getBean(StringPersistenceRepository.class);
        String parameterName = provision.get("param_name").asText();
        String value = provision.get("value").asText();
        StringPersistence persistedProvision = stringPersistenceRepository.findStringPersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision == null) {
            persistedProvision = new StringPersistence();
            persistedProvision.setValueType(ValueType.STRING);
            persistedProvision.setParameterType(ParameterType.INPUT);
            persistedProvision.setParameterName(parameterName);
            persistedProvision.setRunId(runId);
            persistedProvision.setValue(value);
            stringPersistenceRepository.save(persistedProvision);
        } else {
            persistedProvision.setValue(value);
            stringPersistenceRepository.saveAndFlush(persistedProvision);
        }
    }

    @Override
    public void persistResult(Run run, Output currentOutput, String outputValue) {
        StringPersistenceRepository stringPersistenceRepository = AppEngineApplicationContext.getBean(StringPersistenceRepository.class);
        StringPersistence result = stringPersistenceRepository.findStringPersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        if (result == null) {
            result = new StringPersistence();
            result.setValueType(ValueType.STRING);
            result.setParameterType(ParameterType.OUTPUT);
            result.setParameterName(currentOutput.getName());
            result.setRunId(run.getId());
            result.setValue(outputValue);
            stringPersistenceRepository.save(result);
        } else {
            result.setValue(outputValue);
            stringPersistenceRepository.saveAndFlush(result);
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
        StringValue value = new StringValue();
        value.setParameterName(outputName);
        value.setTaskRunId(id);
        value.setType(ValueType.STRING);
        value.setValue(trimmedOutput);

        return value;
    }

    @Override
    public TaskRunParameterValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        StringPersistence stringPersistence = (StringPersistence) typePersistence;
        StringValue value = new StringValue();
        value.setParameterName(stringPersistence.getParameterName());
        value.setTaskRunId(stringPersistence.getRunId());
        value.setValue(stringPersistence.getValue());
        value.setType(ValueType.STRING);

        return value;
    }
}
