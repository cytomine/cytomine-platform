package be.cytomine.controller.annotation;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.domain.annotation.Annotation;
import be.cytomine.domain.annotation.AnnotationLayer;
import be.cytomine.domain.appengine.TaskRunLayer;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@WithMockUser(username = "superadmin")
@AutoConfigureMockMvc
@SpringBootTest
public class AnnotationLayerResourceTest {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private EntityManager manager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getAnnotationLayersByImageShouldReturnAnnotationLayers() throws Exception {
        TaskRunLayer taskRunLayer = builder.given_a_persisted_task_run_layer();

        mockMvc.perform(get("/api/image-instances/{id}/annotation-layers", taskRunLayer.getImage().getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(taskRunLayer.getAnnotationLayer().getId()));
    }

    @Test
    public void getAnnotationsByLayerShouldReturnAnnotations() throws Exception {
        AnnotationLayer annotationLayer = builder.given_a_persisted_annotation_layer();
        Annotation first = builder.given_a_not_persisted_annotation(annotationLayer);
        Annotation second = builder.given_a_not_persisted_annotation(annotationLayer);
        Annotation third = builder.given_a_not_persisted_annotation(annotationLayer);
        manager.persist(first);
        manager.persist(second);
        manager.persist(third);
        manager.flush();

        mockMvc.perform(get("/api/annotation-layers/{id}/annotations", annotationLayer.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(first.getId()))
            .andExpect(jsonPath("$[1].id").value(second.getId()))
            .andExpect(jsonPath("$[2].id").value(third.getId()))
            .andExpect(jsonPath("$[*].annotationLayer.id", everyItem(is(Integer.valueOf(annotationLayer.getId().toString())))));
    }

    @Test
    public void getAnnotationsByLayerShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/annotation-layers/{id}/annotations", 42))
            .andExpect(status().isNotFound());
    }

    @Test
    public void findTaskRunLayerShouldReturnTaskRunLayer() throws Exception {
        TaskRunLayer taskRunLayer = builder.given_a_persisted_task_run_layer();

        mockMvc.perform(get("/api/annotation-layers/{id}/task-run-layer", taskRunLayer.getAnnotationLayer().getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.annotationLayer").value(taskRunLayer.getAnnotationLayer().getId()))
            .andExpect(jsonPath("$.taskRun").value(taskRunLayer.getTaskRun().getId()))
            .andExpect(jsonPath("$.image").value(taskRunLayer.getImage().getId()))
            .andExpect(jsonPath("$.xoffset").value(taskRunLayer.getXOffset()))
            .andExpect(jsonPath("$.yoffset").value(taskRunLayer.getYOffset()));
    }
}
