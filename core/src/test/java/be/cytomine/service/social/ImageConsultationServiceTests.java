package be.cytomine.service.social;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.PersistentImageConsultation;
import be.cytomine.domain.social.PersistentUserPosition;
import be.cytomine.dto.image.AreaDTO;
import be.cytomine.repositorynosql.social.PersistentImageConsultationRepository;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.image.SliceCoordinatesService;
import be.cytomine.utils.JsonObject;
import com.mongodb.client.MongoClient;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static be.cytomine.service.social.UserPositionServiceTests.USER_VIEW;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ImageConsultationServiceTests {

    @Autowired
    ImageConsultationService imageConsultationService;

    @Autowired
    SequenceService sequenceService;

    @Autowired
    PersistentImageConsultationRepository persistentImageConsultationRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    MongoClient mongoClient;

    @Autowired
    UserPositionService userPositionService;

    @Autowired
    SliceCoordinatesService sliceCoordinatesService;

    @BeforeEach
    public void cleanDB() {
        persistentImageConsultationRepository.deleteAll();
    }

    PersistentImageConsultation given_a_persistent_image_consultation(User user, ImageInstance imageInstance, Date created) {
        given_a_persistent_user_position(created,  (User)user, sliceCoordinatesService.getReferenceSlice(imageInstance), USER_VIEW);
        return imageConsultationService.add(user, imageInstance.getId(), "xxx", "mode", created);
    }

    PersistentUserPosition given_a_persistent_user_position(Date creation, User user, SliceInstance sliceInstance, AreaDTO areaDTO) {
        PersistentUserPosition connection =
                userPositionService.add(
                        creation,
                        user,
                        sliceInstance,
                        sliceInstance.getImage(),
                        areaDTO,
                        1,
                        5.0,
                        false
                );
        return connection;
    }

    @Test
    void creation_and_close() {
        User user = builder.given_superadmin();
        ImageInstance imageInstance = builder.given_a_slice_instance().getImage();
        Date before = new Date(new Date().getTime()-1000);
        PersistentImageConsultation consultation = given_a_persistent_image_consultation(user, imageInstance, new Date());
        AssertionsForClassTypes.assertThat(consultation).isNotNull();
        AssertionsForClassTypes.assertThat(consultation.getTime()).isNull();
        Date after = new Date();

        consultation = given_a_persistent_image_consultation(user, imageInstance, new Date());


        Optional<PersistentImageConsultation> connectionOptional =  persistentImageConsultationRepository.findAllByUserAndImageAndCreatedLessThan(builder.given_superadmin().getId(), imageInstance.getId(), after,
                PageRequest.of(0, 1, Sort.Direction.DESC, "created")).stream().findFirst();
        assertThat(connectionOptional).isPresent();
        AssertionsForClassTypes.assertThat(connectionOptional.get().getSession()).isEqualTo("xxx");
        AssertionsForClassTypes.assertThat(connectionOptional.get().getTime()).isEqualTo(0);
    }


    @Test
    void fill_project_connection_update_annotations_counter() {
        User user = builder.given_superadmin();
        Project projet = builder.given_a_project();
        ImageInstance imageInstance = builder.given_a_slice_instance(projet).getImage();

        PersistentImageConsultation consultation = given_a_persistent_image_consultation(user, imageInstance, DateUtils.addSeconds(new Date(), -10));
        assertThat(consultation.getCountCreatedAnnotations()).isNull();

        UserAnnotation annotation = builder.given_a_not_persisted_user_annotation(projet);
        annotation.setImage(imageInstance);
        builder.persistAndReturn(annotation);

        consultation = given_a_persistent_image_consultation(user, imageInstance, DateUtils.addSeconds(new Date(), 1));
        consultation = given_a_persistent_image_consultation(user, imageInstance, DateUtils.addSeconds(new Date(), 10));
        Page<PersistentImageConsultation> allByUserAndProject =
                persistentImageConsultationRepository.findAllByProjectAndUser(projet.getId(), user.getId(), PageRequest.of(0, 50, Sort.Direction.DESC, "created"));

        for (PersistentImageConsultation c : allByUserAndProject) {
            System.out.println("Annotations: " + c.getCountCreatedAnnotations());
        }
        assertThat(allByUserAndProject.getTotalElements()).isEqualTo(3);
        assertThat(allByUserAndProject.getContent().get(0).getCountCreatedAnnotations()).isNull();
        assertThat(allByUserAndProject.getContent().get(1).getCountCreatedAnnotations()).isEqualTo(0);
        assertThat(allByUserAndProject.getContent().get(2).getCountCreatedAnnotations()).isEqualTo(1);
    }

    @Test
    void list_image_consultation_by_project_and_user_do_not_distinct_image() {
        User user = builder.given_superadmin();
        ImageInstance imageInstance = builder.given_a_slice_instance().getImage();

        given_a_persistent_image_consultation(user, imageInstance, new Date());

        Page<PersistentImageConsultation> results = imageConsultationService.listImageConsultationByProjectAndUserNoImageDistinct(imageInstance.getProject(), user, 0, 0);
        assertThat(results).hasSize(1);
    }

    @Test
    void list_image_consultation_by_project_and_user_with_distinct_image() {
        User user = builder.given_superadmin();
        ImageInstance imageInstance1 = builder.given_a_slice_instance().getImage();
        ImageInstance imageInstance2 = builder.given_a_slice_instance(imageInstance1.getProject()).getImage();

        given_a_persistent_image_consultation(user, imageInstance1, new Date());
        given_a_persistent_image_consultation(user, imageInstance1, new Date());


        List<JsonObject> results = imageConsultationService.listImageConsultationByProjectAndUserWithDistinctImage(
                imageInstance1.getProject(), user);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("imageName")).isEqualTo(imageInstance1.getBlindInstanceFilename());

        given_a_persistent_image_consultation(user, imageInstance2, new Date());

        results = imageConsultationService.listImageConsultationByProjectAndUserWithDistinctImage(
                imageInstance1.getProject(), user);
        assertThat(results).hasSize(2);

    }

    @Test
    void list_image_of_users_by_project() {
        User user1 = builder.given_superadmin();
        User user2 = builder.given_a_user();

        ImageInstance imageInstance1 = builder.given_a_slice_instance().getImage();
        ImageInstance imageInstance2 = builder.given_a_slice_instance(imageInstance1.getProject()).getImage();

        given_a_persistent_image_consultation(user1, imageInstance1, DateUtils.addDays(new Date(), -3));
        given_a_persistent_image_consultation(user1, imageInstance2, DateUtils.addDays(new Date(), -2));
        given_a_persistent_image_consultation(user2, imageInstance1, DateUtils.addDays(new Date(), -1));

        List<JsonObject> results = imageConsultationService.lastImageOfUsersByProject(imageInstance1.getProject(),
                List.of(user1.getId(), user2.getId()),
                "created", "desc", 0L, 0L);

        System.out.println(results);
        assertThat(results).hasSize(2);

        assertThat(results.get(0).get("user")).isEqualTo(user2.getId());
        assertThat(results.get(0).get("image")).isEqualTo(imageInstance1.getId());
        assertThat(results.get(1).get("user")).isEqualTo(user1.getId());
        assertThat(results.get(1).get("image")).isEqualTo(imageInstance2.getId());


        results = imageConsultationService.lastImageOfUsersByProject(imageInstance1.getProject(),
                List.of(user1.getId()),
                "created", "desc", 0L, 0L);
        assertThat(results).hasSize(1);

        results = imageConsultationService.lastImageOfUsersByProject(imageInstance1.getProject(),
                null,
                "created", "desc", 0L, 0L);
        assertThat(results).hasSize(2);

        results = imageConsultationService.lastImageOfUsersByProject(imageInstance1.getProject(),
                null,
                "created", "desc", 1L, 0L);
        assertThat(results).hasSize(1);

    }



    @Test
    void last_image_of_users_for_a_project() {
        User user1 = builder.given_superadmin();
        User user2 = builder.given_a_user();
        User userWithNoConsultation = builder.given_a_user();

        ImageInstance imageInstance1 = builder.given_a_slice_instance().getImage();
        ImageInstance imageInstance2 = builder.given_a_slice_instance(imageInstance1.getProject()).getImage();


        List<JsonObject> results = imageConsultationService.lastImageOfGivenUsersByProject(
                imageInstance1.getProject(), List.of(user1.getId(), user2.getId()), "created", "desc", 0L, 0L);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("image")).isNull();
        assertThat(results.get(1).get("image")).isNull();

        given_a_persistent_image_consultation(user1, imageInstance1, DateUtils.addDays(new Date(), -3));
        given_a_persistent_image_consultation(user1, imageInstance2, DateUtils.addDays(new Date(), -2));
        given_a_persistent_image_consultation(user2, imageInstance1, DateUtils.addDays(new Date(), -1));


        results = imageConsultationService.lastImageOfGivenUsersByProject(
                imageInstance1.getProject(), List.of(user1.getId(), user2.getId(),userWithNoConsultation.getId()), "created", "desc", 0L, 0L);
        assertThat(results).hasSize(3);
        assertThat(results.get(0).get("user")).isEqualTo(user2.getId());
        assertThat(results.get(0).get("image")).isEqualTo(imageInstance1.getId());
        assertThat(results.get(1).get("user")).isEqualTo(user1.getId());
        assertThat(results.get(1).get("image")).isEqualTo(imageInstance2.getId());
        assertThat(results.get(2).get("user")).isEqualTo(userWithNoConsultation.getId());
        assertThat(results.get(2).get("image")).isNull();

        results = imageConsultationService.lastImageOfGivenUsersByProject(
                imageInstance1.getProject(), List.of(user1.getId(), user2.getId()), "created", "desc", 1L, 0L);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("user")).isEqualTo(user2.getId());
        assertThat(results.get(0).get("image")).isEqualTo(imageInstance1.getId());
    }


    @Test
    void last_image_of_users_for_a_project_between_range() {
        User user1 = builder.given_superadmin();
        User user2 = builder.given_a_user();

        ImageInstance imageInstance1 = builder.given_a_slice_instance().getImage();
        ImageInstance imageInstance2 = builder.given_a_slice_instance(imageInstance1.getProject()).getImage();


        List<JsonObject> results = imageConsultationService.getImagesOfUsersByProjectBetween(user1.getId(), imageInstance1.getProject().getId(), null, null);
        assertThat(results).hasSize(0);

        given_a_persistent_image_consultation(user1, imageInstance1, DateUtils.addDays(new Date(), -10));
        given_a_persistent_image_consultation(user1, imageInstance2, DateUtils.addDays(new Date(), -5));
        given_a_persistent_image_consultation(user2, imageInstance1, DateUtils.addDays(new Date(), -1));


        results = imageConsultationService.getImagesOfUsersByProjectBetween(
                user1.getId(), imageInstance1.getProject().getId(), DateUtils.addDays(new Date(), -20), DateUtils.addDays(new Date(), 10));
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("user")).isEqualTo(user1.getId());
        assertThat(results.get(0).get("image")).isEqualTo(imageInstance2.getId());
        assertThat(results.get(1).get("user")).isEqualTo(user1.getId());
        assertThat(results.get(1).get("image")).isEqualTo(imageInstance1.getId());


        results = imageConsultationService.getImagesOfUsersByProjectBetween(
                user1.getId(), imageInstance1.getProject().getId(), DateUtils.addDays(new Date(), -6), DateUtils.addDays(new Date(), 10));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("user")).isEqualTo(user1.getId());
        assertThat(results.get(0).get("image")).isEqualTo(imageInstance2.getId());

        results = imageConsultationService.getImagesOfUsersByProjectBetween(
                user1.getId(), imageInstance1.getProject().getId(), DateUtils.addDays(new Date(), -20), DateUtils.addDays(new Date(), -6));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("user")).isEqualTo(user1.getId());
        assertThat(results.get(0).get("image")).isEqualTo(imageInstance1.getId());



    }


    @Test
    void resume_by_project_for_a_user() {
        User user1 = builder.given_superadmin();
        User user2 = builder.given_a_user();

        ImageInstance imageInstance1 = builder.given_a_slice_instance().getImage();
        ImageInstance imageInstance2 = builder.given_a_slice_instance(imageInstance1.getProject()).getImage();


        List<JsonObject> results = imageConsultationService.getImagesOfUsersByProjectBetween(user1.getId(), imageInstance1.getProject().getId(), null, null);
        assertThat(results).hasSize(0);

        given_a_persistent_image_consultation(user1, imageInstance1, DateUtils.addDays(new Date(), -10));
        given_a_persistent_image_consultation(user1, imageInstance1, DateUtils.addDays(new Date(), -7));
        given_a_persistent_image_consultation(user1, imageInstance2, DateUtils.addDays(new Date(), -5));
        given_a_persistent_image_consultation(user2, imageInstance1, DateUtils.addDays(new Date(), -1));


        results = imageConsultationService.resumeByUserAndProject(user1.getId(), imageInstance1.getProject().getId());

        System.out.println(results);

        assertThat(results).hasSize(2);

        Optional<JsonObject> user1image1 = results.stream().
                filter(x -> x.get("user").equals(user1.getId()) && x.get("image").equals(imageInstance1.getId())).findFirst();
        assertThat(user1image1).isPresent();
        assertThat(user1image1.get().get("frequency")).isEqualTo(2);

        Optional<JsonObject> user1image2 = results.stream().
                filter(x -> x.get("user").equals(user1.getId()) && x.get("image").equals(imageInstance2.getId())).findFirst();
        assertThat(user1image2).isPresent();
        assertThat(user1image2.get().get("frequency")).isEqualTo(1);
    }




    @Test
    void total_number_of_consultation_by_project_with_dates() {
        Project projet = builder.given_a_project();
        User user1 = builder.given_superadmin();
        User anotherUser = builder.given_a_user();

        ImageInstance imageInstance1 = builder.given_a_slice_instance(projet).getImage();
        ImageInstance imageInstance2 = builder.given_a_slice_instance(projet).getImage();

        Date noConnectionBefore = DateUtils.addDays(new Date(), -100);
        given_a_persistent_image_consultation(user1, imageInstance1, DateUtils.addDays(new Date(), -10));
        given_a_persistent_image_consultation(user1, imageInstance1, DateUtils.addDays(new Date(), -10));
        Date twoConnectionBefore = DateUtils.addDays(new Date(), -5);
        given_a_persistent_image_consultation(anotherUser, imageInstance1, DateUtils.addDays(new Date(), -1));
        Date threeConnectionBefore = new Date();

        List<JsonObject> results;

        AssertionsForClassTypes.assertThat(imageConsultationService.countByProject(projet, null, null))
                .isEqualTo(3);
        AssertionsForClassTypes.assertThat(imageConsultationService.countByProject(projet, noConnectionBefore.getTime(), twoConnectionBefore.getTime()))
                .isEqualTo(2);
        AssertionsForClassTypes.assertThat(imageConsultationService.countByProject(projet, twoConnectionBefore.getTime(), threeConnectionBefore.getTime()))
                .isEqualTo(1);
    }
}
