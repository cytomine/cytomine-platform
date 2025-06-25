package be.cytomine.appengine.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.responses.errors.ErrorCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class UndefinedCodeException extends RuntimeException {
    private ErrorCode code;
}
