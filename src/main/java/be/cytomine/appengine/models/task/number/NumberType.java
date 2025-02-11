package be.cytomine.appengine.models.task.number;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.number.NumberTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.number.NumberValue;
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
import be.cytomine.appengine.repositories.number.NumberPersistenceRepository;
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
public class NumberType extends Type {

    @Column(nullable = true)
    private Double gt;
    @Column(nullable = true)
    private Double geq;
    @Column(nullable = true)
    private Double lt;
    @Column(nullable = true)
    private Double leq;

    private boolean infinityAllowed = false;
    private boolean nanAllowed = false;

    public void setConstraint(NumberTypeConstraint constraint, String value) {
        switch (constraint) {
            case GREATER_EQUAL:
                this.setGeq(Double.parseDouble(value));
                break;
            case GREATER_THAN:
                this.setGt(Double.parseDouble(value));
                break;
            case LOWER_EQUAL:
                this.setLeq(Double.parseDouble(value));
                break;
            case LOWER_THAN:
                this.setLt(Double.parseDouble(value));
                break;
            case INFINITY_ALLOWED:
                this.setInfinityAllowed(Boolean.parseBoolean(value));
                break;
            case NAN_ALLOWED:
                this.setNanAllowed(Boolean.parseBoolean(value));
                break;
        }
    }

    public boolean hasConstraint(NumberTypeConstraint constraint) {
        return switch (constraint) {
            case GREATER_EQUAL -> this.geq != null;
            case GREATER_THAN -> this.gt != null;
            case LOWER_EQUAL -> this.leq != null;
            case LOWER_THAN -> this.lt != null;
            default -> false;
        };
    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (valueObject == null) {
            return;
        }

        Double value = (Double) valueObject;

        if (this.hasConstraint(NumberTypeConstraint.GREATER_THAN) && value <= this.getGt()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR);
        }

        if (this.hasConstraint(NumberTypeConstraint.GREATER_EQUAL) && value < this.getGeq()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GEQ_VALIDATION_ERROR);
        }

        if (this.hasConstraint(NumberTypeConstraint.LOWER_THAN) && value >= this.getLt()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_LT_VALIDATION_ERROR);
        }

        if (this.hasConstraint(NumberTypeConstraint.LOWER_EQUAL) && value > this.getLeq()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_LEQ_VALIDATION_ERROR);
        }
    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
        NumberPersistenceRepository numberPersistenceRepository = AppEngineApplicationContext.getBean(NumberPersistenceRepository.class);
        String parameterName = provision.get("param_name").asText();
        double value = provision.get("value").asDouble();
        NumberPersistence persistedProvision = numberPersistenceRepository.findNumberPersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision == null) {
            persistedProvision = new NumberPersistence();
            persistedProvision.setValueType(ValueType.NUMBER);
            persistedProvision.setParameterType(ParameterType.INPUT);
            persistedProvision.setParameterName(parameterName);
            persistedProvision.setRunId(runId);
            persistedProvision.setValue(value);
            numberPersistenceRepository.save(persistedProvision);
        } else {
            persistedProvision.setValue(value);
            numberPersistenceRepository.saveAndFlush(persistedProvision);
        }
    }

    @Override
    public void persistResult(Run run, Output currentOutput, StorageData outputValue) {
        NumberPersistenceRepository numberPersistenceRepository = AppEngineApplicationContext.getBean(NumberPersistenceRepository.class);
        NumberPersistence result = numberPersistenceRepository.findNumberPersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        String output = new String(outputValue.poll().getData(), getStorageCharset());
        String trimmedOutput = output.trim();
        double value = Double.parseDouble(trimmedOutput);
        if (result == null) {
            result = new NumberPersistence();
            result.setValueType(ValueType.NUMBER);
            result.setParameterType(ParameterType.OUTPUT);
            result.setParameterName(currentOutput.getName());
            result.setRunId(run.getId());
            result.setValue(value);
            numberPersistenceRepository.save(result);
        } else {
            result.setValue(value);
            numberPersistenceRepository.saveAndFlush(result);
        }
    }

    @Override
    public StorageData mapToStorageFileData(JsonNode provision) {
        String value = provision.get("value").asText();
        String parameterName = provision.get("param_name").asText();
        byte[] inputFileData = value.getBytes(getStorageCharset());
        StorageDataEntry storageDataEntry = new StorageDataEntry(inputFileData, parameterName , StorageDataType.FILE);
        return new StorageData(storageDataEntry);
//        return new FileData(inputFileData, parameterName);
    }

    @Override
    public JsonNode createTypedParameterResponse(JsonNode provision, Run run) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode provisionedParameter = mapper.createObjectNode();
        provisionedParameter.put("param_name", provision.get("param_name").asText());
        provisionedParameter.put("value", provision.get("value").asDouble());
        provisionedParameter.put("task_run_id", String.valueOf(run.getId()));

        return provisionedParameter;
    }

    @Override
    public TaskRunParameterValue buildTaskRunParameterValue(StorageData output, UUID id, String outputName) {
        String outputValue = new String(output.poll().getData(), getStorageCharset());
        String trimmedOutput = outputValue.trim();

        NumberValue value = new NumberValue();
        value.setParameterName(outputName);
        value.setTaskRunId(id);
        value.setType(ValueType.NUMBER);
        value.setValue(Double.parseDouble(trimmedOutput));

        return value;
    }

    @Override
    public TaskRunParameterValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        NumberPersistence numberPersistence = (NumberPersistence) typePersistence;
        NumberValue value = new NumberValue();
        value.setParameterName(numberPersistence.getParameterName());
        value.setTaskRunId(numberPersistence.getRunId());
        value.setType(ValueType.NUMBER);
        value.setValue(numberPersistence.getValue());

        return value;
    }
}
