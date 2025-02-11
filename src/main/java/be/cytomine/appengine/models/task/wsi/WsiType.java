package be.cytomine.appengine.models.task.wsi;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageDataEntry;
import be.cytomine.appengine.handlers.StorageDataType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.types.wsi.WsiTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.wsi.WsiValue;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.models.task.ValueType;
import be.cytomine.appengine.models.task.formats.FileFormat;
import be.cytomine.appengine.repositories.wsi.WsiPersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import be.cytomine.appengine.utils.units.Unit;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class WsiType extends Type {

    @Column(nullable = true)
    private String maxFileSize;

    @Column(nullable = true)
    private Integer maxWidth;

    @Column(nullable = true)
    private Integer maxHeight;

    @Column(nullable = true)
    private List<String> formats;

    @Transient
    private FileFormat format;

    public void setConstraint(WsiTypeConstraint constraint, JsonNode value) {
        switch (constraint) {
            case FORMATS:
                this.setFormats(parse(value.toString()));
                break;
            case MAX_FILE_SIZE:
                this.setMaxFileSize(value.asText());
                break;
            case MAX_WIDTH:
                this.setMaxWidth(value.asInt());
                break;
            case MAX_HEIGHT:
                this.setMaxHeight(value.asInt());
                break;
        }
    }

    private void validateImageFormat(byte[] file) throws TypeValidationException {
        if (formats == null || formats.isEmpty()) {
            this.format = WsiFormatFactory.getGenericFormat();
            return;
        }

        List<FileFormat> checkers = formats
                .stream()
                .map(WsiFormatFactory::getFormat)
                .collect(Collectors.toList());

        this.format = checkers
                .stream()
                .filter(checker -> checker.checkSignature(file))
                .findFirst()
                .orElse(null);

        if (this.format == null) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_FORMAT);
        }
    }

    private void validateImageDimension(byte[] file) throws TypeValidationException {
        if (maxWidth == null && maxHeight == null) {
            return;
        }

        Dimension dimension = format.getDimensions(file);
        if (dimension == null) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_DIMENSION);
        }

        if (maxWidth != null && dimension.getWidth() > maxWidth) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_WIDTH);
        }

        if (maxHeight != null && dimension.getHeight() > maxHeight) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_HEIGHT);
        }
    }

    private void validateImageSize(byte[] file) throws TypeValidationException {
        if (maxFileSize == null) {
            return;
        }

        if (!Unit.isValid(maxFileSize)) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_SIZE_FORMAT);
        }

        Unit unit = new Unit(maxFileSize);
        if (file.length > unit.getBytes()) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_SIZE);
        }
    }

    @Override
    public void validate(Object valueObject) throws TypeValidationException {
        if (!(valueObject instanceof byte[])) {
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_TYPE_ERROR);
        }

        byte[] file = (byte[]) valueObject;

        validateImageFormat(file);

        validateImageDimension(file);

        validateImageSize(file);
    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
        String parameterName = provision.get("param_name").asText();
        WsiPersistenceRepository wsiPersistenceRepository = AppEngineApplicationContext.getBean(WsiPersistenceRepository.class);
        WsiPersistence persistedProvision = wsiPersistenceRepository.findWsiPersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision != null) {
            return;
        }

        persistedProvision = new WsiPersistence();
        persistedProvision.setParameterName(parameterName);
        persistedProvision.setParameterType(ParameterType.INPUT);
        persistedProvision.setRunId(runId);
        persistedProvision.setValueType(ValueType.WSI);

        wsiPersistenceRepository.save(persistedProvision);
    }

    @Override
    public void persistResult(Run run, Output currentOutput, StorageData  outputValue) {
        WsiPersistenceRepository wsiPersistenceRepository = AppEngineApplicationContext.getBean(WsiPersistenceRepository.class);
        WsiPersistence result = wsiPersistenceRepository.findWsiPersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        if (result != null) {
            return;
        }

        result = new WsiPersistence();
        result.setParameterType(ParameterType.OUTPUT);
        result.setParameterName(currentOutput.getName());
        result.setRunId(run.getId());
        result.setValueType(ValueType.WSI);

        wsiPersistenceRepository.save(result);
    }

    @Override
    public StorageData mapToStorageFileData(JsonNode provision) {
        String parameterName = provision.get("param_name").asText();
        byte[] inputFileData = null;

        try {
            inputFileData = provision.get("value").binaryValue();
        } catch (IOException ignored) {}

        StorageDataEntry storageDataEntry = new StorageDataEntry(inputFileData, parameterName , StorageDataType.FILE);
        return new StorageData(storageDataEntry);
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
    public WsiValue buildTaskRunParameterValue(StorageData output, UUID id, String outputName) {
        WsiValue wsiValue = new WsiValue();
        wsiValue.setParameterName(outputName);
        wsiValue.setTaskRunId(id);
        wsiValue.setType(ValueType.WSI);

        return wsiValue;
    }

    @Override
    public WsiValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        WsiPersistence wsiPersistence = (WsiPersistence) typePersistence;
        WsiValue wsiValue = new WsiValue();
        wsiValue.setParameterName(wsiPersistence.getParameterName());
        wsiValue.setTaskRunId(wsiPersistence.getRunId());
        wsiValue.setType(ValueType.WSI);

        return wsiValue;
    }
}
