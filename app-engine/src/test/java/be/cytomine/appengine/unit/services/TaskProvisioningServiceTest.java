package be.cytomine.appengine.unit.services;

import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.repositories.ChecksumRepository;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
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
import be.cytomine.appengine.models.task.integer.IntegerPersistence;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TypePersistenceRepository;
import be.cytomine.appengine.repositories.file.FilePersistenceRepository;
import be.cytomine.appengine.repositories.integer.IntegerPersistenceRepository;
import be.cytomine.appengine.services.TaskProvisioningService;
import be.cytomine.appengine.services.TaskService;
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
    private ChecksumRepository checksumRepository;

    @Mock
    private TypePersistenceRepository typePersistenceRepository;

    @Mock
    private TaskService taskService;

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
        String name = run.getTask().getParameters().stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT)).iterator().next().getName();
        ObjectNode value = new ObjectMapper().createObjectNode();
        value.put("param_name", name);
        value.put("value", 42);

        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));

        Checksum crc32 = new Checksum(UUID.randomUUID(), "task-run-inputs-" + run.getId(), 938453439);
        when(checksumRepository.save(any(Checksum.class))).thenReturn(crc32);

        JsonNode result = taskProvisioningService.provisionRunParameter(run.getId().toString(), name, value);

        assertNotNull(result);
        assertEquals(value.get("param_name"), result.get("param_name"));
        assertEquals(value.get("value"), result.get("value"));
        assertEquals(run.getId().toString(), result.get("task_run_id").asText());
        verify(runRepository, times(1)).findById(run.getId());
        verify(storageHandler, times(1)).saveStorageData(any(Storage.class), any(StorageData.class));
    }

    private long calculateFileCRC32(File file) throws IOException {
        java.util.zip.Checksum crc32 = new CRC32();
        // Use try-with-resources to ensure the input stream is closed automatically
        // BufferedInputStream is used for efficient reading in chunks
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192]; // Define a buffer size (e.g., 8KB)
            int bytesRead;

            // Read the file chunk by chunk and update the CRC32 checksum
            while ((bytesRead = is.read(buffer)) != -1) {
                crc32.update(buffer, 0, bytesRead);
            }
        }
        return crc32.getValue(); // Return the final CRC32 value
    }

    @DisplayName("Successfully provision a run parameter with binary data")
    @Test
    public void provisionRunParameterWithFileShouldReturnJsonNode() throws Exception {
        Run localRun = TaskUtils.createTestRun(true);
        String name = localRun.getTask().getParameters().stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT)).iterator().next().getName();
        File value = File.createTempFile("input", null);
        value.deleteOnExit();

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));

        JsonNode result = taskProvisioningService.provisionRunParameter(localRun.getId().toString(), name, value);

        assertNotNull(result);
        assertEquals(name, result.get("param_name").asText());
        assertEquals(localRun.getId().toString(), result.get("task_run_id").asText());
        verify(runRepository, times(1)).findById(localRun.getId());
    }

    @DisplayName("Failed to provision a run parameter and throw 'ProvisioningException'")
    @Test
    public void provisionRunParameterShouldThrowProvisioningException() throws Exception {
        String name = run.getTask().getParameters().stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT)).iterator().next().getName();
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
        run.setTask(TaskUtils.createTestTaskWithMultipleIO());

        List<JsonNode> values = new ArrayList<>();
        for (Parameter input : run.getTask().getParameters().stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT)).toList()) {
            ObjectNode value = new ObjectMapper().createObjectNode();
            value.put("param_name", input.getName());
            value.put("value", new Random().nextInt(100));
            values.add(value);
        }

        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));

        List<JsonNode> result = taskProvisioningService.provisionMultipleRunParameters(run.getId().toString(), values);

        assertNotNull(result);
        assertEquals(values.size(), result.size());
        verify(runRepository, times(1)).findById(run.getId());
        verify(storageHandler, times(2)).saveStorageData(any(Storage.class), any(StorageData.class));
    }

    @DisplayName("Failed to provision multiple run parameters")
    @Test
    public void provisionMultipleRunParametersShouldThrowProvisioningException() throws Exception {
        run.setTask(TaskUtils.createTestTaskWithMultipleIO());

        List<JsonNode> values = new ArrayList<>();
        for (Parameter input : run.getTask().getParameters().stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT)).toList()) {
            ObjectNode value = new ObjectMapper().createObjectNode();
            value.put("param_name", input.getName());
            value.put("value", new Random().nextInt(100));
            value.put("unwanted", "value");
            values.add(value);
        }

        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.provisionMultipleRunParameters(run.getId().toString(), values)
        );
        assertEquals("Error(s) occurred during a handling of a batch request.", exception.getMessage());
        verify(runRepository, times(1)).findById(run.getId());
        verify(storageHandler, times(0)).saveStorageData(any(Storage.class), any(StorageData.class));
    }

    @DisplayName("Successfully retrieve a zip archive")
    @Test
    public void retrieveIOZipArchiveShouldReturnStorageData() throws Exception {

        Run run = TaskUtils.createTestRun(false);

        String name = run.getTask().getParameters().stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT)).iterator().next().getName();
        String storageId = "inputs-archive-" + run.getId().toString();
        StorageData mockStorageData = TaskUtils.createTestStorageData(name, storageId);

        run.setState(TaskRunState.PROVISIONED);

        IntegerPersistence persistedProvision = new IntegerPersistence();
        persistedProvision.setValueType(ValueType.INTEGER);
        persistedProvision.setParameterType(ParameterType.INPUT);
        persistedProvision.setParameterName("name");
        persistedProvision.setRunId(run.getId());
        persistedProvision.setValue(42);
        persistedProvision.setProvisioned(true);

        when(storageHandler.readStorageData(any(StorageData.class))).thenReturn(mockStorageData);
        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));
        when(typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(run.getId(), ParameterType.INPUT))
            .thenReturn(List.of(persistedProvision));

        Checksum crc32 = new Checksum(UUID.randomUUID(), storageId, calculateFileCRC32(mockStorageData.peek().getData()));
        when(checksumRepository.findByReference(any(String.class))).thenReturn(crc32);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        taskProvisioningService.retrieveIOZipArchive(run.getId().toString(), ParameterType.INPUT,out);

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

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.retrieveIOZipArchive(localRun.getId().toString(), ParameterType.INPUT,
                out)
        );
        assertEquals("run is in invalid state", exception.getMessage());
    }

    @DisplayName("Failed to retrieve a zip archive and throw 'ProvisioningException' when provisions are empty")
    @Test
    public void retrieveIOZipArchiveShouldThrowProvisioningExceptionWhenEmptyProvisions() throws Exception {
        Run run = TaskUtils.createTestRun(false);
        run.setState(TaskRunState.PROVISIONED);

        when(runRepository.findById(run.getId())).thenReturn(Optional.of(run));
        when(typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(run.getId(), ParameterType.INPUT))
            .thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.retrieveIOZipArchive(run.getId().toString(), ParameterType.INPUT,
                out)
        );
        assertEquals("provisions not found", exception.getMessage());
    }

    @DisplayName("Successfully save the outputs archive")
    @Test
    public void postOutputsZipArchiveShouldReturnParameters() throws Exception {
        Task task = TaskUtils.createTestTask(false);
        task.setMatches(new ArrayList<>());
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setTask(task);
        localRun.setState(TaskRunState.RUNNING);

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));
        taskProvisioningService.setBasePath("/tmp/appengine/storage");

        List<TaskRunParameterValue> results = taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(),
            new ByteArrayInputStream(TaskUtils.createFakeOutputsZip("out")));

        assertEquals(localRun.getTask().getParameters().stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT)).toList().size(), results.size());
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

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));
        taskProvisioningService.setBasePath("/tmp/appengine/storage");

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(),
                new ByteArrayInputStream(TaskUtils.createFakeOutputsZip("out", "invalid")))
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

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));
        taskProvisioningService.setBasePath("/tmp/appengine/storage");

        ProvisioningException exception = assertThrows(
            ProvisioningException.class,
            () -> taskProvisioningService.postOutputsZipArchive(localRun.getId().toString(), localRun.getSecret(),
                new ByteArrayInputStream(TaskUtils.createFakeOutputsZip("out 1")))
        );
        assertEquals("some outputs are missing in the archive", exception.getMessage());
        verify(runRepository, times(1)).findById(localRun.getId());
    }

    @DisplayName("Successfully retrieve the outputs")
    @Test
    public void retrieveRunOutputsShouldReturnOutputs() throws Exception {
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setState(TaskRunState.FINISHED);
        IntegerPersistence output = new IntegerPersistence(42);
        output.setParameterName(localRun.getTask().getParameters().stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.OUTPUT)).iterator().next().getName());

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));
        when(typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(localRun.getId(), ParameterType.OUTPUT))
            .thenReturn(List.of(output));

        List<TaskRunParameterValue> results =  taskProvisioningService.retrieveRunOutputs(localRun.getId().toString());

        assertEquals(localRun.getTask().getParameters().stream().filter(parameter -> parameter.getParameterType().equals(ParameterType.OUTPUT)).toList().size(), results.size());
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
        input.setParameterName(localRun
            .getTask()
            .getParameters()
            .stream()
                .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .iterator()
            .next()
            .getName());

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));
        when(typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(localRun.getId(), ParameterType.INPUT))
            .thenReturn(List.of(input));

        List<TaskRunParameterValue> results =  taskProvisioningService.retrieveRunInputs(localRun.getId().toString());

        assertEquals(localRun
            .getTask()
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .toList()
            .size(), results.size());
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
    String parameterName =
        run.getTask().getParameters().stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .iterator()
            .next()
            .getName();
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
        String parameterName = run
            .getTask()
            .getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(ParameterType.INPUT))
            .iterator()
            .next()
            .getName();

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

        IntegerPersistence persistedProvision = new IntegerPersistence();
        persistedProvision.setValueType(ValueType.INTEGER);
        persistedProvision.setParameterType(ParameterType.INPUT);
        persistedProvision.setParameterName("name");
        persistedProvision.setRunId(localRun.getId());
        persistedProvision.setValue(100);
        persistedProvision.setProvisioned(true);

        List<TypePersistence> persistenceList = List.of(persistedProvision);

        when(runRepository.findById(localRun.getId())).thenReturn(Optional.of(localRun));
        when(typePersistenceRepository
            .findTypePersistenceByRunIdAndParameterTypeAndParameterNameIn(
                localRun.getId(),
                ParameterType.INPUT,
                List.of("name")))
            .thenReturn(persistenceList);

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
        Task localTask = TaskUtils.createTestTask(false);
        localTask.setMatches(new ArrayList<>());
        Run localRun = TaskUtils.createTestRun(false);
        localRun.setTask(localTask);
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
