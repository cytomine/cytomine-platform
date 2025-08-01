package be.cytomine.appengine.dto.responses.errors.details;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class ServerError extends BaseErrorDetails {
    private String exceptionMessage;
}
