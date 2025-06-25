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
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.project.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class TagDomainAssociationResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restTagDomainAssociationControllerMockMvc;

    @Test
    @Transactional
    public void get_a_tag_domain_association() throws Exception {
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        restTagDomainAssociationControllerMockMvc.perform(get("/api/tag_domain_association/{id}.json", tagDomainAssociation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tagDomainAssociation.getId().intValue()));
    }

    @Test
    @Transactional
    public void get_an_tag_domain_association_does_not_exists() throws Exception {
        restTagDomainAssociationControllerMockMvc.perform(get("/api/tag_domain_association/{id}.json", 0L))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @Transactional
    public void list_all_tag_domain_association() throws Exception {
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        restTagDomainAssociationControllerMockMvc.perform(get("/api/tag_domain_association.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.domainIdent=='" + tagDomainAssociation.getDomainIdent() + "')]").exists());
    }


    @Test
    @Transactional
    public void list_all_tag_domain_association_with_filters() throws Exception {
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        TagDomainAssociation tagDomainAssociationWithAnotherTagAndAnnotherDomain = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        restTagDomainAssociationControllerMockMvc.perform(get("/api/tag_domain_association.json")
                        .param("tag[equals]", tagDomainAssociation.getTag().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.tag=='" + tagDomainAssociation.getTag().getId() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.tag=='" + tagDomainAssociationWithAnotherTagAndAnnotherDomain.getTag().getId() + "')]").doesNotExist());

        restTagDomainAssociationControllerMockMvc.perform(get("/api/tag_domain_association.json")
                        .param("tag[in]", tagDomainAssociation.getTag().getId().toString() + "," + tagDomainAssociationWithAnotherTagAndAnnotherDomain.getTag().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.tag=='" + tagDomainAssociation.getTag().getId() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.tag=='" + tagDomainAssociationWithAnotherTagAndAnnotherDomain.getTag().getId() + "')]").exists());

        restTagDomainAssociationControllerMockMvc.perform(get("/api/tag_domain_association.json")
                        .param("domainIdent[equals]", tagDomainAssociation.getDomainIdent().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.tag=='" + tagDomainAssociation.getTag().getId() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.tag=='" + tagDomainAssociationWithAnotherTagAndAnnotherDomain.getTag().getId() + "')]").doesNotExist());
    }


    @Test
    @Transactional
    public void list_all_tag_domain_association_with_filters_pagination() throws Exception {
        Tag tag = builder.given_a_tag();
        
        TagDomainAssociation t1 = builder.given_a_tag_association(tag, builder.given_a_project());
        TagDomainAssociation t2 = builder.given_a_tag_association(tag, builder.given_a_project());
        TagDomainAssociation t3 = builder.given_a_tag_association(tag, builder.given_a_project());
        TagDomainAssociation t4 = builder.given_a_tag_association(tag, builder.given_a_project());
        TagDomainAssociation t5 = builder.given_a_tag_association(tag, builder.given_a_project());

        restTagDomainAssociationControllerMockMvc.perform(get("/api/tag_domain_association.json")
                        .param("tag[equals]", tag.getId().toString())
                        .param("max", "3")
                        .param("offset", "0")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection[0].id").value(t1.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(t2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(t3.getId()));

        restTagDomainAssociationControllerMockMvc.perform(get("/api/tag_domain_association.json")
                        .param("tag[equals]", tag.getId().toString())
                        .param("max", "1")
                        .param("offset", "0")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].id").value(t1.getId()));

        restTagDomainAssociationControllerMockMvc.perform(get("/api/tag_domain_association.json")
                        .param("tag[equals]", tag.getId().toString())
                        .param("max", "1")
                        .param("offset", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].id").value(t2.getId()));

        restTagDomainAssociationControllerMockMvc.perform(get("/api/tag_domain_association.json")
                        .param("tag[equals]", tag.getId().toString())
                        .param("max", "3")
                        .param("offset", "3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2))))
                .andExpect(jsonPath("$.collection[0].id").value(t4.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(t5.getId()));
    }


    @Test
    @Transactional
    public void list_tag_domain_associations_by_domain() throws Exception {
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        restTagDomainAssociationControllerMockMvc.perform(get("/api/domain/{domainClassName}/{domainIdent}/tag_domain_association.json", tagDomainAssociation.getDomainClassName(), tagDomainAssociation.getDomainIdent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[0].id").value(tagDomainAssociation.getId().intValue()));
    }

    @Test
    @Transactional
    public void list_tag_domain_associations_by_domain_does_not_exists() throws Exception {
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        restTagDomainAssociationControllerMockMvc.perform(get("/api/domain/{domainClassName}/{domainIdent}/tag_domain_association.json", tagDomainAssociation.getDomainClassName(), 0))
                .andExpect(status().isNotFound());
    }


    @Test
    @Transactional
    public void add_valid_association() throws Exception {
        TagDomainAssociation tagDomainAssociation = builder.given_a_not_persisted_tag_association(builder.given_a_tag(), builder.given_a_project());
        restTagDomainAssociationControllerMockMvc.perform(post("/api/domain/{domainClassName}/{domainIdent}/tag_domain_association.json", tagDomainAssociation.getDomainClassName(), tagDomainAssociation.getDomainIdent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tagDomainAssociation.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.tagdomainassociation.id").exists())
                .andExpect(jsonPath("$.tagdomainassociation.tagName").value(tagDomainAssociation.getTag().getName()));
    }


    @Test
    @Transactional
    public void add_valid_property_other_path() throws Exception {
        TagDomainAssociation tagDomainAssociation = builder.given_a_not_persisted_tag_association(builder.given_a_tag(), builder.given_a_project());
        restTagDomainAssociationControllerMockMvc.perform(post("/api/tag_domain_association.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tagDomainAssociation.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.tagdomainassociation.tagName").value(tagDomainAssociation.getTag().getName()));
    }


    @Test
    @Transactional
    public void delete_tag_domain_association() throws Exception {
        Project project = builder.given_a_project();
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), project);
        restTagDomainAssociationControllerMockMvc.perform(delete("/api/tag_domain_association/{id}.json", tagDomainAssociation.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
