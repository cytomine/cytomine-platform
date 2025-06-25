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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.TestUtils;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Description;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.ontology.AnnotationTrack;
import be.cytomine.domain.ontology.SharedAnnotation;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.security.User;
import be.cytomine.dto.annotation.AnnotationLight;
import be.cytomine.dto.annotation.AnnotationResult;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.service.CommandService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class UserAnnotationServiceTests {

    @Autowired
    UserAnnotationService userAnnotationService;

    @Autowired
    UserAnnotationRepository userAnnotationRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    EntityManager entityManager;

    @Test
    void get_userAnnotation_with_success() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        assertThat(userAnnotation).isEqualTo(userAnnotationService.get(userAnnotation.getId()));
    }

    @Test
    void get_unexisting_userAnnotation_return_null() {
        assertThat(userAnnotationService.get(0L)).isNull();
    }

    @Test
    void find_userAnnotation_with_success() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        assertThat(userAnnotationService.find(userAnnotation.getId()).isPresent());
        assertThat(userAnnotation).isEqualTo(userAnnotationService.find(userAnnotation.getId()).get());
    }

    @Test
    void find_unexisting_userAnnotation_return_empty() {
        assertThat(userAnnotationService.find(0L)).isEmpty();
    }


    static Map<String, String> POLYGONES = Map.of(
            "a", "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))",
            "b", "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))",
            "c", "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))",
            "d", "POLYGON ((4 4,8 4, 8 7,4 7,4 4))",
            "e", "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"
            //e intersect a,b and c
    );

    @Test
    void list_included() throws ParseException {
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();

        Term term1 = builder.given_a_term(sliceInstance.getProject().getOntology());
        Term term2 = builder.given_a_term(sliceInstance.getProject().getOntology());

        UserAnnotation a1 = builder.given_a_user_annotation(sliceInstance, POLYGONES.get("a"),user1,term1);
        UserAnnotation a2 = builder.given_a_user_annotation(sliceInstance, POLYGONES.get("b"),user1,term2);
        UserAnnotation a3 = builder.given_a_user_annotation(sliceInstance, POLYGONES.get("c"),user2,term1);
        UserAnnotation a4 = builder.given_a_user_annotation(sliceInstance, POLYGONES.get("d"),user2,term2);

        List<AnnotationResult> list;
        List<Long> ids;

        list = userAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                user1,
                null,
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).contains(a1.getId());
        assertThat(ids).contains(a2.getId());
        assertThat(ids).doesNotContain(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());

        list = userAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                user2,
                List.of(term1.getId(), term2.getId()),
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).doesNotContain(a1.getId());
        assertThat(ids).doesNotContain(a2.getId());
        assertThat(ids).contains(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());


        list = userAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                user2,
                List.of(term1.getId()),
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).doesNotContain(a1.getId());
        assertThat(ids).doesNotContain(a2.getId());
        assertThat(ids).contains(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());

        UserAnnotation a5 = builder.given_a_user_annotation(sliceInstance, "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",builder.given_superadmin(),term2);

        list = userAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                user1,
                List.of(term1.getId(), term2.getId()),
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).contains(a1.getId());
        assertThat(ids).contains(a2.getId());
        assertThat(ids).doesNotContain(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());
        assertThat(ids).doesNotContain(a5.getId());
    }


    @Test
    void count_by_project() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        assertThat(userAnnotationService.countByProject(userAnnotation.getProject()))
                .isEqualTo(1);
        assertThat(userAnnotationService.countByProject(builder.given_a_project()))
                .isEqualTo(0);
    }

    @Test
    void count_by_project_with_date() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();

        assertThat(userAnnotationService.countByProject(
                userAnnotation.getProject(),
                DateUtils.addDays(userAnnotation.getCreated(),-30),
                DateUtils.addDays(userAnnotation.getCreated(),30)))
                .isEqualTo(1);

        assertThat(userAnnotationService.countByProject(
                userAnnotation.getProject(),
                DateUtils.addDays(userAnnotation.getCreated(),-30),
                DateUtils.addDays(userAnnotation.getCreated(),-15)))
                .isEqualTo(0);

        assertThat(userAnnotationService.countByProject(
                userAnnotation.getProject(),
                DateUtils.addDays(userAnnotation.getCreated(),15),
                DateUtils.addDays(userAnnotation.getCreated(),30)))
                .isEqualTo(0);
    }

    @Test
    void list_all_lights() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();

        Optional<AnnotationLight> first = userAnnotationService.listLight()
                .stream().filter(x -> x.getId().equals(userAnnotation.getId())).findFirst();

        assertThat(first).isPresent();
        assertThat(first.get().getContainer()).isEqualTo(userAnnotation.getProject().getId());
    }

    @Test
    void add_valid_user_annotation_with_success() {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        CommandResponse commandResponse = userAnnotationService.add(userAnnotation.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        UserAnnotation created = userAnnotationService.find(commandResponse.getObject().getId()).get();

        commandService.undo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isEmpty();

        commandService.redo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
    }

    @Test
    void add_valid_guest_annotation_with_success() {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_guest_annotation();
        CommandResponse commandResponse = userAnnotationService.add(userAnnotation.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        UserAnnotation created = userAnnotationService.find(commandResponse.getObject().getId()).get();

        commandService.undo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isEmpty();

        commandService.redo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
    }

    @Test
    void add_big_user_annotation_with_max_number_of_points() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        userAnnotation.setLocation(new WKTReader().read(TestUtils.getResourceFileAsString("dataset/very_big_annotation.txt")));

        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("maxPoint", 100);
        CommandResponse commandResponse = userAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        UserAnnotation created = userAnnotationService.find(commandResponse.getObject().getId()).get();
        assertThat(created.getLocation().getNumPoints()).isLessThanOrEqualTo(100);
    }

    @Test
    void add_too_small_user_annotation() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("location", "POLYGON ((225.73582220103702 306.89723126347087, 225.73582220103702 307.93556995227914, " +
                "226.08028300710947 307.93556995227914, 226.08028300710947 306.89723126347087, " +
                "225.73582220103702 306.89723126347087))");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_user_annotation_multiline() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        userAnnotation.setLocation(new WKTReader().read(
                "LINESTRING( 181.05636403199998 324.87936288, 208.31216076799996 303.464094016)"
        ));

        JsonObject jsonObject = userAnnotation.toJsonObject();
        CommandResponse commandResponse = userAnnotationService.add(jsonObject);

        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(((UserAnnotation)commandResponse.getObject()).getLocation().toText())
                .isEqualTo("LINESTRING (181.05636403199998 324.87936288, 208.31216076799996 303.464094016)");
        assertThat(((UserAnnotation)commandResponse.getObject()).getWktLocation())
                .isEqualTo("LINESTRING (181.05636403199998 324.87936288, 208.31216076799996 303.464094016)");
    }

    @Test
    void add_user_annotation_without_project() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.remove("project");
        CommandResponse commandResponse = userAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200); // project is retrieve from image/slice
    }


    @Test
    void add_valid_user_annotation_with_terms() {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        Term term1 = builder.given_a_term(userAnnotation.getProject().getOntology());
        Term term2 = builder.given_a_term(userAnnotation.getProject().getOntology());

        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("term", List.of(term1.getId(), term2.getId()));

        CommandResponse commandResponse = userAnnotationService.add(jsonObject);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        UserAnnotation created = userAnnotationService.find(commandResponse.getObject().getId()).get();
        entityManager.refresh(created);
        assertThat(created.getTerms()).hasSize(2);

        commandService.undo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isEmpty();

        commandService.redo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        userAnnotation = userAnnotationService.find(commandResponse.getObject().getId()).get();
        entityManager.refresh(userAnnotation);
        assertThat(userAnnotation.terms()).hasSize(2);
    }


    @Test
    void add_user_annotation_bad_geom() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("location", "POINT(BAD GEOMETRY)");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_user_annotation_out_of_bounds() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        userAnnotation.setLocation(new WKTReader().read("" +
                "POLYGON ((" +
                "-1 -1," +
                "-1 " + userAnnotation.getImage().getBaseImage().getHeight() + "," +
                userAnnotation.getImage().getBaseImage().getWidth()+5 + " "+userAnnotation.getImage().getBaseImage().getHeight()+","+
                userAnnotation.getImage().getBaseImage().getWidth() + " 0," +
                "-1 -1))"));
        JsonObject jsonObject = userAnnotation.toJsonObject();

        CommandResponse commandResponse = userAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(((UserAnnotation)commandResponse.getObject()).getLocation().toText())
                .isEqualTo("" +
                        "POLYGON ((" +
                        "0 " + userAnnotation.getImage().getBaseImage().getHeight() + ", " +
                        userAnnotation.getImage().getBaseImage().getWidth() + " "+userAnnotation.getImage().getBaseImage().getHeight()+", "+
                        userAnnotation.getImage().getBaseImage().getWidth() + " 0, " +
                        "0 0, " +
                        "0 " + userAnnotation.getImage().getBaseImage().getHeight() + "))");
    }

    @Test
    void add_user_annotation_empty_polygon() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("location", "POLYGON EMPTY");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
                userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_user_annotation_null_geometry() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.remove("location");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_user_annotation_slice_not_exists() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("slice", -1);
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_user_annotation_slice_null_retrieve_reference_slice() {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();

        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("slice", null);
        CommandResponse commandResponse = userAnnotationService.add(jsonObject);

        assertThat(commandResponse.getStatus()).isEqualTo(200); //referenceSlice is taken
    }

    @Test
    void add_user_annotation_slice_null_and_image_not_exists_fail() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("slice", null);
        jsonObject.put("image", -1);
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }


    @Test
    void edit_valid_user_annotation_with_success() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        String oldLocation = "POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))";
        String newLocation = "POLYGON ((2107 2160, 2047 2074, 1983 2168, 2107 2160))";

        userAnnotation.setLocation(new WKTReader().read(oldLocation));
        builder.persistAndReturn(userAnnotation);
        CommandResponse commandResponse = userAnnotationService.update(userAnnotation,
                userAnnotation.toJsonObject().withChange(
                        "location", newLocation)
        );

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        UserAnnotation edited = userAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getLocation().toText()).isEqualTo(newLocation);
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(newLocation);

        commandService.undo();

        edited = userAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(oldLocation);

        commandService.redo();

        edited = userAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(newLocation);

    }

    @Test
    void edit_user_annotation_out_of_bounds() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.setLocation(new WKTReader().read("" +
                "POLYGON((" +
                "-1 -1," +
                "-1 " + userAnnotation.getImage().getBaseImage().getHeight() + "," +
                userAnnotation.getImage().getBaseImage().getWidth()+5 + " "+userAnnotation.getImage().getBaseImage().getHeight()+","+
                userAnnotation.getImage().getBaseImage().getWidth() + " 0," +
                "-1 -1))"));
        JsonObject jsonObject = userAnnotation.toJsonObject();
        CommandResponse commandResponse = userAnnotationService.update(userAnnotation, jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(((UserAnnotation)commandResponse.getObject()).getLocation().toText())
                .isEqualTo("" +
                        "POLYGON ((" +
                        "0 " + userAnnotation.getImage().getBaseImage().getHeight() + ", " +
                        userAnnotation.getImage().getBaseImage().getWidth() + " "+userAnnotation.getImage().getBaseImage().getHeight()+", "+
                        userAnnotation.getImage().getBaseImage().getWidth() + " 0, " +
                        "0 0, " +
                        "0 " + userAnnotation.getImage().getBaseImage().getHeight() +"))");
    }


    @Test
    void edit_user_annotation_empty_polygon() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("location", "POINT (BAD GEOMETRY)");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void delete_user_annotation_with_success() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();

        CommandResponse commandResponse = userAnnotationService.delete(userAnnotation, null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());

        commandService.undo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(userAnnotation.getId()).isPresent());

        commandService.redo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());
    }


    @Test
    void delete_user_annotation_with_terms() {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();

        Term term1 = builder.given_a_term(userAnnotation.getProject().getOntology());
        Term term2 = builder.given_a_term(userAnnotation.getProject().getOntology());

        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("term", List.of(term1.getId(), term2.getId()));

        CommandResponse commandResponse = userAnnotationService.add(jsonObject);
        userAnnotation = (UserAnnotation)commandResponse.getObject();

        commandResponse = userAnnotationService.delete(userAnnotation, null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());
    }

    @Test
    void delete_user_annotation_with_dependencies() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        SharedAnnotation sharedAnnotation = builder.given_a_shared_annotation();
        sharedAnnotation.setAnnotation(userAnnotation);
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        annotationTrack.setAnnotation(userAnnotation);

        Property property = builder.given_a_property(userAnnotation, "mustbedeleted", "value");
        Description description = builder.given_a_description(userAnnotation);
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), userAnnotation);
        AttachedFile attachedFile = builder.given_a_attached_file(userAnnotation);


        assertThat(entityManager.find(UserAnnotation.class, userAnnotation.getId())).isNotNull();
        assertThat(entityManager.find(SharedAnnotation.class, sharedAnnotation.getId())).isNotNull();
        assertThat(entityManager.find(AnnotationTrack.class, annotationTrack.getId())).isNotNull();
        assertThat(entityManager.find(Property.class, property.getId())).isNotNull();
        assertThat(entityManager.find(Description.class, description.getId())).isNotNull();
        assertThat(entityManager.find(TagDomainAssociation.class, tagDomainAssociation.getId())).isNotNull();
        assertThat(entityManager.find(AttachedFile.class, attachedFile.getId())).isNotNull();

        CommandResponse commandResponse = userAnnotationService.delete(userAnnotation, null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(entityManager.find(UserAnnotation.class, userAnnotation.getId())).isNull();
        assertThat(entityManager.find(SharedAnnotation.class, sharedAnnotation.getId())).isNull();
        assertThat(entityManager.find(AnnotationTrack.class, annotationTrack.getId())).isNull();
        assertThat(entityManager.find(Property.class, property.getId())).isNull();
        assertThat(entityManager.find(Description.class, description.getId())).isNull();
        assertThat(entityManager.find(TagDomainAssociation.class, tagDomainAssociation.getId())).isNull();
        assertThat(entityManager.find(AttachedFile.class, attachedFile.getId())).isNull();
    }


    @Test
    void do_annotation_corrections() throws ParseException {

        UserAnnotation based = builder.given_a_user_annotation();
        based.setLocation(new WKTReader().read("POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"));
        builder.persistAndReturn(based);

        UserAnnotation anotherAnnotation = builder.given_a_user_annotation();
        anotherAnnotation.setLocation(new WKTReader().read("POLYGON ((1 1, 1 5000, 10000 5000, 10000 1, 1 1))"));
        anotherAnnotation.setImage(based.getImage());
        builder.persistAndReturn(anotherAnnotation);

        userAnnotationService.doCorrectUserAnnotation(List.of(based.getId(), anotherAnnotation.getId()), "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))", false);

        assertThat(userAnnotationRepository.findById(based.getId())).isPresent();
        assertThat(userAnnotationRepository.findById(based.getId()).get().getLocation()).isEqualTo(new WKTReader().read("POLYGON ((0 0, 0 10000, 10000 10000, 10000 0, 0 0))"));

        assertThat(userAnnotationRepository.findById(anotherAnnotation.getId())).isEmpty();
    }


    @Test
    void do_annotation_corrections_with_remove() throws ParseException {

        UserAnnotation based = builder.given_a_user_annotation();
        based.setLocation(new WKTReader().read("POLYGON ((0 0, 0 10000, 10000 10000, 10000 0, 0 0))"));
        builder.persistAndReturn(based);

        UserAnnotation anotherAnnotation = builder.given_a_user_annotation();
        anotherAnnotation.setLocation(new WKTReader().read("POLYGON ((10000 10000, 10000 30000, 30000 30000, 30000 10000, 10000 10000))"));
        anotherAnnotation.setImage(based.getImage());
        builder.persistAndReturn(anotherAnnotation);

        userAnnotationService.doCorrectUserAnnotation(List.of(based.getId(), anotherAnnotation.getId()), "POLYGON ((0 5000, 2000 5000, 2000 2000, 0 2000, 0 5000))", true);

        assertThat(userAnnotationRepository.findById(based.getId())).isPresent();
        assertThat(userAnnotationRepository.findById(based.getId()).get().getLocation()).isEqualTo(new WKTReader().read("POLYGON ((0 0, 0 2000, 2000 2000, 2000 5000, 0 5000, 0 10000, 10000 10000, 10000 0, 0 0))"));

        assertThat(userAnnotationRepository.findById(anotherAnnotation.getId())).isPresent();
        assertThat(userAnnotationRepository.findById(anotherAnnotation.getId()).get().getLocation()).isEqualTo(new WKTReader().read("POLYGON ((10000 10000, 10000 16000, 16000 16000, 16000 10000, 10000 10000))"));

    }


    @Test
    @Disabled
    public void repeat_annotation() {
        // must be implemented
    }
}
