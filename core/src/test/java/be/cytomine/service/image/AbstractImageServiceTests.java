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
import be.cytomine.domain.image.*;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.UploadedFileRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class AbstractImageServiceTests {

    @Autowired
    AbstractImageService abstractImageService;

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

    @Autowired
    ImageInstanceService imageInstanceService;

    @Test
    void list_all_image_by_filters() {
        AbstractImage abstractImage1 = builder.given_an_abstract_image();
        abstractImage1.setOriginalFilename("karamazov");
        abstractImage1.setWidth(800);
        builder.persistAndReturn(abstractImage1);
        AbstractImage abstractImage2 = builder.given_an_abstract_image();
        abstractImage2.setOriginalFilename("deray");
        abstractImage2.setWidth(2048);
        builder.persistAndReturn(abstractImage2);

        Page<AbstractImage> images = null;
        images = abstractImageService.list(null, new ArrayList<>(List.of(new SearchParameterEntry("originalFilename", SearchOperation.ilike, "kara"))), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImage1);
        assertThat(images.getContent()).doesNotContain(abstractImage2);

        images = abstractImageService.list(null, new ArrayList<>(List.of(new SearchParameterEntry("width", SearchOperation.gte, 1024))), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImage2);
        assertThat(images.getContent()).doesNotContain(abstractImage1);

        images = abstractImageService.list(null, new ArrayList<>(List.of(new SearchParameterEntry("width", SearchOperation.in, List.of(2048)))), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImage2);
        assertThat(images.getContent()).doesNotContain(abstractImage1);

        images = abstractImageService.list(null, new ArrayList<>(List.of(new SearchParameterEntry("uploadedFile", SearchOperation.in, List.of(abstractImage2.getUploadedFile().getId())))), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImage2);
        assertThat(images.getContent()).doesNotContain(abstractImage1);

        images = abstractImageService.list(null, new ArrayList<>(
                List.of(new SearchParameterEntry("width", SearchOperation.lte, 800),
                        new SearchParameterEntry("originalFilename", SearchOperation.ilike, "kara"))
        ), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImage1);
        assertThat(images.getContent()).doesNotContain(abstractImage2);
    }

    @Test
    void list_all_image_by_project() {
        AbstractImage abstractImageInProject = builder.given_an_abstract_image();
        builder.persistAndReturn(abstractImageInProject);
        AbstractImage abstractImageNotInProject = builder.given_an_abstract_image();
        builder.persistAndReturn(abstractImageNotInProject);

        Project project = builder.given_a_project();
        builder.given_an_image_instance(abstractImageInProject, project);

        Page<AbstractImage> images = null;
        images = abstractImageService.list(project, new ArrayList<>(), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImageInProject);
        assertThat(images.getContent().stream().filter(x -> Objects.equals(x.getId(), abstractImageInProject.getId())).findFirst().get().getInProject()).isTrue();
        assertThat(images.getContent()).contains(abstractImageNotInProject);
        assertThat(images.getContent().stream().filter(x -> Objects.equals(x.getId(), abstractImageNotInProject.getId())).findFirst().get().getInProject()).isFalse();
    }

    @Test
    @WithMockUser(username = "list_all_image_by_user_storage")
    void list_all_image_by_user_storage() {
        User user = builder.given_a_user("list_all_image_by_user_storage");
        Storage storage = builder.given_a_storage(user);
        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        uploadedFile.setStorage(storage);

        AbstractImage abstractImageFromUserStorage = builder.given_an_abstract_image();
        abstractImageFromUserStorage.setOriginalFilename("match");
        abstractImageFromUserStorage.setUploadedFile(uploadedFile);
        builder.persistAndReturn(abstractImageFromUserStorage);

        AbstractImage abstractImageFromAnotherStorage = builder.given_an_abstract_image();
        abstractImageFromAnotherStorage.setOriginalFilename("match");
        builder.persistAndReturn(abstractImageFromAnotherStorage);

        Page<AbstractImage> images = null;
        images = abstractImageService.list(null, new ArrayList<>(List.of(new SearchParameterEntry("originalFilename", SearchOperation.ilike, "match"))), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImageFromUserStorage);
        assertThat(images.getContent()).doesNotContain(abstractImageFromAnotherStorage);
    }

    @Test
    void get_uploaded_file_by_user() {
        AbstractImage abstractImage = builder.given_an_abstract_image();
        assertThat(abstractImage).isEqualTo(abstractImageService.get(abstractImage.getId()));
    }

    @Test
    void get_unexisting_abstractImage_return_null() {
        AssertionsForClassTypes.assertThat(abstractImageService.get(0L)).isNull();
    }

    @Test
    void find_abstractImage_with_success() {
        AbstractImage abstractImage = builder.given_an_abstract_image();
        AssertionsForClassTypes.assertThat(abstractImageService.find(abstractImage.getId()).isPresent());
        assertThat(abstractImage).isEqualTo(abstractImageService.find(abstractImage.getId()).get());
    }

    @Test
    void find_unexisting_abstractImage_return_empty() {
        AssertionsForClassTypes.assertThat(abstractImageService.find(0L)).isEmpty();
    }


    @Test
    void detect_if_unused_abstract_image_is_unused() {
        AbstractImage abstractImage = builder.given_an_abstract_image();
        assertThat(abstractImageService.isAbstractImageUsed(abstractImage.getId())).isFalse();
    }

    @Test
    void detect_if_used_abstract_image_is_used() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        assertThat(abstractImageService.isAbstractImageUsed(imageInstance.getBaseImage().getId())).isTrue();
    }

    @Test
    void detect_if_unused_abstract_image_is_in_unused_list() {
        AbstractImage abstractImage = builder.given_an_abstract_image();
        assertThat(abstractImageService.listUnused()).contains(abstractImage);
    }

    @Test
    void detect_if_used_abstract_image_is_missing_from_unused_list() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        assertThat(abstractImageService.listUnused()).doesNotContain(imageInstance.getBaseImage());
    }


    @Test
    void add_valid_abstract_image_with_success() {
        AbstractImage abstractImage = builder.given_a_not_persisted_abstract_image();

        CommandResponse commandResponse = abstractImageService.add(abstractImage.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(abstractImageService.find(commandResponse.getObject().getId())).isPresent();
        AbstractImage created = abstractImageService.find(commandResponse.getObject().getId()).get();
    }

    @Test
    void add_valid_abstract_image_with_bad_num_field_width() {
        AbstractImage abstractImage = builder.given_a_not_persisted_abstract_image();
        abstractImage.setWidth(0);
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            abstractImageService.add(abstractImage.toJsonObject());
        });
    }

    @Test
    void add_valid_abstract_image_with_bad_num_field_height() {
        AbstractImage abstractImage = builder.given_a_not_persisted_abstract_image();
        abstractImage.setHeight(0);
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            abstractImageService.add(abstractImage.toJsonObject());
        });
    }

    @Test
    void add_valid_abstract_image_with_bad_num_field_depth() {
        AbstractImage abstractImage = builder.given_a_not_persisted_abstract_image();
        abstractImage.setDepth(0);
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            abstractImageService.add(abstractImage.toJsonObject());
        });
    }

    @Test
    void add_valid_abstract_image_with_bad_num_field_duration() {
        AbstractImage abstractImage = builder.given_a_not_persisted_abstract_image();
        abstractImage.setDuration(0);
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            abstractImageService.add(abstractImage.toJsonObject());
        });
    }

    @Test
    void add_valid_abstract_image_with_bad_num_field_channels() {
        AbstractImage abstractImage = builder.given_a_not_persisted_abstract_image();
        abstractImage.setChannels(0);
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            abstractImageService.add(abstractImage.toJsonObject());
        });
    }

    @Test
    void edit_abstract_image_with_success() {
        AbstractImage abstractImage = builder.given_a_not_persisted_abstract_image();
        abstractImage.setHeight(10000);
        abstractImage.setWidth(1000);
        abstractImage.setDuration(1);
        abstractImage.setOriginalFilename("OLDNAME");
        abstractImage = builder.persistAndReturn(abstractImage);

        JsonObject jsonObject = abstractImage.toJsonObject();
        jsonObject.put("height", 90000);
        jsonObject.put("width", 9000);
        jsonObject.put("duration", 2);
        jsonObject.put("originalFilename", "NEWNAME");

        CommandResponse commandResponse = abstractImageService.edit(jsonObject, true);
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(abstractImageService.find(commandResponse.getObject().getId())).isPresent();
        AbstractImage updated = abstractImageService.find(commandResponse.getObject().getId()).get();

        assertThat(updated.getHeight()).isEqualTo(90000);
        assertThat(updated.getWidth()).isEqualTo(9000);
        assertThat(updated.getDuration()).isEqualTo(2);
        assertThat(updated.getOriginalFilename()).isEqualTo("NEWNAME");
    }

    @Test
    void edit_abstract_image_magnification() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        AbstractImage abstractImage = imageInstance.getBaseImage();

        assertThat(imageInstance.getPhysicalSizeX()).isEqualTo(abstractImage.getPhysicalSizeX());
        assertThat(imageInstance.getMagnification()).isEqualTo(abstractImage.getMagnification());

        JsonObject jsonObject = abstractImage.toJsonObject();
        jsonObject.put("physicalSizeX", 2.5d);
        jsonObject.put("magnification", 20);

        CommandResponse commandResponse = abstractImageService.update(abstractImage, jsonObject);
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(abstractImageService.find(commandResponse.getObject().getId())).isPresent();
        AbstractImage updated = abstractImageService.find(commandResponse.getObject().getId()).get();
        entityManager.refresh(abstractImage);

        assertThat(updated.getPhysicalSizeX()).isEqualTo(2.5);
        assertThat(updated.getMagnification()).isEqualTo(20);

        entityManager.refresh(imageInstance);

        assertThat(imageInstance.getPhysicalSizeX()).isEqualTo(2.5);
        assertThat(imageInstance.getMagnification()).isEqualTo(20);

        jsonObject = imageInstance.toJsonObject();
        jsonObject.put("magnification", 40);

        commandResponse = imageInstanceService.update(imageInstance, jsonObject);
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        entityManager.refresh(imageInstance);

        assertThat(imageInstance.getPhysicalSizeX()).isEqualTo(2.5);
        assertThat(imageInstance.getMagnification()).isEqualTo(40);

        jsonObject = abstractImage.toJsonObject();
        jsonObject.put("physicalSizeX", 6);
        jsonObject.put("magnification", 10);

        commandResponse = abstractImageService.update(abstractImage, jsonObject);
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(abstractImage.getPhysicalSizeX()).isEqualTo(6);
        assertThat(abstractImage.getMagnification()).isEqualTo(10);
    }

    @Test
    void delete_abstract_image_with_success() {
        AbstractImage abstractImage = builder.given_an_abstract_image();

        CommandResponse commandResponse = abstractImageService.delete(abstractImage, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(abstractImageService.find(abstractImage.getId()).isEmpty());
    }

    @Test
    void delete_abstract_image_with_dependencies_with_success() {
        AbstractImage abstractImage = builder.given_an_abstract_image();

        AttachedFile attachedFile = builder.given_a_attached_file(abstractImage);

        assertThat(entityManager.find(AttachedFile.class, attachedFile.getId())).isNotNull();

        CommandResponse commandResponse = abstractImageService.delete(abstractImage, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(entityManager.find(AttachedFile.class, attachedFile.getId())).isNull();
    }

    @Test
    void delete_abstract_image_with_image_in_project_is_refused() {
        AbstractImage abstractImage = builder.given_an_abstract_image();
        ImageInstance imageInstance = builder.given_an_image_instance(abstractImage, builder.given_a_project());
        Assertions.assertThrows(ForbiddenException.class, () -> {
            abstractImageService.delete(abstractImage, null, null, true);
        });
    }


}
