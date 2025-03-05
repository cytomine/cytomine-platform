package be.cytomine.appengine.models.task.string;

import java.io.File;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.string.StringTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.string.StringValue;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageDataEntry;
import be.cytomine.appengine.handlers.StorageDataType;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.models.task.ValueType;
import be.cytomine.appengine.repositories.string.StringPersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import be.cytomine.appengine.utils.FileHelper;

@SuppressWarnings("checkstyle:LineLength")
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
            default:
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
    public void validateFiles(
        Run run,
        Output currentOutput,
        StorageData currentOutputStorageData)
        throws TypeValidationException {

        // validate file structure
        File outputFile = getFileIfStructureIsValid(currentOutputStorageData);

        // validate value
        String rawValue = getContentIfValid(outputFile);

        validate(rawValue);

    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (valueObject == null) {
            return;
        }

        String value = (String) valueObject;

        if (
            this.hasConstraint(StringTypeConstraint.MIN_LENGTH)
            && value.length() < this.getMinLength()
        ) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR);
        }

        if (
            this.hasConstraint(StringTypeConstraint.MAX_LENGTH)
            && value.length() > this.getMaxLength()
        ) {
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
    public void persistResult(Run run, Output currentOutput, StorageData outputValue) {
        StringPersistenceRepository stringPersistenceRepository = AppEngineApplicationContext.getBean(StringPersistenceRepository.class);
        StringPersistence result = stringPersistenceRepository.findStringPersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        String output = FileHelper.read(outputValue.peek().getData(), getStorageCharset());
        if (result == null) {
            result = new StringPersistence();
            result.setValueType(ValueType.STRING);
            result.setParameterType(ParameterType.OUTPUT);
            result.setParameterName(currentOutput.getName());
            result.setRunId(run.getId());
            result.setValue(output);
            stringPersistenceRepository.save(result);
        } else {
            result.setValue(output);
            stringPersistenceRepository.saveAndFlush(result);
        }
    }

    @Override
    public StorageData mapToStorageFileData(JsonNode provision) {
        String value = provision.get("value").asText();
        String parameterName = provision.get("param_name").asText();
        File data = FileHelper.write(parameterName, value.getBytes(getStorageCharset()));
        StorageDataEntry entry = new StorageDataEntry(data, parameterName, StorageDataType.FILE);
        return new StorageData(entry);
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
    public TaskRunParameterValue buildTaskRunParameterValue(StorageData output, UUID id, String outputName) {
        String outputValue = FileHelper.read(output.peek().getData(), getStorageCharset());

        StringValue value = new StringValue();
        value.setParameterName(outputName);
        value.setTaskRunId(id);
        value.setType(ValueType.STRING);
        value.setValue(outputValue);

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
