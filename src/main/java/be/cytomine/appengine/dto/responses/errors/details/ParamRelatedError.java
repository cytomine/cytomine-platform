package be.cytomine.appengine.dto.responses.errors.details;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class ParamRelatedError extends BaseErrorDetails {
    @JsonProperty("param_name")
    private String parameterName;

    private String description;
}
