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
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.service.CurrentRoleService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@Transactional
public class CurrentRoleServiceTests {


    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CurrentRoleService currentRoleService;

    @Autowired
    SecRoleRepository secRoleRepository;

    @Test
    @WithMockUser(username = "superadmin")
    public void find_role_for_superadmin() {
        assertThat(currentRoleService.findRealRole(builder.given_superadmin()))
                .contains(secRoleRepository.getSuperAdmin());

        assertThat(currentRoleService.findRealAuthorities(builder.given_superadmin()))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        assertThat(currentRoleService.findCurrentAuthorities(builder.given_superadmin()))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");

        assertThat(currentRoleService.isAdminByNow(builder.given_superadmin())).isTrue();
        assertThat(currentRoleService.isUserByNow(builder.given_superadmin())).isTrue();
        assertThat(currentRoleService.isGuestByNow(builder.given_superadmin())).isFalse();
        assertThat(currentRoleService.isAdmin(builder.given_superadmin())).isTrue();
        assertThat(currentRoleService.isUser(builder.given_superadmin())).isTrue();
        assertThat(currentRoleService.isGuest(builder.given_superadmin())).isFalse();
        assertThat(currentRoleService.hasCurrentUserAdminRole(builder.given_superadmin())).isTrue();
    }

    @Test
    @WithMockUser(username = "admin")
    public void find_role_for_admin() {
        assertThat(currentRoleService.findRealRole(builder.given_default_admin()))
                .contains(secRoleRepository.getAdmin());

        assertThat(currentRoleService.findRealAuthorities(builder.given_default_admin()))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(currentRoleService.findCurrentAuthorities(builder.given_default_admin()))
                .containsExactlyInAnyOrder("ROLE_USER");

        assertThat(currentRoleService.isAdminByNow(builder.given_default_admin())).isFalse();
        assertThat(currentRoleService.isUserByNow(builder.given_default_admin())).isTrue();
        assertThat(currentRoleService.isGuestByNow(builder.given_default_admin())).isFalse();
        assertThat(currentRoleService.isAdmin(builder.given_default_admin())).isTrue();
        assertThat(currentRoleService.isUser(builder.given_default_admin())).isTrue();
        assertThat(currentRoleService.isGuest(builder.given_default_admin())).isFalse();
        assertThat(currentRoleService.hasCurrentUserAdminRole(builder.given_default_admin())).isTrue();
    }

    @Test
    @WithMockUser(username = "user")
    public void find_role_for_user() {
        assertThat(currentRoleService.findRealRole(builder.given_default_user()))
                .contains(secRoleRepository.getUser());

        assertThat(currentRoleService.findRealAuthorities(builder.given_default_user()))
                .containsExactlyInAnyOrder("ROLE_USER");
        assertThat(currentRoleService.findCurrentAuthorities(builder.given_default_user()))
                .containsExactlyInAnyOrder("ROLE_USER");

        assertThat(currentRoleService.isAdminByNow(builder.given_default_user())).isFalse();
        assertThat(currentRoleService.isUserByNow(builder.given_default_user())).isTrue();
        assertThat(currentRoleService.isGuestByNow(builder.given_default_user())).isFalse();
        assertThat(currentRoleService.isAdmin(builder.given_default_user())).isFalse();
        assertThat(currentRoleService.isUser(builder.given_default_user())).isTrue();
        assertThat(currentRoleService.isGuest(builder.given_default_user())).isFalse();
        assertThat(currentRoleService.hasCurrentUserAdminRole(builder.given_default_user())).isFalse();
    }

    @Test
    @WithMockUser(username = "guest")
    public void find_role_for_guest() {
        assertThat(currentRoleService.findRealRole(builder.given_a_guest()))
                .contains(secRoleRepository.getGuest());

        assertThat(currentRoleService.findRealAuthorities(builder.given_a_guest()))
                .containsExactlyInAnyOrder("ROLE_GUEST");
        assertThat(currentRoleService.findCurrentAuthorities(builder.given_a_guest()))
                .containsExactlyInAnyOrder("ROLE_GUEST");

        assertThat(currentRoleService.isAdminByNow(builder.given_default_guest())).isFalse();
        assertThat(currentRoleService.isUserByNow(builder.given_default_guest())).isFalse();
        assertThat(currentRoleService.isGuestByNow(builder.given_default_guest())).isTrue();
        assertThat(currentRoleService.isAdmin(builder.given_default_guest())).isFalse();
        assertThat(currentRoleService.isUser(builder.given_default_guest())).isFalse();
        assertThat(currentRoleService.isGuest(builder.given_default_guest())).isTrue();
        assertThat(currentRoleService.hasCurrentUserAdminRole(builder.given_default_guest())).isFalse();
    }


    @Test
    @WithMockUser(username = "admin")
    public void open_close_admin_session_as_admin() {

        assertThat(currentRoleService.findRealRole(builder.given_default_admin()))
                .contains(secRoleRepository.getAdmin());
        assertThat(currentRoleService.findCurrentAuthorities(builder.given_default_admin()))
                .containsExactlyInAnyOrder("ROLE_USER");

        assertThat(currentRoleService.isAdminByNow(builder.given_default_admin())).isFalse();
        assertThat(currentRoleService.isUserByNow(builder.given_default_admin())).isTrue();

        currentRoleService.activeAdminSession(builder.given_default_admin());

        assertThat(currentRoleService.isAdminByNow(builder.given_default_admin())).isTrue();
        assertThat(currentRoleService.isUserByNow(builder.given_default_admin())).isTrue();

        currentRoleService.closeAdminSession(builder.given_default_admin());

        assertThat(currentRoleService.isAdminByNow(builder.given_default_admin())).isFalse();
        assertThat(currentRoleService.isUserByNow(builder.given_default_admin())).isTrue();
    }

    @Test
    @WithMockUser(username = "user")
    public void open_close_admin_session_as_user() {

        assertThat(currentRoleService.isAdminByNow(builder.given_default_user())).isFalse();
        assertThat(currentRoleService.isUserByNow(builder.given_default_user())).isTrue();

        Assertions.assertThrows(ForbiddenException.class, () -> {
            currentRoleService.activeAdminSession(builder.given_default_user());
        });

        assertThat(currentRoleService.isAdminByNow(builder.given_default_user())).isFalse();
        assertThat(currentRoleService.isUserByNow(builder.given_default_user())).isTrue();
    }
}
