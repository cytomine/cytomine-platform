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
import be.cytomine.authorization.CRDAuthorizationTest;
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.ontology.AnnotationTrack;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.service.PermissionService;
import be.cytomine.service.ontology.AnnotationTrackService;
import be.cytomine.service.security.SecurityACLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class AnnotationTrackAuthorizationTest extends CRDAuthorizationTest {


    private AnnotationTrack annotationTrack = null;

    @Autowired
    AnnotationTrackService annotationTrackService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (annotationTrack == null) {
            annotationTrack = builder.given_a_annotation_track();
            ;
            initACL(annotationTrack.container());
        }
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_annotationTracks() {
        expectOK (() -> { annotationTrackService.list(annotationTrack.getTrack()); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_list_annotationTracks(){
        expectOK(() -> {
            annotationTrackService.list(annotationTrack.getTrack());
        });
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_acl_cannot_list_annotationTracks(){
        expectForbidden(() -> {
            annotationTrackService.list(annotationTrack.getTrack());
        });
    }

    @Override
    public void when_i_get_domain() {
        annotationTrackService.get(annotationTrack.getId());
    }

    @Override
    protected void when_i_add_domain() {
        UserAnnotation annotation = builder.given_a_user_annotation();
        annotation.setImage(annotationTrack.getTrack().getImage());
        annotation.setProject(annotationTrack.getTrack().getProject());

        annotationTrackService.add(
                builder.given_a_not_persisted_annotation_track().toJsonObject().withChange("annotationIdent", annotation.getId()).withChange("track", this.annotationTrack.getTrack().getId()));
    }


    @Override
    protected void when_i_delete_domain() {
        UserAnnotation annotation = builder.given_a_user_annotation();
        annotation.setImage(annotationTrack.getTrack().getImage());
        annotation.setProject(annotationTrack.getTrack().getProject());

        AnnotationTrack annotationTrackToDelete = builder.given_a_annotation_track();
        annotationTrackToDelete.setAnnotation(annotation);
        annotationTrackToDelete.setTrack(this.annotationTrack.getTrack());
        annotationTrackService.delete(annotationTrackToDelete, null, null, true);
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
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForDelete() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForEdit() {
        return Optional.of("ROLE_USER");
    }
}
