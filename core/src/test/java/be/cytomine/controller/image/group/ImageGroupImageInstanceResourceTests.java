package be.cytomine.controller.image.group;

import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.image.group.ImageGroupImageInstance;
import be.cytomine.domain.project.Project;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class ImageGroupImageInstanceResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restImageGroupImageInstanceControllerMockMvc;

    private static WireMockServer wireMockServer = new WireMockServer(8888);

    @BeforeAll
    public static void beforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        wireMockServer.stop();
    }

    @Test
    @Transactional
    public void list_imagegroup_imageinstance_by_imageinstance() throws Exception {
        ImageGroupImageInstance igii = builder.given_an_imagegroup_imageinstance();
        restImageGroupImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/imagegroupimageinstance.json", igii.getImage().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + igii.getId() + ")]").exists());
    }

    @Test
    @Transactional
    public void add_valid_imagegroup_imageinstance() throws Exception {
        ImageGroupImageInstance igii = builder.given_an_imagegroup_imageinstance();
        restImageGroupImageInstanceControllerMockMvc.perform(post("/api/imagegroup/{group}/imageinstance/{image}.json", igii.getGroup().getId(), igii.getImage().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(igii.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imagegroupimageinstanceID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imagegroupimageinstance.id").exists());
    }

    @Test
    @Transactional
    public void delete_imagegroup_imageinstance() throws Exception {
        ImageGroupImageInstance igii = builder.given_an_imagegroup_imageinstance();
        restImageGroupImageInstanceControllerMockMvc.perform(delete("/api/imagegroup/{group}/imageinstance/{image}.json", igii.getGroup().getId(), igii.getImage().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imagegroupimageinstanceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteImageGroupImageInstanceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imagegroupimageinstance.id").exists());
    }

    @Test
    @Transactional
    public void get_previous_imagegroup_imageinstance() throws Exception {
        Project project = builder.given_a_project();
        ImageGroup group = builder.given_an_imagegroup(project);
        ImageGroupImageInstance curr_igii = builder.given_an_imagegroup_imageinstance(group, builder.given_an_image_instance(project));
        ImageGroupImageInstance prev_igii = builder.given_an_imagegroup_imageinstance(group, builder.given_an_image_instance(project));
        restImageGroupImageInstanceControllerMockMvc.perform(get("/api/imagegroup/{group}/imageinstance/{image}/previous.json", curr_igii.getGroup().getId(), curr_igii.getImage().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prev_igii.getImage().getId()));
    }

    @Test
    @Transactional
    public void get_next_imagegroup_imageinstance() throws Exception {
        Project project = builder.given_a_project();
        ImageGroup group = builder.given_an_imagegroup(project);
        ImageGroupImageInstance curr_igii = builder.given_an_imagegroup_imageinstance(group, builder.given_an_image_instance(project));
        ImageGroupImageInstance next_igii = builder.given_an_imagegroup_imageinstance(group, builder.given_an_image_instance(project));
        restImageGroupImageInstanceControllerMockMvc.perform(get("/api/imagegroup/{group}/imageinstance/{image}/next.json", curr_igii.getGroup().getId(), curr_igii.getImage().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(next_igii.getImage().getId()));
    }
}
