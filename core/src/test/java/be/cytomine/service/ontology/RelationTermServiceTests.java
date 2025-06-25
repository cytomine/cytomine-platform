package be.cytomine.service.ontology;

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
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.RelationTermRepository;
import be.cytomine.service.CommandService;
import be.cytomine.utils.CommandResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class RelationTermServiceTests {

    @Autowired
    RelationTermService relationTermService;

    @Autowired
    RelationTermRepository relationTermRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Test
    void find_relation_term_with_success() {
        RelationTerm relationTerm = builder.given_a_relation_term();
        Optional<RelationTerm> result = relationTermService.find(relationTerm.getRelation(), relationTerm.getTerm1(), relationTerm.getTerm2());
        assertThat(result).isPresent();
        assertThat(relationTerm).isEqualTo(result.get());
    }

    @Test
    void find_unexisting_relation_term_return_empty() {
        assertThat(relationTermService.find(builder.given_a_relation(), builder.given_a_term(), builder.given_a_term())).isEmpty();
    }

    @Test
    void list_relation_by_term() {
        Term parent = builder.given_a_term();
        Term child1 = builder.given_a_term(parent.getOntology());
        Term child2 = builder.given_a_term(parent.getOntology());
        RelationTerm parent1 = builder.given_a_relation_term(parent, child1);
        RelationTerm parent2 = builder.given_a_relation_term(parent, child2);

        assertThat(relationTermService.list(parent)).asList().containsExactlyInAnyOrder(parent1, parent2);
        assertThat(relationTermService.list(child1)).asList().containsExactly(parent1);
        assertThat(relationTermService.list(child2)).asList().containsExactly(parent2);
    }

    @Test
    void list_relation_by_term_and_position() {
        Term parent = builder.given_a_term();
        Term child1 = builder.given_a_term(parent.getOntology());
        Term child2 = builder.given_a_term(parent.getOntology());
        RelationTerm parent1 = builder.given_a_relation_term(parent, child1);
        RelationTerm parent2 = builder.given_a_relation_term(parent, child2);

        assertThat(relationTermService.list(parent, "1")).asList().containsExactlyInAnyOrder(parent1, parent2);
        assertThat(relationTermService.list(child1, "2")).asList().containsExactly(parent1);
        assertThat(relationTermService.list(parent, "2")).asList().isEmpty();
        assertThat(relationTermService.list(child1, "1")).asList().isEmpty();
        assertThat(relationTermService.list(child2, "1")).asList().isEmpty();
    }

    @Test
    void add_valid_relation_term_with_success() {
        Term parent = builder.given_a_term();
        Term child1 = builder.given_a_term(parent.getOntology());
        RelationTerm parentRelationTerm = BasicInstanceBuilder.given_a_not_persisted_relation_term(builder.given_a_relation(), parent, child1);

        CommandResponse commandResponse = relationTermService.add(parentRelationTerm.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(relationTermService.find(builder.given_a_relation(), parent, child1)).isPresent();
    }

    @Test
    void add_relation_term_with_null_term_fail() {
        Term parent = builder.given_a_term();
        RelationTerm parentRelationTerm = BasicInstanceBuilder.given_a_not_persisted_relation_term(builder.given_a_relation(), parent, null);
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            relationTermService.add(parentRelationTerm.toJsonObject());
        });
    }

    @Test
    void undo_redo_term_creation_with_success() {
        Term parent = builder.given_a_term();
        Term child1 = builder.given_a_term(parent.getOntology());
        RelationTerm parentRelationTerm = builder.given_a_not_persisted_relation_term(builder.given_a_relation(), parent, child1);

        CommandResponse commandResponse = relationTermService.add(parentRelationTerm.toJsonObject());

        commandService.undo();

        assertThat(relationTermService.find(builder.given_a_relation(), parent, child1)).isEmpty();

        commandService.redo();

        assertThat(relationTermService.find(builder.given_a_relation(), parent, child1)).isPresent();

    }

    @Test
    void delete_relation_term_with_success() {
        Term parent = builder.given_a_term();
        Term child1 = builder.given_a_term(parent.getOntology());
        RelationTerm parentRelationTerm = builder.given_a_relation_term(parent, child1);

        CommandResponse commandResponse = relationTermService.delete(parentRelationTerm, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(relationTermService.find(builder.given_a_relation(), parent, child1)).isEmpty();
    }

    @Test
    void undo_redo_term_deletion_with_success() {
        Term parent = builder.given_a_term();
        Term child1 = builder.given_a_term(parent.getOntology());
        RelationTerm parentRelationTerm = builder.given_a_relation_term(parent, child1);

        CommandResponse commandResponse = relationTermService.delete(parentRelationTerm, null, null, true);

        assertThat(relationTermService.find(builder.given_a_relation(), parent, child1)).isEmpty();

        commandService.undo();

        assertThat(relationTermService.find(builder.given_a_relation(), parent, child1)).isPresent();

        commandService.redo();

        assertThat(relationTermService.find(builder.given_a_relation(), parent, child1)).isEmpty();
    }


}
