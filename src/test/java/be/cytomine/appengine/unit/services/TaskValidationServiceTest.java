package be.cytomine.appengine.unit.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.exceptions.ValidationException;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.services.TaskValidationService;
import be.cytomine.appengine.utils.TaskUtils;

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
        task = TaskUtils.createTestTask();
        archive = TaskUtils.createTestUploadTaskArchive();
    }

    @DisplayName("Successfully return void when no duplicates")
    @Test
    public void checkIsNotDuplicateShouldReturnTrue() throws Exception {
        String namespace = archive.getDescriptorFileAsJson().get("namespace").textValue();
        String version = archive.getDescriptorFileAsJson().get("version").textValue();
        Mockito.when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(null);

        Assertions.assertDoesNotThrow(() -> taskValidationService.checkIsNotDuplicate(archive));
        Mockito.verify(taskRepository, Mockito.times(1)).findByNamespaceAndVersion(namespace, version);
    }

    @DisplayName("Successfully throw 'ValidationException' when duplicates")
    @Test
    public void checkIsNotDuplicateShouldReturnFalse() {
        String namespace = archive.getDescriptorFileAsJson().get("namespace").textValue();
        String version = archive.getDescriptorFileAsJson().get("version").textValue();
        Mockito.when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(task);

        ValidationException exception = Assertions.assertThrows(
                ValidationException.class,
                () -> taskValidationService.checkIsNotDuplicate(archive));
        Assertions.assertEquals("Task already exists.", exception.getMessage());
        Mockito.verify(taskRepository, Mockito.times(1)).findByNamespaceAndVersion(namespace, version);
    }
}
