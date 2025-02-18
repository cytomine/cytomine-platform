package be.cytomine.appengine.models.task.file;

import java.io.File;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.types.file.FileTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.file.FileValue;
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
import be.cytomine.appengine.repositories.file.FilePersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;

@SuppressWarnings("checkstyle:LineLength")
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class FileType extends Type {

    @Column(nullable = true)
    private String maxFileSize;

    @Column(nullable = true)
    private List<String> formats;

    public void setConstraint(FileTypeConstraint constraint, JsonNode value) {
        switch (constraint) {
            case FORMATS:
                this.setFormats(parse(value.toString()));
                break;
            case MAX_FILE_SIZE:
                this.setMaxFileSize(value.asText());
                break;
            default:
        }
    }

    @Override
    public void validateFiles(
        Run run,
        Output currentOutput,
        StorageData currentOutputStorageData)
        throws TypeValidationException {

        // validate file structure
        File outputFile = getFileIfStructureIsValid(currentOutputStorageData);

        validate(outputFile);

    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (!(valueObject instanceof File)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_TYPE_ERROR);
        }
    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
        String parameterName = provision.get("param_name").asText();
        FilePersistenceRepository filePersistenceRepository = AppEngineApplicationContext.getBean(FilePersistenceRepository.class);
        FilePersistence persistedProvision = filePersistenceRepository.findFilePersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision != null) {
            return;
        }

        persistedProvision = new FilePersistence();
        persistedProvision.setParameterName(parameterName);
        persistedProvision.setParameterType(ParameterType.INPUT);
        persistedProvision.setRunId(runId);
        persistedProvision.setValueType(ValueType.FILE);

        filePersistenceRepository.save(persistedProvision);
    }

    @Override
    public void persistResult(Run run, Output currentOutput, StorageData outputValue) {
        FilePersistenceRepository filePersistenceRepository = AppEngineApplicationContext.getBean(FilePersistenceRepository.class);
        FilePersistence result = filePersistenceRepository.findFilePersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        if (result != null) {
            return;
        }
        result = new FilePersistence();
        result.setParameterType(ParameterType.OUTPUT);
        result.setParameterName(currentOutput.getName());
        result.setRunId(run.getId());
        result.setValueType(ValueType.FILE);

        filePersistenceRepository.save(result);
    }

    @Override
    public StorageData mapToStorageFileData(JsonNode provision) {
        String parameterName = provision.get("param_name").asText();
        File data = new File(provision.get("value").asText());
        StorageDataEntry entry = new StorageDataEntry(data, parameterName, StorageDataType.FILE);
        return new StorageData(entry);
    }

    @Override
    public JsonNode createTypedParameterResponse(JsonNode provision, Run run) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode provisionedParameter = mapper.createObjectNode();
        provisionedParameter.put("param_name", provision.get("param_name").asText());
        provisionedParameter.put("task_run_id", String.valueOf(run.getId()));
        return provisionedParameter;
    }

    @Override
    public FileValue buildTaskRunParameterValue(StorageData output, UUID id, String outputName) {
        FileValue fileValue = new FileValue();
        fileValue.setParameterName(outputName);
        fileValue.setTaskRunId(id);
        fileValue.setType(ValueType.FILE);
        return fileValue;
    }

    @Override
    public FileValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        FilePersistence filePersistence = (FilePersistence) typePersistence;
        FileValue fileValue = new FileValue();
        fileValue.setParameterName(filePersistence.getParameterName());
        fileValue.setTaskRunId(filePersistence.getRunId());
        fileValue.setType(ValueType.FILE);
        return fileValue;
    }
}
