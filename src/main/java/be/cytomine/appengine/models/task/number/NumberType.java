package be.cytomine.appengine.models.task.number;

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
import be.cytomine.appengine.utils.FileHelper;

@SuppressWarnings("checkstyle:LineLength")
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
                setGeq(Double.parseDouble(value));
                break;
            case GREATER_THAN:
                setGt(Double.parseDouble(value));
                break;
            case LOWER_EQUAL:
                setLeq(Double.parseDouble(value));
                break;
            case LOWER_THAN:
                setLt(Double.parseDouble(value));
                break;
            case INFINITY_ALLOWED:
                setInfinityAllowed(Boolean.parseBoolean(value));
                break;
            case NAN_ALLOWED:
                setNanAllowed(Boolean.parseBoolean(value));
                break;
            default:
        }
    }

    public boolean hasConstraint(NumberTypeConstraint constraint) {
        return switch (constraint) {
            case GREATER_EQUAL -> geq != null;
            case GREATER_THAN -> gt != null;
            case LOWER_EQUAL -> leq != null;
            case LOWER_THAN -> lt != null;
            case INFINITY_ALLOWED -> true;
            case NAN_ALLOWED -> true;
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

        validate(Double.parseDouble(rawValue));

    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (valueObject == null) {
            return;
        }

        Double value = (Double) valueObject;

        if (hasConstraint(NumberTypeConstraint.GREATER_THAN) && value <= gt) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR);
        }

        if (hasConstraint(NumberTypeConstraint.GREATER_EQUAL) && value < geq) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GEQ_VALIDATION_ERROR);
        }

        if (hasConstraint(NumberTypeConstraint.LOWER_THAN) && value >= lt) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_LT_VALIDATION_ERROR);
        }

        if (hasConstraint(NumberTypeConstraint.LOWER_EQUAL) && value > leq) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_LEQ_VALIDATION_ERROR);
        }

        if (!hasConstraint(NumberTypeConstraint.INFINITY_ALLOWED) && Double.isInfinite(value)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INFINITY_ERROR);
        }

        if (!hasConstraint(NumberTypeConstraint.NAN_ALLOWED) && Double.isNaN(value)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_NAN_ERROR);
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
        String output = FileHelper.read(outputValue.peek().getData(), getStorageCharset());
        double value = Double.parseDouble(output);
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
        File data = FileHelper.write(parameterName, value.getBytes(getStorageCharset()));
        StorageDataEntry entry = new StorageDataEntry(data, parameterName, StorageDataType.FILE);
        return new StorageData(entry);
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
        String outputValue = FileHelper.read(output.peek().getData(), getStorageCharset());

        NumberValue value = new NumberValue();
        value.setParameterName(outputName);
        value.setTaskRunId(id);
        value.setType(ValueType.NUMBER);
        value.setValue(Double.parseDouble(outputValue));

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
