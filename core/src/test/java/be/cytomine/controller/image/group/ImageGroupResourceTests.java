package be.cytomine.controller.image.group;

import java.util.UUID;

import jakarta.transaction.Transactional;
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
import be.cytomine.utils.JsonObject;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class ImageGroupResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restImageGroupControllerMockMvc;

    @Test
    @Transactional
    public void list_imagegroup_by_project() throws Exception {
        ImageGroup imageGroup = builder.given_an_imagegroup();
        restImageGroupControllerMockMvc.perform(get("/api/project/{id}/imagegroup.json", imageGroup.getProject().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageGroup.getId() + ")]").exists());
    }

    @Test
    @Transactional
    public void get_an_imagegroup() throws Exception {
        ImageGroup imageGroup = builder.given_an_imagegroup();
        restImageGroupControllerMockMvc.perform(get("/api/imagegroup/{id}.json", imageGroup.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageGroup.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.group.ImageGroup"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.name").hasJsonPath())
                .andExpect(jsonPath("$.project").value(imageGroup.getProject().getId().intValue()))
                .andExpect(jsonPath("$.numberOfImages").value(0));
    }

    @Test
    @Transactional
    public void get_an_imagegroup_not_exist() throws Exception {
        restImageGroupControllerMockMvc.perform(get("/api/project/{id}/imagegroup.json", 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.message").exists());
    }

    @Test
    @Transactional
    public void add_valid_imagegroup() throws Exception {
        ImageGroup imageGroup = builder.given_a_not_persisted_imagegroup();
        restImageGroupControllerMockMvc.perform(post("/api/imagegroup.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imageGroup.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imagegroupID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imagegroup.id").exists());
    }

    @Test
    @Transactional
    public void edit_valid_imagegroup() throws Exception {
        ImageGroup imageGroup = builder.given_an_imagegroup();
        JsonObject jsonObject = imageGroup.toJsonObject();
        String name = UUID.randomUUID().toString();
        jsonObject.put("name", name);
        restImageGroupControllerMockMvc.perform(put("/api/imagegroup/{id}.json", imageGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imagegroupID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditImageGroupCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imagegroup.id").exists())
                .andExpect(jsonPath("$.imagegroup.name").value(name));
    }

    @Test
    @Transactional
    public void delete_imagegroup() throws Exception {
        ImageGroup imageGroup = builder.given_an_imagegroup();
        restImageGroupControllerMockMvc.perform(delete("/api/imagegroup/{id}.json", imageGroup.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imagegroupID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteImageGroupCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imagegroup.id").exists());
    }
}
