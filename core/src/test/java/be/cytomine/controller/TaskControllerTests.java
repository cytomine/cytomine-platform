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
import be.cytomine.domain.project.Project;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
public class TaskControllerTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restCommandControllerMockMvc;

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void task_workflow() throws Exception {
        Project project = builder.given_a_project();
        MvcResult response = restCommandControllerMockMvc.perform(post("/api/task.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonObject.of("project", project.getId()).toJsonString()))
                .andExpect(status().isOk()).andReturn();

        JsonObject responseObject = JsonObject.toJsonObject(response.getResponse().getContentAsString());
        Integer id = (Integer)((Map<String,Object>)responseObject.get("task")).get("id");

        restCommandControllerMockMvc.perform(get("/api/task/{id}.json", id))
                .andExpect(status().isOk());

        restCommandControllerMockMvc.perform(get("/api/project/{project}/task/comment.json", project.getId()))
                .andExpect(status().isOk());
    }
}
