package be.cytomine.appengine.models.task.bool;

import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageDataEntry;
import be.cytomine.appengine.handlers.StorageDataType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.bool.BooleanValue;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.models.task.ValueType;
import be.cytomine.appengine.repositories.bool.BooleanPersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.UUID;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class BooleanType extends Type {

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (valueObject == null) {
            return;
        }

        if (!(valueObject instanceof Boolean)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_VALIDATION_ERROR);
        }
    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
        BooleanPersistenceRepository booleanPersistenceRepository = AppEngineApplicationContext.getBean(BooleanPersistenceRepository.class);
        String parameterName = provision.get("param_name").asText();
        boolean value = provision.get("value").asBoolean();

        BooleanPersistence persistedProvision = booleanPersistenceRepository.findBooleanPersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision == null) {
            persistedProvision = new BooleanPersistence();
            persistedProvision.setValueType(ValueType.BOOLEAN);
            persistedProvision.setParameterType(ParameterType.INPUT);
            persistedProvision.setParameterName(parameterName);
            persistedProvision.setRunId(runId);
            persistedProvision.setValue(value);
            booleanPersistenceRepository.save(persistedProvision);
        } else {
            persistedProvision.setValue(value);
            booleanPersistenceRepository.saveAndFlush(persistedProvision);
        }
    }

    @Override
    public void persistResult(Run run, Output currentOutput, StorageData outputValue) {
        BooleanPersistenceRepository booleanPersistenceRepository = AppEngineApplicationContext.getBean(BooleanPersistenceRepository.class);
        BooleanPersistence result = booleanPersistenceRepository.findBooleanPersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        String output = new String(outputValue.poll().getData(), getStorageCharset());
        String trimmedOutput = output.trim();
        if (result == null) {
            result = new BooleanPersistence();
            result.setValue(Boolean.parseBoolean(trimmedOutput));
            result.setValueType(ValueType.BOOLEAN);
            result.setParameterType(ParameterType.OUTPUT);
            result.setRunId(run.getId());
            result.setParameterName(currentOutput.getName());
            booleanPersistenceRepository.save(result);
        } else {
            result.setValue(Boolean.parseBoolean(trimmedOutput));
            booleanPersistenceRepository.saveAndFlush(result);
        }
    }

    @Override
    public StorageData mapToStorageFileData(JsonNode provision) {
        String value = provision.get("value").asText();
        String parameterName = provision.get("param_name").asText();
        byte[] inputFileData = value.getBytes(getStorageCharset());
        StorageDataEntry storageDataEntry = new StorageDataEntry(inputFileData, parameterName , StorageDataType.FILE);
        return new StorageData(storageDataEntry);
    }

    @Override
    public JsonNode createTypedParameterResponse(JsonNode provision, Run run) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode provisionedParameter = mapper.createObjectNode();
        provisionedParameter.put("param_name", provision.get("param_name").asText());
        provisionedParameter.put("value", provision.get("value").asBoolean());
        provisionedParameter.put("task_run_id", String.valueOf(run.getId()));

        return provisionedParameter;
    }

    @Override
    public TaskRunParameterValue buildTaskRunParameterValue(StorageData output, UUID id, String outputName) {
        String outputValue = new String(output.poll().getData(), getStorageCharset());
        String trimmedOutput = outputValue.trim();

        BooleanValue booleanValue = new BooleanValue();
        booleanValue.setParameterName(outputName);
        booleanValue.setTaskRunId(id);
        booleanValue.setType(ValueType.BOOLEAN);
        booleanValue.setValue(Boolean.parseBoolean(trimmedOutput));

        return booleanValue;
    }

    @Override
    public TaskRunParameterValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        BooleanPersistence booleanPersistence = (BooleanPersistence) typePersistence;
        BooleanValue booleanValue = new BooleanValue();
        booleanValue.setParameterName(booleanPersistence.getParameterName());
        booleanValue.setTaskRunId(booleanPersistence.getRunId());
        booleanValue.setType(ValueType.BOOLEAN);
        booleanValue.setValue(booleanPersistence.isValue());

        return booleanValue;
    }
}
