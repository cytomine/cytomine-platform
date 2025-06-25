package be.cytomine.authorization.ontology;

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
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.PermissionService;
import be.cytomine.service.ontology.ReviewedAnnotationService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import org.locationtech.jts.io.ParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class ReviewedAnnotationAuthorizationTest extends CRUDAuthorizationTest {


    private ReviewedAnnotation reviewedAnnotation = null;

    @Autowired
    ReviewedAnnotationService reviewedAnnotationService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (reviewedAnnotation == null) {
            reviewedAnnotation = builder.given_a_reviewed_annotation();
            reviewedAnnotation.getImage().setReviewStart(null);
            reviewedAnnotation.getImage().setReviewUser(null);
            reviewedAnnotation.getImage().setReviewStop(null);
            ;
            initACL(reviewedAnnotation.container());
        }
        reviewedAnnotation.getProject().setMode(EditingMode.CLASSIC);
        builder.persistAndReturn(reviewedAnnotation.getProject());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_can_review_annotation_if_reviewer() {
        UserAnnotation annotation
                = builder.given_a_user_annotation();
        annotation.setImage(this.reviewedAnnotation.getImage());
        annotation.setProject(this.reviewedAnnotation.getProject());
        annotation.getImage().setReviewStart(new Date());
        annotation.getImage().setReviewUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_ADMIN).get());

        expectOK (() -> {
            reviewedAnnotationService.reviewAnnotation(annotation.getId(), null);
        });
    }


    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_cannot_review_annotation_if_not_reviewer() {
        UserAnnotation annotation
                = builder.given_a_user_annotation();
        annotation.setImage(this.reviewedAnnotation.getImage());
        annotation.setProject(this.reviewedAnnotation.getProject());
        annotation.getImage().setReviewStart(new Date());
        annotation.getImage().setReviewUser(builder.given_a_user(SUPERADMIN)); // someone else
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            reviewedAnnotationService.reviewAnnotation(annotation.getId(), null);
        });
    }

    @Test
    @WithMockUser(username = CREATOR)
    public void creator_can_edit_its_annotation() {
        ReviewedAnnotation reviewedAnnotation
                = builder.given_a_reviewed_annotation();
        reviewedAnnotation.setImage(this.reviewedAnnotation.getImage());
        reviewedAnnotation.setProject(this.reviewedAnnotation.getProject());
        reviewedAnnotation.getImage().setReviewStart(new Date());
        reviewedAnnotation.getImage().setReviewUser(builder.given_a_user(USER_ACL_ADMIN));
        reviewedAnnotation.setReviewUser(userRepository.findByUsernameLikeIgnoreCase(CREATOR).get());
        expectOK (() -> {
            reviewedAnnotationService.update(reviewedAnnotation, reviewedAnnotation.toJsonObject(), null);
        });
    }

    @Test
    @WithMockUser(username = CREATOR)
    public void creator_can_delete_its_annotation() {
        ReviewedAnnotation reviewedAnnotation
                = builder.given_a_reviewed_annotation();
        reviewedAnnotation.setImage(this.reviewedAnnotation.getImage());
        reviewedAnnotation.setProject(this.reviewedAnnotation.getProject());
        reviewedAnnotation.getImage().setReviewStart(new Date());
        reviewedAnnotation.getImage().setReviewUser(builder.given_a_user(USER_ACL_ADMIN));
        reviewedAnnotation.setReviewUser(userRepository.findByUsernameLikeIgnoreCase(CREATOR).get());
        expectOK (() -> {
            reviewedAnnotationService.delete(reviewedAnnotation, null, null, false);
        });
    }

    @Override
    public void when_i_get_domain() {
        reviewedAnnotationService.get(reviewedAnnotation.getId());
    }

    @Override
    protected void when_i_add_domain() {
        JsonObject jsonObject = builder.given_a_not_persisted_reviewed_annotation(this.reviewedAnnotation.getProject()).toJsonObject();
        reviewedAnnotationService.add(jsonObject);
    }

    @Override
    public void when_i_edit_domain() {
        reviewedAnnotationService.update(reviewedAnnotation, reviewedAnnotation.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        ReviewedAnnotation annotation = builder.persistAndReturn(builder.given_a_not_persisted_reviewed_annotation(this.reviewedAnnotation.getProject()));
        reviewedAnnotationService.delete(annotation, null, null, true);
    }


    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(BasePermission.READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(BasePermission.READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(BasePermission.READ);
    }


    @Override
    protected Optional<String> minimalRoleForCreate() {
        return Optional.of("CREATOR");
    }

    @Override
    protected Optional<String> minimalRoleForDelete() {
        return Optional.of("CREATOR");
    }

    @Override
    protected Optional<String> minimalRoleForEdit() {
        return Optional.of("CREATOR");
    }
}
