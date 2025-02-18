package be.cytomine.appengine.models.task;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.util.Strings;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.models.BaseEntity;
import be.cytomine.appengine.utils.FileHelper;

@Entity
@Table(name = "type")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@EqualsAndHashCode(callSuper = true)
public class Type extends BaseEntity {
    @Id
    @Column(name = "identifier", updatable = false, nullable = false)
    @GeneratedValue(generator = "UUID")
    private UUID identifier;

    private String id;  // as found in the descriptor

    private String charset;

    /**
     * Parse a string representation of a list of string to a list of strings
     *
     * @param input The string representation of the list
     * @return  The list of strings
    */
    public static List<String> parse(String input) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(input, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void validate(Object value) throws TypeValidationException {}

    public void persistProvision(JsonNode provision, UUID runId) {}

    public void persistResult(Run runOptional, Output currentOutput, StorageData outputValue) {}

    public StorageData mapToStorageFileData(JsonNode provision) {
        return null;
    }

    public Charset getStorageCharset() {
        return switch (charset.toUpperCase()) {
            case "US_ASCII" -> StandardCharsets.US_ASCII;
            case "ISO_8859_1" -> StandardCharsets.ISO_8859_1;
            case "UTF_16LE" -> StandardCharsets.UTF_16LE;
            case "UTF_16BE" -> StandardCharsets.UTF_16BE;
            case "UTF_16" -> StandardCharsets.UTF_16;
            default -> StandardCharsets.UTF_8;
        };
    }

    // Todo : rename
    public JsonNode createTypedParameterResponse(JsonNode provision, Run run) {
        return null;
    }

    // Todo : rename
    public TaskRunParameterValue buildTaskRunParameterValue(
        StorageData outputData,
        UUID id,
        String outputName
    ) {
        return null;
    }

    // Todo : rename
    public TaskRunParameterValue buildTaskRunParameterValue(
        TypePersistence typePersistence
    ) {
        return null;
    }

    public void validateFiles(
        Run run,
        Output currentOutput,
        StorageData currentOutputStorageData)
        throws TypeValidationException {}

    public static File getFileIfStructureIsValid(StorageData currentOutputStorageData)
        throws TypeValidationException {
        // validate file structure
        File outputFile = currentOutputStorageData.peek().getData();
        if (!outputFile.exists()) {
            throw new TypeValidationException(
                ErrorCode.INTERNAL_MISSING_OUTPUT_FILE_FOR_PARAMETER
            );
        }

        if (outputFile.isDirectory()) {
            throw new TypeValidationException(
                ErrorCode.INTERNAL_OUTPUT_FILE_FOR_PARAMETER_IS_DIRECTORY
            );
        }

        if (currentOutputStorageData.getEntryList().size() > 1) {
            throw new TypeValidationException(
                ErrorCode.INTERNAL_EXTRA_OUTPUT_FILES_FOR_PARAMETER
            );
        }
        return outputFile;
    }

    public String getContentIfValid(File outputFile) throws TypeValidationException {
        String rawValue = FileHelper.read(outputFile, getStorageCharset());

        if (Strings.isBlank(rawValue)) { // not isEmpty()
            throw new TypeValidationException(
                ErrorCode.INTERNAL_OUTPUT_FILE_FOR_PARAMETER_IS_BLANK
            );
        }
        return rawValue;
    }
}
