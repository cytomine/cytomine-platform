package be.cytomine.appengine.models.task;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.models.BaseEntity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @ElementCollection
    private List<String> constraints; // used to track which constraints are defined for this type object

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

    public void persistProvision(JsonNode provision , UUID runId){};

    public void persistResult(Run runOptional, Output currentOutput, String outputValue){};

    public FileData mapToStorageFileData(JsonNode provision , String charset) {
        return null;
    }

    public Charset getStorageCharset(String charset) {
        return switch (charset.toUpperCase()) {
            case "US_ASCII" -> StandardCharsets.US_ASCII;
            case "ISO_8859_1" -> StandardCharsets.ISO_8859_1;
            case "UTF_16LE" -> StandardCharsets.UTF_16LE;
            case "UTF_16BE" -> StandardCharsets.UTF_16BE;
            case "UTF_16" -> StandardCharsets.UTF_16;
            default -> StandardCharsets.UTF_8;
        };
    }

    public JsonNode createTypedParameterResponse(JsonNode provision, Run run) {
        return null;
    }

    public TaskRunParameterValue buildTaskRunParameterValue(String trimmedOutput, UUID id, String outputName) {return null;}

    public TaskRunParameterValue buildTaskRunParameterValue(TypePersistence typePersistence) {return null;}
}
