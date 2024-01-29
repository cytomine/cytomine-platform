package be.cytomine.appengine.dto.responses.errors.details;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParamRelatedError extends BaseErrorDetails {
    @JsonProperty("param_name")
    private String paramName;
    private String description;

    public ParamRelatedError(String paramName, String description) {
        super();
        this.paramName = paramName;
        this.description = description;
    }
}
