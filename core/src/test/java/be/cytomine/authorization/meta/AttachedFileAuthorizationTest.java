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
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.service.PermissionService;
import be.cytomine.service.meta.AttachedFileService;
import be.cytomine.service.ontology.TermService;
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
public class AttachedFileAuthorizationTest extends CRDAuthorizationTest {


    private AttachedFile attachedFile = null;

    private Project project = null;

    private AnnotationDomain attachedFileAnnotation = null;

    @Autowired
    AttachedFileService attachedFileService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (attachedFile == null) {
            attachedFileAnnotation = builder.given_a_user_annotation();
            project = attachedFileAnnotation.getProject();
            attachedFile = builder.given_a_attached_file(attachedFileAnnotation);
            initACL(attachedFileAnnotation.container());
        }
        project.setMode(EditingMode.CLASSIC);
    }

    @Override
    @Test
    @WithMockUser(username = USER_ACL_CREATE)
    public void user_with_create_permission_delete_domain() {
        expectOK(this::when_i_delete_domain);
        project.setMode(EditingMode.RESTRICTED);
        expectForbidden(this::when_i_delete_domain);
        project.setMode(EditingMode.READ_ONLY);
        expectForbidden(this::when_i_delete_domain);
    }

    @Override
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_add_domain() {
        expectOK(this::when_i_add_domain);
        project.setMode(EditingMode.RESTRICTED);
        expectForbidden(this::when_i_add_domain);
        project.setMode(EditingMode.READ_ONLY);
        expectForbidden(this::when_i_add_domain);
    }

    @Override
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_delete_domain() {
        expectOK(this::when_i_delete_domain);
        project.setMode(EditingMode.RESTRICTED);
        expectForbidden(this::when_i_delete_domain);
        project.setMode(EditingMode.READ_ONLY);
        expectForbidden(this::when_i_delete_domain);
    }

    @Override
    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_permission_delete_domain() {
        expectOK(this::when_i_delete_domain);
        project.setMode(EditingMode.RESTRICTED);
        expectForbidden(this::when_i_delete_domain);
        project.setMode(EditingMode.READ_ONLY);
        expectForbidden(this::when_i_delete_domain);
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_add_domain() {
        expectForbidden(this::when_i_add_domain);
        project.setMode(EditingMode.RESTRICTED);
        expectForbidden(this::when_i_add_domain);
        project.setMode(EditingMode.READ_ONLY);
        expectForbidden(this::when_i_add_domain);
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_by_domain() {
        expectOK (() -> { attachedFileService.findAllByDomain(attachedFile.getDomainClassName(), attachedFile.getDomainIdent()); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_list_by_domain(){
        expectOK(() -> {
            attachedFileService.findAllByDomain(attachedFile.getDomainClassName(), attachedFile.getDomainIdent());
        });
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_not_acl_cannot_list_by_domain(){
        expectForbidden(() -> {
            attachedFileService.findAllByDomain(attachedFile.getDomainClassName(), attachedFile.getDomainIdent());
        });
    }

    // ANNOTATIONS
    @Override
    public void when_i_get_domain() {
        attachedFileService.findById(attachedFile.getId());
    }

    @Override
    protected void when_i_add_domain() {
        try {
            attachedFileService.create("test", "hello".getBytes(), "test", attachedFileAnnotation.getId(), attachedFileAnnotation.getClass().getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void when_i_delete_domain() {
        AttachedFile attachedFile = builder.given_a_attached_file(attachedFileAnnotation);
        attachedFileService.delete(attachedFile, null, null, true);
    }

    //IMAGE

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_add_for_image(){
        ImageInstance attachedFileImage = builder.given_an_image_instance(project);
        expectOK(() ->     attachedFileService.create("test", "hello".getBytes(), "test", attachedFileImage.getId(), attachedFileImage.getClass().getName()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_restricted_mode_for_image(){
        ImageInstance attachedFileImage = builder.given_an_image_instance(project);
        attachedFileImage.getProject().setMode(EditingMode.RESTRICTED);
        expectForbidden(() ->   attachedFileService.create("test", "hello".getBytes(), "test", attachedFileImage.getId(), attachedFileImage.getClass().getName()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_add_in_restricted_mode_for_image_if_owner(){
        ImageInstance attachedFileImage=builder.given_an_image_instance(project);
        attachedFileImage.getProject().setMode(EditingMode.RESTRICTED);
        attachedFileImage.setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        expectOK(() ->   attachedFileService.create("test", "hello".getBytes(), "test", attachedFileImage.getId(), attachedFileImage.getClass().getName()));
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_add_for_image(){
        ImageInstance attachedFileImage = builder.given_an_image_instance(project);
        expectForbidden(() -> attachedFileService.create("test", "hello".getBytes(), "test", attachedFileImage.getId(), attachedFileImage.getClass().getName()));
    }
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_delete_for_image(){
        ImageInstance attachedFileImage = builder.given_an_image_instance(project);
        AttachedFile attachedFile = builder.given_a_attached_file(attachedFileImage);
        expectOK(() ->  attachedFileService.delete(attachedFile, null, null, true));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_delete_in_restricted_mode_for_image(){
        ImageInstance attachedFileImage = builder.given_an_image_instance(project);
        attachedFileImage.getProject().setMode(EditingMode.RESTRICTED);
        AttachedFile attachedFile = builder.given_a_attached_file(attachedFileImage);
        expectForbidden(() ->  attachedFileService.delete(attachedFile, null, null, true));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_delete_in_restricted_mode_for_image_if_owner(){
        ImageInstance attachedFileImage=builder.given_an_image_instance(project);
        attachedFileImage.getProject().setMode(EditingMode.RESTRICTED);
        attachedFileImage.setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        AttachedFile attachedFile = builder.given_a_attached_file(attachedFileImage);
        expectOK(() ->  attachedFileService.delete(attachedFile, null, null, true));
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_delete_for_image(){
        ImageInstance attachedFileImage = builder.given_an_image_instance(project);
        AttachedFile attachedFile = builder.given_a_attached_file(attachedFileImage);
        expectForbidden(() ->  attachedFileService.delete(attachedFile, null, null, true));}

    //PROJECT
    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_add_for_project(){
        expectOK(() ->     attachedFileService.create("test", "hello".getBytes(), "test", project.getId(), project.getClass().getName()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_cannot_add_for_project(){
        expectForbidden (() -> attachedFileService.create("test", "hello".getBytes(), "test", project.getId(), project.getClass().getName()));
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_can_add_for_project(){
        expectOK (() -> attachedFileService.create("test", "hello".getBytes(), "test", project.getId(), project.getClass().getName()));
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_delete_for_project(){
        AttachedFile attachedFile = builder.given_a_attached_file(project);
        expectOK(() ->  attachedFileService.delete(attachedFile, null, null, true));
   }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_cannot_delete_for_project(){

        AttachedFile attachedFile = builder.given_a_attached_file(project);
        expectForbidden (() ->  attachedFileService.delete(attachedFile, null, null, true));
    }
    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_can_delete_for_project(){
        AttachedFile attachedFile = builder.given_a_attached_file(project);
        expectOK (() -> attachedFileService.delete(attachedFile, null, null, true));
    }


    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(BasePermission.WRITE);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(BasePermission.DELETE);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(BasePermission.WRITE);
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
