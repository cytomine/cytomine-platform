package be.cytomine.appengine.unit.services;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import be.cytomine.appengine.dto.inputs.task.TaskDescription;
import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.exceptions.BundleArchiveException;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.exceptions.TaskNotFoundException;
import be.cytomine.appengine.exceptions.TaskServiceException;
import be.cytomine.appengine.exceptions.ValidationException;
import be.cytomine.appengine.handlers.RegistryHandler;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.services.TaskService;
import be.cytomine.appengine.services.TaskValidationService;
import be.cytomine.appengine.utils.ArchiveUtils;
import be.cytomine.appengine.utils.TestTaskBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    private TaskRepository taskRepository;

    @Mock
    private TaskValidationService taskValidationService;

    @InjectMocks
    private TaskService taskService;

    private Task task;

    private UploadTaskArchive uploadTaskArchive;

    @BeforeEach
    public void setUp() throws Exception {
        task = new Task();
        task.setNamespace("namespace");
        task.setVersion("version");
        task.setStorageReference("storageReference");

        uploadTaskArchive = new UploadTaskArchive();
        uploadTaskArchive.setDockerImage(File.createTempFile("docker-image", ".tar"));
        uploadTaskArchive.setDescriptorFile(null);
        String descriptorYml = "name: Integers addition\n" +
                "name_short: add_int\n" +
                "version: 0.1.0\n" +
                "namespace: com.cytomine.dummy.arithmetic.integer.addition\n" +
                "$schema: https://cytomine.com/schema-store/tasks/task.v0.json\n" +
                "authors:\n" +
                "  - first_name: Romain\n" +
                "    last_name: Mormont\n" +
                "    organization: Cytomine Corporation\n" +
                "    email: romain.mormont@cytomine.com\n" +
                "    is_contact: true\n" +
                "\n" +
                "configuration:\n" +
                "  input_folder: /inputs\n" +
                "  output_folder: /outputs\n" +
                "  image:\n" +
                "        file: /image.tar \n" +
                "\n" +
                "inputs:\n" +
                "  a:\n" +
                "    display_name: A \n" +
                "    type:\n" +
                "        id: integer\n" +
                "        lt: 500\n" +
                "        gt: 200\n" +
                "    description: First operand\n" +
                "  b:\n" +
                "    display_name: B\n" +
                "    type:\n" +
                "        id: integer\n" +
                "        lt: 500\n" +
                "    description: Second operand\n" +
                "\n" +
                "outputs:\n" +
                "  out:\n" +
                "    display_name: Sum\n" +
                "    type:\n" +
                "        id: integer\n" +
                "    description: Sum of A and B";
        JsonNode descriptor = getDescriptorJsonNode(descriptorYml);
        uploadTaskArchive.setDescriptorFileAsJson(descriptor);
    }

    public static JsonNode getDescriptorJsonNode(String descriptor) throws ValidationException {
        ObjectMapper descriptorMapper = new ObjectMapper(new YAMLFactory());
        JsonNode descriptorJsonNode;
        try {
            descriptorJsonNode = descriptorMapper.readTree(descriptor.getBytes());
        } catch (IOException e) {
            throw new ValidationException("failed to read descriptor.yml");
        }
        return descriptorJsonNode;
    }

    @DisplayName("Successfully upload a task bundle")
    @Test
    public void uploadTaskShouldUploadTaskBundle() throws IOException, TaskServiceException, ValidationException, BundleArchiveException {
        ClassPathResource resource = TestTaskBuilder.buildCustomImageLocationTask();
        MockMultipartFile testAppBundle = new MockMultipartFile("test_custom_image_location_task.zip", resource.getInputStream());

        String nameSpace = "namespace";
        String version = "version";
        String descriptorFile = "descriptor";
        String storageReference = "storageReference";
        Task task = new Task(UUID.randomUUID(), nameSpace, version, descriptorFile, storageReference);

        lenient().when(taskRepository.findByNamespaceAndVersion(nameSpace, version)).thenReturn(task);
        lenient().when(archiveUtils.readArchive(testAppBundle)).thenReturn(uploadTaskArchive);
        Optional<TaskDescription> result = taskService.uploadTask(testAppBundle);

        Assertions.assertTrue(result.isPresent());
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

    @DisplayName("Fail to retrieve descriptor by namespace and version and throw TaskNotFoundException")
    @Test
    public void retrieveYmlDescriptorByNamespaceAndVersionShouldThrowTaskNotFoundException() throws Exception {
        String namespace = "namespace";
        String version = "version";
        when(taskRepository.findByNamespaceAndVersion(namespace, version)).thenReturn(null);

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, 
            () -> taskService.retrieveYmlDescriptor(namespace, version));
        assertEquals("task not found", exception.getMessage());
    }

    @DisplayName("Fail to retrieve the descriptor by namespace and version and throw a FileStorageException")
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
    }

    @DisplayName("Successfully retrieve descriptor by ID")
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

    @DisplayName("Fail to retrieve descriptor by ID and throw TaskNotFoundException")
    @Test
    public void retrieveYmlDescriptorByIdShouldThrowTaskNotFoundException() throws Exception {
        String id = "d9aad8ab-210c-48fa-8d94-6b03e8776a55";
        when(taskRepository.findById(UUID.fromString(id))).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, 
            () -> taskService.retrieveYmlDescriptor(id));
        assertEquals("task not found", exception.getMessage());
    }

    @DisplayName("Fail to retrieve descriptor by ID and throw FileStorageException")
    @Test
    public void retrieveYmlDescriptorByIdShouldThrowFileStorageException() throws Exception {
        String id = "d9aad8ab-210c-48fa-8d94-6b03e8776a55";
        when(taskRepository.findById(UUID.fromString(id))).thenReturn(Optional.of(task));
        when(storageHandler.readStorageData(any(StorageData.class)))
            .thenThrow(new FileStorageException("File error"));

        TaskServiceException exception = assertThrows(
            TaskServiceException.class,
            () -> taskService.retrieveYmlDescriptor(id)
        );
        assertTrue(exception.getCause() instanceof FileStorageException);
    }

    // TODO : test retrieveYmlDescriptor(id)
    // TODO : test getAuthors
    // TODO : test getOutputs
    // TODO : test getInputs


}
