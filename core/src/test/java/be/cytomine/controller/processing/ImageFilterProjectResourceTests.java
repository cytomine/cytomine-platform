package be.cytomine.controller.processing;

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.processing.ImageFilterProject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class ImageFilterProjectResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restImageFilterProjectMockMvc;

    @Test
    @Transactional
    public void list_all() throws Exception {
        ImageFilterProject imageFilterProject = builder.given_a_image_filter_project();
        restImageFilterProjectMockMvc.perform(get("/api/imagefilterproject.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+imageFilterProject.getImageFilter().getName()+"')]").exists());
    }

    @Test
    @Transactional
    public void list_by_project() throws Exception {
        ImageFilterProject imageFilterProject = builder.given_a_image_filter_project();
        restImageFilterProjectMockMvc.perform(get("/api/project/{id}/imagefilterproject.json", imageFilterProject.getProject().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+imageFilterProject.getId()+"')]").exists());
    }

    @Test
    @Transactional
    public void list_by_unexisting_project() throws Exception {
        restImageFilterProjectMockMvc.perform(get("/api/project/{id}/imagefilterproject.json", 0))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void add_valid_imageFilterProject() throws Exception {
        ImageFilterProject imageFilterProject = builder.given_a_not_persisted_image_filter_project(builder.given_a_image_filter(), builder.given_a_project());
        restImageFilterProjectMockMvc.perform(post("/api/imagefilterproject.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imageFilterProject.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imagefilterproject.id").exists());
    }

    @Test
    @Transactional
    public void delete_valid_imageFilterProject() throws Exception {
        ImageFilterProject imageFilterProject = builder.given_a_image_filter_project(builder.given_a_image_filter(), builder.given_a_project());
        builder.persistAndReturn(imageFilterProject);
        restImageFilterProjectMockMvc.perform(delete("/api/imagefilterproject/{id}.json", imageFilterProject.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void delete_unexisting_imageFilterProject() throws Exception {
        restImageFilterProjectMockMvc.perform(delete("/api/imageFilterProject/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
