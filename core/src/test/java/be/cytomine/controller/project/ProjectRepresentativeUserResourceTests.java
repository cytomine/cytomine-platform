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
import be.cytomine.domain.project.ProjectRepresentativeUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class ProjectRepresentativeUserResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restProjectRepresentativeUserControllerMockMvc;

    @Test
    @Transactional
    public void list_all_project_representative_users() throws Exception {
        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_project_representative_user();
        restProjectRepresentativeUserControllerMockMvc.perform(get("/api/project/{id}/representative.json", projectRepresentativeUser.getProject().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectRepresentativeUser.getId()+"')]").exists());
    }

    @Test
    @Transactional
    public void list_all_project_representative_users_for_unexisting_project() throws Exception {
        restProjectRepresentativeUserControllerMockMvc.perform(get("/api/project/{id}/representative.json", 0L))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void get_a_projectRepresentativeUser() throws Exception {
        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_project_representative_user();

        restProjectRepresentativeUserControllerMockMvc.perform(get("/api/project/{project}/representative/{id}.json",
                        projectRepresentativeUser.getProject().getId(), projectRepresentativeUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectRepresentativeUser.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.project.ProjectRepresentativeUser"))
                .andExpect(jsonPath("$.user").value(projectRepresentativeUser.getUser().getId()))
                .andExpect(jsonPath("$.project").value(projectRepresentativeUser.getProject().getId()));
    }

    @Test
    @Transactional
    public void get_a_projectRepresentativeUser_that_does_not_exists() throws Exception {
        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_project_representative_user();

        restProjectRepresentativeUserControllerMockMvc.perform(get("/api/project/{project}/representative/{id}.json",
                        projectRepresentativeUser.getProject().getId(), 0))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void get_a_projectRepresentativeUser_from_project_that_does_not_exists() throws Exception {
        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_project_representative_user();

        restProjectRepresentativeUserControllerMockMvc.perform(get("/api/project/{project}/representative/{id}.json",
                         0, projectRepresentativeUser.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void add_valid_projectRepresentativeUser() throws Exception {
        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_not_persisted_project_representative_user();
        restProjectRepresentativeUserControllerMockMvc.perform(post("/api/project/{id}/representative.json", projectRepresentativeUser.getProject().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectRepresentativeUser.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddProjectRepresentativeUserCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.projectrepresentativeuser.id").exists());
    }

    @Test
    @Transactional
    public void add_projectRepresentativeUser_refused_if_already_exists() throws Exception {
        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_not_persisted_project_representative_user();
        builder.persistAndReturn(projectRepresentativeUser);
        restProjectRepresentativeUserControllerMockMvc.perform(post("/api/project/{id}/representative.json", projectRepresentativeUser.getProject().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectRepresentativeUser.toJSON()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Transactional
    public void delete_projectRepresentativeUser() throws Exception {
        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_project_representative_user();
        ProjectRepresentativeUser projectRepresentativeUser2 = builder.given_a_project_representative_user(
                projectRepresentativeUser.getProject(), projectRepresentativeUser.getUser()
        );
        restProjectRepresentativeUserControllerMockMvc.perform(delete("/api/project/{project}/representative/{id}.json",
                        projectRepresentativeUser.getProject().getId(),projectRepresentativeUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectRepresentativeUser.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteProjectRepresentativeUserCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists());
    }

    @Test
    @Transactional
    public void delete_projectRepresentativeUser_with_user_parameter() throws Exception {
        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_project_representative_user();
        ProjectRepresentativeUser projectRepresentativeUser2 = builder.given_a_project_representative_user(
                projectRepresentativeUser.getProject(), builder.given_a_user()
        );
        restProjectRepresentativeUserControllerMockMvc.perform(delete("/api/project/{project}/representative.json",
                        projectRepresentativeUser.getProject().getId())
                        .param("user", projectRepresentativeUser.getUser().getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectRepresentativeUser.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteProjectRepresentativeUserCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists());
    }

    @Test
    @Transactional
    public void fail_when_delete_projectRepresentativeUser_not_exists() throws Exception {
        restProjectRepresentativeUserControllerMockMvc.perform(delete("/api/project/{project}/representative/{id}.json",  0,0)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @Transactional
    public void fail_when_delete_projectRepresentativeUser_project_not_exists() throws Exception {
        restProjectRepresentativeUserControllerMockMvc.perform(delete("/api/project/{project}/representative.json",  0)
                        .param("user", builder.given_superadmin().getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @Transactional
    public void fail_when_delete_projectRepresentativeUser_user_not_exists() throws Exception {
        restProjectRepresentativeUserControllerMockMvc.perform(delete("/api/project/{project}/representative.json",  builder.given_a_project().getId())
                        .param("user", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }
}
