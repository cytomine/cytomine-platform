package be.cytomine.appengine.dto.responses.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import be.cytomine.appengine.dto.responses.errors.details.BaseErrorDetails;

@Data
@AllArgsConstructor
public class AppEngineError {
    @JsonProperty("error_code")
    private String errorCode;

    private String message;

    private BaseErrorDetails details;
}
