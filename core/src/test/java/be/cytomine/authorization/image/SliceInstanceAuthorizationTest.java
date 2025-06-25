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
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.service.PermissionService;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.security.SecurityACLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.READ;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class SliceInstanceAuthorizationTest extends CRUDAuthorizationTest {

    // We need more flexibility:

    private SliceInstance sliceInstance = null;

    @Autowired
    SliceInstanceService sliceInstanceService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (sliceInstance == null) {
            sliceInstance = builder.given_a_slice_instance();
            initACL(sliceInstance.container());
        }
        sliceInstance.getProject().setMode(EditingMode.CLASSIC);
        sliceInstance.getProject().setAreImagesDownloadable(true);
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_sliceInstances() {
        assertThat(sliceInstanceService.list(sliceInstance.getImage())).contains(sliceInstance);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_list_sliceInstances(){
        assertThat(sliceInstanceService.list(sliceInstance.getImage())).contains(sliceInstance);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_delete_in_restricted_mode(){
        sliceInstance.getProject().setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> when_i_delete_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_admin_can_add_in_readonly_mode(){
        sliceInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectOK(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_admin_can_edi_in_readonly_mode(){
        sliceInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectOK(() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_admin_can_delete_in_readonly_mode(){
        sliceInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectOK(() -> when_i_delete_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_readonly_mode(){
        sliceInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectForbidden(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_edit_in_readonly_mode(){
        sliceInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectForbidden(() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_delete_in_readonly_mode(){
        sliceInstance.getProject().setMode(EditingMode.READ_ONLY);
        expectForbidden(() -> when_i_delete_domain());
    }


    @Override
    public void when_i_get_domain() {
        sliceInstanceService.get(sliceInstance.getId());
    }

    @Override
    protected void when_i_add_domain() {
        sliceInstanceService.add(
                builder.given_a_not_persisted_slice_instance(
                        builder.given_an_image_instance(sliceInstance.getProject()),
                        builder.given_an_abstract_slice()
                ).toJsonObject());
    }

    @Override
    public void when_i_edit_domain() {
        sliceInstanceService.update(sliceInstance, sliceInstance.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        SliceInstance sliceInstanceToDelete = sliceInstance;
        sliceInstanceService.delete(sliceInstanceToDelete, null, null, true);
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
