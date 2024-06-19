package be.cytomine.appengine.dto.responses.errors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;

public class ErrorDefinitions {
    static final private HashMap<ErrorCode, MessageCode> codes;

    static {
        codes = new HashMap<>();
        codes.put(ErrorCode.INTERNAL_DESCRIPTOR_EXTRACTION_FAILED, new MessageCode("APPE-internal-descriptor-extraction-error", "failed to extract descriptor.yml from from bundle"));
        codes.put(ErrorCode.INTERNAL_DESCRIPTOR_NOT_IN_DEFAULT_LOCATION, new MessageCode("APPE-internal-bundle-validation-error", "descriptor is not found in the default location"));
        codes.put(ErrorCode.INTERNAL_DOCKER_IMAGE_EXTRACTION_FAILED, new MessageCode("APPE-internal-image-extraction-error", "failed to extract docker image tar from bundle"));
        codes.put(ErrorCode.INTERNAL_DOCKER_IMAGE_MANIFEST_MISSING, new MessageCode("APPE-internal-bundle-image-validation-error", "image is not invalid manifest is missing"));
        codes.put(ErrorCode.INTERNAL_DOCKER_IMAGE_TAR_NOT_FOUND, new MessageCode("APPE-internal-bundle-validation-error", "image not found in configured place in descriptor and not in the root directory"));
        codes.put(ErrorCode.INTERNAL_GENERIC_BATCH_ERROR, new MessageCode("APPE-internal-batch-request-error", "Error(s) occurred during a handling of a batch request."));
        codes.put(ErrorCode.INTERNAL_INVALID_BUNDLE_FORMAT, new MessageCode("APPE-internal-bundle-validation-error", "invalid bundle format"));
        codes.put(ErrorCode.INTERNAL_INVALID_OUTPUT, new MessageCode("APPE-internal-task_run-invalid-output-archive", "invalid outputs in archive"));
        codes.put(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE, new MessageCode("APPE-internal-task-run-state-error", "run is in invalid state"));
        codes.put(ErrorCode.INTERNAL_MAX_UPLOAD_SIZE_EXCEEDED, new MessageCode("APPE-internal-bundle-validation-error", "maximum upload size for bundle exceeded"));
        codes.put(ErrorCode.INTERNAL_MISSING_OUTPUTS, new MessageCode("APPE-internal-task-run-missing-outputs", "some outputs are missing in the archive"));
        codes.put(ErrorCode.INTERNAL_NOT_PROVISIONED, new MessageCode("APPE-internal-task-run-state-error", "not provisioned"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_DOES_NOT_EXIST, new MessageCode("APPE-internal-parameter-not-found", "parameter not found"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_GEOJSON_PROCESSING_ERROR, new MessageCode("APPE-internal-request-validation-error", "failed to parse the given GeoJSON object"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_GEOJSON_SUBTYPE_ERROR, new MessageCode("APPE-internal-request-validation-error", "unsupported GeoJSON subtype"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_GEQ_VALIDATION_ERROR, new MessageCode("APPE-internal-request-validation-error", "value must be greater than or equal to define constraint"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR, new MessageCode("APPE-internal-request-validation-error", "value must be greater than defined constraint"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_INVALID_GEOJSON, new MessageCode("APPE-internal-request-validation-error", "invalid GeoJSON object"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_LEQ_VALIDATION_ERROR, new MessageCode("APPE-internal-request-validation-error", "value must be less than or equal to defined constraint"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_LT_VALIDATION_ERROR, new MessageCode("APPE-internal-request-validation-error", "value must be less than defined constraint"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_MISSING_RADIUS_ERROR, new MessageCode("APPE-internal-request-validation-error", "missing radius parameter"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_SCHEMA_VALIDATION_ERROR, new MessageCode("APPE-internal-bundle-io-schema-validation-error", ""));
        codes.put(ErrorCode.INTERNAL_PARAMETER_TYPE_ERROR, new MessageCode("APPE-internal-request-validation-error", "invalid parameter type"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_UNSUPPORTED_GEOMETRY_SUBTYPE, new MessageCode("APPE-internal-request-validation-error", "unsupported geometry subtype"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_UNSUPPORTED_GEOMETRY_TYPE, new MessageCode("APPE-internal-request-validation-error", "unsupported geometry type"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_VALIDATION_ERROR, new MessageCode("APPE-internal-request-validation-error", "value does not match defined constraint."));
        codes.put(ErrorCode.INTERNAL_SCHEMA_VALIDATION_ERROR, new MessageCode("APPE-internal-bundle-schema-validation-error", "schema validation failed for descriptor.yml"));
        codes.put(ErrorCode.INTERNAL_SERVER_ERROR, new MessageCode("APPE-internal-server-error", "Server error."));
        codes.put(ErrorCode.INTERNAL_UNKNOWN_BUNDLE_ARCHIVE_FORAMT, new MessageCode("APPE-internal-bundle-validation-error", "unknown task bundle archive format"));
        codes.put(ErrorCode.INTERNAL_UNKNOWN_IMAGE_ARCHIVE_FORMAT, new MessageCode("APPE-internal-image-validation-error", "unknown image archive format"));
        codes.put(ErrorCode.INTERNAL_UNKNOWN_OUTPUT, new MessageCode("APPE-internal-task-run-unknown-output", "unexpected output, did not match an actual task output"));
        codes.put(ErrorCode.INTERNAL_TASK_EXISTS, new MessageCode("APPE-internal-task-exists", "Task already exists."));
        codes.put(ErrorCode.INTERNAL_TASK_NOT_FOUND, new MessageCode("APPE-internal-task-not-found", "Task not found."));
        codes.put(ErrorCode.REGISTRY_PUSHING_TASK_IMAGE_FAILED, new MessageCode("APPE-registry-push-failed", "pushing task image to registry failed in registry"));
        codes.put(ErrorCode.RUN_NOT_FOUND, new MessageCode("APPE-internal-run-not-found-error", "Run not found."));
        codes.put(ErrorCode.STORAGE_CREATING_STORAGE_FAILED, new MessageCode("APPE-storage-storage-creation-error", "creating storage failed in storage service"));
        codes.put(ErrorCode.STORAGE_STORING_INPUT_FAILED, new MessageCode("APPE-storage-storing-input-failed", "failed to store input file in storage service"));
        codes.put(ErrorCode.STORAGE_STORING_TASK_DEFINITION_FAILED, new MessageCode("APPE-storage-definition-storage-error", "storing task definition failed in storage service"));
        codes.put(ErrorCode.UKNOWN_STATE, new MessageCode("APPE-internal-task-run-state-error", "unknown state in transition request"));
    }

    public static MessageCode fromCode(ErrorCode code) {
        if (!codes.containsKey(code)) {
            throw new UndefinedCodeException(code);
        }
        return codes.get(code);
    }

}

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
class UndefinedCodeException extends RuntimeException {
    private ErrorCode code;
}

