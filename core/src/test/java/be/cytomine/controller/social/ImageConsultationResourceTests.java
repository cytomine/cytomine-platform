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
import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.PersistentImageConsultation;
import be.cytomine.repositorynosql.social.PersistentImageConsultationRepository;
import be.cytomine.service.social.ImageConsultationService;
import be.cytomine.utils.JsonObject;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ImageConsultationResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restImageConsultationControllerMockMvc;

    @Autowired
    private PersistentImageConsultationRepository persistentImageConsultationRepository;

    @Autowired
    private ImageConsultationService imageConsultationService;

    @Autowired
    private ApplicationProperties applicationProperties;

    @BeforeEach
    public void cleanDB() {
        persistentImageConsultationRepository.deleteAll();
    }


    PersistentImageConsultation given_a_persistent_image_consultation(User user, ImageInstance imageInstance, Date created) {
        return imageConsultationService.add(user, imageInstance.getId(), "xxx", "mode", created);
    }

    @Test
    @Transactional
    public void add_consultation() throws Exception {
        User user = builder.given_superadmin();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("image", imageInstance.getId());
        jsonObject.put("mode", "view");

        // Frontend request: {"image":6836067,"mode":"view"}
        restImageConsultationControllerMockMvc.perform(post("/api/imageinstance/{id}/consultation.json", imageInstance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.social.PersistentImageConsultation"))
                .andExpect(jsonPath("$.user").value(user.getId()))
                .andExpect(jsonPath("$.image").value(imageInstance.getId()))
                .andExpect(jsonPath("$.imageName").exists())
                .andExpect(jsonPath("$.imageThumb").exists())
                .andExpect(jsonPath("$.mode").value("view"));

        Page<PersistentImageConsultation> persisted = persistentImageConsultationRepository.findAllByProjectAndUser(imageInstance.getProject().getId(), user.getId(), PageRequest.of(0, 1));
        assertThat(persisted).hasSize(1);
        assertThat(persisted.getContent().get(0).getMode()).isEqualTo("view");
    }


    @Test
    @Transactional
    public void list_last_image_of_users_for_a_project() throws Exception {
        User user = builder.given_a_user();
        ImageInstance imageInstance1 =builder.given_an_image_instance();
        ImageInstance imageInstance2 =builder.given_an_image_instance(imageInstance1.getProject());

        given_a_persistent_image_consultation(user, imageInstance1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_image_consultation(user, imageInstance2, DateUtils.addSeconds(new Date(), -2));

        restImageConsultationControllerMockMvc.perform(get("/api/project/{image}/lastImages.json", imageInstance1.getProject().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].image").value(imageInstance2.getId()));

    }

    @Test
    @Transactional
    public void list_last_user_consultation() throws Exception {
        User user = builder.given_superadmin();
        ImageInstance imageInstance1 =builder.given_an_image_instance();
        ImageInstance imageInstance2 =builder.given_an_image_instance(imageInstance1.getProject());


        imageConsultationService.add(user, imageInstance1.getId(), "xxx", "mode", DateUtils.addSeconds(new Date(), -3));
        imageConsultationService.add(user, imageInstance1.getId(), "xxx", "mode", DateUtils.addSeconds(new Date(), -2));
        imageConsultationService.add(user, imageInstance2.getId(), "xxx", "mode", DateUtils.addSeconds(new Date(), -1));


        restImageConsultationControllerMockMvc.perform(get("/api/imageinstance/method/lastopened.json")
                        .param("max", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2))))
                .andExpect(jsonPath("$.collection[0].id").value(imageInstance2.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(imageInstance1.getId()));

        restImageConsultationControllerMockMvc.perform(get("/api/imageinstance/method/lastopened.json")
                        .param("max", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].id").value(imageInstance2.getId()));
    }

    @Test
    @Transactional
    public void list_last_user_consultation_no_data() throws Exception {
        User user = builder.given_superadmin();
        ImageInstance imageInstance1 =builder.given_an_image_instance();
        ImageInstance imageInstance2 =builder.given_an_image_instance(imageInstance1.getProject());


        restImageConsultationControllerMockMvc.perform(get("/api/imageinstance/method/lastopened.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));

    }

    @Test
    @Transactional
    public void list_last_user_consultation_for_a_project_and_user_distinct_image() throws Exception {
        User user = builder.given_superadmin();
        ImageInstance imageInstance1 =builder.given_an_image_instance();
        ImageInstance imageInstance2 =builder.given_an_image_instance(imageInstance1.getProject());

        given_a_persistent_image_consultation(user, imageInstance1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_image_consultation(user, imageInstance1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_image_consultation(user, imageInstance2, DateUtils.addSeconds(new Date(), -2));

        restImageConsultationControllerMockMvc.perform(get("/api/project/{project}/user/{user}/imageconsultation.json",
                        imageInstance1.getProject().getId().toString(), user.getId().toString())
                        .param("distinctImages", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2))))
                .andExpect(jsonPath("$.collection[0].image").value(imageInstance2.getId()))
                .andExpect(jsonPath("$.collection[1].image").value(imageInstance1.getId()));

        restImageConsultationControllerMockMvc.perform(get("/api/project/{project}/user/{user}/imageconsultation.json",
                        imageInstance1.getProject().getId().toString(), builder.given_a_user().getId().toString())
                        .param("distinctImages", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));
    }

    @Test
    @Transactional
    public void resume_by_user_and_project_empty() throws Exception {
        User user = builder.given_superadmin();
        ImageInstance imageInstance1 =builder.given_an_image_instance();
        ImageInstance imageInstance2 =builder.given_an_image_instance(imageInstance1.getProject());

        restImageConsultationControllerMockMvc.perform(get("/api/imageconsultation/resume.json")
                        .param("project", imageInstance1.getProject().getId().toString())
                        .param("user", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));

    }

    @Test
    @Transactional
    public void resume_by_user_and_project() throws Exception {
        User user = builder.given_superadmin();
        ImageInstance imageInstance1 =builder.given_an_image_instance();
        ImageInstance imageInstance2 =builder.given_an_image_instance(imageInstance1.getProject());

        given_a_persistent_image_consultation(user, imageInstance1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_image_consultation(user, imageInstance1, DateUtils.addSeconds(new Date(), -3));


        restImageConsultationControllerMockMvc.perform(get("/api/imageconsultation/resume.json")
                        .param("project", imageInstance1.getProject().getId().toString())
                        .param("user", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].frequency").value(2));

    }

    @Test
    @Transactional
    public void resume_by_user_and_project_export_as_csv() throws Exception {
        User user = builder.given_superadmin();
        ImageInstance imageInstance1 =builder.given_an_image_instance();
        SliceInstance slice = builder.given_a_slice_instance(imageInstance1, 1, 0, 1);
        builder.given_a_user_annotation(slice);

        Date currentDate = DateUtils.addSeconds(new Date(), -3);
        given_a_persistent_image_consultation(user, imageInstance1, currentDate);
        given_a_persistent_image_consultation(user, imageInstance1, currentDate);

        MvcResult mvcResult = restImageConsultationControllerMockMvc.perform(get("/api/imageconsultation/resume.json")
                        .param("project", imageInstance1.getProject().getId().toString())
                        .param("user", user.getId().toString())
                        .param("export", "csv"))
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(status().isOk()).andReturn();

        String[] rows = mvcResult.getResponse().getContentAsString().split("\n");
        String[] imageConsultationResult = rows[1].split(";");
        String serverUrl = applicationProperties.getServerURL();
        AssertionsForClassTypes.assertThat(imageConsultationResult[0]).isEqualTo("0");
        AssertionsForClassTypes.assertThat(imageConsultationResult[1]).isEqualTo(currentDate.toString());
        AssertionsForClassTypes.assertThat(imageConsultationResult[2]).isEqualTo(currentDate.toString());
        AssertionsForClassTypes.assertThat(imageConsultationResult[3]).isEqualTo("2");
        AssertionsForClassTypes.assertThat(imageConsultationResult[4]).isEqualTo(imageInstance1.getId().toString());
        AssertionsForClassTypes.assertThat(imageConsultationResult[5]).isEqualTo(imageInstance1.getBlindInstanceFilename());
        AssertionsForClassTypes.assertThat(imageConsultationResult[6]).isEqualTo(serverUrl + "/api/imageinstance/"+imageInstance1.getId()+"/thumb.png?maxSize=512");
        AssertionsForClassTypes.assertThat(imageConsultationResult[7].replace("\r", "")).isEqualTo("0");
    }

    @Test
    @Transactional
    public void count_annotation_by_project() throws Exception {
        User user = builder.given_superadmin();
        ImageInstance imageInstance1 =builder.given_an_image_instance();
        ImageInstance imageInstance2 =builder.given_an_image_instance(imageInstance1.getProject());

        given_a_persistent_image_consultation(user, imageInstance2, DateUtils.addDays(new Date(), -2));

        restImageConsultationControllerMockMvc.perform(get("/api/project/{project}/imageconsultation/count.json", imageInstance1.getProject().getId())
                        .param("startDate", ""+DateUtils.addDays(new Date(), -10).getTime())
                        .param("endDate", ""+DateUtils.addDays(new Date(), 10).getTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("1"));

        restImageConsultationControllerMockMvc.perform(get("/api/project/{project}/imageconsultation/count.json", imageInstance1.getProject().getId())
                        .param("endDate", ""+DateUtils.addDays(new Date(), -5).getTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("0"));

        restImageConsultationControllerMockMvc.perform(get("/api/project/{project}/imageconsultation/count.json", imageInstance1.getProject().getId())
                        .param("startDate", ""+DateUtils.addDays(new Date(), -1).getTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("0"));

        restImageConsultationControllerMockMvc.perform(get("/api/project/{project}/imageconsultation/count.json", imageInstance1.getProject().getId())
                        .param("startDate", ""+DateUtils.addDays(new Date(), -10).getTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("1"));

    }
}
