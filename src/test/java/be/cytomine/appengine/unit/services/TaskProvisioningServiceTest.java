package be.cytomine.appengine.unit.services;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.exceptions.ProvisioningException;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.Input;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.file.FilePersistenceRepository;
import be.cytomine.appengine.repositories.integer.IntegerPersistenceRepository;
import be.cytomine.appengine.services.TaskProvisioningService;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import be.cytomine.appengine.utils.TaskUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class TaskProvisioningServiceTest {

    @Mock
    private AppEngineApplicationContext appEngineApplicationContext;

    @Mock
    private SchedulerHandler schedulerHandler;

    @Mock
    private StorageHandler storageHandler;

    @Mock
    private RunRepository runRepository;

    @InjectMocks
    private TaskProvisioningService taskProvisioningService;

    private static ApplicationContext applicationContext;

    private static FilePersistenceRepository filePersistenceRepository;

    private static IntegerPersistenceRepository integerPersistenceRepository;

    private static Run run;

    @BeforeAll
    public static void setUp() {
        run = TaskUtils.createTestRun(false);
        applicationContext = mock(ApplicationContext.class);

        AppEngineApplicationContext appEngineContext = new AppEngineApplicationContext();
        appEngineContext.setApplicationContext(applicationContext);

        filePersistenceRepository = mock(FilePersistenceRepository.class);
        integerPersistenceRepository = mock(IntegerPersistenceRepository.class);
        Mockito.when(applicationContext.getBean(FilePersistenceRepository.class)).thenReturn(filePersistenceRepository);
        Mockito.when(applicationContext.getBean(IntegerPersistenceRepository.class)).thenReturn(integerPersistenceRepository);
    }

    @DisplayName("Successfully provision a run parameter with json data")
    @Test
    public void provisionRunParameterWithJsonShouldReturnJsonNode() throws Exception {
        String name = run.getTask().getInputs().iterator().next().getName();
        ObjectNode value = new ObjectMapper().createObjectNode();
        value.put("param_name", name);
        value.put("value", 42);

        Mockito.when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));

        JsonNode result = taskProvisioningService.provisionRunParameter(run.getId().toString(), name, value);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(value.get("param_name"), result.get("param_name"));
        Assertions.assertEquals(value.get("value"), result.get("value"));
        Assertions.assertEquals(run.getId().toString(), result.get("task_run_id").asText());
        Mockito.verify(runRepository, Mockito.times(1)).findById(run.getId());
        Mockito.verify(storageHandler, Mockito.times(1)).saveStorageData(any(Storage.class), any(StorageData.class));
    }

    @DisplayName("Successfully provision a run parameter with binary data")
    @Test
    public void provisionRunParameterWithFileShouldReturnJsonNode() throws Exception {
        Run localRun = TaskUtils.createTestRun(true);
        String name = localRun.getTask().getInputs().iterator().next().getName();
        File value = File.createTempFile("input", null);
        value.deleteOnExit();

        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        JsonNode result = taskProvisioningService.provisionRunParameter(localRun.getId().toString(), name, value);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(name, result.get("param_name").asText());
        Assertions.assertEquals(localRun.getId().toString(), result.get("task_run_id").asText());
        Mockito.verify(runRepository, Mockito.times(1)).findById(localRun.getId());
        Mockito.verify(storageHandler, Mockito.times(1)).saveStorageData(any(Storage.class), any(StorageData.class));
    }

    @DisplayName("Failed to provision a run parameter and throw 'ProvisioningException'")
    @Test
    public void provisionRunParameterShouldThrowProvisioningException() throws Exception {
        String name = run.getTask().getInputs().iterator().next().getName();
        ObjectNode value = new ObjectMapper().createObjectNode().put("unwanted", "value");

        Mockito.when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));

        ProvisioningException exception = Assertions.assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.provisionRunParameter(run.getId().toString(), name, value)
        );
        Assertions.assertEquals("unable to process json", exception.getMessage());
        Mockito.verify(runRepository, Mockito.times(1)).findById(run.getId());
        Mockito.verify(storageHandler, Mockito.times(0)).saveStorageData(any(Storage.class), any(StorageData.class));
    }

    @DisplayName("Successfully provision multiple run parameters")
    @Test
    public void provisionMultipleRunParametersShouldReturnListJsonNode() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setTask(TaskUtils.createTestTaskWithMultipleInputs());

        List<JsonNode> values = new ArrayList<>();
        for (Input input : localRun.getTask().getInputs()) {
            ObjectNode value = new ObjectMapper().createObjectNode();
            value.put("param_name", input.getName());
            value.put("value", new Random().nextInt(100));
            values.add(value);
        }

        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        List<JsonNode> result = taskProvisioningService.provisionMultipleRunParameters(localRun.getId().toString(), values);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(values.size(), result.size());
        Mockito.verify(runRepository, Mockito.times(1)).findById(localRun.getId());
        Mockito.verify(storageHandler, Mockito.times(2)).saveStorageData(any(Storage.class), any(StorageData.class));
    }

    @DisplayName("Failed to provision multiple run parameters")
    @Test
    public void provisionMultipleRunParametersShouldThrowProvisioningException() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setTask(TaskUtils.createTestTaskWithMultipleInputs());

        List<JsonNode> values = new ArrayList<>();
        for (Input input : localRun.getTask().getInputs()) {
            ObjectNode value = new ObjectMapper().createObjectNode();
            value.put("param_name", input.getName());
            value.put("value", new Random().nextInt(100));
            value.put("unwanted", "value");
            values.add(value);
        }

        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = Assertions.assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.provisionMultipleRunParameters(localRun.getId().toString(), values)
        );
        Assertions.assertEquals("Error(s) occurred during a handling of a batch request.", exception.getMessage());
        Mockito.verify(runRepository, Mockito.times(1)).findById(localRun.getId());
        Mockito.verify(storageHandler, Mockito.times(0)).saveStorageData(any(Storage.class), any(StorageData.class));
    }
}
