package be.cytomine.controller.meta;

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
import be.cytomine.domain.meta.Configuration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class ConfigurationResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restConfigurationControllerMockMvc;

    @Test
    @Transactional
    public void list_all_configs() throws Exception {
        Configuration configuration = builder.given_a_configuration("xxx");
        restConfigurationControllerMockMvc.perform(get("/api/configuration.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.key=='"+configuration.getKey()+"')]").exists());
    }

    @Test
    @Transactional
    public void get_a_configuration() throws Exception {
        Configuration configuration = builder.given_a_configuration("xxx");

        restConfigurationControllerMockMvc.perform(get("/api/configuration/key/{key}.json", configuration.getKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(configuration.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.meta.Configuration"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.key").value("xxx"))
                .andExpect(jsonPath("$.value").value(configuration.getValue()))
                .andExpect(jsonPath("$.readingRole").value("ALL"))

        ;
    }

    @Test
    @Transactional
    public void add_valid_configuration() throws Exception {
        Configuration configuration = builder.given_a_not_persisted_configuration("xxx");
        restConfigurationControllerMockMvc.perform(post("/api/configuration.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuration.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.configurationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddConfigurationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.configuration.id").exists())
                .andExpect(jsonPath("$.configuration.key").value(configuration.getKey()));
    }

    @Test
    @Transactional
    public void edit_valid_configuration() throws Exception {
        Configuration configuration = builder.given_a_configuration("xxx");
        restConfigurationControllerMockMvc.perform(put("/api/configuration/key/{key}.json", configuration.getKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuration.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.configurationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditConfigurationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.configuration.id").exists())
                .andExpect(jsonPath("$.configuration.key").value("xxx"));
    }

    @Test
    @Transactional
    public void delete_configuration() throws Exception {
        Configuration configuration = builder.given_a_configuration("xxx");
        restConfigurationControllerMockMvc.perform(delete("/api/configuration/key/{key}.json", configuration.getKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuration.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.configurationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteConfigurationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.configuration.id").exists())
                .andExpect(jsonPath("$.configuration.key").value(configuration.getKey()));
    }

    @Test
    @Transactional
    public void fail_when_delete_configuration_not_exists() throws Exception {
        restConfigurationControllerMockMvc.perform(delete("/api/configuration/key/{key}.json", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }
}
