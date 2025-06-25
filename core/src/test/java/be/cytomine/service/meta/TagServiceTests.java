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
import be.cytomine.domain.meta.Tag;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.meta.TagRepository;
import be.cytomine.repository.ontology.RelationTermRepository;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class TagServiceTests {

    @Autowired
    TagService tagService;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    RelationTermRepository relationTermRepository;

    @Autowired
    TermRepository termRepository;

    @Autowired
    PermissionService permissionService;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    EntityManager entityManager;

    @Test
    void list_all_tag_with_success() {
        Tag tag = builder.given_a_tag();
        assertThat(tag).isIn(tagService.list());
    }

    @Test
    void get_tag_with_success() {
        Tag tag = builder.given_a_tag();
        assertThat(tag).isEqualTo(tagService.get(tag.getId()));
    }

    @Test
    void get_unexisting_tag_return_null() {
        assertThat(tagService.get(0L)).isNull();
    }

    @Test
    void find_tag_with_success() {
        Tag tag = builder.given_a_tag();
        assertThat(tagService.find(tag.getId()).isPresent());
        assertThat(tag).isEqualTo(tagService.find(tag.getId()).get());
    }

    @Test
    void find_unexisting_tag_return_empty() {
        assertThat(tagService.find(0L)).isEmpty();
    }

    @Test
    void find_tag_by_name_with_success() {
        Tag tag = builder.given_a_tag();
        assertThat(tagService.findByName(tag.getName()).isPresent());
        assertThat(tag).isEqualTo(tagService.find(tag.getId()).get());
    }


    @Test
    void add_valid_tag_with_success() {
        Tag tag = builder.given_a_not_persisted_tag("xxx");

        CommandResponse commandResponse = tagService.add(tag.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(tagService.find(commandResponse.getObject().getId())).isPresent();
        Tag created = tagService.find(commandResponse.getObject().getId()).get();
        assertThat(created.getName()).isEqualTo(tag.getName());
    }

    @Test
    void add_tag_with_null_name_fail() {
        Tag tag = builder.given_a_not_persisted_tag("");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            tagService.add(tag.toJsonObject());
        });
    }

    @Test
    void add_tag_with_already_existing_name() {
        Tag tagWithSameName = builder.given_a_tag();
        Tag tag = builder.given_a_not_persisted_tag(tagWithSameName.getName());
        Assertions.assertThrows(AlreadyExistException.class, () -> {
            tagService.add(tag.toJsonObject());
        });
    }


    @Test
    void edit_valid_tag_with_success() {
        Tag tag = builder.given_a_tag();

        CommandResponse commandResponse = tagService.update(tag, tag.toJsonObject().withChange("name", "NEW NAME"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(tagService.find(commandResponse.getObject().getId())).isPresent();
        Tag edited = tagService.find(commandResponse.getObject().getId()).get();
        assertThat(edited.getName()).isEqualTo("NEW NAME");
    }


    @Test
    void delete_tag_with_success() {
        Tag tag = builder.given_a_tag();

        CommandResponse commandResponse = tagService.delete(tag, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(tagService.find(tag.getId()).isEmpty());
    }

    @Test
    void delete_tag_with_dependencies_with_success() {
        Tag tag = builder.given_a_tag();
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(tag, builder.given_a_project());

        CommandResponse commandResponse = tagService.delete(tag, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(entityManager.find(Tag.class, tag.getId())).isNull();
        AssertionsForClassTypes.assertThat(entityManager.find(TagDomainAssociation.class, tagDomainAssociation.getId())).isNull();
    }
}
