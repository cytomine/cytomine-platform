package be.cytomine.authorization.meta;

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
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.service.PermissionService;
import be.cytomine.service.meta.TagDomainAssociationService;
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

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class TagDomainAssociationAuthorizationTest extends CRDAuthorizationTest {


    private TagDomainAssociation tagDomainAssociationForProject = null;
    private TagDomainAssociation tagDomainAssociationForAnnotation = null;
    private TagDomainAssociation tagDomainAssociationForAbstractImage = null;

    private Project project = null;
    private AnnotationDomain annotationDomain = null;
    private AbstractImage abstractImage = null;

    @Autowired
    TagDomainAssociationService tagDomainAssociationService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (tagDomainAssociationForProject == null) {
            project = builder.given_a_project();
            annotationDomain = builder.given_a_user_annotation();
            abstractImage = builder.given_an_abstract_image();

            tagDomainAssociationForProject = builder.given_a_tag_association(builder.given_a_tag(), project);
            tagDomainAssociationForAnnotation = builder.given_a_tag_association(builder.given_a_tag(), annotationDomain);
            tagDomainAssociationForAbstractImage = builder.given_a_tag_association(builder.given_a_tag(), abstractImage);

            ;
            initACL(project);
            initACL(annotationDomain.getProject());
            initACL(abstractImage.getUploadedFile().getStorage());
        }
        project.setMode(EditingMode.CLASSIC);
        annotationDomain.getProject().setMode(EditingMode.CLASSIC);
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list() {
        expectOK (() -> { tagDomainAssociationService.listAllByDomain(project); });
        expectOK (() -> { tagDomainAssociationService.listAllByTag(tagDomainAssociationForProject.getTag()); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_list(){
        expectForbidden (() -> { tagDomainAssociationService.listAllByTag(tagDomainAssociationForProject.getTag()); });
    }


    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_list_with_filters(){
        expectOK (() -> { tagDomainAssociationService.listAllByDomain(project); });
        assertThat(tagDomainAssociationService.list(new ArrayList<>()))
                .contains(tagDomainAssociationForProject, tagDomainAssociationForAnnotation, tagDomainAssociationForAbstractImage);
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_acl_cannot_list_with_filters(){
        assertThat(tagDomainAssociationService.list(new ArrayList<>()))
                .doesNotContain(tagDomainAssociationForProject, tagDomainAssociationForAnnotation)
                .contains(tagDomainAssociationForAbstractImage);
    }
    // ANNOTATIONS
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_readonly_mode(){
        project.setMode(EditingMode.READ_ONLY);
        expectForbidden(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_restricted_mode(){
        project.setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_restricted_mode_for_annotation(){
        project.setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_canadd_in_restricted_mode_for_annotation_if_owner(){
        annotationDomain.getProject().setMode(EditingMode.RESTRICTED);
        ((UserAnnotation)annotationDomain).setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        expectOK(() -> when_i_add_domain());
    }


    @Override
    public void when_i_get_domain() {
        tagDomainAssociationService.find(tagDomainAssociationForProject.getId());
        tagDomainAssociationService.find(tagDomainAssociationForAnnotation.getId());
        tagDomainAssociationService.find(tagDomainAssociationForAbstractImage.getId());
    }

    @Override
    protected void when_i_add_domain() {
        AnnotationDomain annotationDomain = builder.persistAndReturn(builder.given_a_not_persisted_user_annotation(project));
        tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationDomain).toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), annotationDomain);
        tagDomainAssociationService.delete(tagDomainAssociation, null, null, true);
    }

    //IMAGE
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_add_for_image(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
       expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(),imageInstance).toJsonObject()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_restricted_mode_for_image(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(),imageInstance).toJsonObject()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_add_in_restricted_mode_for_image_if_owner(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        imageInstance.setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(),imageInstance).toJsonObject()));
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_add_for_image(){
        expectForbidden(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(),builder.given_an_image_instance()).toJsonObject()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_delete_for_image(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), imageInstance);
        expectOK (() -> tagDomainAssociationService.delete(tagDomainAssociation, null, null, true));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_delete_in_restricted_mode_for_image(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> tagDomainAssociationService.delete(builder.given_a_tag_association(builder.given_a_tag(),imageInstance),null,null,true));
    }


    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_delete_in_restricted_mode_for_image_if_owner(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        imageInstance.setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        expectOK(() -> tagDomainAssociationService.delete(builder.given_a_tag_association(builder.given_a_tag(),imageInstance),null,null,true));
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_delete_for_image(){
        expectForbidden(() -> tagDomainAssociationService.delete(builder.given_a_tag_association(builder.given_a_tag(),builder.given_an_image_instance()),null,null,true));
    }

//PROJECT

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_add_for_project(){
        expectOK (() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(),project).toJsonObject()));
    }


    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_cannot_add_for_project(){
        expectForbidden (() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(),project).toJsonObject()));

    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_can_add_for_project(){
        expectOK (() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(),project).toJsonObject()));
    }


    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_delete_for_project(){
        expectOK (() -> tagDomainAssociationService.delete(builder.given_a_tag_association(builder.given_a_tag(),project), null,null,true));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_cannot_delete_for_project(){
        expectForbidden (() -> tagDomainAssociationService.delete(builder.given_a_tag_association(builder.given_a_tag(),project),null,null ,true));

    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_can_delete_for_project(){
        expectOK (() -> tagDomainAssociationService.delete(builder.given_a_tag_association(builder.given_a_tag(),project),null,null,true));
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
