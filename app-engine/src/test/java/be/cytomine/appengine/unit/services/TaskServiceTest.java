package be.cytomine.appengine.unit.services;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import be.cytomine.appengine.utils.DescriptorHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private static JsonNode descriptorFileAsJson;

    @BeforeAll
    public static void setUp() throws Exception {
        task = TaskUtils.createTestTask(false);
        File descriptorFile = new ClassPathResource("artifacts/descriptor.yml").getFile();
        descriptorFileAsJson = DescriptorHelper.parseDescriptor(descriptorFile);
    }

    @DisplayName("Successfully upload a task bundle")
    @Test
    public void uploadTaskShouldUploadTaskBundle() throws Exception {
        // Load test ZIP file from resources
        ClassPathResource resource = TestTaskBuilder.buildCustomImageLocationTask();
        byte[] fileBytes = resource.getInputStream().readAllBytes();

        // Create boundary and multipart payload manually
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String CRLF = "\r\n";
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        DataOutputStream writer = new DataOutputStream(payload);

        // Multipart body
        writer.writeBytes("--" + boundary + CRLF);
        writer.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"test_custom_image_location_task.zip\"" + CRLF);
        writer.writeBytes("Content-Type: application/zip" + CRLF);
        writer.writeBytes(CRLF);
        writer.write(fileBytes);
        writer.writeBytes(CRLF);
        writer.writeBytes("--" + boundary + "--" + CRLF);

        byte[] multipartBody = payload.toByteArray();

        // Mock HttpServletRequest
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("multipart/form-data; boundary=" + boundary);
        request.setMethod("POST");
        request.setContent(multipartBody);
        request.setCharacterEncoding("UTF-8");

        Optional<TaskDescription> result = taskService.uploadTask(request);

        assertTrue(result.isPresent());
        verify(storageHandler, times(1)).createStorage(any(Storage.class));
        verify(storageHandler, times(1)).saveStorageData(any(Storage.class), any(StorageData.class));
        verify(registryHandler, times(1)).pushImage(any(InputStream.class), any(String.class));
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @DisplayName("Successfully retrieve the descriptor by namespace and version")
    @Test
    public void retrieveYmlDescriptorByNamespaceAndVersionShouldReturnDescriptor() throws Exception {
        String namespace = "namespace";
        String version = "version";
        StorageData mockStorageData = new StorageData("descriptor.yml", "storageReference");
        when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(task);
        when(storageHandler.readStorageData(any(StorageData.class))).thenReturn(mockStorageData);

        StorageData result = taskService.retrieveYmlDescriptor(namespace, version);

        assertNotNull(result);
        assertEquals("descriptor.yml", result.peek().getName());
        assertEquals("storageReference", result.peek().getStorageId());
        verify(taskRepository, times(1)).findByNamespaceAndVersion(namespace, version);
        verify(storageHandler, times(1)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Fail to retrieve the descriptor by namespace and version and throw TaskNotFoundException")
    @Test
    public void retrieveYmlDescriptorByNamespaceAndVersionShouldThrowTaskNotFoundException() throws Exception {
        String namespace = "namespace";
        String version = "version";
        when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(null);

        TaskNotFoundException exception = assertThrows(
            TaskNotFoundException.class,
            () -> taskService.retrieveYmlDescriptor(namespace, version)
        );
        assertEquals("task not found", exception.getMessage());
        verify(taskRepository, times(1)).findByNamespaceAndVersion(namespace, version);
        verify(storageHandler, times(0)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Fail to retrieve the descriptor by namespace and version and throw FileStorageException")
    @Test
    public void retrieveYmlDescriptorByNamespaceAndVersionShouldThrowFileStorageException() throws Exception {
        String namespace = "namespace";
        String version = "version";
        when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(task);
        when(storageHandler.readStorageData(any(StorageData.class)))
            .thenThrow(new FileStorageException("File error"));

        TaskServiceException exception = assertThrows(
            TaskServiceException.class,
            () -> taskService.retrieveYmlDescriptor(namespace, version)
        );
        assertTrue(exception.getCause() instanceof FileStorageException);
        verify(taskRepository, times(1)).findByNamespaceAndVersion(namespace, version);
        verify(storageHandler, times(1)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Successfully retrieve the descriptor by ID")
    @Test
    public void retrieveYmlDescriptorByIdShouldReturnDescriptor() throws Exception {
        String id = "d9aad8ab-210c-48fa-8d94-6b03e8776a55";
        StorageData mockStorageData = new StorageData("descriptor.yml", "storageReference");
        when(taskRepository.findById(UUID.fromString(id))).thenReturn(Optional.of(task));
        when(storageHandler.readStorageData(any(StorageData.class))).thenReturn(mockStorageData);

        StorageData result = taskService.retrieveYmlDescriptor(id);

        assertNotNull(result);
        assertEquals("descriptor.yml", result.peek().getName());
        assertEquals("storageReference", result.peek().getStorageId());
        verify(taskRepository, times(1)).findById(UUID.fromString(id));
        verify(storageHandler, times(1)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Fail to retrieve the descriptor by ID and throw TaskNotFoundException")
    @Test
    public void retrieveYmlDescriptorByIdShouldThrowTaskNotFoundException() throws Exception {
        when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(
            TaskNotFoundException.class,
            () -> taskService.retrieveYmlDescriptor(task.getIdentifier().toString())
        );
        assertEquals("task not found", exception.getMessage());
        verify(taskRepository, times(1)).findById(task.getIdentifier());
        verify(storageHandler, times(0)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Fail to retrieve the descriptor by ID and throw FileStorageException")
    @Test
    public void retrieveYmlDescriptorByIdShouldThrowFileStorageException() throws Exception {
        when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.of(task));
        when(storageHandler.readStorageData(any(StorageData.class)))
            .thenThrow(new FileStorageException("File error"));

        TaskServiceException exception = assertThrows(
            TaskServiceException.class,
            () -> taskService.retrieveYmlDescriptor(task.getIdentifier().toString())
        );
        assertTrue(exception.getCause() instanceof FileStorageException);
        verify(taskRepository, times(1)).findById(task.getIdentifier());
        verify(storageHandler, times(1)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Successfully retrieve the task description by ID")
    @Test
    void retrieveTaskDescriptionByIdShouldReturnTaskDescription() {
        when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.of(task));

        Optional<TaskDescription> result = taskService.retrieveTaskDescription(task.getIdentifier().toString());

        assertTrue(result.isPresent());
        assertEquals("Test Task Description", result.get().getDescription());
        verify(taskRepository, times(1)).findById(task.getIdentifier());
    }

    @DisplayName("Fail to retrieve the task description by ID")
    @Test
    void retrieveTaskDescriptionByIdShouldReturnEmpty() {
        when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.empty());

        Optional<TaskDescription> result = taskService.retrieveTaskDescription(task.getIdentifier().toString());

        assertFalse(result.isPresent());
        verify(taskRepository, times(1)).findById(task.getIdentifier());
    }

    @DisplayName("Successfully retrieve the task description by namespace and version")
    @Test
    void retrieveTaskDescriptionByNamespaceAndVersionShouldReturnTaskDescription() {
        when(taskRepository.findByNamespaceAndVersion(task.getNamespace(), task.getVersion())).thenReturn(task);

        Optional<TaskDescription> result = taskService.retrieveTaskDescription(task.getNamespace(), task.getVersion());

        assertTrue(result.isPresent());
        assertEquals("Test Task Description", result.get().getDescription());
        verify(taskRepository, times(1)).findByNamespaceAndVersion(task.getNamespace(), task.getVersion());
    }

    @DisplayName("Fail to retrieve the task description by namespace and version")
    @Test
    void retrieveTaskDescriptionByNamespaceAndVersionShouldReturnEmpty() {
        when(taskRepository.findByNamespaceAndVersion(task.getNamespace(), task.getVersion())).thenReturn(null);

        Optional<TaskDescription> result = taskService.retrieveTaskDescription(task.getNamespace(), task.getVersion());

        assertFalse(result.isPresent());
        verify(taskRepository, times(1)).findByNamespaceAndVersion(task.getNamespace(), task.getVersion());
    }

    @DisplayName("Successfully retrieve all task descriptions")
    @Test
    void retrieveTaskDescriptionsShouldReturnAllTaskDescriptions() {
        List<Task> tasks = List.of(task, task);
        when(taskRepository.findAll()).thenReturn(tasks);

        List<TaskDescription> result = taskService.retrieveTaskDescriptions();

        assertTrue(tasks.size() == result.size());
        verify(taskRepository, times(1)).findAll();
    }

    @DisplayName("Successfully retrieve no task descriptions")
    @Test
    void retrieveTaskDescriptionsShouldReturnEmpty() {
        when(taskRepository.findAll()).thenReturn(List.of());

        List<TaskDescription> result = taskService.retrieveTaskDescriptions();

        assertTrue(result.size() == 0);
        verify(taskRepository, times(1)).findAll();
    }

    @DisplayName("Successfully create a task description")
    @Test
    void makeTaskDescriptionShouldReturnTaskDescription() {
        TaskDescription result = taskService.makeTaskDescription(task);

        assertNotNull(result);
        assertEquals(task.getIdentifier(), result.getId());
        assertEquals(task.getNamespace(), result.getNamespace());
        assertEquals(task.getVersion(), result.getVersion());
        assertEquals(task.getDescription(), result.getDescription());
    }

    @DisplayName("Successfully create a task run by ID")
    @Test
    void createRunForTaskByIdShouldReturnTaskRun() throws Exception {
        when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.of(task));

        TaskRun result = taskService.createRunForTask(task.getIdentifier().toString());

        assertNotNull(result);
        assertEquals(task.getIdentifier(), result.getTask().getId());
        assertEquals(task.getNamespace(), result.getTask().getNamespace());
        assertEquals(task.getVersion(), result.getTask().getVersion());
        assertEquals(task.getDescription(), result.getTask().getDescription());
        assertEquals(TaskRunState.CREATED, result.getState());
        verify(taskRepository, times(1)).findById(task.getIdentifier());
        verify(runRepository, times(1)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Fail to create a task run by ID and throw RunTaskServiceException")
    @Test
    void createRunForTaskByIdShouldThrowRunTaskServiceException() throws Exception {
        when(taskRepository.findById(task.getIdentifier())).thenReturn(Optional.empty());

        String expectedMessage = "task {" + task.getIdentifier() + "} not found to associate with this run";

        RunTaskServiceException exception = assertThrows(
            RunTaskServiceException.class,
            () -> taskService.createRunForTask(task.getIdentifier().toString())
        );
        assertEquals(expectedMessage, exception.getMessage());
        verify(taskRepository, times(1)).findById(task.getIdentifier());
        verify(runRepository, times(0)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Successfully create a task run by namespace and version")
    @Test
    void createRunForTaskByNamespaceAndVersionShouldReturnTaskRun() throws Exception {
        when(taskRepository.findByNamespaceAndVersion(task.getNamespace(), task.getVersion())).thenReturn(task);

        TaskRun result = taskService.createRunForTask(task.getNamespace(), task.getVersion());

        assertNotNull(result);
        assertEquals(task.getIdentifier(), result.getTask().getId());
        assertEquals(task.getNamespace(), result.getTask().getNamespace());
        assertEquals(task.getVersion(), result.getTask().getVersion());
        assertEquals(task.getDescription(), result.getTask().getDescription());
        assertEquals(TaskRunState.CREATED, result.getState());
        verify(taskRepository, times(1)).findByNamespaceAndVersion(task.getNamespace(), task.getVersion());
        verify(runRepository, times(1)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Fail to create a task run by namespace and version and throw RunTaskServiceException")
    @Test
    void createRunForTaskByNamespaceAndVersionShouldThrowRunTaskServiceException() throws Exception {
        when(taskRepository.findByNamespaceAndVersion(task.getNamespace(), task.getVersion())).thenReturn(null);

        String expectedMessage = "task {" + task.getNamespace() + ":" +  task.getVersion() + "} not found to associate with this run";

        RunTaskServiceException exception = assertThrows(
            RunTaskServiceException.class,
            () -> taskService.createRunForTask(task.getNamespace(), task.getVersion())
        );
        assertEquals(expectedMessage, exception.getMessage());
        verify(taskRepository, times(1)).findByNamespaceAndVersion(task.getNamespace(), task.getVersion());
        verify(runRepository, times(0)).saveAndFlush(any(Run.class));
    }
}
