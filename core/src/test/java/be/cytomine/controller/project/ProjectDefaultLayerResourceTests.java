package be.cytomine.controller.project;

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
import be.cytomine.domain.project.ProjectDefaultLayer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class ProjectDefaultLayerResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restProjectDefaultLayerControllerMockMvc;

    @Test
    @Transactional
    public void list_all_project_default_layers() throws Exception {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();
        restProjectDefaultLayerControllerMockMvc.perform(get("/api/project/{id}/defaultlayer.json", projectDefaultLayer.getProject().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectDefaultLayer.getId()+"')]").exists());
    }


    @Test
    @Transactional
    public void list_all_project_default_layers_for_unexisting_project() throws Exception {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();
        restProjectDefaultLayerControllerMockMvc.perform(get("/api/project/{id}/defaultlayer.json", 0L))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void get_a_projectDefaultLayer() throws Exception {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();

        restProjectDefaultLayerControllerMockMvc.perform(get("/api/project/{project}/defaultlayer/{id}.json",
                        projectDefaultLayer.getProject().getId(), projectDefaultLayer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectDefaultLayer.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.project.ProjectDefaultLayer"))
                .andExpect(jsonPath("$.user").value(projectDefaultLayer.getUser().getId()))
                .andExpect(jsonPath("$.project").value(projectDefaultLayer.getProject().getId()))
                .andExpect(jsonPath("$.hideByDefault").value(false))
        ;
    }


    @Test
    @Transactional
    public void get_a_projectDefaultLayer_that_does_not_exists() throws Exception {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();

        restProjectDefaultLayerControllerMockMvc.perform(get("/api/project/{project}/defaultlayer/{id}.json",
                        projectDefaultLayer.getProject().getId(), 0))
                .andExpect(status().isNotFound())
        ;
    }


    @Test
    @Transactional
    public void get_a_projectDefaultLayer_from_project_that_does_not_exists() throws Exception {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();

        restProjectDefaultLayerControllerMockMvc.perform(get("/api/project/{project}/defaultlayer/{id}.json",
                         0, projectDefaultLayer.getId()))
                .andExpect(status().isNotFound())
        ;
    }
    
    @Test
    @Transactional
    public void add_valid_projectDefaultLayer() throws Exception {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_not_persisted_project_default_layer();
        restProjectDefaultLayerControllerMockMvc.perform(post("/api/project/{id}/defaultlayer.json", projectDefaultLayer.getProject().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectDefaultLayer.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddProjectDefaultLayerCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.projectdefaultlayer.id").exists());

    }

    @Test
    @Transactional
    public void add_projectDefaultLayer_refused_if_already_exists() throws Exception {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_not_persisted_project_default_layer();
        builder.persistAndReturn(projectDefaultLayer);
        restProjectDefaultLayerControllerMockMvc.perform(post("/api/project/{id}/defaultlayer.json", projectDefaultLayer.getProject().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectDefaultLayer.toJSON()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Transactional
    public void edit_valid_projectDefaultLayer() throws Exception {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();
        restProjectDefaultLayerControllerMockMvc.perform(put("/api/project/{project}/defaultlayer/{id}.json",
                        projectDefaultLayer.getProject().getId(),projectDefaultLayer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectDefaultLayer.toJsonObject().withChange("hideByDefault", true).toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditProjectDefaultLayerCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.projectdefaultlayer.id").exists())
                .andExpect(jsonPath("$.projectdefaultlayer.hideByDefault").value(true));

    }


    @Test
    @Transactional
    public void fail_when_editing_projectDefaultLayer_does_not_exists() throws Exception {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();
        em.remove(projectDefaultLayer);
        restProjectDefaultLayerControllerMockMvc.perform(put("/api/project/{project}/defaultlayer/{id}.json", 0, 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectDefaultLayer.toJSON()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());

    }

    @Test
    @Transactional
    public void delete_projectDefaultLayer() throws Exception {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();
        restProjectDefaultLayerControllerMockMvc.perform(delete("/api/project/{project}/defaultlayer/{id}.json",
                        projectDefaultLayer.getProject().getId(),projectDefaultLayer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectDefaultLayer.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.projectdefaultlayerID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteProjectDefaultLayerCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists());
    }

    @Test
    @Transactional
    public void fail_when_delete_projectDefaultLayer_not_exists() throws Exception {
        restProjectDefaultLayerControllerMockMvc.perform(delete("/api/project/{project}/defaultlayer/{id}.json",  0,0)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }
}
