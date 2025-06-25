package be.cytomine.controller.security;

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
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.domain.security.User;
import be.cytomine.repository.security.SecRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class SecUserSecRoleResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restSecUserSecRoleControllerMockMvc;

    @Autowired
    private SecRoleRepository secRoleRepository;

    @Test
    @Transactional
    public void list_roles() throws Exception {

        restSecUserSecRoleControllerMockMvc.perform(get("/api/user/{user}/role.json", builder.given_superadmin().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.collection[?(@.authority=='ROLE_SUPER_ADMIN')]").exists());
    }

    @Test
    @Transactional
    public void list_highest_roles() throws Exception {

        restSecUserSecRoleControllerMockMvc.perform(get("/api/user/{user}/role.json", builder.given_superadmin().getId())
                        .param("highest", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[?(@.authority=='ROLE_SUPER_ADMIN')]").exists());
    }

    @Test
    @Transactional
    public void get_roles() throws Exception {
        restSecUserSecRoleControllerMockMvc.perform(get("/api/user/{user}/role/{role}.json",
                        builder.given_superadmin().getId(), secRoleRepository.getSuperAdmin().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority").value( "ROLE_SUPER_ADMIN"));
    }

    @Test
    @Transactional
    public void get_role_with_unexisting_user() throws Exception {
        restSecUserSecRoleControllerMockMvc.perform(get("/api/user/{user}/role/{role}.json",
                        builder.given_superadmin().getId(), 0L))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void get_role_with_unexisting_role() throws Exception {
        restSecUserSecRoleControllerMockMvc.perform(get("/api/user/{user}/role/{role}.json",
                        0L, secRoleRepository.getSuperAdmin().getId()))
                .andExpect(status().isNotFound());
    }


    @Test
    @Transactional
    public void add_valid_role() throws Exception {
        User user = builder.given_a_user();
        SecUserSecRole secSecUserSecRole = builder.given_a_not_persisted_user_role(user, secRoleRepository.getAdmin());
         restSecUserSecRoleControllerMockMvc.perform(post("/api/user/{user}/role.json", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(secSecUserSecRole.toJSON()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.printMessage").value(true))
            .andExpect(jsonPath("$.callback").exists())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.command").exists());
    }




    @Test
    @Transactional
    public void delete_user_role() throws Exception {
        User user = builder.given_a_user();
        SecUserSecRole secSecUserSecRole = builder.given_a_not_persisted_user_role(user, secRoleRepository.getAdmin());
        builder.persistAndReturn(secSecUserSecRole);
        restSecUserSecRoleControllerMockMvc.perform(delete("/api/user/{user}/role/{role}.json", user.getId(), secSecUserSecRole.getSecRole().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secSecUserSecRole.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists());
    }

    @Test
    @Transactional
    public void delete_parent_relation_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restSecUserSecRoleControllerMockMvc.perform(delete("/api/relation/parent/term1/{idTerm1}/term2/{idTerm2}.json", relationTerm.getTerm1().getId(), relationTerm.getTerm2().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(relationTerm.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.relationtermID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteRelationTermCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.relationterm.id").exists());
    }

    @Test
    @Transactional
    public void define() throws Exception {
        User user = builder.given_a_user();

        restSecUserSecRoleControllerMockMvc.perform(put("/api/user/{user}/role/{role}/define.json", user.getId(), secRoleRepository.getAdmin().getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        em.refresh(user);
        assertThat(user.getRoles().stream().map(x -> x.getAuthority()))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER", "ROLE_GUEST");
    }

}
