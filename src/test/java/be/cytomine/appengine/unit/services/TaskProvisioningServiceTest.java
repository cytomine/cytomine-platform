package be.cytomine.appengine.unit.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.multipart.MultipartFile;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.inputs.task.State;
import be.cytomine.appengine.dto.inputs.task.StateAction;
import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.exceptions.ProvisioningException;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageHandler;
import be.cytomine.appengine.models.task.Input;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.integer.IntegerPersistence;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TypePersistenceRepository;
import be.cytomine.appengine.repositories.file.FilePersistenceRepository;
import be.cytomine.appengine.repositories.integer.IntegerPersistenceRepository;
import be.cytomine.appengine.services.TaskProvisioningService;
import be.cytomine.appengine.states.TaskRunState;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import be.cytomine.appengine.utils.TaskUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    private static ApplicationContext applicationContext;

    private static FilePersistenceRepository filePersistenceRepository;

    private static IntegerPersistenceRepository integerPersistenceRepository;

    private static Run run;

    @BeforeAll
    public static void setUp() {
        run = TaskUtils.createTestRun(false);
        applicationContext = Mockito.mock(ApplicationContext.class);

        AppEngineApplicationContext appEngineContext = new AppEngineApplicationContext();
        appEngineContext.setApplicationContext(applicationContext);

        filePersistenceRepository = Mockito.mock(FilePersistenceRepository.class);
        integerPersistenceRepository = Mockito.mock(IntegerPersistenceRepository.class);
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
        localRun.setTask(TaskUtils.createTestTaskWithMultipleIO());

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
        localRun.setTask(TaskUtils.createTestTaskWithMultipleIO());

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

    @DisplayName("Successfully retrieve a zip archive")
    @Test
    public void retrieveIOZipArchiveShouldReturnStorageData() throws Exception {
        String name = run.getTask().getInputs().iterator().next().getName();
        String storageId = "inputs-archive-" + run.getId().toString();
        StorageData mockStorageData = TaskUtils.createTestStorageData(name, storageId);

        Mockito.when(storageHandler.readStorageData(any(StorageData.class))).thenReturn(mockStorageData);
        Mockito.when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));
        Mockito.when(typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(run.getId(), ParameterType.INPUT))
            .thenReturn(List.of(new IntegerPersistence(42)));

        StorageData result = taskProvisioningService.retrieveIOZipArchive(run.getId().toString(), ParameterType.INPUT);
    
        Assertions.assertEquals(mockStorageData.getEntryList().size(), result.getEntryList().size());
        Assertions.assertTrue(result.peek().getData().getName().matches("inputs-archive-\\d*" + run.getId()));
        Mockito.verify(runRepository, Mockito.times(1)).findById(run.getId());
        Mockito.verify(storageHandler, Mockito.times(1)).readStorageData(any(StorageData.class));
        Mockito.verify(typePersistenceRepository, Mockito.times(1)).findTypePersistenceByRunIdAndParameterType(run.getId(), ParameterType.INPUT);
    }

    @DisplayName("Failed to retrieve a zip archive and throw 'ProvisioningException' when run state is invalid")
    @Test
    public void retrieveIOZipArchiveShouldThrowProvisioningExceptionWhenInvalidRunState() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.CREATED);

        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = Assertions.assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.retrieveIOZipArchive(localRun.getId().toString(), ParameterType.INPUT)
        );
        Assertions.assertEquals("run is in invalid state", exception.getMessage());
    }

    @DisplayName("Failed to retrieve a zip archive and throw 'ProvisioningException' when provisions are empty")
    @Test
    public void retrieveIOZipArchiveShouldThrowProvisioningExceptionWhenEmptyProvisions() throws Exception {
        Mockito.when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));
        Mockito.when(typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(run.getId(), ParameterType.INPUT))
            .thenReturn(List.of());

        ProvisioningException exception = Assertions.assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.retrieveIOZipArchive(run.getId().toString(), ParameterType.INPUT)
        );
        Assertions.assertEquals("provisions not found", exception.getMessage());
    }

    @DisplayName("Successfully save the outputs archive")
    @Test
    public void postOutputsZipArchiveShouldReturnParameters() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.RUNNING);
        MultipartFile outputs = Mockito.mock(MultipartFile.class);

        Mockito.when(outputs.getInputStream()).thenReturn(new ByteArrayInputStream(TaskUtils.createFakeOutputsZip("out")));
        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        List<TaskRunParameterValue> results = taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(), outputs);

        Assertions.assertEquals(localRun.getTask().getOutputs().size(), results.size());
        Mockito.verify(runRepository, Mockito.times(1)).findById(localRun.getId());
        Mockito.verify(runRepository, Mockito.times(1)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Failed to save the outputs archive and throw 'ProvisioningException' when not authenticated")
    @Test
    public void postOutputsZipArchiveShouldThrowProvisioningExceptionWhenNotAuth() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.RUNNING);

        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = Assertions.assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), run.getSecret(), null)
        );
        Assertions.assertEquals("unauthenticated task failed to provision outputs for this run", exception.getMessage());
        Mockito.verify(runRepository, Mockito.times(1)).findById(localRun.getId());
    }

    @DisplayName("Failed to save the outputs archive and throw 'ProvisioningException' when not in correct state")
    @Test
    public void postOutputsZipArchiveShouldThrowProvisioningExceptionWhenNotCorrectState() {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.CREATED);

        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = Assertions.assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(), null)
        );
        Assertions.assertEquals("run is in invalid state", exception.getMessage());
        Mockito.verify(runRepository, Mockito.times(1)).findById(localRun.getId());
    }

    @DisplayName("Failed to save the outputs archive and throw 'ProvisioningException' when not in correct output")
    @Test
    public void postOutputsZipArchiveShouldThrowProvisioningExceptionWhenNotCorrectOutput() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.RUNNING);
        MultipartFile outputs = Mockito.mock(MultipartFile.class);

        Mockito.when(outputs.getInputStream()).thenReturn(new ByteArrayInputStream(TaskUtils.createFakeOutputsZip("out", "invalid")));
        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = Assertions.assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(), outputs)
        );
        Assertions.assertEquals("unexpected output, did not match an actual task output", exception.getMessage());
        Mockito.verify(runRepository, Mockito.times(1)).findById(localRun.getId());
    }

    @DisplayName("Failed to save the outputs archive and throw 'ProvisioningException' when missing output")
    @Test
    public void postOutputsZipArchiveShouldThrowProvisioningExceptionWhenMissingOutput() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.RUNNING);
        localRun.setTask(TaskUtils.createTestTaskWithMultipleIO());
        MultipartFile outputs = Mockito.mock(MultipartFile.class);

        Mockito.when(outputs.getInputStream()).thenReturn(new ByteArrayInputStream(TaskUtils.createFakeOutputsZip("out 1")));
        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = Assertions.assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(), outputs)
        );
        Assertions.assertEquals("some outputs are missing in the archive", exception.getMessage());
        Mockito.verify(runRepository, Mockito.times(1)).findById(localRun.getId());
    }

    @DisplayName("Successfully get the storage charset")
    @Test
    public void getStorageCharsetShouldReturnCorrectCharset() {
        assertEquals(StandardCharsets.US_ASCII, taskProvisioningService.getStorageCharset("US_ASCII"));
        assertEquals(StandardCharsets.ISO_8859_1, taskProvisioningService.getStorageCharset("ISO_8859_1"));
        assertEquals(StandardCharsets.UTF_16LE, taskProvisioningService.getStorageCharset("UTF_16LE"));
        assertEquals(StandardCharsets.UTF_16BE, taskProvisioningService.getStorageCharset("UTF_16BE"));
        assertEquals(StandardCharsets.UTF_16, taskProvisioningService.getStorageCharset("UTF_16"));
    }

    @DisplayName("Successfully get UTF-8 for unknown charset")
    @Test
    public void getStorageCharsetShouldReturnUTF8ByDefault() {
        assertEquals(StandardCharsets.UTF_8, taskProvisioningService.getStorageCharset("UNKNOWN_CHARSET"));
    }

    @DisplayName("Successfully get the storage charset for mixed case")
    @Test
    public void getStorageCharsetShouldReturnCorrectCharsetForMixedCase() {
        assertEquals(StandardCharsets.US_ASCII, taskProvisioningService.getStorageCharset("us_ascii"));
        assertEquals(StandardCharsets.ISO_8859_1, taskProvisioningService.getStorageCharset("iso_8859_1"));
        assertEquals(StandardCharsets.UTF_16LE, taskProvisioningService.getStorageCharset("utf_16le"));
    }

    @DisplayName("Successfully update the run state to PROVISIONED")
    @Test
    public void updateRunStateShouldUpdateStateToProvisioned() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        State desiredState = new State();
        desiredState.setDesired(TaskRunState.PROVISIONED);

        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        StateAction result = taskProvisioningService.updateRunState(localRun.getId().toString(), desiredState);

        assertEquals("success", result.getStatus());
        assertEquals(localRun.getId(), result.getResource().getId());
        assertEquals(TaskRunState.PROVISIONED, result.getResource().getState());
        Mockito.verify(runRepository, Mockito.times(1)).findById(localRun.getId());
        Mockito.verify(runRepository, Mockito.times(1)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Successfully update the run state to RUNNING")
    @Test
    public void updateRunStateShouldUpdateStateToRunning() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.PROVISIONED);
        State desiredState = new State();
        desiredState.setDesired(TaskRunState.RUNNING);

        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        StateAction result = taskProvisioningService.updateRunState(localRun.getId().toString(), desiredState);

        assertEquals("success", result.getStatus());
        assertEquals(localRun.getId(), result.getResource().getId());
        assertEquals(TaskRunState.QUEUING, result.getResource().getState());
        Mockito.verify(runRepository, Mockito.times(1)).findById(localRun.getId());
        Mockito.verify(runRepository, Mockito.times(1)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Failed to update the run state and throw 'ProvisioningException'")
    @Test
    public void updateRunStateShouldThrowProvisioningException() {
        Run localRun = TaskUtils.createTestRun(false);
        State desiredState = new State();
        desiredState.setDesired(TaskRunState.PENDING);

        Mockito.when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.updateRunState(localRun.getId().toString(), desiredState)
        );
        assertEquals("unknown state in transition request", exception.getMessage());
        verify(runRepository, times(1)).findById(localRun.getId());
    }
}
