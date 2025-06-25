package be.cytomine.service.ontology;

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
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.security.User;
import be.cytomine.dto.ReviewedAnnotationStatsEntry;
import be.cytomine.dto.UserTermMapping;
import be.cytomine.dto.annotation.AnnotationLight;
import be.cytomine.dto.annotation.AnnotationResult;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ReviewedAnnotationListing;
import be.cytomine.repository.ontology.ReviewedAnnotationRepository;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ReviewedAnnotationServiceTests {

    @Autowired
    ReviewedAnnotationService reviewedAnnotationService;

    @Autowired
    ReviewedAnnotationRepository reviewedAnnotationRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    ImageInstanceService imageInstanceService;

    @Test
    void get_reviewedAnnotation_with_success() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        assertThat(reviewedAnnotation).isEqualTo(reviewedAnnotationService.get(reviewedAnnotation.getId()));
    }

    @Test
    void get_unexisting_reviewedAnnotation_return_null() {
        assertThat(reviewedAnnotationService.get(0L)).isNull();
    }

    @Test
    void find_reviewedAnnotation_with_success() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        assertThat(reviewedAnnotationService.find(reviewedAnnotation.getId()).isPresent());
        assertThat(reviewedAnnotation).isEqualTo(reviewedAnnotationService.find(reviewedAnnotation.getId()).get());
    }

    @Test
    void find_unexisting_reviewedAnnotation_return_empty() {
        assertThat(reviewedAnnotationService.find(0L)).isEmpty();
    }

    @Test
    void count_reviewedAnnotation_with_success() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        assertThat(reviewedAnnotationService.count((User)reviewedAnnotation.getUser())).isGreaterThanOrEqualTo(1L);
        assertThat(reviewedAnnotationService.count(builder.given_a_user())).isEqualTo(0);
    }


    @Test
    void count_by_project_with_date() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();

        assertThat(reviewedAnnotationService.countByProject(
                reviewedAnnotation.getProject(),
                DateUtils.addDays(reviewedAnnotation.getCreated(),-30),
                DateUtils.addDays(reviewedAnnotation.getCreated(),30)))
                .isEqualTo(1);

        assertThat(reviewedAnnotationService.countByProject(
                reviewedAnnotation.getProject(),
                DateUtils.addDays(reviewedAnnotation.getCreated(),-30),
                DateUtils.addDays(reviewedAnnotation.getCreated(),-15)))
                .isEqualTo(0);

        assertThat(reviewedAnnotationService.countByProject(
                reviewedAnnotation.getProject(),
                DateUtils.addDays(reviewedAnnotation.getCreated(),15),
                DateUtils.addDays(reviewedAnnotation.getCreated(),30)))
                .isEqualTo(0);
    }
    @Test

    void count_by_project_with_terms() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        reviewedAnnotation.getTerms().clear();

        ReviewedAnnotation reviewedAnnotationWithTerms = builder.given_a_reviewed_annotation();
        reviewedAnnotation.getTerms().add(builder.given_a_term(reviewedAnnotationWithTerms.getProject().getOntology()));

        assertThat(reviewedAnnotationService.countByProjectAndWithTerms(reviewedAnnotation.getProject())).isEqualTo(0);
        assertThat(reviewedAnnotationService.countByProjectAndWithTerms(reviewedAnnotationWithTerms.getProject())).isEqualTo(1);
    }

    @Test
    void list_all_for_project() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        ReviewedAnnotation reviewedAnnotationFromAnotherProject = builder.given_a_reviewed_annotation();

        Optional<AnnotationLight> first = reviewedAnnotationService.list(reviewedAnnotation.getProject(), new ArrayList<>(ReviewedAnnotationListing.availableColumnsDefault))
                .stream().filter(x -> ((AnnotationResult)x).get("id").equals(reviewedAnnotation.getId())).findFirst();
        assertThat(first).isPresent();

        first = reviewedAnnotationService.list(reviewedAnnotation.getProject(), new ArrayList<>(ReviewedAnnotationListing.availableColumnsDefault))
                .stream().filter(x -> ((AnnotationResult)x).get("id").equals(reviewedAnnotationFromAnotherProject.getId())).findFirst();
        assertThat(first).isEmpty();
    }


    @Test
    void stats_group_by_user() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        User reviewer = reviewedAnnotation.getReviewUser();
        User anotherUser = builder.given_a_user();

        List<ReviewedAnnotationStatsEntry> results = reviewedAnnotationService.statsGroupByUser(reviewedAnnotation.getImage());

        Optional<ReviewedAnnotationStatsEntry> resultForUser = results.stream().filter(x -> x.getUser().equals(reviewer.getId())).findFirst();
        assertThat(resultForUser).isPresent();
        assertThat(resultForUser.get().getReviewed()).isGreaterThanOrEqualTo(1);
        assertThat(resultForUser.get().getAll()).isGreaterThanOrEqualTo(1);

        resultForUser = results.stream().filter(x -> x.getUser().equals(anotherUser.getId())).findFirst();
        assertThat(resultForUser).isEmpty();
    }


    static Map<String, String> POLYGONES = Map.of(
            "a", "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))",
            "b", "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))",
            "c", "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))",
            "d", "POLYGON ((4 4,8 4, 8 7,4 7,4 4))",
            "e", "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"
    ); //e intersect a,b and c

    @Test
    void list_included() throws ParseException {
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();

        Term term1 = builder.given_a_term(sliceInstance.getProject().getOntology());
        Term term2 = builder.given_a_term(sliceInstance.getProject().getOntology());

        ReviewedAnnotation a1 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("a"),user1,term1);
        ReviewedAnnotation a2 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("b"),user1,term2);
        ReviewedAnnotation a3 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("c"),user2,term1);
        ReviewedAnnotation a4 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("d"),user2,term2);

        List<AnnotationResult> list;
        List<Long> ids;

        list = reviewedAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                null,
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).contains(a1.getId());
        assertThat(ids).contains(a2.getId());
        assertThat(ids).contains(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());

        list = reviewedAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                List.of(term1.getId(), term2.getId()),
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).contains(a1.getId());
        assertThat(ids).contains(a2.getId());
        assertThat(ids).contains(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());


        list = reviewedAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                List.of(term1.getId()),
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).contains(a1.getId());
        assertThat(ids).doesNotContain(a2.getId());
        assertThat(ids).contains(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());

        ReviewedAnnotation a5 = builder.given_a_reviewed_annotation(sliceInstance, "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",builder.given_superadmin(),term2);

        list = reviewedAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                List.of(term1.getId(), term2.getId()),
                a5,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).contains(a1.getId());
        assertThat(ids).contains(a2.getId());
        assertThat(ids).contains(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());
        assertThat(ids).doesNotContain(a5.getId());
    }


    @Test
    void list_terms_for_reviewed() throws ParseException {
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ReviewedAnnotation reviewedAnnotation
                = builder.given_a_reviewed_annotation(sliceInstance, "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", builder.given_superadmin(), builder.given_a_term(sliceInstance.getProject().getOntology()));
        reviewedAnnotation.getImage().setReviewUser(reviewedAnnotation.getReviewUser());

        List<UserTermMapping> terms = reviewedAnnotationService.listTerms(reviewedAnnotation);

        assertThat(terms.stream().map(UserTermMapping::getUser)).contains(reviewedAnnotation.getReviewUser().getId());
        assertThat(terms.stream().map(UserTermMapping::getTerm)).contains(reviewedAnnotation.getTerms().get(0).getId());
    }



    @Test
    void add_valid_reviewed_annotation_with_success() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_not_persisted_reviewed_annotation();
        CommandResponse commandResponse = reviewedAnnotationService.add(reviewedAnnotation.toJsonObject()
                .withChange("term", builder.given_a_term(reviewedAnnotation.getProject().getOntology()).getId()));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(reviewedAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        ReviewedAnnotation created = reviewedAnnotationService.find(commandResponse.getObject().getId()).get();

        commandService.undo();

        AssertionsForClassTypes.assertThat(reviewedAnnotationService.find(commandResponse.getObject().getId())).isEmpty();

        commandService.redo();

        AssertionsForClassTypes.assertThat(reviewedAnnotationService.find(commandResponse.getObject().getId())).isPresent();
    }


    @Test
    void add_valid_reviewed_annotation_is_refuse_if_already_exists() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_not_persisted_reviewed_annotation();
        builder.persistAndReturn(reviewedAnnotation);

        Assertions.assertThrows(AlreadyExistException.class, () -> {
            reviewedAnnotationService.add(reviewedAnnotation.toJsonObject()
                    .withChange("id", null));
        });
    }

    @Test
    void add_reviewed_annotation_multiline() throws ParseException {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_not_persisted_reviewed_annotation();
        reviewedAnnotation.setLocation(new WKTReader().read(
                "LINESTRING( 181.05636403199998 324.87936288, 208.31216076799996 303.464094016)"
        ));
        JsonObject jsonObject = reviewedAnnotation.toJsonObject();
        CommandResponse commandResponse = reviewedAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(((ReviewedAnnotation)commandResponse.getObject()).getLocation().toText())
                .isEqualTo("LINESTRING (181.05636403199998 324.87936288, 208.31216076799996 303.464094016)");
        assertThat(((ReviewedAnnotation)commandResponse.getObject()).getWktLocation())
                .isEqualTo("LINESTRING (181.05636403199998 324.87936288, 208.31216076799996 303.464094016)");
    }


    @Test
    void edit_valid_reviewed_annotation_with_success() throws ParseException {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_not_persisted_reviewed_annotation();
        String oldLocation = "POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))";
        String newLocation = "POLYGON ((2107 2160, 2047 2074, 1983 2168, 2107 2160))";

        reviewedAnnotation.setLocation(new WKTReader().read(oldLocation));
        builder.persistAndReturn(reviewedAnnotation);
        CommandResponse commandResponse = reviewedAnnotationService.update(reviewedAnnotation,
                reviewedAnnotation.toJsonObject().withChange(
                        "location", newLocation)
        );

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(reviewedAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        ReviewedAnnotation edited = reviewedAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getLocation().toText()).isEqualTo(newLocation);
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(newLocation);

        commandService.undo();

        edited = reviewedAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(oldLocation);

        commandService.redo();

        edited = reviewedAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(newLocation);

    }

    @Test
    void edit_reviewed_annotation_empty_polygon() throws ParseException {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        JsonObject jsonObject = reviewedAnnotation.toJsonObject();
        jsonObject.put("location", "POINT (BAD GEOMETRY)");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            reviewedAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void delete_reviewed_annotation_with_success() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();

        CommandResponse commandResponse = reviewedAnnotationService.delete(reviewedAnnotation, null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(reviewedAnnotationService.find(reviewedAnnotation.getId()).isEmpty());

        commandService.undo();

        AssertionsForClassTypes.assertThat(reviewedAnnotationService.find(reviewedAnnotation.getId()).isPresent());

        commandService.redo();

        AssertionsForClassTypes.assertThat(reviewedAnnotationService.find(reviewedAnnotation.getId()).isEmpty());
    }


    @Test
    void delete_reviewed_annotation_with_terms() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_not_persisted_reviewed_annotation();
        Term term1 = builder.given_a_term(reviewedAnnotation.getProject().getOntology());
        Term term2 = builder.given_a_term(reviewedAnnotation.getProject().getOntology());

        JsonObject jsonObject = reviewedAnnotation.toJsonObject();
        jsonObject.put("term", List.of(term1.getId(), term2.getId()));

        CommandResponse commandResponse = reviewedAnnotationService.add(jsonObject);

        commandResponse = reviewedAnnotationService.delete((ReviewedAnnotation)commandResponse.getObject(), null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
    }



    @Test
    void image_reviewing_with_new_reviewed_annotation() {

        ImageInstance image = builder.given_an_image_instance();
        imageInstanceService.startReview(image);
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(image.getProject());
        userAnnotation.setImage(image);
        builder.persistAndReturn(userAnnotation);
        reviewedAnnotationService.reviewAnnotation(userAnnotation.getId(), null);
    }

    @Test
    void lock_image_reviewing_for_other_users() {

        ImageInstance image = builder.given_an_image_instance();
        imageInstanceService.startReview(image);
        image.setReviewUser(builder.given_a_user());
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(image.getProject());
        userAnnotation.setImage(image);
        builder.persistAndReturn(userAnnotation);

        Assertions.assertThrows(WrongArgumentException.class, () -> {
            reviewedAnnotationService.reviewAnnotation(userAnnotation.getId(), null);
        });
    }


    @Test
    void lock_image_reviewing_if_review_stop() {

        ImageInstance image = builder.given_an_image_instance();
        imageInstanceService.startReview(image);
        imageInstanceService.stopReview(image, false);

        Assertions.assertThrows(WrongArgumentException.class, () -> {
            UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(image.getProject());
            userAnnotation.setImage(image);
            builder.persistAndReturn(userAnnotation);
            reviewedAnnotationService.reviewAnnotation(userAnnotation.getId(), null);
        });
    }



    @Test
    void lock_image_reviewing_if_review_has_never_been_started() {

        ImageInstance image = builder.given_an_image_instance();
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(image.getProject());
        userAnnotation.setImage(image);
        builder.persistAndReturn(userAnnotation);

        Assertions.assertThrows(WrongArgumentException.class, () -> {
            reviewedAnnotationService.reviewAnnotation(userAnnotation.getId(), null);
        });
    }


    @Test
    void add_review_with_terms() {

        ImageInstance image = builder.given_an_image_instance();
        imageInstanceService.startReview(image);

        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(image.getProject());
        userAnnotation.setImage(image);
        builder.persistAndReturn(userAnnotation);
        AnnotationTerm annotationTerm = builder.given_an_annotation_term(userAnnotation);

        entityManager.refresh(userAnnotation);

        CommandResponse response = reviewedAnnotationService.reviewAnnotation(userAnnotation.getId(), null);
        entityManager.refresh(((ReviewedAnnotation)response.getObject()));
        assertThat(((ReviewedAnnotation)response.getObject()).getTerms()).containsExactly(annotationTerm.getTerm());

    }

    @Test
    void add_review_with_terms_with_bad_ontology() {

        ImageInstance image = builder.given_an_image_instance();
        imageInstanceService.startReview(image);

        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(image.getProject());
        userAnnotation.setImage(image);
        builder.persistAndReturn(userAnnotation);

        Assertions.assertThrows(WrongArgumentException.class, () -> {
                Term termFromAnotherOntology = builder.given_a_term(builder.given_an_ontology());
                reviewedAnnotationService.reviewAnnotation(userAnnotation.getId(), List.of(termFromAnotherOntology.getId()));
        });

    }

    @Test
    public void remove_review_for_annotation() {
        ReviewedAnnotation annotation = builder.given_a_reviewed_annotation();
        reviewedAnnotationService.unReviewAnnotation(annotation.getParentIdent());
    }

    @Test
    public void remove_review_for_unreviewed_annotation_fails() {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            reviewedAnnotationService.unReviewAnnotation(userAnnotation.getId());
        });
    }


    @Test
    public void remove_review_by_another_user_than_reviewer_fails() {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();

        imageInstanceService.startReview(userAnnotation.getImage());
        userAnnotation.getImage().setReviewUser(builder.given_a_user());

        Assertions.assertThrows(WrongArgumentException.class, () -> {
            reviewedAnnotationService.unReviewAnnotation(userAnnotation.getId());
        });
    }

    @Test
    void add_review_and_update_geometry() {

        ImageInstance image = builder.given_an_image_instance();
        imageInstanceService.startReview(image);

        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(image.getProject());
        userAnnotation.setImage(image);
        builder.persistAndReturn(userAnnotation);


        CommandResponse response = reviewedAnnotationService.reviewAnnotation(userAnnotation.getId(), null);


        reviewedAnnotationService.edit(
                ((ReviewedAnnotation)response.getObject()).toJsonObject().withChange("location", "POLYGON ((19830 21680, 21070 21600, 20470 20740, 19830 21680))"), false);
        assertThat(((ReviewedAnnotation)response.getObject()).getWktLocation()).isEqualTo("POLYGON ((19830 21680, 21070 21600, 20470 20740, 19830 21680))");
    }

    @Test
    void add_review_delete_parent_and_reject() {

        ImageInstance image = builder.given_an_image_instance();
        imageInstanceService.startReview(image);

        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(image.getProject());
        userAnnotation.setImage(image);
        builder.persistAndReturn(userAnnotation);
        CommandResponse response = reviewedAnnotationService.reviewAnnotation(userAnnotation.getId(), null);

        entityManager.remove(userAnnotation);

        CommandResponse commandResponse = reviewedAnnotationService.unReviewAnnotation(userAnnotation.getId());
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void add_review_already_exists() {
       ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
       Assertions.assertThrows(WrongArgumentException.class, () -> {
           reviewedAnnotationService.reviewAnnotation(reviewedAnnotation.getParentIdent(), null);
       });
    }


    @Test
    void review_all_user_layers() {
        ImageInstance image = builder.given_an_image_instance();
        imageInstanceService.startReview(image);
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(image.getProject());
        userAnnotation.setImage(image);
        builder.persistAndReturn(userAnnotation);

        List<Long> ids = reviewedAnnotationService.reviewLayer(image.getId(), List.of(userAnnotation.getUser().getId()), null);
        assertThat(ids).hasSize(1);
        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isPresent();
    }


    @Test
    void review_all_user_layers_not_in_review_mode() {
        ImageInstance image = builder.given_an_image_instance();
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            reviewedAnnotationService.reviewLayer(image.getId(), List.of(image.getUser().getId()), null);
        });
    }

    @Test
    void review_all_user_layers_user_is_not_reviewer() {
        ImageInstance image = builder.given_an_image_instance();
        imageInstanceService.startReview(image);
        image.setReviewUser(builder.given_a_user());
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            reviewedAnnotationService.reviewLayer(image.getId(), List.of(image.getUser().getId()), null);
        });
    }

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    UserAnnotationRepository userAnnotationRepository;

    @Test
    void annotation_reviewed_counter_for_user_annotation() {

        ImageInstance image = builder.given_an_image_instance();
        imageInstanceService.startReview(image);
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.setImage(image);
        userAnnotation.setProject(image.getProject());
        builder.persistAndReturn(userAnnotation);

        assertThat(userAnnotation.getCountReviewedAnnotations()).isEqualTo(0);
        assertThat(image.getCountImageReviewedAnnotations()).isEqualTo(0);
        assertThat(image.getProject().getCountReviewedAnnotations()).isEqualTo(0);

        reviewedAnnotationService.reviewAnnotation(userAnnotation.getId(), null);

        entityManager.refresh(userAnnotation);
        entityManager.refresh(image);
        entityManager.refresh(image.getProject());

        assertThat(userAnnotation.getCountReviewedAnnotations()).isEqualTo(1);
        assertThat(image.getCountImageReviewedAnnotations()).isEqualTo(1);
        assertThat(image.getProject().getCountReviewedAnnotations()).isEqualTo(1);

        reviewedAnnotationService.unReviewAnnotation(userAnnotation.getId());

        entityManager.refresh(userAnnotation);
        entityManager.refresh(image);
        entityManager.refresh(image.getProject());

        assertThat(userAnnotation.getCountReviewedAnnotations()).isEqualTo(0);
        assertThat(image.getCountImageReviewedAnnotations()).isEqualTo(0);
        assertThat(image.getProject().getCountReviewedAnnotations()).isEqualTo(0);
    }

    @Test
    void do_annotation_corrections() throws ParseException {

        ReviewedAnnotation based = builder.given_a_reviewed_annotation();
        based.setLocation(new WKTReader().read("POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"));
        builder.persistAndReturn(based);

        ReviewedAnnotation anotherAnnotation = builder.given_a_reviewed_annotation();
        anotherAnnotation.setLocation(new WKTReader().read("POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))"));
        anotherAnnotation.setImage(based.getImage());
        builder.persistAndReturn(anotherAnnotation);

        CommandResponse commandResponse = reviewedAnnotationService.doCorrectReviewedAnnotation(List.of(based.getId(), anotherAnnotation.getId()), "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))", false);

        assertThat(reviewedAnnotationRepository.findById(based.getId())).isPresent();
        assertThat(reviewedAnnotationRepository.findById(based.getId()).get().getLocation().equals(new WKTReader().read("POLYGON ((0 0, 0 10000, 10000 10000, 10000 0, 0 0))"))).isTrue();

        assertThat(reviewedAnnotationRepository.findById(anotherAnnotation.getId())).isEmpty();
    }


    @Test
    void do_annotation_corrections_with_remove() throws ParseException {

        ReviewedAnnotation based = builder.given_a_reviewed_annotation();
        based.setLocation(new WKTReader().read("POLYGON ((0 0, 0 10000, 10000 10000, 10000 0, 0 0))"));
        builder.persistAndReturn(based);

        ReviewedAnnotation anotherAnnotation = builder.given_a_reviewed_annotation();
        anotherAnnotation.setLocation(new WKTReader().read("POLYGON ((10000 10000, 10000 30000, 30000 30000, 30000 10000, 10000 10000))"));
        anotherAnnotation.setImage(based.getImage());
        builder.persistAndReturn(anotherAnnotation);

        reviewedAnnotationService.doCorrectReviewedAnnotation(List.of(based.getId(), anotherAnnotation.getId()), "POLYGON ((0 5000, 2000 5000, 2000 2000, 0 2000, 0 5000))", true);

        assertThat(reviewedAnnotationRepository.findById(based.getId())).isPresent();
        assertThat(reviewedAnnotationRepository.findById(based.getId()).get().getLocation().equals(new WKTReader().read("POLYGON ((0 0, 0 2000, 2000 2000, 2000 5000, 0 5000, 0 10000, 10000 10000, 10000 0, 0 0))"))).isTrue();

        assertThat(reviewedAnnotationRepository.findById(anotherAnnotation.getId())).isPresent();
    }


}
