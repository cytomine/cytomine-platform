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
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class GenericAnnotationServiceTests {

    @Autowired
    GenericAnnotationService genericAnnotationService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    EntityManager entityManager;

    @Test
    void find_user_annotation_that_touch_location() throws ParseException {
        String basedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))";
        String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))";

        UserAnnotation annotation = builder.given_a_user_annotation();
        annotation.setLocation(new WKTReader().read(basedLocation));
        UserAnnotation anotherAnnotationOutsideTheLocation = builder.given_a_user_annotation();
        anotherAnnotationOutsideTheLocation.setLocation(new WKTReader().read("POLYGON ((20000 50000, 300000 50000, 300000 100000, 20000 100000, 20000 50000))"));
        anotherAnnotationOutsideTheLocation.setImage(annotation.getImage());

        List<AnnotationDomain> results = genericAnnotationService.findAnnotationThatTouch(
                addedLocation, List.of(annotation.getUser().getId()), annotation.getImage().getId(), "user_annotation"
        );

        assertThat(results).contains(annotation).doesNotContain(anotherAnnotationOutsideTheLocation);

    }

    @Test
    void find_reviewed_annotation_that_touch_location() throws ParseException {
        String basedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))";
        String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))";

        ReviewedAnnotation annotation = builder.given_a_reviewed_annotation();
        annotation.setLocation(new WKTReader().read(basedLocation));
        ReviewedAnnotation anotherAnnotationOutsideTheLocation = builder.given_a_reviewed_annotation();
        anotherAnnotationOutsideTheLocation.setLocation(new WKTReader().read("POLYGON ((20000 50000, 300000 50000, 300000 100000, 20000 100000, 20000 50000))"));
        anotherAnnotationOutsideTheLocation.setImage(annotation.getImage());

        List<AnnotationDomain> results = genericAnnotationService.findAnnotationThatTouch(
                addedLocation, List.of(annotation.getUser().getId()), annotation.getImage().getId(), "reviewed_annotation"
        );

        assertThat(results).contains(annotation).doesNotContain(anotherAnnotationOutsideTheLocation);

    }

    @Test
    void find_user_annotation_with_terms() {
        UserAnnotation annotation = builder.given_an_annotation_term().getUserAnnotation();
        assertThat(genericAnnotationService.findUserAnnotationWithTerm(List.of(annotation.getId()), annotation.termsId()))
                .contains(annotation);
    }

    @Test
    void find_reviewed_annotation_with_terms() {
        ReviewedAnnotation annotation = builder.given_a_reviewed_annotation();
        annotation.getTerms().add(builder.given_a_term(annotation.getProject().getOntology()));
        builder.persistAndReturn(annotation);

        assertThat(genericAnnotationService.findReviewedAnnotationWithTerm(List.of(annotation.getId()), annotation.termsId()))
                .contains(annotation);
    }
}
