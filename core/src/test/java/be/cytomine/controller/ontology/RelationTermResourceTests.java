package be.cytomine.controller.ontology;

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
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.RelationTerm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class RelationTermResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restRelationTermControllerMockMvc;

    @Test
    @Transactional
    public void list_by_term_position_1() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restRelationTermControllerMockMvc.perform(get("/api/relation/term/{i}/{id}.json", "1", relationTerm.getTerm1().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[?(@.id=='"+relationTerm.getId()+"')]").exists());
    }

    @Test
    @Transactional
    public void list_by_term_position_2() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restRelationTermControllerMockMvc.perform(get("/api/relation/term/{i}/{id}.json", "2", relationTerm.getTerm1().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(0)));
    }

    @Test
    @Transactional
    public void list_by_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restRelationTermControllerMockMvc.perform(get("/api/relation/term/{id}.json",  relationTerm.getTerm1().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[?(@.id=='"+relationTerm.getId()+"')]").exists());
    }





    @Test
    @Transactional
    public void get_a_relation_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();

        restRelationTermControllerMockMvc.perform(get("/api/relation/{idRelation}/term1/{idTerm1}/term2/{idTerm2}.json", relationTerm.getRelation().getId(), relationTerm.getTerm1().getId(), relationTerm.getTerm2().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relationTerm.getId().intValue()))
                .andExpect(jsonPath("$.term1").value(relationTerm.getTerm1().getId().intValue()))
                .andExpect(jsonPath("$.term2").value(relationTerm.getTerm2().getId().intValue()))
                .andExpect(jsonPath("$.relation").value(relationTerm.getRelation().getId().intValue()))
        ;
    }

    @Test
    @Transactional
    public void get_a_parent_relation_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();

        restRelationTermControllerMockMvc.perform(get("/api/relation/parent/term1/{idTerm1}/term2/{idTerm2}.json", relationTerm.getTerm1().getId(), relationTerm.getTerm2().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relationTerm.getId().intValue()))
                .andExpect(jsonPath("$.term1").value(relationTerm.getTerm1().getId().intValue()))
                .andExpect(jsonPath("$.term2").value(relationTerm.getTerm2().getId().intValue()))
                .andExpect(jsonPath("$.relation").value(relationTerm.getRelation().getId().intValue()))
        ;
    }

    @Test
    @Transactional
    public void add_valid_relation() throws Exception {
        Ontology ontology = builder.given_an_ontology();
        RelationTerm relationTerm = BasicInstanceBuilder.given_a_not_persisted_relation_term(builder.given_a_relation(), builder.given_a_term(ontology), builder.given_a_term(ontology));
        restRelationTermControllerMockMvc.perform(post("/api/relation/parent/term.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(relationTerm.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.relationtermID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddRelationTermCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.relationterm.id").exists())
                .andExpect(jsonPath("$.relationterm.term1").exists());

    }




    @Test
    @Transactional
    public void delete_relation_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restRelationTermControllerMockMvc.perform(delete("/api/relation/{idRelation}/term1/{idTerm1}/term2/{idTerm2}.json", relationTerm.getRelation().getId(), relationTerm.getTerm1().getId(), relationTerm.getTerm2().getId())
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
    public void delete_parent_relation_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restRelationTermControllerMockMvc.perform(delete("/api/relation/parent/term1/{idTerm1}/term2/{idTerm2}.json", relationTerm.getTerm1().getId(), relationTerm.getTerm2().getId())
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
    public void delete_unexisting_relation_term_fails() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        em.remove(relationTerm);
        restRelationTermControllerMockMvc.perform(delete("/api/relation/{idRelation}/term1/{idTerm1}/term2/{idTerm2}.json", relationTerm.getRelation().getId(), relationTerm.getTerm1().getId(), relationTerm.getTerm2().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(relationTerm.toJSON()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }
}
