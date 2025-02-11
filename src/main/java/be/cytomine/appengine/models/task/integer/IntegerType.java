package be.cytomine.appengine.models.task.integer;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerValue;
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
import be.cytomine.appengine.repositories.integer.IntegerPersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;

@SuppressWarnings("checkstyle:LineLength")
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class IntegerType extends Type {

    @Column(nullable = true)
    private Integer gt;

    @Column(nullable = true)
    private Integer lt;

    @Column(nullable = true)
    private Integer geq;

    @Column(nullable = true)
    private Integer leq;

    public void setConstraint(IntegerTypeConstraint constraint, Integer value) {
        switch (constraint) {
            case GREATER_EQUAL:
                this.setGeq(value);
                break;
            case GREATER_THAN:
                this.setGt(value);
                break;
            case LOWER_EQUAL:
                this.setLeq(value);
                break;
            case LOWER_THAN:
                this.setLt(value);
                break;
            default:
                break;
        }
    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (valueObject == null) {
            return;
        }

        Integer value = (Integer) valueObject;
        if (this.hasConstraint(IntegerTypeConstraint.GREATER_THAN) && value <= this.getGt()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR);
        }
        if (this.hasConstraint(IntegerTypeConstraint.GREATER_EQUAL) && value < this.getGeq()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GEQ_VALIDATION_ERROR);
        }
        if (this.hasConstraint(IntegerTypeConstraint.LOWER_THAN) && value >= this.getLt()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_LT_VALIDATION_ERROR);
        }
        if (this.hasConstraint(IntegerTypeConstraint.LOWER_EQUAL) && value > this.getLeq()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_LEQ_VALIDATION_ERROR);
        }
    }

    public boolean hasConstraint(IntegerTypeConstraint constraint) {
        return switch (constraint) {
            case GREATER_EQUAL -> this.geq != null;
            case GREATER_THAN -> this.gt != null;
            case LOWER_EQUAL -> this.leq != null;
            case LOWER_THAN -> this.lt != null;
            default -> false;
        };
    }

    public boolean hasConstraint(String constraintKey) {
        return this.hasConstraint(IntegerTypeConstraint.getConstraint(constraintKey));
    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
        IntegerPersistenceRepository integerPersistenceRepository = AppEngineApplicationContext.getBean(IntegerPersistenceRepository.class);
        String parameterName = provision.get("param_name").asText();
        int value = provision.get("value").asInt();
        IntegerPersistence persistedProvision = integerPersistenceRepository.findIntegerPersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision == null) {
            persistedProvision = new IntegerPersistence();
            persistedProvision.setValueType(ValueType.INTEGER);
            persistedProvision.setParameterType(ParameterType.INPUT);
            persistedProvision.setParameterName(parameterName);
            persistedProvision.setRunId(runId);
            persistedProvision.setValue(value);
            integerPersistenceRepository.save(persistedProvision);
        } else {
            persistedProvision.setValue(value);
            integerPersistenceRepository.saveAndFlush(persistedProvision);
        }
    }

    @Override
    public void persistResult(Run run, Output currentOutput, StorageData outputValue) {
        IntegerPersistenceRepository integerPersistenceRepository = AppEngineApplicationContext.getBean(IntegerPersistenceRepository.class);
        IntegerPersistence result = integerPersistenceRepository.findIntegerPersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        String output = new String(outputValue.peek().getData(), getStorageCharset());
        String trimmedOutput = output.trim();
        if (result == null) {
            result = new IntegerPersistence();
            result.setValue(Integer.parseInt(trimmedOutput));
            result.setValueType(ValueType.INTEGER);
            result.setParameterType(ParameterType.OUTPUT);
            result.setRunId(run.getId());
            result.setParameterName(currentOutput.getName());
            integerPersistenceRepository.save(result);
        } else {
            result.setValue(Integer.parseInt(trimmedOutput));
            integerPersistenceRepository.saveAndFlush(result);
        }
    }

    @Override
    public StorageData mapToStorageFileData(JsonNode provision) {
        String value = provision.get("value").asText();
        String parameterName = provision.get("param_name").asText();
        byte[] inputFileData = value.getBytes(getStorageCharset());
        StorageDataEntry storageDataEntry = new StorageDataEntry(inputFileData, parameterName, StorageDataType.FILE);
        return new StorageData(storageDataEntry);
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
    public IntegerValue buildTaskRunParameterValue(StorageData output, UUID id, String outputName) {
        String outputValue = new String(output.poll().getData(), getStorageCharset());
        String trimmedOutput = outputValue.trim();
        IntegerValue integerValue = new IntegerValue();
        integerValue.setParameterName(outputName);
        integerValue.setTaskRunId(id);
        integerValue.setType(ValueType.INTEGER);
        integerValue.setValue(Integer.parseInt(trimmedOutput));
        return integerValue;
    }

    @Override
    public TaskRunParameterValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        IntegerPersistence integerPersistence = (IntegerPersistence) typePersistence;
        IntegerValue integerValue = new IntegerValue();
        integerValue.setParameterName(integerPersistence.getParameterName());
        integerValue.setTaskRunId(integerPersistence.getRunId());
        integerValue.setType(ValueType.INTEGER);
        integerValue.setValue(integerPersistence.getValue());
        return integerValue;
    }
}
