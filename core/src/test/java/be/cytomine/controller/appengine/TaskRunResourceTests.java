package be.cytomine.controller.appengine;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.appengine.TaskRun;
import be.cytomine.repository.appengine.TaskRunRepository;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class TaskRunResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private TaskRunRepository taskRunRepository;

    @Autowired
    private MockMvc mockMvc;

    @Value("${application.appEngine.apiBasePath}")
    private String apiBasePath;

    private final static WireMockServer wireMockServer = new WireMockServer(8888);

    @BeforeAll
    public static void beforeAll() {
        wireMockServer.start();
        configureFor("localhost", 8888);
    }

    @AfterAll
    public static void afterAll() {
        wireMockServer.stop();
    }

    @Test
    @Transactional
    public void add_valid_task_run() throws Exception {
        TaskRun taskRun = builder.given_a_not_persisted_task_run();
        taskRun.setTaskRunId(UUID.randomUUID());
        String taskId = UUID.randomUUID().toString();
        String queryBody = "{\"image\": \"" + taskRun.getImage().getId() + "\"}";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> mockResponseMap = Map.of(
            "task", Map.of(
                "name", "string",
                "namespace", "string",
                "version", "string",
                "description", "string",
                "authors", List.of(Map.of(
                    "first_name", "string",
                    "last_name", "string",
                    "organization", "string",
                    "email", "string",
                    "is_contact", true
                ))
            ),
            "id", taskRun.getTaskRunId().toString(),
            "state", "created"
        );
        String mockResponse = objectMapper.writeValueAsString(mockResponseMap);

        stubFor(WireMock.post(urlEqualTo(apiBasePath + "tasks/" + taskId + "/runs"))
            .willReturn(
                aResponse().withBody(mockResponse).withHeader("Content-Type", "application/json")
            )
        );

        mockMvc.perform(post("/api/app-engine/project/" + taskRun.getProject().getId() + "/tasks/"+ taskId + "/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(queryBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(taskRun.getTaskRunId().toString()))
            .andExpect(jsonPath("$.state").value("created"))
            .andExpect(jsonPath("$.task").isNotEmpty());
    }

    @Test
    @Transactional
    public void single_param_provision_of_a_task() throws Exception {
        TaskRun taskRun = builder.given_a_not_persisted_task_run();
        taskRunRepository.saveAndFlush(taskRun);
        UUID taskRunId = taskRun.getTaskRunId();

        String paramName = "my_param";
        String queryBody = "{\"value\": 0, \"param_name\": \"" + paramName + "\", \"type\": { \"id\" : \"integer\"}}";
        String mockResponse = "{\"value\": 0, \"param_name\": \"" + paramName + "\", \"task_run_id\": \"" + taskRunId + "\"}";
        String appEngineUriSection = "task-runs/" + taskRunId + "/input-provisions/" + paramName;
        stubFor(WireMock.put(urlEqualTo(apiBasePath + appEngineUriSection))
            .willReturn(
                aResponse().withBody(mockResponse)
            )
        );

        mockMvc.perform(put("/api/app-engine/project/" + taskRun.getProject().getId() + "/" + appEngineUriSection)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody))
            .andExpect(status().isOk())
                .andExpect(jsonPath("$.task_run_id").value(taskRunId.toString()))
                .andExpect(jsonPath("$.param_name").value(paramName))
                .andExpect(jsonPath("$.value").value(0));
    }

    @Test
    @Transactional
    public void batch_param_provision_of_a_task() throws Exception {
        TaskRun taskRun = builder.given_a_not_persisted_task_run();
        taskRunRepository.saveAndFlush(taskRun);
        UUID taskRunId = taskRun.getTaskRunId();

        String paramName = "my_param";
        /*
         * The queryBody should be an array but it appears that the mock mvc class does not support haveing
         * an array at the root of the json string. Because object is just forwarded to the App Engine
         * and we mock the response to be what it should be, I will just pass a single object here to avoid the
         * error and it should have no consequence unless we start sniffing the json object in the core endpoint
         * for provisioning.
         **/
        String queryBody = "[{\"value\": 0, \"param_name\": \"" + paramName + "\", \"type\": { \"id\" : \"integer\"}}]";
        String mockResponse = "[{\"value\": 0, \"param_name\": \"" + paramName + "\", \"task_run_id\": \"" + taskRunId + "\"}]";
        String appEngineUriSection = "task-runs/" + taskRunId + "/input-provisions";
        stubFor(WireMock.put(urlEqualTo(apiBasePath + appEngineUriSection))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        mockMvc.perform(put("/api/app-engine/project/" + taskRun.getProject().getId() + "/" + appEngineUriSection)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].task_run_id").value(taskRunId.toString()))
                .andExpect(jsonPath("[0].param_name").value(paramName))
                .andExpect(jsonPath("[0].value").value(0));
    }

    @Test
    @Transactional
    public void get_task_run() throws Exception {
        TaskRun taskRun = builder.given_a_not_persisted_task_run();
        taskRunRepository.saveAndFlush(taskRun);
        UUID taskRunId = taskRun.getTaskRunId();
        String mockResponse = getTaskRunBody(taskRunId);
        String appEngineUriSection = "task-runs/" + taskRunId;
        stubFor(WireMock.get(urlEqualTo(apiBasePath + appEngineUriSection))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        mockMvc.perform(get("/api/app-engine/project/" + taskRun.getProject().getId() + "/" + appEngineUriSection)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(taskRunId.toString()))
                .andExpect(jsonPath("state").value("CREATED"))
                .andExpect(jsonPath("task").isNotEmpty());
    }

    @Test
    @Transactional
    public void post_state_action() throws Exception {
        TaskRun taskRun = builder.given_a_not_persisted_task_run();
        taskRunRepository.saveAndFlush(taskRun);
        UUID taskRunId = taskRun.getTaskRunId();
        String queryBody = "{\"desired\": \"running\"}";
        String mockResponse = getTaskRunBody(taskRunId);
        String appEngineUriSection = "task-runs/" + taskRunId + "/state-actions";
        stubFor(WireMock.post(urlEqualTo(apiBasePath + appEngineUriSection))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        mockMvc.perform(post("/api/app-engine/project/" + taskRun.getProject().getId() + "/" + appEngineUriSection)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(taskRunId.toString()))
                .andExpect(jsonPath("state").value("CREATED"))
                .andExpect(jsonPath("task").isNotEmpty());
    }

    protected String getTaskRunBody(UUID taskRunId) {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> task = Map.of(
            "name", "string",
            "namespace", "string",
            "version", "string",
            "description", "string",
            "authors", List.of(
                Map.of(
                    "first_name", "string",
                    "last_name", "string",
                    "organization", "string",
                    "email", "string",
                    "is_contact", true
                )
            )
        );

        Map<String, Object> taskRunBody = Map.of(
            "task", task,
            "id", taskRunId.toString(),
            "state", "CREATED",
            "created_at", "2023-12-20T10:49:21.272Z",
            "updated_at", "2023-12-20T10:49:21.272Z",
            "last_state_transition_at", "2023-12-20T10:49:21.272Z"
        );

        try {
            return objectMapper.writeValueAsString(taskRunBody);
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
