package be.cytomine.appengine.dto.responses.errors.details;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class ParameterError extends BaseErrorDetails {
    private String param_name;
}
