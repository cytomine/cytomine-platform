package be.cytomine.service.meta;

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
import be.cytomine.domain.meta.Configuration;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.repository.meta.ConfigurationRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ConfigurationServiceTests {

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ConfigurationRepository configurationRepository;

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

    @Test
    void list_all_configuration_with_success() {
        Configuration configuration = builder.given_a_configuration("xxx");
        assertThat(configuration).isIn(configurationService.list());
    }

    @Test
    void find_configuration_with_success() {
        Configuration configuration = builder.given_a_configuration("xxx");
        assertThat(configurationService.findByKey("xxx")).contains(configuration);
    }

    @Test
    void find_unexisting_configuration_return_empty() {
        assertThat(configurationService.findByKey("empty")).isEmpty();
    }

    @Test
    void add_valid_configuration_with_success() {
        Configuration configuration = builder.given_a_not_persisted_configuration("xxx");

        CommandResponse commandResponse = configurationService.add(configuration.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void add_configuration_with_already_existing_key() {
        Configuration configuration = builder.given_a_configuration("xxx");

        Assertions.assertThrows(AlreadyExistException.class, () -> {
                configurationService.add(configuration.toJsonObject().withChange("id", null));
        });
    }

    @Test
    void edit_valid_configuration_with_success() {
        Configuration configuration = builder.given_a_configuration("xxx");

        CommandResponse commandResponse = configurationService.update(configuration, configuration.toJsonObject().withChange("value", "NEW VALUE"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(configurationService.findByKey("xxx")).isPresent();
        Configuration edited = configurationService.findByKey("xxx").get();
        assertThat(edited.getValue()).isEqualTo("NEW VALUE");
    }


    @Test
    void delete_configuration_with_success() {
        Configuration configuration = builder.given_a_configuration("xxx");

        CommandResponse commandResponse = configurationService.delete(configuration, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(configurationService.findByKey("xxx").isEmpty());
    }
}
