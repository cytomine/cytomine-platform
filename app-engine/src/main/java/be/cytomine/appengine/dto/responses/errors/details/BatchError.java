package be.cytomine.appengine.dto.responses.errors.details;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.responses.errors.AppEngineError;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class BatchError extends BaseErrorDetails {
    private List<AppEngineError> errors;
}
