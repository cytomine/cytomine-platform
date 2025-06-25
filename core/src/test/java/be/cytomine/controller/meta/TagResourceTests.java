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

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.meta.Tag;
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
public class TagResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restTagControllerMockMvc;

    @Test
    @Transactional
    public void list_all_tags() throws Exception {
        Tag tag = builder.given_a_tag();
        restTagControllerMockMvc.perform(get("/api/tag.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+tag.getName()+"')]").exists());
    }

    @Test
    @Transactional
    public void get_a_tag() throws Exception {
        Tag tag = builder.given_a_tag();

        restTagControllerMockMvc.perform(get("/api/tag/{id}.json", tag.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tag.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.meta.Tag"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.name").value(tag.getName()))
                .andExpect(jsonPath("$.creatorName").value(builder.given_superadmin().getUsername()))
                .andExpect(jsonPath("$.user").value(builder.given_superadmin().getId()))
        ;
    }

    @Test
    @Transactional
    public void add_valid_tag() throws Exception {
        Tag tag = builder.given_a_not_persisted_tag("xxx");
        restTagControllerMockMvc.perform(post("/api/tag.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tag.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.tagID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddTagCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.tag.id").exists())
                .andExpect(jsonPath("$.tag.name").value(tag.getName()));

    }

    @Test
    @Transactional
    public void add_tag_refused_if_already_exists() throws Exception {
        Tag tag = builder.given_a_not_persisted_tag("xxx");
        builder.persistAndReturn(tag);
        restTagControllerMockMvc.perform(post("/api/tag.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tag.toJSON()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Transactional
    public void add_tag_refused_if_name_not_set() throws Exception {
        Tag tag = builder.given_a_not_persisted_tag(null);
        restTagControllerMockMvc.perform(post("/api/tag.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tag.toJSON()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Transactional
    public void edit_valid_tag() throws Exception {
        Tag tag = builder.given_a_tag();
        restTagControllerMockMvc.perform(put("/api/tag/{id}.json", tag.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tag.toJsonObject().withChange("name", "new name").toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.tagID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditTagCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.tag.id").exists())
                .andExpect(jsonPath("$.tag.name").value("new name"));

    }


    @Test
    @Transactional
    public void fail_when_editing_tag_does_not_exists() throws Exception {
        Tag tag = builder.given_a_tag();
        em.remove(tag);
        restTagControllerMockMvc.perform(put("/api/tag/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tag.toJSON()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());

    }

    @Test
    @Transactional
    public void delete_tag() throws Exception {
        Tag tag = builder.given_a_tag();
        restTagControllerMockMvc.perform(delete("/api/tag/{id}.json", tag.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tag.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.tagID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteTagCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.tag.id").exists())
                .andExpect(jsonPath("$.tag.name").value(tag.getName()));
    }

    @Test
    @Transactional
    public void fail_when_delete_tag_not_exists() throws Exception {
        restTagControllerMockMvc.perform(delete("/api/tag/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }

}
