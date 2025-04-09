package be.cytomine.appengine.unit.services;

import java.nio.file.Files;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.exceptions.ValidationException;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.services.TaskValidationService;
import be.cytomine.appengine.utils.TaskUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskValidationServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskValidationService taskValidationService;

    private static Task task;

    private static UploadTaskArchive archive;

    @BeforeAll
    public static void setUp() throws Exception {
        task = TaskUtils.createTestTask(false);
        archive = TaskUtils.createTestUploadTaskArchive();
    }

    @DisplayName("Successfully return void when no duplicates")
    @Test
    public void checkIsNotDuplicateShouldReturnVoid() throws Exception {
        String namespace = archive.getDescriptorFileAsJson().get("namespace").textValue();
        String version = archive.getDescriptorFileAsJson().get("version").textValue();
        when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(null);

        assertDoesNotThrow(() -> taskValidationService.checkIsNotDuplicate(archive));
        verify(taskRepository, times(1)).findByNamespaceAndVersion(namespace, version);
    }

    @DisplayName("Successfully throw 'ValidationException' when duplicates")
    @Test
    public void checkIsNotDuplicateShouldThrowValidationException() {
        String namespace = archive.getDescriptorFileAsJson().get("namespace").textValue();
        String version = archive.getDescriptorFileAsJson().get("version").textValue();
        when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(task);

        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> taskValidationService.checkIsNotDuplicate(archive)
        );
        assertEquals("Task already exists.", exception.getMessage());
        verify(taskRepository, times(1)).findByNamespaceAndVersion(namespace, version);
    }

    @DisplayName("Successfully return void when image is valid")
    @Test
    public void validateImageShouldReturnVoid() throws Exception {
        assertDoesNotThrow(() -> taskValidationService.validateImage(archive));
    }

    @DisplayName("Fail to validate image and throw 'ValidationException'")
    @Test
    public void validateImageShouldThrowValidationException() throws Exception {
        UploadTaskArchive missing = TaskUtils.createTestUploadTaskArchive();
        missing.setDockerImage(Files.createTempFile("docker-image", ".tar").toFile());

        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> taskValidationService.validateImage(missing)
        );
        assertEquals("image is not invalid manifest is missing", exception.getMessage());
    }

    @DisplayName("Successfully validate the descriptor")
    @Test
    public void validateDescriptorFileShouldReturnVoid() throws Exception {
        assertDoesNotThrow(() -> taskValidationService.validateDescriptorFile(archive));
    }

    @DisplayName("Fail to validate the descriptor throw 'ValidationException'")
    @Test
    public void validateDescriptorFileShouldThrowValidationException() throws Exception {
        UploadTaskArchive invalid = TaskUtils.createTestUploadTaskArchive();
        JsonNode descriptor = invalid.getDescriptorFileAsJson();

        ObjectNode node = (ObjectNode) descriptor;
        node.put("name", "a");
        invalid.setDescriptorFileAsJson(descriptor);

        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> taskValidationService.validateDescriptorFile(invalid)
        );
        assertEquals("schema validation failed for descriptor.yml", exception.getMessage());
    }
}
