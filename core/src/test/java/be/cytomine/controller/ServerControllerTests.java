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
import be.cytomine.domain.social.LastConnection;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
public class ServerControllerTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restConfigurationControllerMockMvc;

    @Autowired
    private LastConnectionRepository lastConnectionRepository;

    @BeforeEach
    public void before() {
        lastConnectionRepository.deleteAll();
    }


    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void ping_as_auth() throws Exception {
        restConfigurationControllerMockMvc.perform(post("/server/ping.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\": null}"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("Content-type", "application/json"))
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath());

        // TODO 2024.2 - LAST CONNECTION (IN A PROJECT)
//        List<LastConnection> lastConnection = lastConnectionRepository.findByUserOrderByCreatedDesc(builder.given_superadmin().getId());
//        assertThat(lastConnection).hasSize(1);
//        assertThat(lastConnection.get(0).getProject()).isNull();
    }

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void ping_as_auth_with_get() throws Exception {
        restConfigurationControllerMockMvc.perform(get("/server/ping.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath());

        // TODO 2024.2 - LAST CONNECTION (IN A PROJECT)
//        List<LastConnection> lastConnection = lastConnectionRepository.findByUserOrderByCreatedDesc(builder.given_superadmin().getId());
//        assertThat(lastConnection).hasSize(1);
//        assertThat(lastConnection.get(0).getProject()).isNull();
    }

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void ping_with_project() throws Exception {
        Project project = builder.given_a_project();
        restConfigurationControllerMockMvc.perform(post("/server/ping.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\": "+project.getId()+"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath());

        //TODO 2024.2 - LAST CONNECTION (IN A PROJECT)
//        List<LastConnection> lastConnection = lastConnectionRepository.findByUserOrderByCreatedDesc(builder.given_superadmin().getId());
//        assertThat(lastConnection).hasSize(1);
//        assertThat(lastConnection.get(0).getProject()).isEqualTo(project.getId());

    }

    @Test
    @Transactional
    public void ping_as_unauth() throws Exception {
        restConfigurationControllerMockMvc.perform(post("/server/ping.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath())
                .andExpect(jsonPath("$.user").doesNotExist());

//         TODO 2024.2 - LAST CONNECTION (IN A PROJECT)
//        assertThat(lastConnectionRepository.count()).isEqualTo(0);
    }

    @Test
    @Transactional
    public void check_status() throws Exception {
        restConfigurationControllerMockMvc.perform(get("/status.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath());
    }

}
