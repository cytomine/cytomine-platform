package be.cytomine.service.image;

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
import be.cytomine.domain.image.NestedImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.UploadedFileRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class NestedImageInstanceServiceTests {

    @Autowired
    NestedImageInstanceService nestedImageInstanceService;

    @Autowired
    UploadedFileRepository uploadedFileRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    EntityManager entityManager;


    @Test
    void list_all_nested_image_image_by_image_instance() {
        NestedImageInstance nestedImageInstance1 = builder.given_a_nested_image_instance();
        NestedImageInstance nestedImageInstance2 = builder.given_a_nested_image_instance();

        List<NestedImageInstance> list = nestedImageInstanceService.list(nestedImageInstance1.getParent());

        assertThat(list).contains(nestedImageInstance1);
        assertThat(list).doesNotContain(nestedImageInstance2);
    }

    @Test
    void get_nested_image_intance_with_success() {
        NestedImageInstance nestedImageInstance = builder.given_a_nested_image_instance();
        assertThat(nestedImageInstance).isEqualTo(nestedImageInstanceService.get(nestedImageInstance.getId()));
    }

    @Test
    void get_unexisting_nestedImageInstance_return_null() {
        AssertionsForClassTypes.assertThat(nestedImageInstanceService.get(0L)).isNull();
    }

    @Test
    void find_nested_image_instance_with_success() {
        NestedImageInstance nestedImageInstance = builder.given_a_nested_image_instance();
        AssertionsForClassTypes.assertThat(nestedImageInstanceService.find(nestedImageInstance.getId()).isPresent());
        assertThat(nestedImageInstance).isEqualTo(nestedImageInstanceService.find(nestedImageInstance.getId()).get());
    }

    @Test
    void find_unexisting_nested_image_instance_return_empty() {
        AssertionsForClassTypes.assertThat(nestedImageInstanceService.find(0L)).isEmpty();
    }

    @Test
    void add_valid_nested_image_instance_with_success() {
        NestedImageInstance nestedImageInstance = builder.given_a_not_persisted_nested_image_instance();

        CommandResponse commandResponse = nestedImageInstanceService.add(nestedImageInstance.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(nestedImageInstanceService.find(commandResponse.getObject().getId())).isPresent();
        NestedImageInstance created = nestedImageInstanceService.find(commandResponse.getObject().getId()).get();
    }



    @Test
    void add_already_existing_nested_image_instance_fails() {
        NestedImageInstance nestedImageInstance = builder.given_a_nested_image_instance();
        Assertions.assertThrows(AlreadyExistException.class, () -> {
            nestedImageInstanceService.add(nestedImageInstance.toJsonObject().withChange("id", null));
        });
    }

    @Test
    void add_valid_nested_image_instance_with_unexsting_abstract_image_fails() {
        NestedImageInstance nestedImageInstance = builder.given_a_not_persisted_nested_image_instance();
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            nestedImageInstanceService.add(nestedImageInstance.toJsonObject().withChange("baseImage", null));
        });
    }

    @Test
    void add_valid_nested_image_instance_with_unexsting_parent_fails() {
        NestedImageInstance nestedImageInstance = builder.given_a_not_persisted_nested_image_instance();
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            nestedImageInstanceService.add(nestedImageInstance.toJsonObject().withChange("parent", null));
        });
    }

    @Test
    void add_valid_nested_image_instance_with_unexsting_project_fails() {
        NestedImageInstance nestedImageInstance = builder.given_a_not_persisted_nested_image_instance();
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            nestedImageInstanceService.add(nestedImageInstance.toJsonObject().withChange("project", null));
        });
    }

    @Test
    void edit_nested_image_instance_with_success() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();

        NestedImageInstance nestedImageInstance = builder.given_a_not_persisted_nested_image_instance();
        nestedImageInstance.setProject(project1);
        nestedImageInstance = builder.persistAndReturn(nestedImageInstance);

        JsonObject jsonObject = nestedImageInstance.toJsonObject();
        jsonObject.put("project", project2.getId());

        CommandResponse commandResponse = nestedImageInstanceService.edit(jsonObject, true);
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(nestedImageInstanceService.find(commandResponse.getObject().getId())).isPresent();
        NestedImageInstance updated = nestedImageInstanceService.find(commandResponse.getObject().getId()).get();

        assertThat(updated.getProject().getId()).isEqualTo(project2.getId());
    }

    @Test
    void delete_nested_image_instance_with_success() {
        NestedImageInstance nestedImageInstance = builder.given_a_nested_image_instance();

        CommandResponse commandResponse = nestedImageInstanceService.delete(nestedImageInstance, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(nestedImageInstanceService.find(nestedImageInstance.getId()).isEmpty());
    }
}
