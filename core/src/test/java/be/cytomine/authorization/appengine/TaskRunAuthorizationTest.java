package be.cytomine.authorization.appengine;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.authorization.CRDAuthorizationTest;
import be.cytomine.domain.appengine.TaskRun;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.service.appengine.TaskRunService;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.security.acls.domain.BasePermission.READ;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class TaskRunAuthorizationTest extends CRDAuthorizationTest {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private TaskRunService taskRunService;

    private static WireMockServer wireMockServer = new WireMockServer(8888);

    private TaskRun taskRun = null;

    @BeforeAll
    public static void beforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        wireMockServer.stop();
    }

    @BeforeEach
    public void before() throws Exception {
        if (taskRun == null) {
            taskRun = builder.given_a_task_run();
            initACL(taskRun.container());
        }
        taskRun.getProject().setMode(EditingMode.CLASSIC);
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_admin_can_add_in_readonly_mode(){
        taskRun.getProject().setMode(EditingMode.READ_ONLY);
        expectOK(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_readonly_mode(){
        taskRun.getProject().setMode(EditingMode.READ_ONLY);
        expectForbidden(() -> when_i_add_domain());
    }

    @Override
    public void when_i_get_domain() {
        //TODO: taskRunService.get(taskRun.getId());
    }

    @Override
    public void user_without_permission_get_domain() {
        //TODO: to remove when when_i_get_domain is implemented
    }

    @Override
    protected void when_i_delete_domain() {
        //TODO: taskRunService.delete(taskRunToDelete, null, null, true);
    }

    @Override
    public void user_without_permission_delete_domain() {
        //TODO: to remove when when_i_delete_domain is implemented
    }

    @Override
    public void guest_delete_domain() {
        //TODO: to remove when when_i_delete_domain is implemented
    }

    @Override
    protected void when_i_add_domain() {
        UUID taskId = taskRun.getTaskRunId();

        String mockResponse = "{\n" +
                "  \"task\": {\n" +
                "    \"name\": \"string\",\n" +
                "    \"namespace\": \"string\",\n" +
                "    \"version\": \"string\",\n" +
                "    \"description\": \"string\",\n" +
                "    \"authors\": [\n" +
                "      {\n" +
                "        \"first_name\": \"string\",\n" +
                "        \"last_name\": \"string\",\n" +
                "        \"organization\": \"string\",\n" +
                "        \"email\": \"string\",\n" +
                "        \"is_contact\": true\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                "  \"state\": \"created\"\n" +
                "}";

        configureFor("localhost", 8888);
        stubFor(WireMock.post(urlEqualTo("/api/v1/tasks/" + taskId + "/runs"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode body = objectMapper.createObjectNode().put("image", taskRun.getImage().getId());

        taskRunService.addTaskRun(taskRun.getProject().getId(), "/tasks/" + taskId + "/runs", body);
    }

    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(READ);
    }


    @Override
    protected Optional<String> minimalRoleForCreate() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForDelete() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForEdit() {
        return Optional.of("ROLE_USER");
    }
}
