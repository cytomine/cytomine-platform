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
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class TermResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restTermControllerMockMvc;

    @Test
    @Transactional
    public void list_all_terms() throws Exception {
        Term term = builder.given_a_term();
        restTermControllerMockMvc.perform(get("/api/term.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+term.getName()+"')].ontology").value(term.getOntology().getId().intValue()));
    }


    @Test
    @Transactional
    public void get_a_term() throws Exception {
        Term term = builder.given_a_term();
        Term parent = builder.given_a_term(term.getOntology());
        builder.given_a_relation_term(parent, term);
        em.refresh(term);
        restTermControllerMockMvc.perform(get("/api/term/{id}.json", term.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(term.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.ontology.Term"))
                .andExpect(jsonPath("$.color").value(term.getColor()))
                .andExpect(jsonPath("$.created").isNotEmpty())
                .andExpect(jsonPath("$.ontology").value(term.getOntology().getId().intValue()))
                .andExpect(jsonPath("$.parent").value(parent.getId().intValue()))
        ;
    }

    @Test
    @Transactional
    public void list_terms_by_ontology() throws Exception {
        Term term = builder.given_a_term();
        restTermControllerMockMvc.perform(get("/api/ontology/{id}/term.json", term.getOntology().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+term.getName()+"')].ontology").value(term.getOntology().getId().intValue()));
    }

    @Test
    @Transactional
    public void list_terms_by_project() throws Exception {
        Term term = builder.given_a_term();
        Project project = builder.given_a_project_with_ontology(term.getOntology());
        restTermControllerMockMvc.perform(get("/api/project/{id}/term.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+term.getName()+"')].ontology").value(term.getOntology().getId().intValue()));
    }

    @Test
    @Transactional
    public void add_valid_term() throws Exception {
        Term term = BasicInstanceBuilder.given_a_not_persisted_term(builder.given_an_ontology());
        restTermControllerMockMvc.perform(post("/api/term.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(term.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.termID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddTermCommand"))
                .andExpect(jsonPath("$.callback.ontologyID").value(String.valueOf(term.getOntology().getId())))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.term.id").exists())
                .andExpect(jsonPath("$.term.name").value(term.getName()))
                .andExpect(jsonPath("$.term.ontology").value(term.getOntology().getId()));
    }

    @Test
    @Transactional
    public void add_valid_term_by_group() throws Exception {
        Term term1 = BasicInstanceBuilder.given_a_not_persisted_term(builder.given_an_ontology());
        Term term2 = BasicInstanceBuilder.given_a_not_persisted_term(term1.getOntology());
        restTermControllerMockMvc.perform(post("/api/term.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonObject.toJsonString(List.of(term1.toJsonObject(),term2.toJsonObject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @Transactional
    public void add_term_refused_if_already_exists() throws Exception {
        Term term = BasicInstanceBuilder.given_a_not_persisted_term(builder.given_an_ontology());
        builder.persistAndReturn(term);
        restTermControllerMockMvc.perform(post("/api/term.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(term.toJSON()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").value("Term " + term.getName() + " already exist in this ontology!"));
    }

    @Test
    @Transactional
    public void add_term_refused_if_ontology_not_set() throws Exception {
        Term term = BasicInstanceBuilder.given_a_not_persisted_term(null);
        restTermControllerMockMvc.perform(post("/api/term.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(term.toJSON()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").value("Ontology is mandatory for term creation"));
    }

    @Test
    @Transactional
    public void add_term_refused_if_name_not_set() throws Exception {
        Term term = BasicInstanceBuilder.given_a_not_persisted_term(builder.given_an_ontology());
        term.setName(null);
        restTermControllerMockMvc.perform(post("/api/term.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(term.toJSON()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Transactional
    public void edit_valid_term() throws Exception {
        Term term = builder.given_a_term();
        restTermControllerMockMvc.perform(put("/api/term/{id}.json", term.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(term.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.termID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditTermCommand"))
                .andExpect(jsonPath("$.callback.ontologyID").value(String.valueOf(term.getOntology().getId())))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.term.id").exists())
                .andExpect(jsonPath("$.term.name").value(term.getName()))
                .andExpect(jsonPath("$.term.ontology").value(term.getOntology().getId()));

    }


    @Test
    @Transactional
    public void edit_term_not_exists_fails() throws Exception {
        Term term = builder.given_a_term();
        em.remove(term);
        restTermControllerMockMvc.perform(put("/api/term/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(term.toJSON()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());

    }

    @Test
    @Transactional
    public void delete_term() throws Exception {
        Term term = builder.given_a_term();
        restTermControllerMockMvc.perform(delete("/api/term/{id}.json", term.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(term.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.termID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteTermCommand"))
                .andExpect(jsonPath("$.callback.ontologyID").value(String.valueOf(term.getOntology().getId())))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.term.id").exists())
                .andExpect(jsonPath("$.term.name").value(term.getName()))
                .andExpect(jsonPath("$.term.ontology").value(term.getOntology().getId()));

    }

    @Test
    @Transactional
    public void delete_term_not_exist_fails() throws Exception {
        Term term = builder.given_a_term();
        restTermControllerMockMvc.perform(delete("/api/term/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(term.toJSON()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());

    }
}
