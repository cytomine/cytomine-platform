package be.cytomine.appengine.unit.services;

import java.util.Optional;

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
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.repositories.RunRepository;
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

    private static IntegerPersistenceRepository integerPersistenceRepository;

    private static Run run;

    @BeforeAll
    public static void setUp() {
        run = TaskUtils.createTestRun();
        applicationContext = mock(ApplicationContext.class);

        AppEngineApplicationContext appEngineContext = new AppEngineApplicationContext();
        appEngineContext.setApplicationContext(applicationContext);

        integerPersistenceRepository = mock(IntegerPersistenceRepository.class);
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

    // @DisplayName("Successfully provision a run parameter with binary data")
    // @Test
    // public void provisionRunParameterWithFileShouldReturnJsonNode() {

    // }

    // @DisplayName("Failed to provision a run parameter and throw 'ProvisioningException'")
    // @Test
    // public void provisionRunParameterShouldThrowProvisioningException() {

    // }
}
