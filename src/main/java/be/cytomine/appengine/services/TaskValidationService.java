package be.cytomine.appengine.services;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.dto.responses.errors.details.ParameterError;
import be.cytomine.appengine.exceptions.ValidationException;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.repositories.TaskRepository;

@Slf4j
@AllArgsConstructor
@Service
public class TaskValidationService {

    private final TaskRepository repository;

    public void checkIsNotDuplicate(UploadTaskArchive task) throws ValidationException {
        String namespace = task.getDescriptorFileAsJson().get("namespace").textValue();
        String version = task.getDescriptorFileAsJson().get("version").textValue();

        Task found = repository.findByNamespaceAndVersion(namespace, version);
        if (found != null) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_TASK_EXISTS);
            throw new ValidationException(error, false, true);
        }
    }

    public void validateImage(UploadTaskArchive task) throws ValidationException {
        checkManifestJsonExists(task);
    }

    private void checkManifestJsonExists(UploadTaskArchive task) throws ValidationException {
        try (TarArchiveInputStream tais = new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(task.getDockerImage())))) {
            TarArchiveEntry tarArchiveEntry;

            while ((tarArchiveEntry = tais.getNextTarEntry()) != null) {
                String name = tarArchiveEntry.getName();
                if (name.equalsIgnoreCase("manifest.json")) {
                    return;
                }
            }
        } catch (IOException e) {
            log.error("Failed to check for manifest.json in the Docker image", e);
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_DOCKER_IMAGE_EXTRACTION_FAILED);
            throw new ValidationException(error);
        }

        log.info("Validation error [manifest.json does not exist]");
        AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_DOCKER_IMAGE_MANIFEST_MISSING);
        throw new ValidationException(error);
    }

    public void validateDescriptorFile(UploadTaskArchive task) throws ValidationException {
        Set<ValidationMessage> errors = getDescriptorJsonSchemaV7().validate(task.getDescriptorFileAsJson());
        List<AppEngineError> multipleErrors = new ArrayList<>();

        for (ValidationMessage message : errors) {
            AppEngineError error = buildErrorFromValidationMessage(message);
            multipleErrors.add(error);
        }

        if (!multipleErrors.isEmpty()) {
            AppEngineError error = ErrorBuilder.buildSchemaValidationError(multipleErrors);
            throw new ValidationException(error);
        }
    }

    @NotNull
    private static AppEngineError buildErrorFromValidationMessage(ValidationMessage message) {
        String parameterPathInSchema = message.getMessage().substring(0, message.getMessage().indexOf(':')).replace("$.", "");
        ParameterError parameterError = new ParameterError(parameterPathInSchema);
        String formattedMessage = message.getMessage().replace("$.", "").replace(":", "").replace(".", " ");
        AppEngineError error = ErrorBuilder.buildWithMessage(ErrorCode.INTERNAL_PARAMETER_SCHEMA_VALIDATION_ERROR, formattedMessage, parameterError);
        return error;
    }

    private static JsonSchema getDescriptorJsonSchemaV7() throws ValidationException {
        ClassPathResource resource = new ClassPathResource("/schemas/tasks/task.v0.json");

        JsonNode schemaJson;
        try (InputStream inputStream = resource.getInputStream()) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            try (JsonParser jsonParser = mapper.getFactory().createParser(content)) {
                schemaJson = jsonParser.readValueAsTree();
            }
        } catch (IOException e) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_DESCRIPTOR_EXTRACTION_FAILED);
            throw new ValidationException(error, true);
        }

        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        return schemaFactory.getSchema(schemaJson);
    }
}
