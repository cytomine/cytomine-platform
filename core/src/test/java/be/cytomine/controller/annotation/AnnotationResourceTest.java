package be.cytomine.controller.annotation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.domain.annotation.Annotation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "superadmin")
@AutoConfigureMockMvc
@SpringBootTest
public class AnnotationResourceTest {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getByIdShouldReturnAnnotation() throws Exception {
        Annotation annotation = builder.given_a_persisted_annotation();

        mockMvc.perform(get("/api/annotations/{id}", annotation.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(annotation.getId()))
            .andExpect(jsonPath("$.annotationLayer").value(annotation.getAnnotationLayer().getId()))
            .andExpect(jsonPath("$.location").exists());
    }

    @Test
    public void getByIdShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/annotations/{id}", 42))
            .andExpect(status().isNotFound());
    }
}
