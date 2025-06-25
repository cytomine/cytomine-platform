package be.cytomine.controller;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.project.Project;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;
import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
public class CustomUIControllerTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restConfigurationControllerMockMvc;

    @Autowired
    private LastConnectionRepository lastConnectionRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @BeforeEach
    public void before() {
        lastConnectionRepository.deleteAll();
    }

    @Test
    @Transactional
    @WithMockUser(username = "user")
    public void load_custom_ui_default_config() throws Exception {
        assertThat(applicationProperties.getCustomUI().getProject().get("project-images-tab").get("ADMIN_PROJECT")).isEqualTo(true);
        System.out.println(applicationProperties.getCustomUI().getProject().get("project-annotations-tab"));
        assertThat(applicationProperties.getCustomUI().getProject().get("project-annotations-tab").get("ADMIN_PROJECT")).isEqualTo(true);
    }


    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void retrieve_global_custom_ui_as_superadmin() throws Exception {
        restConfigurationControllerMockMvc.perform(get("/api/custom-ui/config.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity").value(true))
                .andExpect(jsonPath("$.admin").value(true))
                .andExpect(jsonPath("$.explore").value(true))
                .andExpect(jsonPath("$.feedback").value(true))
                .andExpect(jsonPath("$.feedback").value(true))
                .andExpect(jsonPath("$.help").value(true))
                .andExpect(jsonPath("$.ontology").value(true))
                .andExpect(jsonPath("$.project").value(true))
                .andExpect(jsonPath("$.search").value(true))
                .andExpect(jsonPath("$.storage").value(true))
        ;
    }


    @Test
    @Transactional
    @WithMockUser(username = "user")
    public void retrieve_global_custom_ui_as_user() throws Exception {
        restConfigurationControllerMockMvc.perform(get("/api/custom-ui/config.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity").value(true))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.explore").value(true))
                .andExpect(jsonPath("$.feedback").value(true))
                .andExpect(jsonPath("$.help").value(true))
                .andExpect(jsonPath("$.ontology").value(false))
                .andExpect(jsonPath("$.project").value(true))
                .andExpect(jsonPath("$.search").value(false))
                .andExpect(jsonPath("$.storage").value(true))
        ;
    }

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void retrieve_project_custom_ui() throws Exception {
        Project project = builder.given_a_project();
        restConfigurationControllerMockMvc.perform(get("/api/custom-ui/config.json")
                    .param("project", project.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value(true)) //1 global
                .andExpect(jsonPath("$.project-images-tab").value(true)) // 1project
        ;
    }

    @Test
    @Transactional
    @WithMockUser(username = "user")
    public void retrieve_project_custom_ui_as_contributor() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, "user", READ);
        restConfigurationControllerMockMvc.perform(get("/api/custom-ui/config.json")
                        .param("project", project.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project-jobs-tab").value(false))
        ;
    }

    @Test
    @Transactional
    @WithMockUser(username = "user")
    public void retrieve_project_custom_ui_as_manager() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, "user", ADMINISTRATION);
        restConfigurationControllerMockMvc.perform(get("/api/custom-ui/config.json")
                        .param("project", project.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project-jobs-tab").value(false))
        ;
    }


    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void retrieve_project_custom_ui_as_superadmin() throws Exception {
        Project project = builder.given_a_project();
        restConfigurationControllerMockMvc.perform(get("/api/custom-ui/project/{project}.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project-images-tab.ADMIN_PROJECT").value(true))
                .andExpect(jsonPath("$.project-explore-hide-tools.ADMIN_PROJECT").value(true))
                .andExpect(jsonPath("$.project-jobs-tab.CONTRIBUTOR_PROJECT").value(false))
        ;
    }


    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void change_project_custom_ui() throws Exception {
        Project project = builder.given_a_project();

        String customUI = "" +
                "{\n" +
                "   \"project-images-tab\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-annotations-tab\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-jobs-tab\":{\n" +
                "      \"ADMIN_PROJECT\":false,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-activities-tab\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":false\n" +
                "   },\n" +
                "   \"project-information-tab\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-configuration-tab\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":false\n" +
                "   },\n" +
                "   \"project-explore-image-overview\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-status\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-description\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-tags\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-properties\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-attached-files\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-slide-preview\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-original-filename\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-format\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-vendor\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-size\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-resolution\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-magnification\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-hide-tools\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-overview\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-info\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-digital-zoom\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-link\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-color-manipulation\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-image-layers\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-ontology\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-review\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-job\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-property\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-follow\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-guided-tour\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-main\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-geometry-info\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-info\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-comments\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-preview\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-properties\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-description\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-panel\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-terms\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-tags\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-attached-files\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-explore-annotation-creation-info\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-main\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-select\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-point\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-line\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-freehand-line\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-arrow\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-rectangle\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-diamond\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-circle\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-polygon\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-freehand-polygon\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-magic\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-freehand\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-union\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-diff\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-fill\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-rule\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-edit\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-resize\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-rotate\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-move\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-delete\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-screenshot\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-tools-undo-redo\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-annotations-term-piegraph\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-annotations-term-bargraph\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-annotations-users-graph\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-annotated-slides-term-graph\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-annotated-slides-users-graph\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-annotation-graph\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-users-global-activities-graph\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":true\n" +
                "   },\n" +
                "   \"project-users-heatmap-graph\":{\n" +
                "      \"ADMIN_PROJECT\":true,\n" +
                "      \"CONTRIBUTOR_PROJECT\":false\n" +
                "   }\n" +
                "}";


        restConfigurationControllerMockMvc.perform(post("/api/custom-ui/project/{project}.json", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(customUI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project-users-heatmap-graph.ADMIN_PROJECT").value(true))
                .andExpect(jsonPath("$.project-users-heatmap-graph.CONTRIBUTOR_PROJECT").value(false))
        ;

        // re save
        restConfigurationControllerMockMvc.perform(post("/api/custom-ui/project/{project}.json", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customUI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project-users-heatmap-graph.ADMIN_PROJECT").value(true))
                .andExpect(jsonPath("$.project-users-heatmap-graph.CONTRIBUTOR_PROJECT").value(false))
        ;
    }
}
