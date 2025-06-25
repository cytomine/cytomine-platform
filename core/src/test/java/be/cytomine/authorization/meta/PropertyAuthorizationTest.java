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
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.meta.Description;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.service.PermissionService;
import be.cytomine.service.meta.PropertyService;
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
public class PropertyAuthorizationTest extends CRUDAuthorizationTest {


    private Property propertyForProject = null;
    private Property propertyForAnnotation = null;
    private Property propertyForAbstractImage = null;
    private Property propertyForImageInstance = null;

    private Project project = null;
    private AnnotationDomain annotationDomain = null;
    private AbstractImage abstractImage = null;
    private ImageInstance imageInstance = null;


    @Autowired
    PropertyService propertyService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (propertyForProject == null) {
            project = builder.given_a_project();
            annotationDomain = builder.given_a_user_annotation();
            abstractImage = builder.given_an_abstract_image();
            imageInstance=builder.given_an_image_instance(project);


            propertyForProject = builder.given_a_property(project);
            propertyForAnnotation = builder.given_a_property(annotationDomain);
            propertyForAbstractImage = builder.given_a_property(abstractImage);
            propertyForImageInstance=builder.given_a_property(imageInstance);



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
        expectOK (() -> { propertyService.list(); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_list(){
        expectForbidden(() -> {
            propertyService.list();
        });
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_read_cannot_list_for_domain(){
        expectForbidden(() -> {
            propertyService.list(abstractImage);
        });
        expectForbidden(() -> {
            propertyService.list(imageInstance);
        });

        expectForbidden(() -> {
            propertyService.list(project);
        });
        expectForbidden(() -> {
            propertyService.list(annotationDomain);
        });
    }


    //ANNOTATIONS
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
    public void user_can_add_in_restricted_mode_for_annotation_if_owner(){
        project.setMode(EditingMode.RESTRICTED);
        ((UserAnnotation)annotationDomain).setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        expectOK(() -> {
            AnnotationDomain annotationDomain = builder.persistAndReturn(builder.given_a_not_persisted_user_annotation(project));
            ((UserAnnotation)annotationDomain).setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
            propertyService.add(builder.given_a_not_persisted_property(annotationDomain, "key", "value").toJsonObject());
        });
    }


    @Override
    public void when_i_get_domain() {
        propertyService.findByDomainAndKey(project, "key");
        propertyService.findByDomainAndKey(annotationDomain, "key");
        propertyService.findByDomainAndKey(abstractImage, "key");
    }

    @Override
    protected void when_i_add_domain() {
        AnnotationDomain annotationDomain = builder.persistAndReturn(builder.given_a_not_persisted_user_annotation(project));
        propertyService.add(builder.given_a_not_persisted_property(annotationDomain, "key", "value").toJsonObject());
    }

    @Override
    protected void when_i_edit_domain() {
        propertyService.update(propertyForAnnotation, propertyForAnnotation.toJsonObject());
    }


    @Override
    protected void when_i_delete_domain() {
        Property property = builder.given_a_property(annotationDomain);
        propertyService.delete(property, null, null, true);
    }

    //IMAGE

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_add_for_image(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(imageInstance, "key", "value").toJsonObject()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_restricted_mode_for_image(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> propertyService.add(builder.given_a_not_persisted_property(imageInstance, "key", "value").toJsonObject()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_add_in_restricted_mode_for_image_if_owner(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        imageInstance.setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(imageInstance, "key", "value").toJsonObject()));
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_add_for_image(){
        expectForbidden(() -> propertyService.add(builder.given_a_not_persisted_property(builder.given_an_image_instance(), "key", "value").toJsonObject()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_edit_for_image(){
        expectOK(() -> propertyService.update(propertyForImageInstance, propertyForImageInstance.toJsonObject(),null,null));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_edit_in_restricted_mode_for_image(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> propertyService.update(builder.given_a_property(imageInstance), builder.given_a_property(imageInstance).toJsonObject(),null));

    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_edit_in_restricted_mode_for_image_if_owner(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        imageInstance.setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        Property propertyForImageInstance = builder.given_a_property(imageInstance);
        expectOK(() -> propertyService.update(propertyForImageInstance, propertyForImageInstance.toJsonObject(),null));
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_edit_image(){
        Property propertyForImageInstanceLocal = builder.given_a_property(imageInstance);
        expectForbidden(() -> propertyService.update(propertyForImageInstanceLocal, propertyForImageInstanceLocal.toJsonObject(),null));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_delete_for_image(){
        expectOK(() -> propertyService.delete(propertyForImageInstance, null,null,true));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_delete_in_restricted_mode_for_image(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> propertyService.delete(builder.given_a_property(imageInstance), null,null,true));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_delete_in_restricted_mode_for_image_if_owner(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        imageInstance.setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        expectOK(() -> propertyService.delete(builder.given_a_property(imageInstance), null,null,true));
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_delete_for_image(){
        expectForbidden(() -> propertyService.delete(propertyForImageInstance, null,null,true));
    }

    //PROJECT

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_add_for_project(){
        expectOK (() -> propertyService.add(builder.given_a_not_persisted_property(builder.given_a_project(), "key", "value").toJsonObject()));
    }
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_cannot_add_for_project(){
        expectForbidden (() -> propertyService.add(builder.given_a_not_persisted_property(builder.given_a_project(), "key", "value").toJsonObject()));
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_can_add_for_project(){
        Project projectLocal = builder.given_a_project();
        initACL(projectLocal);
        expectOK (() -> propertyService.add(builder.given_a_not_persisted_property(projectLocal,"key", "value").toJsonObject()));
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_edit_for_project(){
        expectOK (() -> propertyService.update(propertyForProject,propertyForProject.toJsonObject(),null,null));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_cannot_edit_for_project(){
        expectForbidden (() -> propertyService.update(propertyForProject,propertyForProject.toJsonObject(),null,null));
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_can_edit_for_project(){
        Project projectLocal = builder.given_a_project();
        initACL(projectLocal);
        Property property = builder.given_a_property(projectLocal);
        expectOK (() -> propertyService.update(property,property.toJsonObject(),null));
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_delete_for_project(){
        expectOK (() -> propertyService.delete(builder.given_a_property(builder.given_a_project()),null,null,true));
    }
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_cannot_delete_for_project(){
        expectForbidden (() -> propertyService.delete(builder.given_a_property(builder.given_a_project()),null,null,true));
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_can_delete_for_project(){
        Project projectLocal = builder.given_a_project();
        initACL(projectLocal);
        expectOK (() -> propertyService.delete(builder.given_a_property(projectLocal),null,null,true));
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
