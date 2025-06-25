package be.cytomine.controller.social;

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
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.AnnotationAction;
import be.cytomine.repositorynosql.social.AnnotationActionRepository;
import be.cytomine.service.social.AnnotationActionService;
import be.cytomine.utils.JsonObject;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class AnnotationActionResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restUserPositionControllerMockMvc;

    @Autowired
    private AnnotationActionRepository annotationActionRepository;

    @Autowired
    private AnnotationActionService annotationActionService;

    @BeforeEach
    public void cleanDB() {
        annotationActionRepository.deleteAll();
    }

    AnnotationAction given_a_persistent_annotation_action(Date creation, AnnotationDomain annotationDomain, User user, String action) {
        AnnotationAction annotationAction =
                annotationActionService.add(
                        annotationDomain,
                        user,
                        action,
                        creation
                );
        return annotationAction;
    }

    @Test
    public void add_action_for_annotation() throws Exception {
        AssertionsForClassTypes.assertThat(annotationActionRepository.count()).isEqualTo(0);
        User user = builder.given_superadmin();
        AnnotationDomain annotationDomain = builder.given_a_user_annotation();

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("annotationIdent", annotationDomain.getId());
        jsonObject.put("action", "select");

        restUserPositionControllerMockMvc.perform(post("/api/annotation_action.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.class").value( "be.cytomine.domain.social.AnnotationAction"))
                .andExpect(jsonPath("$.class").value( "be.cytomine.domain.social.AnnotationAction"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.user").value( user.getId()))
                .andExpect(jsonPath("$.image").value( annotationDomain.getImage().getId()))
                .andExpect(jsonPath("$.project").value( annotationDomain.getProject().getId()))
                .andExpect(jsonPath("$.action").value( "select"))
                .andExpect(jsonPath("$.annotationIdent").value( annotationDomain.getId()))
                .andExpect(jsonPath("$.annotationClassName").value( annotationDomain.getClass().getName()))
                .andExpect(jsonPath("$.annotationCreator").value( user.getId()));

        AssertionsForClassTypes.assertThat(annotationActionRepository.count()).isEqualTo(1);
    }


    @Test
    @Transactional
    public void list_last_user_on_image() throws Exception {
        User user = builder.given_a_user();

        AnnotationDomain annotationDomain = builder.given_a_user_annotation();
        given_a_persistent_annotation_action(new Date(), annotationDomain, user, "select");

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/annotation_action.json", annotationDomain.getImage().getId())
                        .param("user", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))));

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/annotation_action.json", builder.given_an_image_instance().getId())
                    .param("user", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));
    }

    @Test
    @Transactional
    public void list_last_user_on_slice() throws Exception {
        User user = builder.given_a_user();

        AnnotationDomain annotationDomain = builder.given_a_user_annotation();
        given_a_persistent_annotation_action(new Date(), annotationDomain, user, "select");

        restUserPositionControllerMockMvc.perform(get("/api/sliceinstance/{image}/annotation_action.json", annotationDomain.getSlice().getId())
                    .param("user", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))));

        restUserPositionControllerMockMvc.perform(get("/api/sliceinstance/{image}/annotation_action.json", builder.given_a_slice_instance().getId())
                    .param("user", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));
    }

    @Test
    @Transactional
    public void count_annotation_by_project() throws Exception {
        User user = builder.given_superadmin();
        AnnotationDomain annotationDomain = builder.given_a_user_annotation();
        given_a_persistent_annotation_action(DateUtils.addDays(new Date(), -2), annotationDomain, user, "select");

        restUserPositionControllerMockMvc.perform(get("/api/project/{project}/annotation_action/count.json", annotationDomain.getProject().getId())
                        .param("startDate", ""+DateUtils.addDays(new Date(), -10).getTime())
                        .param("endDate", ""+DateUtils.addDays(new Date(), 10).getTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("1"));

        restUserPositionControllerMockMvc.perform(get("/api/project/{project}/annotation_action/count.json", annotationDomain.getProject().getId())
                        .param("endDate", ""+DateUtils.addDays(new Date(), -5).getTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("0"));

        restUserPositionControllerMockMvc.perform(get("/api/project/{project}/annotation_action/count.json", annotationDomain.getProject().getId())
                        .param("startDate", ""+DateUtils.addDays(new Date(), -1).getTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("0"));

        restUserPositionControllerMockMvc.perform(get("/api/project/{project}/annotation_action/count.json", annotationDomain.getProject().getId())
                        .param("startDate", ""+DateUtils.addDays(new Date(), -10).getTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("1"));

    }
}
