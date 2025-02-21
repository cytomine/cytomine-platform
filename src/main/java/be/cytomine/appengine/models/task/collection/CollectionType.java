package be.cytomine.appengine.models.task.collection;

import be.cytomine.appengine.dto.inputs.task.types.collection.CollectionGenericTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.collection.CollectionGenericTypeDependency;
import be.cytomine.appengine.dto.inputs.task.types.enumeration.EnumerationValue;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageDataEntry;
import be.cytomine.appengine.handlers.StorageDataType;
import be.cytomine.appengine.models.task.Parameter;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.models.task.ValueType;
import be.cytomine.appengine.utils.FileHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;

@SuppressWarnings("checkstyle:LineLength")
@Data
@Entity
@EqualsAndHashCode(callSuper = false)
public class CollectionType extends Type {

    @Column(nullable = true)
    private Integer minSize;

    @Column(nullable = true)
    private Integer maxSize;

    @Column(nullable = true)
    private String subTypeId ;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    private Type subType;

  public void setConstraint(CollectionGenericTypeConstraint constraint, String value) {
    switch (constraint) {
      case MIN_SIZE:
        this.setMinSize(Integer.parseInt(value));
        break;
      case MAX_SIZE:
        this.setMaxSize(Integer.parseInt(value));
        break;
      default:
    }
  }

//  public void setDependency(CollectionGenericTypeDependency dependency, String value) {
//        switch (dependency) {
//            case MATCHING:
//                if (Objects.isNull(this.matching) || this.matching.isEmpty()){
//                    this.matching = new ArrayList<>();
//                }
//                this.matching.add(value);
//                break;
//            case DERIVED_FROM:
//                this.setDerivedFrom(value);
//                break;
//            default:
//        }
//    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
//        if (valueObject == null) {
//            return;
//        }
//
//        if (!(valueObject instanceof String)) {
//            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_VALIDATION_ERROR);
//        }
//
//        String value = (String) valueObject;
//
//        if (value.contains(NEW_LINE)) {
//            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR);
//        }
//
//        if (value.length() > LIMIT) {
//            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR);
//        }
//
//        if (!values.contains(value)) {
//            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_VALIDATION_ERROR);
//        }
    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
//        EnumerationPersistenceRepository repository = AppEngineApplicationContext.getBean(EnumerationPersistenceRepository.class);
//        String parameterName = provision.get("param_name").asText();
//        String value = provision.get("value").asText();
//        CollectionPersistence persistedProvision = repository.findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
//        if (persistedProvision == null) {
//            persistedProvision = new CollectionPersistence();
//            persistedProvision.setValueType(ValueType.ENUMERATION);
//            persistedProvision.setParameterType(ParameterType.INPUT);
//            persistedProvision.setParameterName(parameterName);
//            persistedProvision.setRunId(runId);
//            persistedProvision.setValue(value);
//            repository.save(persistedProvision);
//        } else {
//            persistedProvision.setValue(value);
//            repository.saveAndFlush(persistedProvision);
//        }
    }

    @Override
    public void persistResult(Run run, Parameter currentOutput, StorageData outputValue) {
//        EnumerationPersistenceRepository enumerationPersistenceRepository = AppEngineApplicationContext.getBean(EnumerationPersistenceRepository.class);
//        CollectionPersistence result = enumerationPersistenceRepository.findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
//        String output = new String(outputValue.poll().getData(), getStorageCharset());
//        String trimmedOutput = output.trim();
//        if (result == null) {
//            result = new CollectionPersistence();
//            result.setValue(trimmedOutput);
//            result.setValueType(ValueType.INTEGER);
//            result.setParameterType(ParameterType.OUTPUT);
//            result.setRunId(run.getId());
//            result.setParameterName(currentOutput.getName());
//            enumerationPersistenceRepository.save(result);
//        } else {
//            result.setValue(trimmedOutput);
//            enumerationPersistenceRepository.saveAndFlush(result);
//        }
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
        String outputValue = FileHelper.read(output.peek().getData(), getStorageCharset());
        String trimmedOutput = outputValue.trim();

        EnumerationValue enumerationValue = new EnumerationValue();
        enumerationValue.setParameterName(outputName);
        enumerationValue.setTaskRunId(id);
        enumerationValue.setType(ValueType.ENUMERATION);
        enumerationValue.setValue(trimmedOutput);
        return enumerationValue;
    }

    @Override
    public EnumerationValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        CollectionPersistence enumerationPersistence = (CollectionPersistence) typePersistence;
        EnumerationValue enumerationValue = new EnumerationValue();
//        enumerationValue.setParameterName(enumerationPersistence.getParameterName());
//        enumerationValue.setTaskRunId(enumerationPersistence.getRunId());
//        enumerationValue.setType(ValueType.ENUMERATION);
//        EnumerationValue.setValue(enumerationPersistence.getValue());

        return enumerationValue;
    }
}
