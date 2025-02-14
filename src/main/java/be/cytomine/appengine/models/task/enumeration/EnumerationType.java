package be.cytomine.appengine.models.task.enumeration;

import java.io.File;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.types.enumeration.EnumerationTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.enumeration.EnumerationValue;
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
import be.cytomine.appengine.repositories.enumeration.EnumerationPersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import be.cytomine.appengine.utils.FileHelper;

@SuppressWarnings("checkstyle:LineLength")
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class EnumerationType extends Type {

    public static final Integer LIMIT = 256;

    public static final String NEW_LINE = System.getProperty("line.separator");

    private List<String> values;

    public void setConstraint(EnumerationTypeConstraint constraint, String value) {
        switch (constraint) {
            case VALUES:
                this.setValues(parse(value));
                break;
            default:
        }
    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (valueObject == null) {
            return;
        }

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
        EnumerationPersistenceRepository repository = AppEngineApplicationContext.getBean(EnumerationPersistenceRepository.class);
        String parameterName = provision.get("param_name").asText();
        String value = provision.get("value").asText();
        EnumerationPersistence persistedProvision = repository.findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision == null) {
            persistedProvision = new EnumerationPersistence();
            persistedProvision.setValueType(ValueType.ENUMERATION);
            persistedProvision.setParameterType(ParameterType.INPUT);
            persistedProvision.setParameterName(parameterName);
            persistedProvision.setRunId(runId);
            persistedProvision.setValue(value);
            repository.save(persistedProvision);
        } else {
            persistedProvision.setValue(value);
            repository.saveAndFlush(persistedProvision);
        }
    }

    @Override
    public void persistResult(Run run, Output currentOutput, StorageData outputValue) {
        EnumerationPersistenceRepository enumerationPersistenceRepository = AppEngineApplicationContext.getBean(EnumerationPersistenceRepository.class);
        EnumerationPersistence result = enumerationPersistenceRepository.findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        String output = FileHelper.read(outputValue.poll().getData(), getStorageCharset());
        if (result == null) {
            result = new EnumerationPersistence();
            result.setValue(output);
            result.setValueType(ValueType.INTEGER);
            result.setParameterType(ParameterType.OUTPUT);
            result.setRunId(run.getId());
            result.setParameterName(currentOutput.getName());
            enumerationPersistenceRepository.save(result);
        } else {
            result.setValue(output);
            enumerationPersistenceRepository.saveAndFlush(result);
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
    public EnumerationValue buildTaskRunParameterValue(StorageData output, UUID id, String outputName) {
        String outputValue = FileHelper.read(output.poll().getData(), getStorageCharset());

        EnumerationValue enumerationValue = new EnumerationValue();
        enumerationValue.setParameterName(outputName);
        enumerationValue.setTaskRunId(id);
        enumerationValue.setType(ValueType.ENUMERATION);
        enumerationValue.setValue(outputValue);
        return enumerationValue;
    }

    @Override
    public EnumerationValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        EnumerationPersistence enumerationPersistence = (EnumerationPersistence) typePersistence;
        EnumerationValue enumerationValue = new EnumerationValue();
        enumerationValue.setParameterName(enumerationPersistence.getParameterName());
        enumerationValue.setTaskRunId(enumerationPersistence.getRunId());
        enumerationValue.setType(ValueType.ENUMERATION);
        enumerationValue.setValue(enumerationPersistence.getValue());
        return enumerationValue;
    }
}
