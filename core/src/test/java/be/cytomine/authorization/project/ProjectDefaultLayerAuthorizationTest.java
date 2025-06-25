package be.cytomine.authorization.project;

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
import be.cytomine.domain.project.ProjectDefaultLayer;
import be.cytomine.domain.security.User;
import be.cytomine.service.PermissionService;
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
public class ProjectDefaultLayerAuthorizationTest extends CRDAuthorizationTest {


    private ProjectDefaultLayer projectDefaultLayer = null;

    @Autowired
    ProjectDefaultLayerService projectDefaultLayerService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (projectDefaultLayer == null) {
            projectDefaultLayer = builder.given_a_project_default_layer();
            ;
            initACL(projectDefaultLayer.container());
        }
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_project_representative_user() {
        expectOK (() -> { projectDefaultLayerService
                .listByProject(projectDefaultLayer.getProject()); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_can_list_project_representative_user(){
        expectOK (() -> { projectDefaultLayerService
                .listByProject(projectDefaultLayer.getProject()); });
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_no_acl_cannot_list_project_representative_user(){
        expectForbidden(() -> {
            projectDefaultLayerService
                    .listByProject(projectDefaultLayer.getProject());
        });
    }

    @Override
    public void when_i_get_domain() {
        projectDefaultLayerService.get(projectDefaultLayer.getId());
        projectDefaultLayerService.find(projectDefaultLayer.getId());
    }

    @Override
    protected void when_i_add_domain() {
        User user = builder.given_a_user();
        builder.addUserToProject(projectDefaultLayer.getProject(), user.getUsername());
        projectDefaultLayerService.add(
                builder.given_a_not_persisted_project_representative_user(
                        projectDefaultLayer.getProject(), user
                ).toJsonObject()
        );
    }

    @Override
    protected void when_i_delete_domain() {
        User user = projectDefaultLayer.getUser();
        builder.addUserToProject(projectDefaultLayer.getProject(), user.getUsername());
        ProjectDefaultLayer projectDefaultLayerToDelete = builder.given_a_not_persisted_project_default_layer(projectDefaultLayer.getProject(),
                user);
        builder.persistAndReturn(projectDefaultLayerToDelete);
        projectDefaultLayerService.delete(projectDefaultLayerToDelete, null, null, true);
    }
    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(BasePermission.WRITE);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(BasePermission.WRITE);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(BasePermission.WRITE);
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
