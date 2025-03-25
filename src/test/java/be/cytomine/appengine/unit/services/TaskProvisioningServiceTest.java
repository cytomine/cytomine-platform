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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.inputs.task.State;
import be.cytomine.appengine.dto.inputs.task.StateAction;
import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.TaskRunResponse;
import be.cytomine.appengine.exceptions.FileStorageException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        applicationContext = mock(ApplicationContext.class);

        AppEngineApplicationContext appEngineContext = new AppEngineApplicationContext();
        appEngineContext.setApplicationContext(applicationContext);

        filePersistenceRepository = mock(FilePersistenceRepository.class);
        integerPersistenceRepository = mock(IntegerPersistenceRepository.class);
        when(applicationContext.getBean(FilePersistenceRepository.class)).thenReturn(filePersistenceRepository);
        when(applicationContext.getBean(IntegerPersistenceRepository.class)).thenReturn(integerPersistenceRepository);
    }

    @DisplayName("Successfully provision a run parameter with json data")
    @Test
    public void provisionRunParameterWithJsonShouldReturnJsonNode() throws Exception {
        String name = run.getTask().getInputs().iterator().next().getName();
        ObjectNode value = new ObjectMapper().createObjectNode();
        value.put("param_name", name);
        value.put("value", 42);

        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));

        JsonNode result = taskProvisioningService.provisionRunParameter(run.getId().toString(), name, value);

        assertNotNull(result);
        assertEquals(value.get("param_name"), result.get("param_name"));
        assertEquals(value.get("value"), result.get("value"));
        assertEquals(run.getId().toString(), result.get("task_run_id").asText());
        verify(runRepository, times(1)).findById(run.getId());
        verify(storageHandler, times(1)).saveStorageData(any(Storage.class), any(StorageData.class));
    }

    @DisplayName("Successfully provision a run parameter with binary data")
    @Test
    public void provisionRunParameterWithFileShouldReturnJsonNode() throws Exception {
        Run localRun = TaskUtils.createTestRun(true);
        String name = localRun.getTask().getInputs().iterator().next().getName();
        File value = File.createTempFile("input", null);
        value.deleteOnExit();

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        JsonNode result = taskProvisioningService.provisionRunParameter(localRun.getId().toString(), name, value);

        assertNotNull(result);
        assertEquals(name, result.get("param_name").asText());
        assertEquals(localRun.getId().toString(), result.get("task_run_id").asText());
        verify(runRepository, times(1)).findById(localRun.getId());
        verify(storageHandler, times(1)).saveStorageData(any(Storage.class), any(StorageData.class));
    }

    @DisplayName("Failed to provision a run parameter and throw 'ProvisioningException'")
    @Test
    public void provisionRunParameterShouldThrowProvisioningException() throws Exception {
        String name = run.getTask().getInputs().iterator().next().getName();
        ObjectNode value = new ObjectMapper().createObjectNode().put("unwanted", "value");

        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.provisionRunParameter(run.getId().toString(), name, value)
        );
        assertEquals("unable to process json", exception.getMessage());
        verify(runRepository, times(1)).findById(run.getId());
        verify(storageHandler, times(0)).saveStorageData(any(Storage.class), any(StorageData.class));
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

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        List<JsonNode> result = taskProvisioningService.provisionMultipleRunParameters(localRun.getId().toString(), values);

        assertNotNull(result);
        assertEquals(values.size(), result.size());
        verify(runRepository, times(1)).findById(localRun.getId());
        verify(storageHandler, times(2)).saveStorageData(any(Storage.class), any(StorageData.class));
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

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.provisionMultipleRunParameters(localRun.getId().toString(), values)
        );
        assertEquals("Error(s) occurred during a handling of a batch request.", exception.getMessage());
        verify(runRepository, times(1)).findById(localRun.getId());
        verify(storageHandler, times(0)).saveStorageData(any(Storage.class), any(StorageData.class));
    }

    @DisplayName("Successfully retrieve a zip archive")
    @Test
    public void retrieveIOZipArchiveShouldReturnStorageData() throws Exception {
        String name = run.getTask().getInputs().iterator().next().getName();
        String storageId = "inputs-archive-" + run.getId().toString();
        StorageData mockStorageData = TaskUtils.createTestStorageData(name, storageId);

        when(storageHandler.readStorageData(any(StorageData.class))).thenReturn(mockStorageData);
        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));
        when(typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(run.getId(), ParameterType.INPUT))
            .thenReturn(List.of(new IntegerPersistence(42)));

        StorageData result = taskProvisioningService.retrieveIOZipArchive(run.getId().toString(), ParameterType.INPUT);
    
        assertEquals(mockStorageData.getEntryList().size(), result.getEntryList().size());
        assertTrue(result.peek().getData().getName().matches("inputs-archive-\\d*" + run.getId()));
        verify(runRepository, times(1)).findById(run.getId());
        verify(storageHandler, times(1)).readStorageData(any(StorageData.class));
        verify(typePersistenceRepository, times(1)).findTypePersistenceByRunIdAndParameterType(run.getId(), ParameterType.INPUT);
    }

    @DisplayName("Failed to retrieve a zip archive and throw 'ProvisioningException' when run state is invalid")
    @Test
    public void retrieveIOZipArchiveShouldThrowProvisioningExceptionWhenInvalidRunState() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.CREATED);

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.retrieveIOZipArchive(localRun.getId().toString(), ParameterType.INPUT)
        );
        assertEquals("run is in invalid state", exception.getMessage());
    }

    @DisplayName("Failed to retrieve a zip archive and throw 'ProvisioningException' when provisions are empty")
    @Test
    public void retrieveIOZipArchiveShouldThrowProvisioningExceptionWhenEmptyProvisions() throws Exception {
        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));
        when(typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(run.getId(), ParameterType.INPUT))
            .thenReturn(List.of());

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.retrieveIOZipArchive(run.getId().toString(), ParameterType.INPUT)
        );
        assertEquals("provisions not found", exception.getMessage());
    }

    @DisplayName("Successfully save the outputs archive")
    @Test
    public void postOutputsZipArchiveShouldReturnParameters() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.RUNNING);
        MultipartFile outputs = mock(MultipartFile.class);

        when(outputs.getInputStream()).thenReturn(new ByteArrayInputStream(TaskUtils.createFakeOutputsZip("out")));
        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        List<TaskRunParameterValue> results = taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(), outputs);

        assertEquals(localRun.getTask().getOutputs().size(), results.size());
        verify(runRepository, times(1)).findById(localRun.getId());
        verify(runRepository, times(1)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Failed to save the outputs archive and throw 'ProvisioningException' when not authenticated")
    @Test
    public void postOutputsZipArchiveShouldThrowProvisioningExceptionWhenNotAuth() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.RUNNING);

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), run.getSecret(), null)
        );
        assertEquals("unauthenticated task failed to provision outputs for this run", exception.getMessage());
        verify(runRepository, times(1)).findById(localRun.getId());
    }

    @DisplayName("Failed to save the outputs archive and throw 'ProvisioningException' when not in correct state")
    @Test
    public void postOutputsZipArchiveShouldThrowProvisioningExceptionWhenNotCorrectState() {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.CREATED);

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(), null)
        );
        assertEquals("run is in invalid state", exception.getMessage());
        verify(runRepository, times(1)).findById(localRun.getId());
    }

    @DisplayName("Failed to save the outputs archive and throw 'ProvisioningException' when not in correct output")
    @Test
    public void postOutputsZipArchiveShouldThrowProvisioningExceptionWhenNotCorrectOutput() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.RUNNING);
        MultipartFile outputs = mock(MultipartFile.class);

        when(outputs.getInputStream()).thenReturn(new ByteArrayInputStream(TaskUtils.createFakeOutputsZip("out", "invalid")));
        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(), outputs)
        );
        assertEquals("unexpected output, did not match an actual task output", exception.getMessage());
        verify(runRepository, times(1)).findById(localRun.getId());
    }

    @DisplayName("Failed to save the outputs archive and throw 'ProvisioningException' when missing output")
    @Test
    public void postOutputsZipArchiveShouldThrowProvisioningExceptionWhenMissingOutput() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.RUNNING);
        localRun.setTask(TaskUtils.createTestTaskWithMultipleIO());
        MultipartFile outputs = mock(MultipartFile.class);

        when(outputs.getInputStream()).thenReturn(new ByteArrayInputStream(TaskUtils.createFakeOutputsZip("out 1")));
        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(), outputs)
        );
        assertEquals("some outputs are missing in the archive", exception.getMessage());
        verify(runRepository, times(1)).findById(localRun.getId());
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

    @DisplayName("Successfully retrieve the outputs")
    @Test
    public void retrieveRunOutputsShouldReturnOutputs() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.FINISHED);
        IntegerPersistence output = new IntegerPersistence(42);
        output.setParameterName(localRun.getTask().getOutputs().iterator().next().getName());

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));
        when(typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(localRun.getId(), ParameterType.OUTPUT))
            .thenReturn(List.of(output));

        List<TaskRunParameterValue> results =  taskProvisioningService.retrieveRunOutputs(localRun.getId().toString());

        assertEquals(localRun.getTask().getOutputs().size(), results.size());
        verify(runRepository, times(1)).findById(localRun.getId());
        verify(typePersistenceRepository, times(1)).findTypePersistenceByRunIdAndParameterType(localRun.getId(), ParameterType.OUTPUT);
    }

    @DisplayName("Failed to retrieve the outputs and throw 'ProvisioningException'")
    @Test
    public void retrieveRunOutputsShouldThrowProvisioningException() throws Exception {
        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.retrieveRunOutputs(run.getId().toString())
        );
        assertEquals("run is in invalid state", exception.getMessage());
        verify(runRepository, times(1)).findById(run.getId());
    }

    @DisplayName("Successfully retrieve the input")
    @Test
    public void retrieveRunInputsShouldReturnInputs() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.FINISHED);
        IntegerPersistence input = new IntegerPersistence(42);
        input.setParameterName(localRun.getTask().getInputs().iterator().next().getName());

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));
        when(typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(localRun.getId(), ParameterType.INPUT))
            .thenReturn(List.of(input));

        List<TaskRunParameterValue> results =  taskProvisioningService.retrieveRunInputs(localRun.getId().toString());

        assertEquals(localRun.getTask().getInputs().size(), results.size());
        verify(runRepository, times(1)).findById(localRun.getId());
        verify(typePersistenceRepository, times(1)).findTypePersistenceByRunIdAndParameterType(localRun.getId(), ParameterType.INPUT);
    }

    @DisplayName("Failed to retrieve the inputs and throw 'ProvisioningException'")
    @Test
    public void retrieveRunInputsShouldThrowProvisioningException() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.CREATED);

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.retrieveRunInputs(localRun.getId().toString())
        );
        assertEquals("run is in invalid state", exception.getMessage());
        verify(runRepository, times(1)).findById(localRun.getId());
    }

    @DisplayName("Successfully retrieve a single IO")
    @Test
    public void retrieveSingleRunIOShouldReturnSingleIO() throws Exception {
        String storageId = "task-run-inputs-" + run.getId();
        String parameterName = run.getTask().getInputs().iterator().next().getName();
        IntegerPersistence input = new IntegerPersistence(42);
        input.setParameterName(parameterName);
        StorageData mockStorageData = TaskUtils.createTestStorageData(parameterName, storageId);

        when(storageHandler.readStorageData(any(StorageData.class))).thenReturn(mockStorageData);

        File result =  taskProvisioningService.retrieveSingleRunIO(run.getId().toString(), input.getParameterName(), ParameterType.INPUT);

        assertEquals(mockStorageData.peek().getData().getName(), result.getName());
        verify(storageHandler, times(1)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Failed to retrieve the single IO and throw 'ProvisioningException'")
    @Test
    public void retrieveSingleRunIOShouldThrowProvisioningException() throws Exception {
        String parameterName = run.getTask().getInputs().iterator().next().getName();

        when(storageHandler.readStorageData(any(StorageData.class))).thenThrow(new FileStorageException("failed to read file from storage service"));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.retrieveSingleRunIO(run.getId().toString(), parameterName, ParameterType.INPUT)
        );
        assertEquals("failed to read file from storage service", exception.getMessage());
        verify(storageHandler, times(1)).readStorageData(any(StorageData.class));
    }

    @DisplayName("Successfully update the run state to PROVISIONED")
    @Test
    public void updateRunStateShouldUpdateStateToProvisioned() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        State desiredState = new State();
        desiredState.setDesired(TaskRunState.PROVISIONED);

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        StateAction result = taskProvisioningService.updateRunState(localRun.getId().toString(), desiredState);

        assertEquals("success", result.getStatus());
        assertEquals(localRun.getId(), result.getResource().getId());
        assertEquals(TaskRunState.PROVISIONED, result.getResource().getState());
        verify(runRepository, times(1)).findById(localRun.getId());
        verify(runRepository, times(1)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Successfully update the run state to RUNNING")
    @Test
    public void updateRunStateShouldUpdateStateToRunning() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.PROVISIONED);
        State desiredState = new State();
        desiredState.setDesired(TaskRunState.RUNNING);

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        StateAction result = taskProvisioningService.updateRunState(localRun.getId().toString(), desiredState);

        assertEquals("success", result.getStatus());
        assertEquals(localRun.getId(), result.getResource().getId());
        assertEquals(TaskRunState.QUEUING, result.getResource().getState());
        verify(runRepository, times(1)).findById(localRun.getId());
        verify(runRepository, times(1)).saveAndFlush(any(Run.class));
    }

    @DisplayName("Failed to update the run state and throw 'ProvisioningException'")
    @Test
    public void updateRunStateShouldThrowProvisioningException() {
        Run localRun = TaskUtils.createTestRun(false);
        State desiredState = new State();
        desiredState.setDesired(TaskRunState.PENDING);

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.updateRunState(localRun.getId().toString(), desiredState)
        );
        assertEquals("unknown state in transition request", exception.getMessage());
        verify(runRepository, times(1)).findById(localRun.getId());
    }

    @DisplayName("Successfully retrieve a task run")
    @Test
    public void retrieveRunShouldReturnCorrectRun() throws Exception {
        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));

        TaskRunResponse result = taskProvisioningService.retrieveRun(run.getId().toString());

        assertEquals(result.getId(), run.getId());
        assertEquals(result.getState(), run.getState());
    }

    @DisplayName("Failed to retrieve a task run and throw 'ProvisioningException'")
    @Test
    public void retrieveRunShouldThrowProvisioningException() throws ProvisioningException {
        when(runRepository.findById(run.getId())).thenReturn(Optional.empty());

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.retrieveRun(run.getId().toString())
        );
        assertEquals("Run not found.", exception.getMessage());
        verify(runRepository, times(1)).findById(run.getId());
    }
}
