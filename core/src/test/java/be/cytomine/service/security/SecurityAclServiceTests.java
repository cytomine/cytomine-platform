package be.cytomine.service.security;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.social.ProjectConnectionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;
import static org.springframework.security.acls.domain.BasePermission.READ;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@Transactional
public class SecurityAclServiceTests {

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    ProjectConnectionService projectConnectionService;

    @Autowired
    UserAnnotationService userAnnotationService;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @WithMockUser(username = "user")
    @Test
    void check_is_user_allowed() {
        Project project = builder.given_a_project();
        User user = builder.given_default_user();

        Assertions.assertThrows(ForbiddenException.class, () -> {
            securityACLService.check(project.getId(), project.getClass().getName(), READ);
        });

        Assertions.assertThrows(ForbiddenException.class, () -> {
            securityACLService.check(project.getId(), project.getClass(), READ);
        });

        Assertions.assertThrows(ForbiddenException.class, () -> {
            securityACLService.check(project, READ, user);
        });

        Assertions.assertThrows(ForbiddenException.class, () -> {
            securityACLService.check(project, READ);
        });

        builder.addUserToProject(project, user.getUsername());

        securityACLService.check(project.getId(), project.getClass().getName(), READ);
        securityACLService.check(project.getId(), project.getClass(), READ);
        securityACLService.check(project, READ, user);
        securityACLService.check(project, READ);
    }

    @WithMockUser(username = "user")
    @Test
    void check_if_user_is_container_admin() {
        Project project = builder.given_a_project();
        User user = builder.given_default_user();

        Assertions.assertThrows(ForbiddenException.class, () -> {
            securityACLService.checkIsAdminContainer(project);
        });
        Assertions.assertThrows(ForbiddenException.class, () -> {
            securityACLService.checkIsAdminContainer(project, user);
        });

        builder.addUserToProject(project, user.getUsername(), ADMINISTRATION);

        securityACLService.checkIsAdminContainer(project);
        securityACLService.checkIsAdminContainer(project, user);
    }

    @WithMockUser(username = "user")
    @Test
    void has_user_permission() {
        Project project = builder.given_a_project();
        User user = builder.given_default_user();

        assertThat(securityACLService.hasPermission(project, READ, false)).isFalse();
        assertThat(securityACLService.hasPermission(project, READ)).isFalse();
        assertThat(securityACLService.hasPermission(project, READ, true)).isTrue();

        builder.addUserToProject(project, user.getUsername());

        assertThat(securityACLService.hasPermission(project, READ, false)).isTrue();
        assertThat(securityACLService.hasPermission(project, READ)).isTrue();
        assertThat(securityACLService.hasPermission(project, READ, true)).isTrue();
    }

    @WithMockUser(username = "user")
    @Test
    void has_right_to_read_abstract_image() {
        Project project = builder.given_a_project();
        ImageInstance imageInstance = builder.given_an_image_instance(project);
        User user = builder.given_default_user();

        assertThat(securityACLService.hasRightToReadAbstractImageWithProject(imageInstance.getBaseImage())).isFalse();

        builder.addUserToProject(project, user.getUsername());

        assertThat(securityACLService.hasRightToReadAbstractImageWithProject(imageInstance.getBaseImage())).isTrue();

    }

    @WithMockUser(username = "user")
    @Test
    void list_authorized_storages() {
        Storage storage = builder.given_a_storage();
        User user = builder.given_default_user();

        assertThat(securityACLService.getStorageList(user, false)).doesNotContain(storage);

        permissionService.addPermission(storage, user.getUsername(), READ);

        assertThat(securityACLService.getStorageList(user, false)).contains(storage);
        assertThat(securityACLService.getStorageList(user, false, storage.getName())).contains(storage);

    }


    @WithMockUser(username = "user")
    @Test
    void list_authorized_projects() {
        Project project = builder.given_a_project();
        User user = builder.given_default_user();

        assertThat(securityACLService.getProjectList(user, project.getOntology())).doesNotContain(project);

        permissionService.addPermission(project, user.getUsername(), READ);

        assertThat(securityACLService.getProjectList(user, project.getOntology())).contains(project);

    }

    @WithMockUser(username = "user")
    @Test
    void list_user_from_projects() {
        Project project = builder.given_a_project();
        User user = builder.given_default_user();

        assertThat(securityACLService.getProjectUsers(project)).doesNotContain(user.getUsername());

        permissionService.addPermission(project, user.getUsername(), READ);

        assertThat(securityACLService.getProjectUsers(project)).contains(user.getUsername());

    }


    @WithMockUser(username = "user")
    @Test
    void list_authorized_ontologies() {
        Ontology ontology = builder.given_an_ontology();
        User user = builder.given_default_user();

        assertThat(securityACLService.getOntologyList(user)).doesNotContain(ontology);

        permissionService.addPermission(ontology, user.getUsername(), READ);

        assertThat(securityACLService.getOntologyList(user)).contains(ontology);

    }

    @WithMockUser(username = "user")
    @Test
    void check_same_user() {
        User user = builder.given_default_user();
        Assertions.assertThrows(ForbiddenException.class, () -> {
            securityACLService.checkIsSameUser(builder.given_superadmin(), user);
        });
        securityACLService.checkIsSameUser(user, user);
        securityACLService.checkIsSameUser(user, builder.given_superadmin());
    }

    @WithMockUser(username = "user")
    @Test
    void check_is_admin() {
        User user = builder.given_default_user();
        Assertions.assertThrows(ForbiddenException.class, () -> {
            securityACLService.checkAdmin(user);
        });
        securityACLService.checkAdmin(builder.given_superadmin());
    }

    @WithMockUser(username = "user")
    @Test
    void check_is_user() {
        User user = builder.given_default_user();
        User guest = builder.given_a_guest();

        Assertions.assertThrows(ForbiddenException.class, () -> {
            securityACLService.checkAdmin(guest);
        });
        securityACLService.checkUser(user);
        securityACLService.checkUser(builder.given_superadmin());
    }

    @WithMockUser(username = "user")
    @Test
    void check_is_guest() {
        User user = builder.given_default_user();
        User guest = builder.given_a_guest();

        securityACLService.checkGuest(guest);
        securityACLService.checkGuest(user);
        securityACLService.checkGuest(builder.given_superadmin());
    }

    @WithMockUser(username = "user")
    @Test
    void check_not_readonly() {
        Project project = builder.given_a_project();
        User user = builder.given_default_user();
        permissionService.addPermission(project, user.getUsername(), READ);

        securityACLService.checkIsNotReadOnly(project);

        project.setMode(EditingMode.READ_ONLY);

        Assertions.assertThrows(ForbiddenException.class,() -> {
            securityACLService.checkIsNotReadOnly(project);
        });

        permissionService.addPermission(project, user.getUsername(), ADMINISTRATION);

        securityACLService.checkIsNotReadOnly(project);
    }

    @WithMockUser(username = "superadmin")
    @Test
    void check_is_user_in_project() {
        Project project = builder.given_a_project();
        User user = builder.given_a_user();
        assertThat(securityACLService.isUserInProject(user, project))
                .isFalse();
        builder.addUserToProject(project, user.getUsername());
        assertThat(securityACLService.isUserInProject(user, project))
                .isTrue();
    }

}
