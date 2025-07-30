package be.cytomine.appengine.unit.services;

import java.io.File;
import java.nio.file.Files;

import be.cytomine.appengine.utils.DescriptorHelper;
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
import org.springframework.core.io.ClassPathResource;

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

    private static JsonNode descriptorFileAsJson;

    @BeforeAll
    public static void setUp() throws Exception {
        task = TaskUtils.createTestTask(false);
        File descriptorFile = new ClassPathResource("artifacts/descriptor.yml").getFile();
        descriptorFileAsJson = DescriptorHelper.parseDescriptor(descriptorFile);
    }

    @DisplayName("Successfully return void when no duplicates")
    @Test
    public void checkIsNotDuplicateShouldReturnVoid() throws Exception {
        String namespace = descriptorFileAsJson.get("namespace").textValue();
        String version = descriptorFileAsJson.get("version").textValue();
        when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(null);

        assertDoesNotThrow(() -> taskValidationService.checkIsNotDuplicate(descriptorFileAsJson));
        verify(taskRepository, times(1)).findByNamespaceAndVersion(namespace, version);
    }

    @DisplayName("Successfully throw 'ValidationException' when duplicates")
    @Test
    public void checkIsNotDuplicateShouldThrowValidationException() {
        String namespace = descriptorFileAsJson.get("namespace").textValue();
        String version = descriptorFileAsJson.get("version").textValue();
        when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(task);

        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> taskValidationService.checkIsNotDuplicate(descriptorFileAsJson)
        );
        assertEquals("Task already exists.", exception.getMessage());
        verify(taskRepository, times(1)).findByNamespaceAndVersion(namespace, version);
    }

    @DisplayName("Successfully validate the descriptor")
    @Test
    public void validateDescriptorFileShouldReturnVoid() throws Exception {
        File descriptorFile = new ClassPathResource("artifacts/descriptor.yml").getFile();
        descriptorFileAsJson = DescriptorHelper.parseDescriptor(descriptorFile);

        assertDoesNotThrow(() -> taskValidationService.validateDescriptorFile(descriptorFileAsJson));
    }

    @DisplayName("Fail to validate the descriptor throw 'ValidationException'")
    @Test
    public void validateDescriptorFileShouldThrowValidationException() throws Exception {
        JsonNode descriptor = descriptorFileAsJson;

        ObjectNode node = (ObjectNode) descriptor;
        node.put("name", "a");

        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> taskValidationService.validateDescriptorFile(node)
        );
        assertEquals("schema validation failed for descriptor.yml", exception.getMessage());
    }
}
