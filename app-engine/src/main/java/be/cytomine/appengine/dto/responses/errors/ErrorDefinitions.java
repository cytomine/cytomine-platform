package be.cytomine.appengine.dto.responses.errors;

import java.util.HashMap;

import be.cytomine.appengine.exceptions.UndefinedCodeException;

@SuppressWarnings("checkstyle:LineLength")
public class ErrorDefinitions {
    private static final HashMap<ErrorCode, MessageCode> codes;

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
        codes.put(ErrorCode.INTERNAL_JSON_PROCESSING_ERROR, new MessageCode("APPE-internal-task-run-state-error", "unable to process json"));
        codes.put(ErrorCode.INTERNAL_MAX_UPLOAD_SIZE_EXCEEDED, new MessageCode("APPE-internal-bundle-validation-error", "maximum upload size for bundle exceeded"));
        codes.put(ErrorCode.INTERNAL_MISSING_OUTPUTS, new MessageCode("APPE-internal-task-run-missing-outputs", "some outputs are missing in the archive"));
        codes.put(ErrorCode.INTERNAL_NOT_PROVISIONED, new MessageCode("APPE-internal-task-run-provisions-not-found", "not provisioned"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_DOES_NOT_EXIST, new MessageCode("APPE-internal-parameter-not-found", "parameter not found"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_AFTER_ERROR, new MessageCode("APPE-internal-request-validation-error", "date must be after the defined constraint"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_BEFORE_ERROR, new MessageCode("APPE-internal-request-validation-error", "date must be before the defined constraint"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_GEOJSON_PROCESSING_ERROR, new MessageCode("APPE-internal-request-validation-error", "failed to parse the given GeoJSON object"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_GEOJSON_SUBTYPE_ERROR, new MessageCode("APPE-internal-request-validation-error", "unsupported GeoJSON subtype"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_GEQ_VALIDATION_ERROR, new MessageCode("APPE-internal-request-validation-error", "value must be greater than or equal to define constraint"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR, new MessageCode("APPE-internal-request-validation-error", "value must be greater than defined constraint"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_INFINITY_ERROR, new MessageCode("APPE-internal-request-validation-error", "value cannot be infinity"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_INVALID_GEOJSON, new MessageCode("APPE-internal-request-validation-error", "invalid GeoJSON object"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE, new MessageCode("APPE-internal-request-validation-error", "invalid image"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_DIMENSION, new MessageCode("APPE-internal-request-validation-error", "invalid image dimension"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_FORMAT, new MessageCode("APPE-internal-request-validation-error", "invalid image format"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_HEIGHT, new MessageCode("APPE-internal-request-validation-error", "invalid image height"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_SIZE, new MessageCode("APPE-internal-request-validation-error", "invalid image size"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_SIZE_FORMAT, new MessageCode("APPE-internal-request-validation-error", "invalid size format"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_INVALID_IMAGE_WIDTH, new MessageCode("APPE-internal-request-validation-error", "invalid image width"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_LEQ_VALIDATION_ERROR, new MessageCode("APPE-internal-request-validation-error", "value must be less than or equal to defined constraint"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_LT_VALIDATION_ERROR, new MessageCode("APPE-internal-request-validation-error", "value must be less than defined constraint"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_MISSING_RADIUS_ERROR, new MessageCode("APPE-internal-request-validation-error", "missing radius parameter"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_NAN_ERROR, new MessageCode("APPE-internal-request-validation-error", "value cannot be nan"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_SCHEMA_VALIDATION_ERROR, new MessageCode("APPE-internal-bundle-io-schema-validation-error", ""));
        codes.put(ErrorCode.INTERNAL_PARAMETER_TYPE_ERROR, new MessageCode("APPE-internal-request-validation-error", "invalid parameter type"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_UNSUPPORTED_GEOMETRY_SUBTYPE, new MessageCode("APPE-internal-request-validation-error", "unsupported geometry subtype"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_UNSUPPORTED_GEOMETRY_TYPE, new MessageCode("APPE-internal-request-validation-error", "unsupported geometry type"));
        codes.put(ErrorCode.INTERNAL_PARAMETER_VALIDATION_ERROR, new MessageCode("APPE-internal-request-validation-error", "value does not match defined constraint."));
        codes.put(ErrorCode.INTERNAL_PROVISIONS_NOT_FOUND, new MessageCode("APPE-internal-task-run-provisions-not-found", "provisions not found"));
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
        codes.put(ErrorCode.STORAGE_READING_FILE_FAILED, new MessageCode("APPE-storage-reading-file-error", "failed to read file from storage service"));
        codes.put(ErrorCode.STORAGE_STORING_INPUT_FAILED, new MessageCode("APPE-storage-storing-input-failed", "failed to store input file in storage service"));
        codes.put(ErrorCode.STORAGE_STORING_TASK_DEFINITION_FAILED, new MessageCode("APPE-storage-definition-storage-error", "storing task definition failed in storage service"));
        codes.put(ErrorCode.UNKNOWN_STATE, new MessageCode("APPE-internal-task-run-state-error", "unknown state in transition request"));
        codes.put(ErrorCode.SCHEDULER_UNAUTHENTICATED_OUTPUT_PROVISIONING, new MessageCode("APPE-scheduler-unauthenticated-output-provisioning", "unauthenticated task failed to provision outputs for this run"));
        codes.put(ErrorCode.INTERNAL_MISSING_OUTPUT_FILE_FOR_PARAMETER, new MessageCode("APPE-internal-missing-output-files", "file structure for primitive parameter is missing"));
        codes.put(ErrorCode.INTERNAL_OUTPUT_FILE_FOR_PARAMETER_IS_DIRECTORY, new MessageCode("APPE-internal-is-directory", "file structure for primitive parameter is a directory"));
        codes.put(ErrorCode.INTERNAL_EXTRA_OUTPUT_FILES_FOR_PARAMETER, new MessageCode("APPE-internal-extra-output-files", "file structure for parameter contains extra files"));
        codes.put(ErrorCode.INTERNAL_OUTPUT_FILE_FOR_PARAMETER_IS_BLANK, new MessageCode("APPE-internal-is-blank", "file for parameter is blank"));
        codes.put(ErrorCode.INTERNAL_INVALID_INDEXES_PATTERN, new MessageCode("APPE-internal-invalid-indexes", "indexes don't match pattern {^(0(/[0-9]+)*|[1-9][0-9]*(/[0-9]+)*)$}"));
        codes.put(ErrorCode.INTERNAL_NOT_MATCHING_DIFF_SIZE, new MessageCode("APPE-internal-not-matching", "collections don't have the same size"));
        codes.put(ErrorCode.INTERNAL_MISSING_METADATA, new MessageCode("APPE-internal-missing-metadata", "collection does not have array.yml file"));
        codes.put(ErrorCode.INTERNAL_INVALID_FEATURE_COLLECTION, new MessageCode("APPE-internal-invalid-collection", "invalid feature collection"));
        codes.put(ErrorCode.INTERNAL_WRONG_PROVISION_STRUCTURE, new MessageCode("APPE-internal-invalid-structure", "invalid provision structure"));
        codes.put(ErrorCode.INTERNAL_INVALID_COLLECTION_DIMENSIONS, new MessageCode("APPE-internal-invalid-dimensions", "invalid collection dimensions"));
        codes.put(ErrorCode.INTERNAL_UNKNOWN_PARAMETER, new MessageCode("APPE-internal-unknown", "unknown parameter"));
        codes.put(ErrorCode.INTERNAL_NULL_PROVISION, new MessageCode("APPE-internal-null-provision", "provision object is null"));
        codes.put(ErrorCode.INTERNAL_UNKNOWN_SUBTYPE, new MessageCode("APPE-internal-unknown-subtype", "collection subtype unknown"));
        codes.put(ErrorCode.INTERNAL_INVALID_METADATA, new MessageCode("APPE-internal-invalid-metadata", "collection array.yml is malformed"));
        codes.put(ErrorCode.INTERNAL_NOT_MULTIPART, new MessageCode("APPE-internal-not-multipart", "payload is not multipart"));
        codes.put(ErrorCode.INTERNAL_NO_FILE_PARTS_FOUND, new MessageCode("APPE-internal-no-file-parts", "payload does not contain file parts"));
        codes.put(ErrorCode.INTERNAL_NO_FILE_BUT_FORM_FIELD, new MessageCode("APPE-internal-no-file-but-form-field", "payload does not contain file but form field"));
        codes.put(ErrorCode.INTERNAL_CRC32_CALC_FAILED, new MessageCode("APPE-internal-checksum-failure", "failed to calculate CRC32 checksum for zip entries"));
    }


    public static MessageCode fromCode(ErrorCode code) {
        if (!codes.containsKey(code)) {
            throw new UndefinedCodeException(code);
        }
        return codes.get(code);
    }
}
