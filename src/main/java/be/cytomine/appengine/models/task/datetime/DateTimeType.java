package be.cytomine.appengine.models.task.datetime;

import java.io.File;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.datetime.DateTimeTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.datetime.DateTimeValue;
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
import be.cytomine.appengine.repositories.datetime.DateTimePersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import be.cytomine.appengine.utils.FileHelper;

@SuppressWarnings("checkstyle:LineLength")
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class DateTimeType extends Type {

    @Column(nullable = true)
    private Instant before;

    @Column(nullable = true)
    private Instant after;

    public void setConstraint(DateTimeTypeConstraint constraint, String value) {
        switch (constraint) {
            case BEFORE:
                setBefore(Instant.parse(value));
                break;
            case AFTER:
                setAfter(Instant.parse(value));
                break;
            default:
                throw new RuntimeException("Invalid value: " + value);
        }
    }

    public boolean hasConstraint(DateTimeTypeConstraint constraint) {
        return switch (constraint) {
            case BEFORE -> before != null;
            case AFTER -> after != null;
            default -> false;
        };
    }

    @Override
    public void validateFiles(
        Run run,
        Output currentOutput,
        StorageData currentOutputStorageData
    ) throws TypeValidationException {
        File outputFile = getFileIfStructureIsValid(currentOutputStorageData);

        String rawValue = getContentIfValid(outputFile);

        validate(rawValue);
    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (!(valueObject instanceof String)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_TYPE_ERROR);
        }

        Instant value = Instant.parse((String) valueObject);

        if (hasConstraint(DateTimeTypeConstraint.BEFORE) && value.isBefore(before)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_BEFORE_ERROR);
        }

        if (hasConstraint(DateTimeTypeConstraint.AFTER) && value.isAfter(after)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_AFTER_ERROR);
        }
    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
        DateTimePersistenceRepository dateTimePersistenceRepository = AppEngineApplicationContext.getBean(DateTimePersistenceRepository.class);
        String parameterName = provision.get("param_name").asText();
        Instant value = Instant.parse(provision.get("value").asText());
        DateTimePersistence persistedProvision = dateTimePersistenceRepository.findDateTimePersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision == null) {
            persistedProvision = new DateTimePersistence();
            persistedProvision.setValueType(ValueType.DATETIME);
            persistedProvision.setParameterType(ParameterType.INPUT);
            persistedProvision.setParameterName(parameterName);
            persistedProvision.setRunId(runId);
            persistedProvision.setValue(value);
            dateTimePersistenceRepository.save(persistedProvision);
        } else {
            persistedProvision.setValue(value);
            dateTimePersistenceRepository.saveAndFlush(persistedProvision);
        }
    }

    @Override
    public void persistResult(Run run, Output currentOutput, StorageData outputValue) {
        DateTimePersistenceRepository dateTimePersistenceRepository = AppEngineApplicationContext.getBean(DateTimePersistenceRepository.class);
        DateTimePersistence result = dateTimePersistenceRepository.findDateTimePersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        String output = FileHelper.read(outputValue.peek().getData(), getStorageCharset());
        Instant value = Instant.parse(output);
        if (result == null) {
            result = new DateTimePersistence();
            result.setValueType(ValueType.DATETIME);
            result.setParameterType(ParameterType.OUTPUT);
            result.setParameterName(currentOutput.getName());
            result.setRunId(run.getId());
            result.setValue(value);
            dateTimePersistenceRepository.save(result);
        } else {
            result.setValue(value);
            dateTimePersistenceRepository.saveAndFlush(result);
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

        DateTimeValue value = new DateTimeValue();
        value.setParameterName(outputName);
        value.setTaskRunId(id);
        value.setType(ValueType.DATETIME);
        value.setValue(Instant.parse(outputValue));

        return value;
    }

    @Override
    public TaskRunParameterValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        DateTimePersistence dateTimePersistence = (DateTimePersistence) typePersistence;
        DateTimeValue value = new DateTimeValue();
        value.setParameterName(dateTimePersistence.getParameterName());
        value.setTaskRunId(dateTimePersistence.getRunId());
        value.setType(ValueType.DATETIME);
        value.setValue(dateTimePersistence.getValue());

        return value;
    }
}
