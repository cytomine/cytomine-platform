package be.cytomine.appengine.unit.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.handlers.registry.DockerImage;
import be.cytomine.appengine.dto.inputs.task.TaskDescription;
import be.cytomine.appengine.dto.inputs.task.TaskRun;
import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.exceptions.RunTaskServiceException;
import be.cytomine.appengine.exceptions.TaskNotFoundException;
import be.cytomine.appengine.exceptions.TaskServiceException;
import be.cytomine.appengine.handlers.RegistryHandler;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.services.TaskService;
import be.cytomine.appengine.services.TaskValidationService;
import be.cytomine.appengine.states.TaskRunState;
import be.cytomine.appengine.utils.ArchiveUtils;
import be.cytomine.appengine.utils.TaskUtils;
import be.cytomine.appengine.utils.TestTaskBuilder;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private ArchiveUtils archiveUtils;

    @Mock
    private RegistryHandler registryHandler;

    @Mock
    private StorageHandler storageHandler;

    @Mock
    private RunRepository runRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskValidationService taskValidationService;

    @InjectMocks
    private TaskService taskService;

    private static Task task;

    private static UploadTaskArchive uploadTaskArchive;

    @BeforeAll
    public static void setUp() throws Exception {
        task = TaskUtils.createTestTask(false);
        uploadTaskArchive = TaskUtils.createTestUploadTaskArchive();
    }

    @DisplayName("Successfully upload a task bundle")
    @Test
    public void uploadTaskShouldUploadTaskBundle() throws Exception {
        ClassPathResource resource = TestTaskBuilder.buildCustomImageLocationTask();
        MockMultipartFile testAppBundle = new MockMultipartFile("test_custom_image_location_task.zip", resource.getInputStream());

        Mockito.when(archiveUtils.readArchive(testAppBundle)).thenReturn(uploadTaskArchive);
        Optional<TaskDescription> result = taskService.uploadTask(testAppBundle);

        Assertions.assertTrue(result.isPresent());
        Mockito.verify(archiveUtils, Mockito.times(1)).readArchive(testAppBundle);
        Mockito.verify(storageHandler, Mockito.times(1)).createStorage(any(Storage.class));
        Mockito.verify(storageHandler, Mockito.times(1)).saveStorageData(any(Storage.class), any(StorageData.class));
        Mockito.verify(registryHandler, Mockito.times(1)).pushImage(any(DockerImage.class));
        Mockito.verify(taskRepository, Mockito.times(1)).save(any(Task.class));
    }

    @DisplayName("Successfully retrieve the descriptor by namespace and version")
    @Test
    public void retrieveYmlDescriptorByNamespaceAndVersionShouldReturnDescriptor() throws Exception {
        String namespace = "namespace";
        String version = "version";
        StorageData mockStorageData = new StorageData("descriptor.yml", "storageReference");
        Mockito.when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(task);
        Mockito.when(storageHandler.readStorageData(any(StorageData.class))).thenReturn(mockStorageData);

        StorageData result = taskService.retrieveYmlDescriptor(namespace, version);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("descriptor.yml", result.peek().getName());
        Assertions.assertEquals("storageReference", result.peek().getStorageId());
        Mockito.verify(taskRepository, Mockito.times(1)).findByNamespaceAndVersion(namespace, version);
        Mockito.verify(storageHandler, Mockito.times(1)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Fail to retrieve the descriptor by namespace and version and throw TaskNotFoundException")
    @Test
    public void retrieveYmlDescriptorByNamespaceAndVersionShouldThrowTaskNotFoundException() throws Exception {
        String namespace = "namespace";
        String version = "version";
        Mockito.when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(null);

        TaskNotFoundException exception = Assertions.assertThrows(
            TaskNotFoundException.class,
            () -> taskService.retrieveYmlDescriptor(namespace, version)
        );
        Assertions.assertEquals("task not found", exception.getMessage());
        Mockito.verify(taskRepository, Mockito.times(1)).findByNamespaceAndVersion(namespace, version);
        Mockito.verify(storageHandler, Mockito.times(0)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Fail to retrieve the descriptor by namespace and version and throw FileStorageException")
    @Test
    public void retrieveYmlDescriptorByNamespaceAndVersionShouldThrowFileStorageException() throws Exception {
        String namespace = "namespace";
        String version = "version";
        Mockito.when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(task);
        Mockito.when(storageHandler.readStorageData(any(StorageData.class)))
            .thenThrow(new FileStorageException("File error"));

        TaskServiceException exception = Assertions.assertThrows(
            TaskServiceException.class,
            () -> taskService.retrieveYmlDescriptor(namespace, version)
        );
        Assertions.assertTrue(exception.getCause() instanceof FileStorageException);
        Mockito.verify(taskRepository, Mockito.times(1)).findByNamespaceAndVersion(namespace, version);
        Mockito.verify(storageHandler, Mockito.times(1)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Successfully retrieve the descriptor by ID")
    @Test
    public void retrieveYmlDescriptorByIdShouldReturnDescriptor() throws Exception {
        String id = "d9aad8ab-210c-48fa-8d94-6b03e8776a55";
        StorageData mockStorageData = new StorageData("descriptor.yml", "storageReference");
        Mockito.when(taskRepository.findById(UUID.fromString(id))).thenReturn(Optional.of(task));
        Mockito.when(storageHandler.readStorageData(any(StorageData.class))).thenReturn(mockStorageData);

        StorageData result = taskService.retrieveYmlDescriptor(id);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("descriptor.yml", result.peek().getName());
        Assertions.assertEquals("storageReference", result.peek().getStorageId());
        Mockito.verify(taskRepository, Mockito.times(1)).findById(UUID.fromString(id));
        Mockito.verify(storageHandler, Mockito.times(1)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Fail to retrieve the descriptor by ID and throw TaskNotFoundException")
    @Test
    public void retrieveYmlDescriptorByIdShouldThrowTaskNotFoundException() throws Exception {
        Mockito.when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.empty());

        TaskNotFoundException exception = Assertions.assertThrows(
            TaskNotFoundException.class,
            () -> taskService.retrieveYmlDescriptor(task.getIdentifier().toString())
        );
        Assertions.assertEquals("task not found", exception.getMessage());
        Mockito.verify(taskRepository, Mockito.times(1)).findById(task.getIdentifier());
        Mockito.verify(storageHandler, Mockito.times(0)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Fail to retrieve the descriptor by ID and throw FileStorageException")
    @Test
    public void retrieveYmlDescriptorByIdShouldThrowFileStorageException() throws Exception {
        Mockito.when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.of(task));
        Mockito.when(storageHandler.readStorageData(any(StorageData.class)))
            .thenThrow(new FileStorageException("File error"));

        TaskServiceException exception = Assertions.assertThrows(
            TaskServiceException.class,
            () -> taskService.retrieveYmlDescriptor(task.getIdentifier().toString())
        );
        Assertions.assertTrue(exception.getCause() instanceof FileStorageException);
        Mockito.verify(taskRepository, Mockito.times(1)).findById(task.getIdentifier());
        Mockito.verify(storageHandler, Mockito.times(1)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Successfully retrieve the task description by ID")
    @Test
    void retrieveTaskDescriptionByIdShouldReturnTaskDescription() {
        Mockito.when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.of(task));

        Optional<TaskDescription> result = taskService.retrieveTaskDescription(task.getIdentifier().toString());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Test Task Description", result.get().getDescription());
        Mockito.verify(taskRepository, Mockito.times(1)).findById(task.getIdentifier());
    }

    @DisplayName("Fail to retrieve the task description by ID")
    @Test
    void retrieveTaskDescriptionByIdShouldReturnEmpty() {
        Mockito.when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.empty());

        Optional<TaskDescription> result = taskService.retrieveTaskDescription(task.getIdentifier().toString());

        Assertions.assertFalse(result.isPresent());
        Mockito.verify(taskRepository, Mockito.times(1)).findById(task.getIdentifier());
    }

    @DisplayName("Successfully retrieve the task description by namespace and version")
    @Test
    void retrieveTaskDescriptionByNamespaceAndVersionShouldReturnTaskDescription() {
        Mockito.when(taskRepository.findByNamespaceAndVersion(task.getNamespace(), task.getVersion())).thenReturn(task);

        Optional<TaskDescription> result = taskService.retrieveTaskDescription(task.getNamespace(), task.getVersion());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Test Task Description", result.get().getDescription());
        Mockito.verify(taskRepository, Mockito.times(1)).findByNamespaceAndVersion(task.getNamespace(), task.getVersion());
    }

    @DisplayName("Fail to retrieve the task description by namespace and version")
    @Test
    void retrieveTaskDescriptionByNamespaceAndVersionShouldReturnEmpty() {
        Mockito.when(taskRepository.findByNamespaceAndVersion(task.getNamespace(), task.getVersion())).thenReturn(null);

        Optional<TaskDescription> result = taskService.retrieveTaskDescription(task.getNamespace(), task.getVersion());

        Assertions.assertFalse(result.isPresent());
        Mockito.verify(taskRepository, Mockito.times(1)).findByNamespaceAndVersion(task.getNamespace(), task.getVersion());
    }

    @DisplayName("Successfully retrieve all task descriptions")
    @Test
    void retrieveTaskDescriptionsShouldReturnAllTaskDescriptions() {
        List<Task> tasks = List.of(task, task);
        Mockito.when(taskRepository.findAll()).thenReturn(tasks);

        List<TaskDescription> result = taskService.retrieveTaskDescriptions();

        Assertions.assertTrue(tasks.size() == result.size());
        Mockito.verify(taskRepository, Mockito.times(1)).findAll();
    }

    @DisplayName("Successfully retrieve no task descriptions")
    @Test
    void retrieveTaskDescriptionsShouldReturnEmpty() {
        Mockito.when(taskRepository.findAll()).thenReturn(List.of());

        List<TaskDescription> result = taskService.retrieveTaskDescriptions();

        Assertions.assertTrue(result.size() == 0);
        Mockito.verify(taskRepository, Mockito.times(1)).findAll();
    }

    @DisplayName("Successfully create a task description")
    @Test
    void makeTaskDescriptionShouldReturnTaskDescription() {
        TaskDescription result = taskService.makeTaskDescription(task);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(task.getIdentifier(), result.getId());
        Assertions.assertEquals(task.getNamespace(), result.getNamespace());
        Assertions.assertEquals(task.getVersion(), result.getVersion());
        Assertions.assertEquals(task.getDescription(), result.getDescription());
    }

    @DisplayName("Successfully create a task run by ID")
    @Test
    void createRunForTaskByIdShouldReturnTaskRun() throws Exception {
        Mockito.when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.of(task));

        TaskRun result = taskService.createRunForTask(task.getIdentifier().toString());

        Assertions.assertNotNull(result);
        Assertions.assertEquals(task.getIdentifier(), result.getTask().getId());
        Assertions.assertEquals(task.getNamespace(), result.getTask().getNamespace());
        Assertions.assertEquals(task.getVersion(), result.getTask().getVersion());
        Assertions.assertEquals(task.getDescription(), result.getTask().getDescription());
        Assertions.assertEquals(TaskRunState.CREATED, result.getState());
        Mockito.verify(taskRepository, Mockito.times(1)).findById(task.getIdentifier());
        Mockito.verify(runRepository, Mockito.times(1)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Fail to create a task run by ID and throw RunTaskServiceException")
    @Test
    void createRunForTaskByIdShouldThrowRunTaskServiceException() throws Exception {
        Mockito.when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.empty());

        String expectedMessage = "task {" + task.getIdentifier() + "} not found to associate with this run";

        RunTaskServiceException exception = Assertions.assertThrows(
            RunTaskServiceException.class,
            () -> taskService.createRunForTask(task.getIdentifier().toString())
        );
        Assertions.assertEquals(expectedMessage, exception.getMessage());
        Mockito.verify(taskRepository, Mockito.times(1)).findById(task.getIdentifier());
        Mockito.verify(runRepository, Mockito.times(0)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Successfully create a task run by namespace and version")
    @Test
    void createRunForTaskByNamespaceAndVersionShouldReturnTaskRun() throws Exception {
        Mockito.when(taskRepository.findByNamespaceAndVersion(task.getNamespace(), task.getVersion())).thenReturn(task);

        TaskRun result = taskService.createRunForTask(task.getNamespace(), task.getVersion());

        Assertions.assertNotNull(result);
        Assertions.assertEquals(task.getIdentifier(), result.getTask().getId());
        Assertions.assertEquals(task.getNamespace(), result.getTask().getNamespace());
        Assertions.assertEquals(task.getVersion(), result.getTask().getVersion());
        Assertions.assertEquals(task.getDescription(), result.getTask().getDescription());
        Assertions.assertEquals(TaskRunState.CREATED, result.getState());
        Mockito.verify(taskRepository, Mockito.times(1)).findByNamespaceAndVersion(task.getNamespace(), task.getVersion());
        Mockito.verify(runRepository, Mockito.times(1)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Fail to create a task run by namespace and version and throw RunTaskServiceException")
    @Test
    void createRunForTaskByNamespaceAndVersionShouldThrowRunTaskServiceException() throws Exception {
        Mockito.when(taskRepository.findByNamespaceAndVersion(task.getNamespace(), task.getVersion())).thenReturn(null);

        String expectedMessage = "task {" + task.getNamespace() + ":" +  task.getVersion() + "} not found to associate with this run";

        RunTaskServiceException exception = Assertions.assertThrows(
            RunTaskServiceException.class,
            () -> taskService.createRunForTask(task.getNamespace(), task.getVersion())
        );
        Assertions.assertEquals(expectedMessage, exception.getMessage());
        Mockito.verify(taskRepository, Mockito.times(1)).findByNamespaceAndVersion(task.getNamespace(), task.getVersion());
        Mockito.verify(runRepository, Mockito.times(0)).saveAndFlush(any(Run.class));
    }
}
