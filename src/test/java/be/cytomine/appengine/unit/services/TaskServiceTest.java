package be.cytomine.appengine.unit.services;

import be.cytomine.appengine.dto.inputs.task.TaskDescription;
import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.exceptions.BundleArchiveException;
import be.cytomine.appengine.exceptions.TaskServiceException;
import be.cytomine.appengine.exceptions.ValidationException;
import be.cytomine.appengine.handlers.RegistryHandler;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.services.TaskService;
import be.cytomine.appengine.services.TaskValidationService;
import be.cytomine.appengine.utils.ArchiveUtils;
import be.cytomine.appengine.utils.TestTaskBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {


    @Mock
    TaskRepository taskRepository;

    @Mock
    ArchiveUtils archiveUtils;

    @Mock
    StorageHandler fileStorageHandler;

    @Mock
    RegistryHandler registryHandler;

    @Mock
    TaskValidationService taskValidationService;

    @InjectMocks
    TaskService taskService;


    @Test
    @DisplayName("Testing successful upload")
    public void succesfulUpload() throws IOException, TaskServiceException, ValidationException, BundleArchiveException {
        ClassPathResource resource = TestTaskBuilder.buildCustomImageLocationTask();
        MockMultipartFile testAppBundle = new MockMultipartFile("test_custom_image_location_task.zip", resource.getInputStream());

        String nameSpace = "namespace";
        String version = "version";
        String descriptorFile = "descriptor";
        String storageReference = "storageReference";
        Task task = new Task(UUID.randomUUID(), nameSpace, version, descriptorFile, storageReference);

        UploadTaskArchive uploadTaskArchive = new UploadTaskArchive();
        uploadTaskArchive.setDockerImage(new byte[]{});
        uploadTaskArchive.setDescriptorFile(new byte[]{});
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

        lenient().when(taskRepository.findByNamespaceAndVersion(nameSpace, version)).thenReturn(task);
        lenient().when(archiveUtils.readArchive(testAppBundle)).thenReturn(uploadTaskArchive);
        Optional<TaskDescription> result = taskService.uploadTask(testAppBundle);

        Assertions.assertTrue(result.isPresent());
    }

    // TODO : test generateTaskIdentifiers
    // TODO : test retrieveYmlDescriptor(namespace , version)
    // TODO : test retrieveYmlDescriptor(id)
    // TODO : test getAuthors
    // TODO : test getOutputs
    // TODO : test getInputs

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
}
