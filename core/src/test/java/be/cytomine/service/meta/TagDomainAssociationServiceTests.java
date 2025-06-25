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
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.repository.meta.TagDomainAssociationRepository;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class TagDomainAssociationServiceTests {

    @Autowired
    TagDomainAssociationService tagDomainAssociationService;

    @Autowired
    TagDomainAssociationRepository tagDomainAssociationRepository;

    @Autowired
    TagService tagService;

    @Autowired
    BasicInstanceBuilder builder;

    @Test
    public void specification_test() {
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());

        Specification specification =
        (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get("tag"))
                        .value(tagDomainAssociation.getTag());

        assertThat(tagDomainAssociationRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "domainClassName"))).contains(tagDomainAssociation);

    }

    @Test
    public void find_by_id() {
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        assertThat(tagDomainAssociationService.find(tagDomainAssociation.getId())).isPresent();
    }

    @Test
    public void find_by_id_that_do_not_exists() {
        assertThat(tagDomainAssociationService.find(0L)).isEmpty();
    }

    @Test
    public void get_by_id() {
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        assertThat(tagDomainAssociationService.get(tagDomainAssociation.getId())).isNotNull();
    }

    @Test
    public void list_all_for_domain() {
        Project project = builder.given_a_project();
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), project);
        TagDomainAssociation tagDomainAssociationFromOtherDomain = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        assertThat(tagDomainAssociationService.listAllByDomain(project))
                .contains(tagDomainAssociation)
                .doesNotContain(tagDomainAssociationFromOtherDomain);
    }

    @Test
    public void list_all_for_tag() {
        Project project = builder.given_a_project();
        Tag tag = builder.given_a_tag();
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(tag, project);
        TagDomainAssociation tagDomainAssociationFromOtherTag = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        assertThat(tagDomainAssociationService.listAllByTag(tag)).contains(tagDomainAssociation)
                .doesNotContain(tagDomainAssociationFromOtherTag);
    }

    @Test
    public void list_all_for_tag_and_domain() {
        Project domain1 = builder.given_a_project();
        UserAnnotation domain2 = builder.given_a_user_annotation();
        Tag tag1 = builder.given_a_tag();
        Tag tag2 = builder.given_a_tag();

        TagDomainAssociation tag1Domain1 = builder.given_a_tag_association(tag1, domain1);
        TagDomainAssociation tag2Domain1 = builder.given_a_tag_association(tag2, domain1);
        TagDomainAssociation tag1Domain2 = builder.given_a_tag_association(tag1, domain2);
        TagDomainAssociation tag2Domain2 = builder.given_a_tag_association(tag2, domain2);

        assertThat(tagDomainAssociationService.list(new ArrayList<>(List.of(
                new SearchParameterEntry("tag", SearchOperation.in, List.of(tag1.getId(), tag2.getId())),
                new SearchParameterEntry("domainIdent", SearchOperation.in, List.of(domain1.getId(), domain2.getId()))
        )))).contains(tag1Domain1, tag2Domain1, tag1Domain2, tag2Domain2);

        assertThat(tagDomainAssociationService.list(new ArrayList<>(List.of(
                new SearchParameterEntry("tag", SearchOperation.in, List.of(tag1.getId())),
                new SearchParameterEntry("domainIdent", SearchOperation.in, List.of(domain1.getId(), domain2.getId()))
        )))).contains(tag1Domain1, tag1Domain2).doesNotContain(tag2Domain1, tag2Domain2);

        assertThat(tagDomainAssociationService.list(new ArrayList<>(List.of(
                new SearchParameterEntry("tag", SearchOperation.in, List.of(tag1.getId())),
                new SearchParameterEntry("domainIdent", SearchOperation.in, List.of(domain1.getId()))
        )))).contains(tag1Domain1).doesNotContain(tag2Domain1);

        assertThat(tagDomainAssociationService.list(new ArrayList<>(List.of(
                new SearchParameterEntry("tag", SearchOperation.in, List.of(builder.given_a_tag().getId())),
                new SearchParameterEntry("domainIdent", SearchOperation.in, List.of(domain1.getId()))
        )))).doesNotContain(tag1Domain1, tag1Domain2, tag2Domain1, tag2Domain2);
    }


    @Test
    public void create_tag_association() throws ClassNotFoundException {
        CommandResponse add = tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), builder.given_a_project()).toJsonObject());
        assertThat(tagDomainAssociationService.listAllByTag(((TagDomainAssociation)add.getObject()).getTag())).hasSize(1);
    }

    @Test
    public void delete_tag_association() {
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), builder.given_a_project());
        CommandResponse delete = tagDomainAssociationService.delete(tagDomainAssociation, null, null, false);
        assertThat(tagDomainAssociationService.listAllByTag(tagDomainAssociation.getTag())).hasSize(0);
    }

}
