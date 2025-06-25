package be.cytomine.authorization.processing;

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
import be.cytomine.domain.processing.ImageFilterProject;
import be.cytomine.domain.project.ProjectDefaultLayer;
import be.cytomine.domain.security.User;
import be.cytomine.service.PermissionService;
import be.cytomine.service.processing.ImageFilterProjectService;
import be.cytomine.service.project.ProjectDefaultLayerService;
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
public class ImageFilterProjectAuthorizationTest extends CRDAuthorizationTest {


    private ImageFilterProject imageFilterProject = null;

    @Autowired
    ImageFilterProjectService imageFilterProjectService;
    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (imageFilterProject == null) {
            imageFilterProject = builder.given_a_image_filter_project();
            initACL(imageFilterProject.container());
        }
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_all_image_filters() {
        expectOK (() -> { imageFilterProjectService.list(); });
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_no_acl_cannot_list_project_representative_user(){
        expectForbidden(() -> {
            imageFilterProjectService.list();
        });
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void project_admin_can_list_project_image_filters() {
        expectOK (() -> { imageFilterProjectService.list(imageFilterProject.getProject()); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void contributor_can_list_project_image_filter(){
        expectOK(() -> {
            imageFilterProjectService.list(imageFilterProject.getProject());
        });
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_not_in_project_cannot_list_project_image_filter(){
        expectForbidden(() -> {
            imageFilterProjectService.list(imageFilterProject.getProject());
        });
    }

    @Override
    public void when_i_get_domain() {
        imageFilterProjectService.find(imageFilterProject.getImageFilter(), imageFilterProject.getProject());
    }

    @Override
    protected void when_i_add_domain() {
        imageFilterProjectService.add(
                builder.given_a_not_persisted_image_filter_project(builder.given_a_image_filter(), imageFilterProject.getProject()).toJsonObject()
        );
    }

    @Override
    protected void when_i_delete_domain() {
        ImageFilterProject imageFilterProjectThatMustBeDeleted = builder.given_a_not_persisted_image_filter_project(builder.given_a_image_filter(), imageFilterProject.getProject());
        builder.persistAndReturn(imageFilterProjectThatMustBeDeleted);
        imageFilterProjectService.delete(imageFilterProjectThatMustBeDeleted, null, null, true);
    }
    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(BasePermission.ADMINISTRATION);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(BasePermission.ADMINISTRATION);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(BasePermission.ADMINISTRATION);
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
