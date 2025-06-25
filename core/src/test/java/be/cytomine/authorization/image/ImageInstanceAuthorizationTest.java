package be.cytomine.authorization.image;

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
import be.cytomine.domain.project.EditingMode;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.PermissionService;
import be.cytomine.service.image.AbstractImageService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.security.SecurityACLService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class ImageInstanceAuthorizationTest extends CRUDAuthorizationTest {

    private ImageInstance imageInstance = null;

    @Autowired
    ImageInstanceService imageInstanceService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (imageInstance == null) {
            imageInstance = builder.given_an_image_instance();
            initACL(imageInstance.container());
        }
        imageInstance.getProject().setMode(EditingMode.CLASSIC);
        imageInstance.getProject().setAreImagesDownloadable(true);
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_imageInstances() {
        assertThat(imageInstanceService.listByProject(imageInstance.getProject())).contains(imageInstance);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_list_imageInstances(){
        assertThat(imageInstanceService.listByProject(imageInstance.getProject())).contains(imageInstance);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_delete_in_restricted_mode(){
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> when_i_delete_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_admin_can_add_in_readonly_mode(){
        imageInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectOK(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_admin_can_edi_in_readonly_mode(){
        imageInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectOK(() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_admin_can_delete_in_readonly_mode(){
        imageInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectOK(() -> when_i_delete_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_readonly_mode(){
        imageInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectForbidden(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_edit_in_readonly_mode(){
        imageInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectForbidden(() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_delete_in_readonly_mode(){
        imageInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectForbidden(() -> when_i_delete_domain());
    }


    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_cannot_stop_review_started_by_someone_else() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        imageInstance.setProject(this.imageInstance.getProject());
        imageInstance.setReviewStart(new Date());
        imageInstance.setReviewUser(builder.given_superadmin());
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            imageInstanceService.stopReview(imageInstance, false);
        });
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_can_stop_review_started_by_himself() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        imageInstance.setProject(this.imageInstance.getProject());
        imageInstance.setReviewStart(new Date());
        imageInstance.setReviewUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_ADMIN).get());
        imageInstanceService.stopReview(imageInstance, false);
    }

    @Override
    public void when_i_get_domain() {
        imageInstanceService.get(imageInstance.getId());
    }

    @Override
    protected void when_i_add_domain() {
        imageInstanceService.add(builder.given_a_not_persisted_image_instance(imageInstance.getProject()).toJsonObject());
    }

    @Override
    public void when_i_edit_domain() {
        imageInstanceService.update(imageInstance, imageInstance.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        ImageInstance imageInstanceToDelete = imageInstance;
        imageInstanceService.delete(imageInstanceToDelete, null, null, true);
    }

    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(READ);
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
