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
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class OntologyResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restOntologyControllerMockMvc;

    @Test
    @Transactional
    public void list_all_ontologies() throws Exception {
        Ontology ontology = builder.given_an_ontology();
        restOntologyControllerMockMvc.perform(get("/api/ontology.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+ontology.getName()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.name=='"+ontology.getName()+"')].projects").exists());
    }

    @Test
    @Transactional
    public void list_all_ontologies_light() throws Exception {
        Ontology ontology = builder.given_an_ontology();
        restOntologyControllerMockMvc.perform(get("/api/ontology.json").param("light", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+ontology.getName()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.name=='"+ontology.getName()+"')].projects").doesNotExist());
    }



    @Test
    @Transactional
    public void get_a_ontology() throws Exception {
        Ontology ontology = builder.given_an_ontology();
        Term parent = builder.given_a_term(ontology);
        Term child1 = builder.given_a_term(ontology);
        Term child2 = builder.given_a_term(ontology);
        Term directChild = builder.given_a_term(ontology);
        RelationTerm relationTerm1 = builder.given_a_relation_term(parent, child1);
        RelationTerm relationTerm2 = builder.given_a_relation_term(parent, child2);
        Project project = builder.given_a_project_with_ontology(ontology);

        em.refresh(ontology);
        em.refresh(parent);
        em.refresh(child1);
        em.refresh(child2);
        em.refresh(directChild);

        restOntologyControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ontology.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.ontology.Ontology"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.name").value(ontology.getName()))
                .andExpect(jsonPath("$.user").value(ontology.getUser().getId().intValue()))
                .andExpect(jsonPath("$.attr.id").value(ontology.getId().intValue()))
                .andExpect(jsonPath("$.attr.type").value("be.cytomine.domain.ontology.Ontology"))
                .andExpect(jsonPath("$.data").value(ontology.getName()))
                .andExpect(jsonPath("$.isFolder").value(true))
                .andExpect(jsonPath("$.projects", hasSize(1)))
                .andExpect(jsonPath("$.projects[0].id").value(project.getId().intValue()))

                .andExpect(jsonPath("$.children", hasSize(2)))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')]").exists())
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].id").value(parent.getId().intValue()))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].title").value(parent.getName()))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].data").value(parent.getName()))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].color").value(parent.getColor()))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].class").value("be.cytomine.domain.ontology.Term"))
//                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].parent").value(nullValue())) // does not work :(
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].attr.id").value(parent.getId().intValue()))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].attr.type").value("be.cytomine.domain.ontology.Term"))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].checked").value(false))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].key").value(parent.getId().intValue()))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].isFolder").value(true))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].hideCheckbox").value(true))

                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].children[?(@.name=='"+child1.getName()+"')]").exists())
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].children[?(@.name=='"+child1.getName()+"')].id").value(child1.getId().intValue()))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].children[?(@.name=='"+child1.getName()+"')].isFolder").value(false))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].children[?(@.name=='"+child1.getName()+"')].parent").value(parent.getId().intValue()))

                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].children[?(@.name=='"+child2.getName()+"')]").exists())
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].children[?(@.name=='"+child2.getName()+"')].id").value(child2.getId().intValue()))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].children[?(@.name=='"+child2.getName()+"')].isFolder").value(false))
                .andExpect(jsonPath("$.children[?(@.name=='"+parent.getName()+"')].children[?(@.name=='"+child2.getName()+"')].parent").value(parent.getId().intValue()))

                .andExpect(jsonPath("$.children[?(@.name=='"+directChild.getName()+"')]").exists())
                .andExpect(jsonPath("$.children[?(@.name=='"+directChild.getName()+"')].id").value(directChild.getId().intValue()))
        ;
    }

    @Test
    @Transactional
    public void add_valid_ontology() throws Exception {
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        restOntologyControllerMockMvc.perform(post("/api/ontology.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ontology.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.ontologyID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddOntologyCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.ontology.id").exists())
                .andExpect(jsonPath("$.ontology.name").value(ontology.getName()));

    }

    @Test
    @Transactional
    public void add_ontology_refused_if_already_exists() throws Exception {
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        builder.persistAndReturn(ontology);
        restOntologyControllerMockMvc.perform(post("/api/ontology.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ontology.toJSON()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Transactional
    public void add_ontology_refused_if_name_not_set() throws Exception {
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        ontology.setName(null);
        restOntologyControllerMockMvc.perform(post("/api/ontology.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ontology.toJSON()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Transactional
    public void edit_valid_ontology() throws Exception {
        Ontology ontology = builder.given_an_ontology();
        restOntologyControllerMockMvc.perform(put("/api/ontology/{id}.json", ontology.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ontology.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.ontologyID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditOntologyCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.ontology.id").exists())
                .andExpect(jsonPath("$.ontology.name").value(ontology.getName()));

    }


    @Test
    @Transactional
    public void fail_when_editing_ontology_does_not_exists() throws Exception {
        Ontology ontology = builder.given_an_ontology();
        em.remove(ontology);
        restOntologyControllerMockMvc.perform(put("/api/ontology/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ontology.toJSON()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());

    }

    @Test
    @Transactional
    public void delete_ontology() throws Exception {
        Ontology ontology = builder.given_an_ontology();
        restOntologyControllerMockMvc.perform(delete("/api/ontology/{id}.json", ontology.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ontology.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.ontologyID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteOntologyCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.ontology.id").exists())
                .andExpect(jsonPath("$.ontology.name").value(ontology.getName()));
    }

    @Test
    @Transactional
    public void fail_when_delete_ontology_not_exists() throws Exception {
        restOntologyControllerMockMvc.perform(delete("/api/ontology/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }
}
