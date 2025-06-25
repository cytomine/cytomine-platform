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
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
public class SecRoleServiceTests {


    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecRoleService secRoleService;

    @Test
    public void find_role_from_authority() {
        assertThat(secRoleService.findByAuthority("ROLE_GUEST")).isPresent();
        assertThat(secRoleService.findByAuthority("ROLE_USER")).isPresent();
        assertThat(secRoleService.findByAuthority("ROLE_ADMIN")).isPresent();
    }

    @Test
    public void find_role_from_id() {
        assertThat(secRoleService.find(secRoleService.findByAuthority("ROLE_GUEST").get().getId())).isPresent();
    }

    @Test
    public void list_roles() {
        assertThat(secRoleService.list().stream().map(SecRole::getAuthority))
                .contains("ROLE_GUEST", "ROLE_USER", "ROLE_ADMIN");
    }
}
