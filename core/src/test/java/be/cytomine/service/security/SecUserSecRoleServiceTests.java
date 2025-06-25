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
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.utils.CommandResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class SecUserSecRoleServiceTests {

    @Autowired
    SecUserSecRoleService secSecUserSecRoleService;

    @Autowired
    SecUserSecRoleRepository secSecUserSecRoleRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    UserService userService;

    @Autowired
    SecRoleRepository secRoleRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void get_secSecUserSecRole_with_success() {
        SecUserSecRole secSecUserSecRole = builder.given_a_user_role();
        assertThat(secSecUserSecRoleService.find(secSecUserSecRole.getSecUser(), secSecUserSecRole.getSecRole())).isPresent();
    }

    @Test
    void get_unexisting_secSecUserSecRole() {
        SecUserSecRole secSecUserSecRole = builder.given_a_not_persisted_user_role(builder.given_a_user(), "ROLE_ADMIN");
        assertThat(secSecUserSecRoleService.find(secSecUserSecRole.getSecUser(), secSecUserSecRole.getSecRole())).isEmpty();
    }

    @Test
    void list_all_role_for_a_user() {
        assertThat(secSecUserSecRoleService.list(builder.given_superadmin()).stream()
                .map(SecUserSecRole::getSecRole)
                .map(SecRole::getAuthority))
                .contains("ROLE_SUPER_ADMIN");


        assertThat(secSecUserSecRoleService.list(builder.given_a_user()).stream()
                .map(SecUserSecRole::getSecRole)
                .map(SecRole::getAuthority))
                .contains("ROLE_USER").doesNotContain("ROLE_ADMIN");
    }

    @Test
    void find_highest_role() {
        User user = builder.given_a_guest();
        assertThat(secSecUserSecRoleService.getHighest(user)).isEqualTo(secRoleRepository.getGuest());

        secSecUserSecRoleRepository.save(builder.given_a_not_persisted_user_role(user, secRoleRepository.getUser()));

        assertThat(secSecUserSecRoleService.getHighest(user)).isEqualTo(secRoleRepository.getUser());

        secSecUserSecRoleRepository.save(builder.given_a_not_persisted_user_role(user, secRoleRepository.getAdmin()));

        assertThat(secSecUserSecRoleService.getHighest(user)).isEqualTo(secRoleRepository.getAdmin());

        secSecUserSecRoleRepository.save(builder.given_a_not_persisted_user_role(user, secRoleRepository.getSuperAdmin()));

        assertThat(secSecUserSecRoleService.getHighest(user)).isEqualTo(secRoleRepository.getSuperAdmin());
    }


    @Test
    void add_valid_secUser_SecRole_with_success() {
        SecUserSecRole secSecUserSecRole = builder.given_a_not_persisted_user_role(builder.given_a_user(), secRoleRepository.getAdmin());

        CommandResponse commandResponse = secSecUserSecRoleService.add(secSecUserSecRole.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }

    @Test
    @WithMockUser(username = "user")
    void add_valid_secUser_SecRole_with_success_with_admin_as_a_user() {
        SecUserSecRole secSecUserSecRole = builder.given_a_not_persisted_user_role(builder.given_a_user(), secRoleRepository.getAdmin());

        Assertions.assertThrows(ForbiddenException.class, () -> {
            secSecUserSecRoleService.add(secSecUserSecRole.toJsonObject());
        });
    }


    @Test
    void add_already_existing_secSecUserSecRole_fails() {
        SecUserSecRole secSecUserSecRole = builder.given_a_not_persisted_user_role(builder.given_a_user(), secRoleRepository.getAdmin());
        builder.persistAndReturn(secSecUserSecRole);
        Assertions.assertThrows(AlreadyExistException.class, () -> {
            secSecUserSecRoleService.add(secSecUserSecRole.toJsonObject().withChange("id", null));
        });
    }


    @Test
    @WithMockUser(username = "user")
    void user_cannot_add_user_role_to_a_guest() {
        SecUserSecRole secSecUserSecRole = builder.given_a_not_persisted_user_role(builder.given_a_guest(), secRoleRepository.getUser());
        entityManager.refresh(secSecUserSecRole.getSecUser());
        Assertions.assertThrows(ForbiddenException.class, () -> {
            secSecUserSecRoleService.add(secSecUserSecRole.toJsonObject().withChange("id", null));
        });
    }

    @Test
    void delete_secSecUserSecRole_with_success() {
        SecUserSecRole secSecUserSecRole = builder.given_a_not_persisted_user_role(builder.given_a_user(), secRoleRepository.getAdmin());
        builder.persistAndReturn(secSecUserSecRole);

        CommandResponse commandResponse = secSecUserSecRoleService.delete(secSecUserSecRole, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }

    @Test
    @WithMockUser(username = "user")
    void delete_secSecUserSecRole_with_simple_user_fail() {
        SecUserSecRole secSecUserSecRole = builder.given_a_not_persisted_user_role(builder.given_default_user(), secRoleRepository.getGuest());
        builder.persistAndReturn(secSecUserSecRole);
        Assertions.assertThrows(ForbiddenException.class, () -> {
            secSecUserSecRoleService.delete(secSecUserSecRole, null, null, true);
        });
    }


    @Test
    @WithMockUser(username = "user")
    void delete_secSecUserSecRole_to_remove_its_own_role() {
        SecUserSecRole secSecUserSecRole = builder.given_a_not_persisted_user_role(builder.given_default_user(), secRoleRepository.getAdmin());
        builder.persistAndReturn(secSecUserSecRole);
        Assertions.assertThrows(ForbiddenException.class, () -> {
            secSecUserSecRoleService.delete(secSecUserSecRole, null, null, true);
        });
    }

    @Test
    void re_define_role() {
        User user = builder.given_a_guest();

        secSecUserSecRoleService.define(user, secRoleRepository.getGuest());
        assertThat(secSecUserSecRoleService.list(user).stream().map(x -> x.getSecRole().getAuthority()))
                .containsExactlyInAnyOrder("ROLE_GUEST");

        secSecUserSecRoleService.define(user, secRoleRepository.getUser());
        assertThat(secSecUserSecRoleService.list(user).stream().map(x -> x.getSecRole().getAuthority()))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_GUEST");

        secSecUserSecRoleService.define(user, secRoleRepository.getAdmin());
        assertThat(secSecUserSecRoleService.list(user).stream().map(x -> x.getSecRole().getAuthority()))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_GUEST", "ROLE_ADMIN");

        secSecUserSecRoleService.define(user, secRoleRepository.getSuperAdmin());
        assertThat(secSecUserSecRoleService.list(user).stream().map(x -> x.getSecRole().getAuthority()))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_GUEST", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");

        secSecUserSecRoleService.define(user, secRoleRepository.getAdmin());
        assertThat(secSecUserSecRoleService.list(user).stream().map(x -> x.getSecRole().getAuthority()))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_GUEST", "ROLE_ADMIN");

        secSecUserSecRoleService.define(user, secRoleRepository.getUser());
        assertThat(secSecUserSecRoleService.list(user).stream().map(x -> x.getSecRole().getAuthority()))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_GUEST");

        secSecUserSecRoleService.define(user, secRoleRepository.getGuest());
        assertThat(secSecUserSecRoleService.list(user).stream().map(x -> x.getSecRole().getAuthority()))
                .containsExactlyInAnyOrder("ROLE_GUEST");

    }


}
