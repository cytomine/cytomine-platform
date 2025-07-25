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

import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.ontology.RelationTermRepository;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.utils.CommandResponse;

import static be.cytomine.service.search.RetrievalService.CBIR_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.*;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class OntologyServiceTests {

    @Autowired
    OntologyService ontologyService;

    @Autowired
    OntologyRepository ontologyRepository;

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
    ProjectService projectService;

    private static WireMockServer wireMockServer;

    private static void setupStub() {
        /* Simulate call to CBIR */
        wireMockServer.stubFor(post(urlPathEqualTo(CBIR_API_BASE_PATH + "/storages"))
            .withRequestBody(matching(".*"))
            .willReturn(aResponse().withBody(UUID.randomUUID().toString()))
        );
    }

    @BeforeAll
    public static void beforeAll() {
        wireMockServer = new WireMockServer(8888);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        setupStub();
    }

    @AfterAll
    public static void afterAll() {
        wireMockServer.stop();
    }

    @Test
    void list_all_ontology_with_success() {
        Ontology ontology = builder.given_an_ontology();
        assertThat(ontology).isIn(ontologyService.list());
    }

    @Test
    void get_ontology_with_success() {
        Ontology ontology = builder.given_an_ontology();
        assertThat(ontology).isEqualTo(ontologyService.get(ontology.getId()));
    }

    @Test
    void get_unexisting_ontology_return_null() {
        assertThat(ontologyService.get(0L)).isNull();
    }

    @Test
    void find_ontology_with_success() {
        Ontology ontology = builder.given_an_ontology();
        assertThat(ontologyService.find(ontology.getId()).isPresent());
        assertThat(ontology).isEqualTo(ontologyService.find(ontology.getId()).get());
    }

    @Test
    void find_unexisting_ontology_return_empty() {
        assertThat(ontologyService.find(0L)).isEmpty();
    }


    @Test
    void list_light_ontology() {
        Ontology ontology = builder.given_an_ontology();
        assertThat(ontologyService.listLight().stream().anyMatch(json -> json.get("id").equals(ontology.getId()))).isTrue();
    }

    @Test
    void add_valid_ontology_with_success() {
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();

        CommandResponse commandResponse = ontologyService.add(ontology.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(ontologyService.find(commandResponse.getObject().getId())).isPresent();
        Ontology created = ontologyService.find(commandResponse.getObject().getId()).get();
        assertThat(created.getName()).isEqualTo(ontology.getName());
    }

    @Test
    void add_ontology_with_null_name_fail() {
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        ontology.setName("");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            ontologyService.add(ontology.toJsonObject());
        });
    }


    @Test
    void undo_redo_ontology_creation_with_success() {
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        CommandResponse commandResponse = ontologyService.add(ontology.toJsonObject());
        assertThat(ontologyService.find(commandResponse.getObject().getId())).isPresent();
        System.out.println("id = " + commandResponse.getObject().getId() + " name = " + ontology.getName());

        commandService.undo();

        assertThat(ontologyService.find(commandResponse.getObject().getId())).isEmpty();

        commandService.redo();

        assertThat(ontologyService.find(commandResponse.getObject().getId())).isPresent();

    }

    @Test
    void redo_ontology_creation_fail_if_ontology_already_exist() {
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        CommandResponse commandResponse = ontologyService.add(ontology.toJsonObject());
        assertThat(ontologyService.find(commandResponse.getObject().getId())).isPresent();
        System.out.println("id = " + commandResponse.getObject().getId() + " name = " + ontology.getName());

        commandService.undo();

        assertThat(ontologyService.find(commandResponse.getObject().getId())).isEmpty();

        Ontology ontologyWithSameName = BasicInstanceBuilder.given_a_not_persisted_ontology();
        ontologyWithSameName.setName(ontology.getName());
        builder.persistAndReturn(ontologyWithSameName);

        // re-create a ontology with a name that already exist in this ontology
        Assertions.assertThrows(AlreadyExistException.class, () -> {
            commandService.redo();
        });
    }

    @Test
    void edit_valid_ontology_with_success() {
        Ontology ontology = builder.given_an_ontology();

        CommandResponse commandResponse = ontologyService.update(ontology, ontology.toJsonObject().withChange("name", "NEW NAME"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(ontologyService.find(commandResponse.getObject().getId())).isPresent();
        Ontology edited = ontologyService.find(commandResponse.getObject().getId()).get();
        assertThat(edited.getName()).isEqualTo("NEW NAME");
    }

    @Test
    void undo_redo_ontology_edition_with_success() {
        Ontology ontology = builder.given_an_ontology();
        ontology.setName("OLD NAME");
        ontology = builder.persistAndReturn(ontology);

        ontologyService.update(ontology, ontology.toJsonObject().withChange("name", "NEW NAME"));

        assertThat(ontologyRepository.getById(ontology.getId()).getName()).isEqualTo("NEW NAME");

        commandService.undo();

        assertThat(ontologyRepository.getById(ontology.getId()).getName()).isEqualTo("OLD NAME");

        commandService.redo();

        assertThat(ontologyRepository.getById(ontology.getId()).getName()).isEqualTo("NEW NAME");

    }

    @Test
    void delete_ontology_with_success() {
        Ontology ontology = builder.given_an_ontology();

        CommandResponse commandResponse = ontologyService.delete(ontology, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(ontologyService.find(ontology.getId()).isEmpty());
    }

    @Test
    void delete_ontology_with_dependencies_with_success() {
        Ontology ontology = builder.given_an_ontology();
        Term term1 = builder.given_a_term(ontology);
        Term term2 = builder.given_a_term(ontology);
        RelationTerm relationTerm = builder.given_a_relation_term(term1, term2);

        CommandResponse commandResponse = ontologyService.delete(ontology, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(ontologyService.find(ontology.getId()).isEmpty());
    }


    @Test
    void undo_redo_ontology_deletion_with_success() {
        Ontology ontology = builder.given_an_ontology();

        ontologyService.delete(ontology, null, null, true);

        assertThat(ontologyService.find(ontology.getId()).isEmpty());

        commandService.undo();

        assertThat(ontologyService.find(ontology.getId()).isPresent());

        commandService.redo();

        assertThat(ontologyService.find(ontology.getId()).isEmpty());
    }

    @Test
    void undo_redo_ontology_deletion_restore_dependencies() {
        Ontology ontology = builder.given_an_ontology();
        Term term1 = builder.given_a_term(ontology);
        Term term2 = builder.given_a_term(ontology);
        RelationTerm relationTerm = builder.given_a_relation_term(term1, term2);

        CommandResponse commandResponse = ontologyService.delete(ontology, transactionService.start(), null, true);

        assertThat(ontologyService.find(ontology.getId()).isEmpty());
        assertThat(relationTermRepository.findById(relationTerm.getId())).isEmpty();
        assertThat(termRepository.findById(term1.getId())).isEmpty();
        assertThat(termRepository.findById(term2.getId())).isEmpty();
        commandService.undo();

        assertThat(ontologyService.find(ontology.getId()).isPresent());
        assertThat(relationTermRepository.findById(relationTerm.getId())).isPresent();
        assertThat(termRepository.findById(term1.getId())).isPresent();
        assertThat(termRepository.findById(term2.getId())).isPresent();

        commandService.redo();

        assertThat(ontologyService.find(ontology.getId()).isEmpty());
        assertThat(relationTermRepository.findById(relationTerm.getId())).isEmpty();
        assertThat(termRepository.findById(term1.getId())).isEmpty();
        assertThat(termRepository.findById(term2.getId())).isEmpty();

        commandService.undo();

        assertThat(ontologyService.find(ontology.getId()).isPresent());
        assertThat(relationTermRepository.findById(relationTerm.getId())).isPresent();
        assertThat(termRepository.findById(term1.getId())).isPresent();
        assertThat(termRepository.findById(term2.getId())).isPresent();

        commandService.redo();

        assertThat(ontologyService.find(ontology.getId()).isEmpty());
        assertThat(relationTermRepository.findById(relationTerm.getId())).isEmpty();
        assertThat(termRepository.findById(term1.getId())).isEmpty();
        assertThat(termRepository.findById(term2.getId())).isEmpty();
    }

    @Test
    void determine_rights_for_users_admin_in_project() {
        Ontology ontology = builder.given_an_ontology();
        Project project = builder.given_a_project_with_ontology(ontology);
        User userAdminInProject = builder.given_a_user();
        User userNotAdminInProject = builder.given_a_user();
        User userNotInProject = builder.given_a_user();

        permissionService.addPermission(project, userAdminInProject.getUsername(), ADMINISTRATION);
        permissionService.addPermission(project, userNotAdminInProject.getUsername(), WRITE);

        ontologyService.determineRightsForUsers(ontology, List.of(userAdminInProject, userNotAdminInProject, userNotInProject));

        assertThat(permissionService.hasACLPermission(ontology, userAdminInProject.getUsername(), ADMINISTRATION)).isTrue();

        assertThat(permissionService.hasACLPermission(ontology, userNotAdminInProject.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(ontology, userNotAdminInProject.getUsername(), READ)).isTrue();

        assertThat(permissionService.hasACLPermission(ontology, userNotInProject.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(ontology, userNotInProject.getUsername(), READ)).isFalse();

    }



    @Test
    @WithMockUser("user")
    void determine_rights_for_users_keep_rights_for_ontology_creator() {

        // create ontology for user
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        CommandResponse commandResponse = ontologyService.add(ontology.toJsonObject());
        ontology = (Ontology) commandResponse.getObject();

        assertThat(ontology.getUser().getUsername()).isEqualTo("user");
        assertThat(permissionService.hasACLPermission(ontology, "user", ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(ontology, "user", READ)).isTrue();

        // create project with ontology
        Project project = BasicInstanceBuilder.given_a_not_persisted_project();
        project.setOntology(ontology);
        commandResponse = projectService.add(project.toJsonObject());
        project = (Project)commandResponse.getObject() ;

        assertThat(ontology.getUser().getUsername()).isEqualTo("user");
        assertThat(permissionService.hasACLPermission(ontology, "user", ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(ontology, "user", READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project, "user", ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(project, "user", READ)).isTrue();

        // change project ontology
        commandResponse = projectService.update(project, project.toJsonObject()
                .withChange("ontology", null));

        // check that use still keep its rights to access ontology
        assertThat(ontology.getUser().getUsername()).isEqualTo("user");
        assertThat(permissionService.hasACLPermission(ontology, "user", ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(ontology, "user", READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project, "user", ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(project, "user", READ)).isTrue();
    }
}
