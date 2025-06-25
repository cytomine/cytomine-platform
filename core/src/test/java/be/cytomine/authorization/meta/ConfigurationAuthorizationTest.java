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
import be.cytomine.authorization.AbstractAuthorizationTest;
import be.cytomine.authorization.CRDAuthorizationTest;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Configuration;
import be.cytomine.domain.meta.ConfigurationReadingRole;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.service.PermissionService;
import be.cytomine.service.meta.AttachedFileService;
import be.cytomine.service.meta.ConfigurationService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class ConfigurationAuthorizationTest extends AbstractAuthorizationTest {


    @Autowired
    ConfigurationService configurationService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    Configuration configForAdmin;

    Configuration configForUser;

    Configuration configForAll;

    @BeforeEach
    public void before() throws Exception {

        if (configForAdmin == null) {
            configForAdmin = builder.given_a_not_persisted_configuration("ADMIN");
            configForAdmin.setReadingRole(ConfigurationReadingRole.ADMIN);
            builder.persistAndReturn(configForAdmin);
            ;
        }
        if (configForUser == null) {
            configForUser = builder.given_a_not_persisted_configuration("USER");
            configForUser.setReadingRole(ConfigurationReadingRole.USER);
            builder.persistAndReturn(configForUser);
        }
        if (configForAll == null) {
            configForAll = builder.given_a_not_persisted_configuration("ALL");
            configForAll.setReadingRole(ConfigurationReadingRole.ALL);
            builder.persistAndReturn(configForAll);
        }
    }


    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_config() {
        assertThat(configurationService.list())
                .contains(configForAdmin, configForUser, configForAll);
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_can_list_config() {
        assertThat(configurationService.list())
                .contains(configForUser, configForAll).doesNotContain(configForAdmin);
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_can_list_config() {
        assertThat(configurationService.list())
                .contains(configForAll).doesNotContain(configForUser, configForAdmin);
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_read_config() {
        expectOK(() -> {configurationService.findByKey(configForAdmin.getKey());});
        expectOK(() -> {configurationService.findByKey(configForUser.getKey());});
        expectOK(() -> {configurationService.findByKey(configForAll.getKey());});
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_can_read_config() {
        expectForbidden(() -> {configurationService.findByKey(configForAdmin.getKey());});
        expectOK(() -> {configurationService.findByKey(configForUser.getKey());});
        expectOK(() -> {configurationService.findByKey(configForAll.getKey());});
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_can_read_config() {
        expectForbidden(() -> {configurationService.findByKey(configForAdmin.getKey());});
        expectForbidden(() -> {configurationService.findByKey(configForUser.getKey());});
        expectOK(() -> {configurationService.findByKey(configForAll.getKey());});
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_create_config() {
        expectOK(() -> {configurationService.add(configForUser.toJsonObject().withChange("id", null).withChange("key", UUID.randomUUID().toString()));});
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_cannot_create_config() {
        expectForbidden(() -> {configurationService.add(configForUser.toJsonObject().withChange("id", null).withChange("key", UUID.randomUUID().toString()));});
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_edit_config() {
        expectOK(() -> {configurationService.update(configForUser, configForUser.toJsonObject().withChange("value", "newvalue"));});
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_cannot_edit_config() {
        expectForbidden(() -> {configurationService.update(configForUser, configForUser.toJsonObject().withChange("value", "newvalue"));});
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_delete_config() {
        Configuration configuration = builder.given_a_configuration("xxx");
        expectOK(() -> {configurationService.delete(configuration, null, null, false);});
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_cannot_delete_config() {
        Configuration configuration = builder.given_a_configuration("xxx");
        expectForbidden(() -> {configurationService.delete(configuration, null, null, false);});
    }

}
