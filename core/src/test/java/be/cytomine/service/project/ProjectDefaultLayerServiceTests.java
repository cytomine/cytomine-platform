package be.cytomine.service.project;

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
import be.cytomine.domain.project.ProjectDefaultLayer;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.project.ProjectDefaultLayerRepository;
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
public class ProjectDefaultLayerServiceTests {

    @Autowired
    ProjectDefaultLayerService projectDefaultLayerService;

    @Autowired
    ProjectDefaultLayerRepository projectDefaultLayerRepository;

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
    void get_projectDefaultLayer_with_success() {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();
        assertThat(projectDefaultLayer).isEqualTo(projectDefaultLayerService.get(projectDefaultLayer.getId()));
    }

    @Test
    void get_unexisting_projectDefaultLayer_return_null() {
        assertThat(projectDefaultLayerService.get(0L)).isNull();
    }

    @Test
    void find_projectDefaultLayer_with_success() {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();
        assertThat(projectDefaultLayerService.find(projectDefaultLayer.getId()).isPresent());
        assertThat(projectDefaultLayer).isEqualTo(projectDefaultLayerService.find(projectDefaultLayer.getId()).get());
    }

    @Test
    void find_unexisting_projectDefaultLayer_return_empty() {
        assertThat(projectDefaultLayerService.find(0L)).isEmpty();
    }

    @Test
    void list_all_projectDefaultLayer_by_project_with_success() {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();
        ProjectDefaultLayer projectDefaultLayerFromAnotherProject = builder.given_a_project_default_layer();
        assertThat(projectDefaultLayer).isIn(projectDefaultLayerService.listByProject(projectDefaultLayer.getProject()));
        assertThat(projectDefaultLayerFromAnotherProject).isNotIn(projectDefaultLayerService.listByProject(projectDefaultLayer.getProject()));
    }

    @Test
    void add_valid_projectDefaultLayer_with_success() {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_not_persisted_project_default_layer();

        CommandResponse commandResponse = projectDefaultLayerService.add(projectDefaultLayer.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(projectDefaultLayerService.find(commandResponse.getObject().getId())).isPresent();
        ProjectDefaultLayer created = projectDefaultLayerService.find(commandResponse.getObject().getId()).get();
        assertThat(created.getProject()).isEqualTo(projectDefaultLayer.getProject());
    }

    @Test
    void add_projectDefaultLayer_with_bad_project() {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_not_persisted_project_default_layer();
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            projectDefaultLayerService.add(projectDefaultLayer.toJsonObject().withChange("project", 0L));
        });
    }

    @Test
    void add_projectDefaultLayer_with_bad_user() {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_not_persisted_project_default_layer();
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            projectDefaultLayerService.add(projectDefaultLayer.toJsonObject().withChange("user", 0L));
        });
    }

    @Test
    void add_already_existing_projectDefaultLayer_fails() {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();
        Assertions.assertThrows(AlreadyExistException.class, () -> {
            projectDefaultLayerService.add(projectDefaultLayer.toJsonObject().withChange("id", null));
        });
    }

    @Test
    void delete_projectDefaultLayer_with_success() {
        ProjectDefaultLayer projectDefaultLayer = builder.given_a_project_default_layer();

        CommandResponse commandResponse = projectDefaultLayerService.delete(projectDefaultLayer, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(projectDefaultLayerService.find(projectDefaultLayer.getId()).isEmpty());
    }
}
