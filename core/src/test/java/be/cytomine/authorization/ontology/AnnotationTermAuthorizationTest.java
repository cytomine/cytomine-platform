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
import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.service.PermissionService;
import be.cytomine.service.ontology.AnnotationTermService;
import be.cytomine.service.ontology.RelationTermService;
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
public class AnnotationTermAuthorizationTest extends CRDAuthorizationTest {


    private AnnotationTerm annotationTerm = null;

    @Autowired
    AnnotationTermService annotationTermService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (annotationTerm == null) {
            annotationTerm = builder.given_an_annotation_term();
            ;
            initACL(annotationTerm.container());
        }
        annotationTerm.getUserAnnotation().getProject().setMode(EditingMode.CLASSIC);
        builder.persistAndReturn(annotationTerm.getUserAnnotation().getProject());
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_relation_terms() {
        expectOK (() -> { annotationTermService.list(annotationTerm.getUserAnnotation()); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_can_list_relation_terms(){
        expectOK (() -> { annotationTermService.list(annotationTerm.getUserAnnotation()); });
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_no_acl_cannot_list_relation_terms(){
        expectForbidden(() -> {
            annotationTermService.list(annotationTerm.getUserAnnotation());
        });
    }



    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_update_annotation_in_restricted_project() {
        AnnotationTerm annotationTerm
                = builder.given_an_annotation_term(this.annotationTerm.getUserAnnotation());
        Project project = (Project)annotationTerm.container();
        project.setMode(EditingMode.RESTRICTED);
        builder.persistAndReturn(project);
        expectOK (() -> { when_i_get_domain(); });
        expectOK (() -> { when_i_add_domain(); });
        expectOK (() -> { when_i_delete_domain(); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_update_annotation_in_classic_project() {
        AnnotationTerm annotationTerm
                = builder.given_an_annotation_term(this.annotationTerm.getUserAnnotation());
        Project project = (Project)annotationTerm.container();
        project.setMode(EditingMode.CLASSIC);
        builder.persistAndReturn(project);
        expectOK (() -> { when_i_get_domain(); });
        expectOK (() -> { when_i_add_domain(); });
        expectOK (() -> { when_i_delete_domain(); });
    }


    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_update_annotation_in_readonly_project() {
        AnnotationTerm annotationTerm
                = builder.given_an_annotation_term(this.annotationTerm.getUserAnnotation());
        Project project = (Project)annotationTerm.container();
        project.setMode(EditingMode.READ_ONLY);
        builder.persistAndReturn(project);
        expectOK (() -> { when_i_get_domain(); });
        expectForbidden (() -> { when_i_delete_domain(); });
    }


    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_update_annotation_in_restricted_project() {
        AnnotationTerm annotationTerm
                = builder.given_an_annotation_term(this.annotationTerm.getUserAnnotation());
        Project project = (Project)annotationTerm.container();
        project.setMode(EditingMode.RESTRICTED);
        builder.persistAndReturn(project);
        expectOK (() -> { when_i_get_domain(); });
        expectForbidden (() -> { when_i_delete_domain(); });
    }

    @Override
    public void when_i_get_domain() {
        annotationTermService.find(annotationTerm.getUserAnnotation(),
                annotationTerm.getTerm(), annotationTerm.getUser());
    }

    @Override
    protected void when_i_add_domain() {
        annotationTermService.add(
                builder.given_a_not_persisted_annotation_term(
                        annotationTerm.getUserAnnotation()).toJsonObject()
        );
    }

    @Override
    protected void when_i_delete_domain() {
        AnnotationTerm annotationTerm = builder.given_a_not_persisted_annotation_term(this.annotationTerm.getUserAnnotation());
        builder.persistAndReturn(annotationTerm);
        annotationTermService.delete(annotationTerm, null, null, true);
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
        return Optional.of("ROLE_GUEST");
    }

    @Override
    protected Optional<String> minimalRoleForDelete() {
        return Optional.of("ROLE_GUEST");
    }

    @Override
    protected Optional<String> minimalRoleForEdit() {
        return Optional.of("ROLE_GUEST");
    }
}
