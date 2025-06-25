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
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.AnnotationTermRepository;
import be.cytomine.service.CommandService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class AnnotationTermServiceTests {

    @Autowired
    AnnotationTermService annotationTermService;

    @Autowired
    AnnotationTermRepository annotationTermRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Test
    void find_annotation_term_with_success() {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        Optional<AnnotationTerm> result = annotationTermService.find
                (annotationTerm.getUserAnnotation(), annotationTerm.getTerm(), annotationTerm.getUser());
        assertThat(result).isPresent();
        assertThat(annotationTerm).isEqualTo(result.get());
    }


    @Test
    void list_annotation_term_for_annotation_with_success() {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        AnnotationTerm annotationTermFromAnotherAnnotation = builder.given_an_annotation_term();

        assertThat(annotationTermService.list(annotationTerm.getUserAnnotation()))
                .contains(annotationTerm).doesNotContain(annotationTermFromAnotherAnnotation);

    }

    @Test
    void list_annotation_term_for_project_with_success() {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        AnnotationTerm annotationTermFromAnotherProject = builder.given_an_annotation_term();

        assertThat(annotationTermService.list(annotationTerm.getUserAnnotation().getProject()))
                .contains(annotationTerm).doesNotContain(annotationTermFromAnotherProject);

    }

    @Test
    void list_annotation_term_not_defined_by_user() {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();

        assertThat(annotationTermService.listAnnotationTermNotDefinedByUser(annotationTerm.getUserAnnotation(), (User)annotationTerm.getUser()))
                .doesNotContain(annotationTerm);

        assertThat(annotationTermService.listAnnotationTermNotDefinedByUser(annotationTerm.getUserAnnotation(), builder.given_a_user()))
                .contains(annotationTerm);
    }



    @Test
    void add_valid_annotation_term_with_success() {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(builder.given_a_user_annotation());
        CommandResponse commandResponse = annotationTermService.add(annotationTerm.toJsonObject());
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotationTerm.getUserAnnotation(), annotationTerm.getTerm(), builder.given_superadmin())).isPresent();
        commandService.undo();
        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotationTerm.getUserAnnotation(), annotationTerm.getTerm(), builder.given_superadmin())).isEmpty();
        commandService.redo();
        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotationTerm.getUserAnnotation(), annotationTerm.getTerm(), builder.given_superadmin())).isPresent();

    }

    @Test
    void add_annotation_term_fails_if_already_exists_for_same_user() {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(builder.given_a_user_annotation());
        annotationTermService.add(annotationTerm.toJsonObject());
        Assertions.assertThrows(AlreadyExistException.class, () -> {
            annotationTermService.add(annotationTerm.toJsonObject());
        }) ;
    }

    @Test
    void add_valid_annotation_term_with_direct_method_success() {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(builder.given_a_user_annotation());
        CommandResponse commandResponse = annotationTermService.addAnnotationTerm(
                annotationTerm.getUserAnnotation().getId(),
                annotationTerm.getTerm().getId(),
                null,
                annotationTerm.getUser().getId(),
                annotationTerm.getUser(),
                null
        );
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void add_annotation_term_fails_if_term_is_from_other_ontology() {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(builder.given_a_user_annotation());
        annotationTerm.setTerm(builder.given_a_term(builder.given_an_ontology()));
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            annotationTermService.add(annotationTerm.toJsonObject());
        }) ;
    }

    @Test
    void add_valid_annotation_term_and_delete_other_terms() {
        UserAnnotation annotation = builder.given_a_user_annotation();
        AnnotationTerm annotationTerm = builder.given_an_annotation_term(annotation);
        Term oldTerm = annotationTerm.getTerm();
        Term newTerm = builder.given_a_term(annotation.getProject().getOntology());

        CommandResponse commandResponse = annotationTermService.addWithDeletingOldTerm(
                annotationTerm.getUserAnnotation().getId(),
                newTerm.getId(),
                false
        );
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotation, newTerm, builder.given_superadmin())).isPresent();
        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotation, oldTerm, builder.given_superadmin())).isEmpty();

        commandService.undo();

        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotation, newTerm, builder.given_superadmin())).isEmpty();
        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotation, oldTerm, builder.given_superadmin())).isPresent();

        commandService.redo();

        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotation, newTerm, builder.given_superadmin())).isPresent();
        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotation, oldTerm, builder.given_superadmin())).isEmpty();
    }

    @Test
    void add_valid_annotation_term_and_delete_other_terms_no_impact_for_terms_added_by_other_user() {
        UserAnnotation annotation = builder.given_a_user_annotation();
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(annotation);
        annotationTerm.setUser(builder.given_a_user());
        builder.persistAndReturn(annotationTerm);
        Term oldTerm = annotationTerm.getTerm();
        Term newTerm = builder.given_a_term(annotation.getProject().getOntology());

        CommandResponse commandResponse = annotationTermService.addWithDeletingOldTerm(
                annotationTerm.getUserAnnotation().getId(),
                newTerm.getId(),
                false
        );
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotation, newTerm, builder.given_superadmin())).isPresent();
        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotation, oldTerm, annotationTerm.getUser())).isPresent();

    }

    @Test
    void add_valid_annotation_term_and_delete_other_terms_with_force_for_terms_added_by_other() {
        UserAnnotation annotation = builder.given_a_user_annotation();
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(annotation);
        annotationTerm.setUser(builder.given_a_user());
        Term oldTerm = annotationTerm.getTerm();
        Term newTerm = builder.given_a_term(annotation.getProject().getOntology());

        CommandResponse commandResponse = annotationTermService.addWithDeletingOldTerm(
                annotationTerm.getUserAnnotation().getId(),
                newTerm.getId(),
                true
        );
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotation, newTerm, builder.given_superadmin())).isPresent();
        assertThat(annotationTermRepository
                .findByUserAnnotationAndTermAndUser(annotation, oldTerm, annotationTerm.getUser())).isEmpty();

    }


    @Test
    void add_valid_annotation_term_and_delete_other_terms_for_reviewed_annotation() {
        ReviewedAnnotation annotation = builder.given_a_reviewed_annotation();
        Term oldTerm = builder.given_a_term(annotation.getProject().getOntology());
        annotation.getTerms().add(oldTerm);
        builder.persistAndReturn(annotation);
        Term newTerm = builder.given_a_term(annotation.getProject().getOntology());

        CommandResponse commandResponse = annotationTermService.addWithDeletingOldTerm(
                annotation.getId(),
                newTerm.getId(),
                false
        );
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void add_annotation_term_fails_if_annotation_does_not_exists() {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(builder.given_a_user_annotation());
        JsonObject jsonObject = annotationTerm.toJsonObject();
        jsonObject.put("userannotation", -1L);
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            annotationTermService.add(jsonObject);
        }) ;
    }

    @Test
    void add_annotation_term_fails_if_term_does_not_exists() {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(builder.given_a_user_annotation());
        JsonObject jsonObject = annotationTerm.toJsonObject();
        jsonObject.put("term", -1L);
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            annotationTermService.add(jsonObject);
        }) ;
    }

    @Test
    void delete_annotation_term_with_success() {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term(builder.given_a_user_annotation());

        CommandResponse commandResponse =
                annotationTermService.delete(annotationTerm, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(annotationTermService.find(annotationTerm.getUserAnnotation(), annotationTerm.getTerm(), annotationTerm.getUser())).isEmpty();

        commandService.undo();

        assertThat(annotationTermService.find(annotationTerm.getUserAnnotation(), annotationTerm.getTerm(), annotationTerm.getUser())).isPresent();


        commandService.redo();

        assertThat(annotationTermService.find(annotationTerm.getUserAnnotation(), annotationTerm.getTerm(), annotationTerm.getUser())).isEmpty();

    }

}
