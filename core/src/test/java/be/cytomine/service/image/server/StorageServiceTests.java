package be.cytomine.service.image.server;

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
import be.cytomine.domain.image.server.Storage;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.server.StorageRepository;
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
import static org.springframework.security.acls.domain.BasePermission.*;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class StorageServiceTests {

    @Autowired
    StorageService storageService;

    @Autowired
    StorageRepository storageRepository;

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
    void list_all_storage_with_success() {
        Storage storage = builder.given_a_storage();
        assertThat(storage).isIn(storageService.list());
    }

    @Test
    void list_user_storage_with_success() {
        Storage storage = builder.given_a_storage();
        assertThat(storage).isIn(storageService.list(builder.given_superadmin(), null));
    }

    @Test
    void get_storage_with_success() {
        Storage storage = builder.given_a_storage();
        assertThat(storage).isEqualTo(storageService.get(storage.getId()));
    }

    @Test
    void get_unexisting_storage_return_null() {
        assertThat(storageService.get(0L)).isNull();
    }

    @Test
    void find_storage_with_success() {
        Storage storage = builder.given_a_storage();
        assertThat(storageService.find(storage.getId()).isPresent());
        assertThat(storage).isEqualTo(storageService.find(storage.getId()).get());
    }

    @Test
    void find_unexisting_storage_return_empty() {
        assertThat(storageService.find(0L)).isEmpty();
    }

    @Test
    void add_valid_storage_with_success() {
        Storage storage = builder.given_a_not_persisted_storage();
        CommandResponse commandResponse = storageService.add(storage.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(storageService.find(commandResponse.getObject().getId())).isPresent();
        Storage created = storageService.find(commandResponse.getObject().getId()).get();
        assertThat(created.getName()).isEqualTo(storage.getName());
    }

    @Test
    void add_storage_with_null_name_fail() {
        Storage storage = builder.given_a_not_persisted_storage();
        storage.setName("");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            storageService.add(storage.toJsonObject());
        });
    }

    @Test
    void add_valid_storage_grant_permission_on_creator() {
        Storage storage = builder.given_a_not_persisted_storage();
        CommandResponse commandResponse = storageService.add(storage.toJsonObject());
        Storage createdStorage = storageService.find(commandResponse.getObject().getId()).get();
        assertThat(permissionService.hasACLPermission(createdStorage, "superadmin", READ)).isTrue();
        assertThat(permissionService.hasACLPermission(createdStorage, "superadmin", WRITE)).isTrue();
        assertThat(permissionService.hasACLPermission(createdStorage, "superadmin", ADMINISTRATION)).isTrue();
    }


    @Test
    void edit_valid_storage_with_success() {
        Storage storage = builder.given_a_storage();

        CommandResponse commandResponse = storageService.update(storage, storage.toJsonObject().withChange("name", "NEW NAME"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(storageService.find(commandResponse.getObject().getId())).isPresent();
        Storage edited = storageService.find(commandResponse.getObject().getId()).get();
        assertThat(edited.getName()).isEqualTo("NEW NAME");
    }

    @Test
    void delete_storage_with_success() {
        Storage storage = builder.given_a_storage();

        CommandResponse commandResponse = storageService.delete(storage, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(storageService.find(storage.getId()).isEmpty());
    }
}
