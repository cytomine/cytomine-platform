package be.cytomine.appengine.dto.responses.errors;

import java.util.List;

import be.cytomine.appengine.dto.responses.errors.details.BaseErrorDetails;
import be.cytomine.appengine.dto.responses.errors.details.BatchError;
import be.cytomine.appengine.dto.responses.errors.details.EmptyErrorDetails;
import be.cytomine.appengine.dto.responses.errors.details.MultipleErrors;
import be.cytomine.appengine.dto.responses.errors.details.ParamRelatedError;
import be.cytomine.appengine.dto.responses.errors.details.ServerError;

public class ErrorBuilder {

    /**
     * Build an error with empty details object
     *
     * @param code The error code
     */
    public static AppEngineError build(ErrorCode code) {
        return build(code, new EmptyErrorDetails());
    }

    public static AppEngineError build(ErrorCode code, BaseErrorDetails details) {
        MessageCode container = ErrorDefinitions.fromCode(code);
        return new AppEngineError(container.code, container.message, details);
    }

    public static AppEngineError buildWithMessage(
        ErrorCode code,
        String message,
        BaseErrorDetails details
    ) {
        MessageCode container = ErrorDefinitions.fromCode(code);
        return new AppEngineError(container.code, message, details);
    }

    /***
     * Build an error message for errors originating from a batch request
     *
     * @param errors list of underlying errors, each error corresponds to an item of
     *               the batch
     */
    public static AppEngineError buildBatchError(List<AppEngineError> errors) {
        ErrorCode code = ErrorCode.INTERNAL_GENERIC_BATCH_ERROR;
        return build(code, new BatchError(errors));
    }

    public static AppEngineError buildSchemaValidationError(List<AppEngineError> errors) {
        ErrorCode code = ErrorCode.INTERNAL_SCHEMA_VALIDATION_ERROR;
        return build(code, new BatchError(errors));
    }

    public static AppEngineError buildMultipleErrors(List<AppEngineError> errors) {
        ErrorCode code = ErrorCode.INTERNAL_MULTIPLE_ERRORS;
        return build(code, new MultipleErrors(errors));
    }

    /**
     * Build an error originating from a batch param
     *
     * @param code        top-level error code
     * @param paramName   param name
     * @param description param error description
     */
    public static AppEngineError buildParamRelatedError(
        ErrorCode code,
        String paramName,
        String description
    ) {
        return build(code, new ParamRelatedError(paramName, description));
    }

    /**
     * Build a server error
     *
     * @param e Exception triggered by the server error
     */
    public static AppEngineError buildServerError(Exception e) {
        ServerError serverError = new ServerError(e.getMessage());
        return build(ErrorCode.INTERNAL_SERVER_ERROR, serverError);
    }
}
