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
import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.repository.ontology.AnnotationTermRepository;
import be.cytomine.service.ontology.AnnotationTermService;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class AnnotationTermResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restAnnotationTermControllerMockMvc;

    @Autowired
    private AnnotationTermService annotationTermService;

    @Autowired
    private AnnotationTermRepository annotationTermRepository;

    @Test
    @Transactional
    public void list_by_user_annotation() throws Exception {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        restAnnotationTermControllerMockMvc.perform(get("/api/annotation/{id}/term.json", annotationTerm.getUserAnnotation().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[?(@.id=='"+annotationTerm.getId()+"')]").exists());
    }

    @Test
    @Transactional
    public void list_by_user_annotation_and_user() throws Exception {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        restAnnotationTermControllerMockMvc.perform(get("/api/annotation/{id}/term.json", annotationTerm.getUserAnnotation().getId())
                        .param("idUser", annotationTerm.getUser().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[?(@.id=='"+annotationTerm.getId()+"')]").exists());

        restAnnotationTermControllerMockMvc.perform(get("/api/annotation/{id}/term.json", annotationTerm.getUserAnnotation().getId())
                        .param("idUser", builder.given_a_user().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(0)));
    }


    @Test
    @Transactional
    public void list_by_reviewed_annotation() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        reviewedAnnotation.getTerms().add(builder.given_a_term(reviewedAnnotation.getProject().getOntology()));
        restAnnotationTermControllerMockMvc.perform(get("/api/annotation/{id}/term.json", reviewedAnnotation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[?(@.term=='"+reviewedAnnotation.getTerms().get(0).getId()+"')]").exists());
    }


    @Test
    @Transactional
    public void list_by_user_annotation_but_with_term_not_defined_by_user() throws Exception {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        restAnnotationTermControllerMockMvc.perform(get("/api/annotation/{idAnnotation}/notuser/{idNotUser}/term.json",
                        annotationTerm.getUserAnnotation().getId(), annotationTerm.getUser().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(0)));

        restAnnotationTermControllerMockMvc.perform(get("/api/annotation/{idAnnotation}/notuser/{idNotUser}/term.json",
                        annotationTerm.getUserAnnotation().getId(), builder.given_a_user().getId())) // this user has not defined anything
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[?(@.id=='"+annotationTerm.getId()+"')]").exists());
    }


    @Test
    @Transactional
    public void get_a_annotation_term() throws Exception {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();

        restAnnotationTermControllerMockMvc.perform(get("/api/annotation/{idAnnotation}/term/{idTerm}.json",
                        annotationTerm.getUserAnnotation().getId(),
                        annotationTerm.getTerm().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userannotation").value(annotationTerm.getUserAnnotation().getId().intValue()))
                .andExpect(jsonPath("$.term").value(annotationTerm.getTerm().getId().intValue()))
                .andExpect(jsonPath("$.user").value(annotationTerm.getUser().getId().intValue()))
        ;
    }


    @Test
    @Transactional
    public void get_a_annotation_term_with_user() throws Exception {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        restAnnotationTermControllerMockMvc.perform(get("/api/annotation/{idAnnotation}/term/{idTerm}/user/{idUser}.json",
                        annotationTerm.getUserAnnotation().getId(),
                        annotationTerm.getTerm().getId(),
                        annotationTerm.getUser().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userannotation").value(annotationTerm.getUserAnnotation().getId().intValue()))
                .andExpect(jsonPath("$.term").value(annotationTerm.getTerm().getId().intValue()))
                .andExpect(jsonPath("$.user").value(annotationTerm.getUser().getId().intValue()));
    }

    @Test
    @Transactional
    public void get_a_annotation_term_with_bad_annotation() throws Exception {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();

        restAnnotationTermControllerMockMvc.perform(get("/api/annotation/{idAnnotation}/term/{idTerm}.json",
                        0,
                        annotationTerm.getTerm().getId()))
                .andExpect(status().is4xxClientError())
        ;
    }


    @Test
    @Transactional
    public void add_valid_annotation_term() throws Exception {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(builder.given_a_user_annotation());
        restAnnotationTermControllerMockMvc.perform(post("/api/annotation/{idAnnotation}/term/{idTerm}.json", annotationTerm.getUserAnnotation().getId(),annotationTerm.getTerm().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationTerm.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.annotationtermID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddAnnotationTermCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotationterm.id").exists())
                .andExpect(jsonPath("$.annotationterm.term").exists());

    }


    @Test
    @Transactional
    public void add_annotation_term_with_term_from_bad_ontology() throws Exception {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(builder.given_a_user_annotation());
        annotationTerm.setTerm(builder.given_a_term());
        restAnnotationTermControllerMockMvc.perform(post("/api/annotation/{idAnnotation}/term/{idTerm}.json", annotationTerm.getUserAnnotation().getId(),annotationTerm.getTerm().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationTerm.toJSON()))
                .andExpect(status().isBadRequest());

    }

    @Test
    @Transactional
    public void add_annotation_term_with_bad_annotation() throws Exception {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(builder.given_a_user_annotation());
        JsonObject jsonObject = annotationTerm.toJsonObject().withChange("userannotation", 0);
        restAnnotationTermControllerMockMvc.perform(post("/api/annotation/{idAnnotation}/term/{idTerm}.json", annotationTerm.getUserAnnotation().getId(),annotationTerm.getTerm().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().is4xxClientError());

    }

    @Test
    @Transactional
    public void add_annotation_term_with_bad_term() throws Exception {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(builder.given_a_user_annotation());
        annotationTerm.setTerm(builder.given_a_term());
        restAnnotationTermControllerMockMvc.perform(post("/api/annotation/{idAnnotation}/term/{idTerm}.json", annotationTerm.getTerm().getId(), 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationTerm.toJSON()))
                .andExpect(status().isBadRequest());

    }


    @Test
    @Transactional
    public void add_valid_annotation_term_clean_before_for_current_user() throws Exception {

        AnnotationTerm previousAnnotationTerm = builder.given_an_annotation_term();

        AnnotationTerm previousAnnotationTermFromOtherUser = builder.given_an_annotation_term(previousAnnotationTerm.getUserAnnotation());
        previousAnnotationTermFromOtherUser.setUser(builder.given_a_user());

        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(previousAnnotationTerm.getUserAnnotation());

        assertThat(annotationTermService.find(previousAnnotationTerm.getUserAnnotation(), previousAnnotationTerm.getTerm(), previousAnnotationTerm.getUser())).isPresent();
        assertThat(annotationTermService.find(previousAnnotationTermFromOtherUser.getUserAnnotation(), previousAnnotationTermFromOtherUser.getTerm(), previousAnnotationTermFromOtherUser.getUser())).isPresent();

        restAnnotationTermControllerMockMvc.perform(post("/api/annotation/{idAnnotation}/term/{idTerm}/clearBefore.json",
                        annotationTerm.getUserAnnotation().getId(),annotationTerm.getTerm().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationTerm.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.annotationtermID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddAnnotationTermCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotationterm.id").exists())
                .andExpect(jsonPath("$.annotationterm.term").exists());

        assertThat(annotationTermRepository.findByUserAnnotationIdAndTermIdAndUserId(
                previousAnnotationTerm.getUserAnnotation().getId(),
                previousAnnotationTerm.getTerm().getId(),
                previousAnnotationTerm.getUser().getId())
        ).isEmpty();
        assertThat(annotationTermRepository.findByUserAnnotationIdAndTermIdAndUserId(
                previousAnnotationTermFromOtherUser.getUserAnnotation().getId(),
                previousAnnotationTermFromOtherUser.getTerm().getId(),
                previousAnnotationTermFromOtherUser.getUser().getId())
        ).isPresent();
        assertThat(annotationTermRepository.findByUserAnnotationIdAndTermIdAndUserId(
                annotationTerm.getUserAnnotation().getId(),
                annotationTerm.getTerm().getId(),
                annotationTerm.getUser().getId())
        ).isPresent();
    }

    @Test
    @Transactional
    public void add_valid_annotation_term_clean_before_for_all_user() throws Exception {

        AnnotationTerm previousAnnotationTerm = builder.given_an_annotation_term();
        previousAnnotationTerm.setUser(builder.given_a_user());

        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(previousAnnotationTerm.getUserAnnotation());

        assertThat(annotationTermService.find(previousAnnotationTerm.getUserAnnotation(), previousAnnotationTerm.getTerm(), previousAnnotationTerm.getUser())).isPresent();

        restAnnotationTermControllerMockMvc.perform(post("/api/annotation/{idAnnotation}/term/{idTerm}/clearBefore.json",
                        annotationTerm.getUserAnnotation().getId(),annotationTerm.getTerm().getId())
                        .param("clearForAll", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationTerm.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.annotationtermID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddAnnotationTermCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotationterm.id").exists())
                .andExpect(jsonPath("$.annotationterm.term").exists());


        assertThat(annotationTermRepository.findByUserAnnotationIdAndTermIdAndUserId(
                previousAnnotationTerm.getUserAnnotation().getId(),
                previousAnnotationTerm.getTerm().getId(),
                previousAnnotationTerm.getUser().getId())
        ).isEmpty();
        assertThat(annotationTermRepository.findByUserAnnotationIdAndTermIdAndUserId(
                annotationTerm.getUserAnnotation().getId(),
                annotationTerm.getTerm().getId(),
                annotationTerm.getUser().getId())
        ).isPresent();
    }


    @Test
    @Transactional
    public void delete_annotation_term_with_user() throws Exception {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        restAnnotationTermControllerMockMvc.perform(delete("/api/annotation/{idAnnotation}/term/{idTerm}/user/{idUser}.json",
                        annotationTerm.getUserAnnotation().getId(), annotationTerm.getTerm().getId(), annotationTerm.getUser().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationTerm.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.annotationtermID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteAnnotationTermCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotationterm.id").exists());
    }

    @Test
    @Transactional
    public void delete_annotation_term_for_current_user() throws Exception {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        restAnnotationTermControllerMockMvc.perform(delete("/api/annotation/{idAnnotation}/term/{idTerm}.json",
                        annotationTerm.getUserAnnotation().getId(), annotationTerm.getTerm().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationTerm.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.annotationtermID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteAnnotationTermCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotationterm.id").exists());
    }

}
