package be.cytomine.appengine.dto.responses.errors.details;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class BatchError extends BaseErrorDetails {
    private List<AppEngineError> errors;

}
