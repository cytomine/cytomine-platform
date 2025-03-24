package be.cytomine.appengine.unit.services;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TypePersistenceRepository;
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

    @Mock
    private TypePersistenceRepository typePersistenceRepository;

    @InjectMocks
    private TaskProvisioningService taskProvisioningService;

    private ApplicationContext applicationContext;

    private IntegerPersistenceRepository integerPersistenceRepository;

    private Run run;

    @BeforeEach
    public void setUp() throws Exception {
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
        try (MockedStatic<AppEngineApplicationContext> mockedContext = Mockito.mockStatic(AppEngineApplicationContext.class)) {
            mockedContext.when(() -> AppEngineApplicationContext.getBean(IntegerPersistenceRepository.class)).thenReturn(integerPersistenceRepository);
        }

        JsonNode response = taskProvisioningService.provisionRunParameter(run.getId().toString(), name, value);

        Assertions.assertNotNull(response);
        Mockito.verify(runRepository).findById(run.getId());
        Mockito.verify(storageHandler).saveStorageData(any(Storage.class), any(StorageData.class));
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
