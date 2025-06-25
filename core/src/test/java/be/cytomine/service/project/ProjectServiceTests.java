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

import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Description;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.dto.NamedCytomineDomain;
import be.cytomine.dto.ProjectBounds;
import be.cytomine.exceptions.ConstraintException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import be.cytomine.service.PermissionService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.search.ProjectSearchExtension;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;
import static org.springframework.security.acls.domain.BasePermission.READ;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ProjectServiceTests {

    @Autowired
    ProjectService projectService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    ProjectConnectionService projectConnectionService;

    @Autowired
    UserAnnotationService userAnnotationService;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    ProjectRepresentativeUserService projectRepresentativeUserService;

    @BeforeEach
    void cleanMongo() {
        persistentProjectConnectionRepository.deleteAll();
    }

    @Test
    void get_project_with_success() {
        Project project = builder.given_a_project();
        assertThat(project).isEqualTo(projectService.get(project.getId()));
    }

    @Test
    void get_unexisting_project_return_null() {
        assertThat(projectService.get(0L)).isNull();
    }

    @Test
    void find_project_with_success() {
        Project project = builder.given_a_project();
        assertThat(projectService.find(project.getId()).isPresent());
        assertThat(project).isEqualTo(projectService.find(project.getId()).get());
    }

    @Test
    void find_unexisting_project_return_empty() {
        assertThat(projectService.find(0L)).isEmpty();
    }

    @Test
    void read_many_project_from_ids() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();

        assertThat(projectService.readMany(List.of(project1.getId(), project2.getId())))
                .contains(project1, project2);
        assertThat(projectService.readMany(List.of(project1.getId())))
                .contains(project1).doesNotContain(project2);
        assertThat(projectService.readMany(List.of()))
                .doesNotContain(project1, project2);
    }

    @Test
    void list_last_opened() {
        User user1 = builder.given_superadmin();
        Project project1 = builder.given_a_project_with_user(user1);
        Project project2 = builder.given_a_project_with_user(user1);
        Project project3 = builder.given_a_project_with_user(user1);

        given_a_persistent_connection_in_project(user1, project1, DateUtils.addDays(new Date(), 1));
        given_a_persistent_connection_in_project(user1, project2, DateUtils.addDays(new Date(), 2));

        // connection from another user
        given_a_persistent_connection_in_project(builder.given_a_user(), project1, DateUtils.addDays(new Date(), -7));

        List<Map<String, Object>> results = projectService.listLastOpened(user1, 2L);
        assertThat(results.get(0).get("id")).isEqualTo(project2.getId());
        assertThat(results.get(0).get("opened")).isEqualTo(true);
        assertThat(results.get(1).get("id")).isEqualTo(project1.getId());
        assertThat(results.get(1).get("opened")).isEqualTo(true);

        results = projectService.listLastOpened(user1, 3L);
        System.out.println(results);
        assertThat(results).hasSize(3);
        assertThat(results.get(0).get("id")).isEqualTo(project2.getId());
        assertThat(results.get(0).get("opened")).isEqualTo(true);
        assertThat(results.get(1).get("id")).isEqualTo(project1.getId());
        assertThat(results.get(1).get("opened")).isEqualTo(true);
        // last result is project 3 that has been created at new Date(), even if no activities
        assertThat(results.get(2).get("id")).isEqualTo(project3.getId());
        assertThat(results.get(2).get("opened")).isEqualTo(false);
    }

    @Test
    void list_project_for_current_user() {
        User user1 = builder.given_superadmin();
        Project project1 = builder.given_a_project_with_user(user1);
        Project project2 = builder.given_a_project();

        assertThat(projectService.listForCurrentUser())
                .contains(project1).doesNotContain(project2);
    }

    @Test
    void retrieve_project_bounds() {

        List<Date> dateChoices = new ArrayList<>(List.of(
                new GregorianCalendar(2021, Calendar.JANUARY, 1).getTime(),
                new GregorianCalendar(2021, Calendar.JULY, 1).getTime(),
                new GregorianCalendar(2021, Calendar.DECEMBER, 1).getTime()
        ));
        Collections.shuffle(dateChoices);

        List<Integer> intChoices = new ArrayList<>(List.of(0,1,2));
        Collections.shuffle(intChoices);
        List<Double> doubleChoices = new ArrayList<>(List.of(0.5, 10.1, 99.99));
        Collections.shuffle(doubleChoices);
        List<String> stringChoices = new ArrayList<>(List.of("aaa", "zzzz", "AAAA"));
        Collections.shuffle(stringChoices);
        List<EditingMode> editingChoices = new ArrayList<>(List.of(EditingMode.CLASSIC, EditingMode.RESTRICTED, EditingMode.READ_ONLY));
        Collections.shuffle(editingChoices);

        for (int k = 0 ; k < 2 ; k++) { // execute twice the creation of projects (6 projects)
            for (int i = 0; i < 3; i++) {
                Project project = builder.given_a_project();
                project.setUpdated(dateChoices.get(i));
                project.setMode(editingChoices.get(i));
                project.setName("project"+k+i);

                project.setCountAnnotations((long)intChoices.get(i));
                project.setCountReviewedAnnotations((long)intChoices.get(i));
                project.setCountJobAnnotations((long)intChoices.get(i));
                project.setCountImages((long)intChoices.get(i));
                for(int j = 0; j<intChoices.size(); j++) {
                    builder.addUserToProject(project, builder.given_a_user().getUsername());
                }
                builder.persistAndReturn(project);
            }
        }

        ProjectBounds projectBounds = projectService.computeBounds(true);

        //Created cannot be set (auto generated), so we cannot test it

        // same for updated...

//        assertThat(projectBounds.getUpdated().getMin()).isEqualTo(new GregorianCalendar(2021, Calendar.JANUARY, 1).getTime());
//        assertThat(projectBounds.getUpdated().getMax()).isEqualTo(new GregorianCalendar(2021, Calendar.DECEMBER, 1).getTime());
//
//        assertThat(projectBounds.getMode().getMin()).isEqualTo(1);
//        assertThat(projectBounds.getMode().getMax()).isEqualTo(3);

        assertThat(projectBounds.getName().getMin()).isNotNull();
        assertThat(projectBounds.getName().getMax()).isNotNull();

        assertThat(projectBounds.getNumberOfImages().getMin()).isEqualTo(0);
        assertThat(projectBounds.getNumberOfImages().getMax()).isEqualTo(2);

        assertThat(projectBounds.getNumberOfAnnotations().getMin()).isEqualTo(0L);
        assertThat(projectBounds.getNumberOfAnnotations().getMax()).isEqualTo(2L);
        assertThat(projectBounds.getNumberOfJobAnnotations().getMin()).isEqualTo(0L);
        assertThat(projectBounds.getNumberOfJobAnnotations().getMax()).isEqualTo(2L);
        assertThat(projectBounds.getNumberOfReviewedAnnotations().getMin()).isEqualTo(0L);
        assertThat(projectBounds.getNumberOfReviewedAnnotations().getMax()).isEqualTo(2L);
        assertThat(projectBounds.getNumberOfImages().getMin()).isEqualTo(0L);
        assertThat(projectBounds.getNumberOfImages().getMax()).isEqualTo(2L);
    }

    @Test
    void list_user_project_with_many_filters() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();
        builder.addUserToProject(project1, builder.given_superadmin().getUsername());
        builder.addUserToProject(project2, builder.given_superadmin().getUsername());

        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(project2);
        userAnnotationService.add(userAnnotation.toJsonObject());
        userAnnotation = builder.given_a_not_persisted_user_annotation(project1);
        userAnnotationService.add(userAnnotation.toJsonObject());

        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();
        projectSearchExtension.setWithMembersCount(true);
        projectSearchExtension.setWithLastActivity(true);
        projectSearchExtension.setWithCurrentUserRoles(true);
        List<SearchParameterEntry> searchParameterEntries = new ArrayList<>();
        searchParameterEntries.add(new SearchParameterEntry("numberOfImages", SearchOperation.lte, 10));
        searchParameterEntries.add(new SearchParameterEntry("membersCount", SearchOperation.lte, 10));
        searchParameterEntries.add(new SearchParameterEntry("numberOfAnnotations", SearchOperation.lte, 10));
        searchParameterEntries.add(new SearchParameterEntry("numberOfJobAnnotations", SearchOperation.lte, 10));
        searchParameterEntries.add(new SearchParameterEntry("numberOfReviewedAnnotations", SearchOperation.lte, 10));
        searchParameterEntries.add(new SearchParameterEntry("numberOfImages", SearchOperation.lte, 10));

        Page<JsonObject> page = projectService.list(builder.given_superadmin(), projectSearchExtension, searchParameterEntries, "lastActivity", "desc", 10L, 0L);

        assertThat(page.getTotalElements()).isEqualTo(2);

        assertThat(page.getContent().get(0).get("id")).isEqualTo(project1.getId());
        assertThat(page.getContent().get(0).get("membersCount")).isEqualTo(1L);
        assertThat(((Date)page.getContent().get(0).get("lastActivity"))).isBetween(DateUtils.addSeconds(new Date(),-120), DateUtils.addSeconds(new Date(),120));
        assertThat(page.getContent().get(1).get("id")).isEqualTo(project2.getId());
    }

    @Test
    void list_command_history_for_project() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();
        UserAnnotation userAnnotation1 = builder.given_a_not_persisted_user_annotation(project1);
        UserAnnotation userAnnotation2 = builder.given_a_not_persisted_user_annotation(project2);
        userAnnotationService.add(userAnnotation1.toJsonObject());
        userAnnotationService.add(userAnnotation2.toJsonObject());

        assertThat(projectService.findCommandHistory(List.of(project1, project2), builder.given_superadmin().getId(), 0L, 0L, true, null, null))
                .hasSize(2);

        assertThat(projectService.findCommandHistory(List.of(project1, project2), null, 0L, 0L, true, null, null))
                .hasSize(2);

        assertThat(projectService.findCommandHistory(List.of(project1), builder.given_superadmin().getId(), 0L, 0L, false, null, null))
                .hasSize(1);

        assertThat(projectService.findCommandHistory(List.of(project1), builder.given_superadmin().getId(), 0L, 0L, true, DateUtils.addSeconds(new Date(), 10).getTime(), null))
                .hasSize(0);

        assertThat(projectService.findCommandHistory(List.of(project1), builder.given_superadmin().getId(), 0L, 0L, true, DateUtils.addSeconds(new Date(), -10).getTime(), DateUtils.addSeconds(new Date(), +10).getTime()))
                .hasSize(1);

        assertThat(projectService.findCommandHistory(List.of(), builder.given_superadmin().getId(), 0L, 0L, true, null, null))
                .hasSize(0);

    }


    @Test
    void list_user_project_with_annotation_filters() {
        Project project1 = builder.given_a_project();
        builder.addUserToProject(project1, builder.given_superadmin().getUsername());

        project1.setCountImages(100L);
        project1.setCountAnnotations(200L);
        project1.setCountReviewedAnnotations(300L);
        project1.setCountJobAnnotations(400L);

        builder.persistAndReturn(project1);

        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();
        Page<JsonObject> page = null;

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of()), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfImages", SearchOperation.lte, 50)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(0);

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfImages", SearchOperation.equals, 100)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).get("id")).isEqualTo(project1.getId());

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfImages", SearchOperation.gte, 150)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(0);



        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfAnnotations", SearchOperation.lte, 150)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(0);

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfAnnotations", SearchOperation.equals, 200)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).get("id")).isEqualTo(project1.getId());

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfAnnotations", SearchOperation.gte, 250)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(0);



        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfReviewedAnnotations", SearchOperation.lte, 250)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(0);

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfReviewedAnnotations", SearchOperation.equals, 300)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).get("id")).isEqualTo(project1.getId());

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfReviewedAnnotations", SearchOperation.gte, 350)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(0);




        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfJobAnnotations", SearchOperation.lte, 350)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(0);

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfJobAnnotations", SearchOperation.equals, 400)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).get("id")).isEqualTo(project1.getId());

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("numberOfJobAnnotations", SearchOperation.gte, 450)
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(0);
    }



    @Test
    void list_user_project_with_name_filter() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();
        Project project3 = builder.given_a_project();
        builder.addUserToProject(project1, builder.given_superadmin().getUsername());
        builder.addUserToProject(project2, builder.given_superadmin().getUsername());
        builder.addUserToProject(project3, builder.given_superadmin().getUsername());

        project1.setName("T2");
        project2.setName("S2");
        project3.setName("S_intermediate_2_end");

        builder.persistAndReturn(project1);
        builder.persistAndReturn(project2);
        builder.persistAndReturn(project3);

        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();

        Page<JsonObject> page = null;

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("name", SearchOperation.like, "S2")
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).get("id")).isEqualTo(project2.getId());
    }

    @Test
    void list_user_project_with_ontology_filter() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();
        builder.addUserToProject(project1, builder.given_superadmin().getUsername());
        builder.addUserToProject(project2, builder.given_superadmin().getUsername());

        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();

        Page<JsonObject> page = null;

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("ontology", SearchOperation.in, List.of(project2.getOntology().getId()))
        )), "created", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).get("id")).isEqualTo(project2.getId());

//        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
//                new SearchParameterEntry("ontology", SearchOperation.in, null)
//        )), "created", "desc", 0L, 0L);
//        assertThat(page.getTotalElements()).isEqualTo(2);
    }


    @Test
    void list_user_project_with_pagination() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project_with_ontology(project1.getOntology());
        Project project3 = builder.given_a_project_with_ontology(project1.getOntology());
        Project project4 = builder.given_a_project_with_ontology(project1.getOntology());
        Project project5 = builder.given_a_project_with_ontology(project1.getOntology());
        builder.addUserToProject(project1, builder.given_superadmin().getUsername());
        builder.addUserToProject(project2, builder.given_superadmin().getUsername());
        builder.addUserToProject(project3, builder.given_superadmin().getUsername());
        builder.addUserToProject(project4, builder.given_superadmin().getUsername());
        builder.addUserToProject(project5, builder.given_superadmin().getUsername());

        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();

        Page<JsonObject> page = null;

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("ontology", SearchOperation.in, List.of(project1.getOntology().getId()))
        )), "created", "asc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getContent().get(0).get("id")).isEqualTo(project1.getId());
        assertThat(page.getContent().get(1).get("id")).isEqualTo(project2.getId());
        assertThat(page.getContent().get(2).get("id")).isEqualTo(project3.getId());
        assertThat(page.getContent().get(3).get("id")).isEqualTo(project4.getId());
        assertThat(page.getContent().get(4).get("id")).isEqualTo(project5.getId());

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("ontology", SearchOperation.in, List.of(project1.getOntology().getId()))
        )), "created", "asc", 3L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).get("id")).isEqualTo(project1.getId());
        assertThat(page.getContent().get(1).get("id")).isEqualTo(project2.getId());
        assertThat(page.getContent().get(2).get("id")).isEqualTo(project3.getId());


        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("ontology", SearchOperation.in, List.of(project1.getOntology().getId()))
        )), "created", "asc", 3L, 1L);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).get("id")).isEqualTo(project2.getId());
        assertThat(page.getContent().get(1).get("id")).isEqualTo(project3.getId());
        assertThat(page.getContent().get(2).get("id")).isEqualTo(project4.getId());

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("ontology", SearchOperation.in, List.of(project1.getOntology().getId()))
        )), "created", "asc", 3L, 3L);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).get("id")).isEqualTo(project4.getId());
        assertThat(page.getContent().get(1).get("id")).isEqualTo(project5.getId());

        page = projectService.list(builder.given_superadmin(), projectSearchExtension, new ArrayList<>(List.of(
                new SearchParameterEntry("ontology", SearchOperation.in, List.of(project1.getOntology().getId()))
        )), "created", "asc", 3L, 6L);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(0);
    }


    @Test
    void list_user_project_with_no_user() {
        Project project1 = builder.given_a_project();
        Project projectWhereUserIsMissing = builder.given_a_project();

        builder.addUserToProject(project1, "superadmin");

        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();
        projectSearchExtension.setWithCurrentUserRoles(true);
        List<SearchParameterEntry> searchParameterEntries = new ArrayList<>();

        Page<JsonObject> page = projectService.list(null, projectSearchExtension, searchParameterEntries, "id", "desc", 0L, 0L);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);

        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(project1.getId());
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(projectWhereUserIsMissing.getId());
    }

    @Test
    void list_user_project_with_no_filter() {
        Project project1 = builder.given_a_project();
        Project projectWhereUserIsMissing = builder.given_a_project();

        builder.addUserToProject(project1, "superadmin");


        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();
        projectSearchExtension.setWithCurrentUserRoles(true);
        List<SearchParameterEntry> searchParameterEntries = new ArrayList<>();

        Page<JsonObject> page = projectService.list(builder.given_superadmin(), projectSearchExtension, searchParameterEntries, "id", "desc", 0L, 0L);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);

        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(project1.getId());
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(projectWhereUserIsMissing.getId());
    }

    @Test
    void list_user_project_with_members_count() {
        Project project1 = builder.given_a_project();

        builder.addUserToProject(project1, "superadmin");


        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();
        projectSearchExtension.setWithMembersCount(true);
        List<SearchParameterEntry> searchParameterEntries = new ArrayList<>();

        Page<JsonObject> page = projectService.list(builder.given_superadmin(), projectSearchExtension, searchParameterEntries, "id", "desc", 0L, 0L);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);

        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(project1.getId());
        assertThat(page.getContent().stream().map(x -> x.get("membersCount")).collect(Collectors.toList())).contains(1L);
    }

    @Test
    void list_user_project_with_filters() {
        Project project1 = builder.given_a_project();

        Project projectWhereUserIsMissing = builder.given_a_project();

        builder.addUserToProject(project1, "superadmin");


        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();
        projectSearchExtension.setWithCurrentUserRoles(true);
        List<SearchParameterEntry> searchParameterEntries = new ArrayList<>();

        Page<JsonObject> page = projectService.list(builder.given_superadmin(), projectSearchExtension, searchParameterEntries, "id", "desc", 0L, 0L);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);

        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(project1.getId());
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(projectWhereUserIsMissing.getId());
    }


    @Test
    void list_project_by_onotology() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();
        builder.addUserToProject(project1, "superadmin");
        builder.addUserToProject(project2, "superadmin");

        assertThat(projectService.listByOntology(project1.getOntology())).contains(project1).doesNotContain(project2);
    }

    @Test
    void list_last_action() {
        Project project1 = builder.given_a_project();

        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(project1);

        assertThat(projectService.lastAction(project1, 10)).hasSize(0);
        userAnnotationService.add(userAnnotation.toJsonObject());
        assertThat(projectService.lastAction(project1, 10)).hasSize(1);
    }

    @Test
    void list_by_roles() {
        Project project1 = builder.given_a_project();
        User creator = builder.given_superadmin();
        User admin = builder.given_a_user();
        User user = builder.given_a_user();

        builder.addUserToProject(project1, creator.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project1, admin.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project1, user.getUsername(), READ);

        assertThat(projectService.listByCreator(creator)).contains(new NamedCytomineDomain(project1.getId()));
        assertThat(projectService.listByCreator(admin)).doesNotContain(new NamedCytomineDomain(project1.getId()));
        assertThat(projectService.listByCreator(user)).doesNotContain(new NamedCytomineDomain(project1.getId()));
        assertThat(projectService.listByAdmin(creator)).contains(new NamedCytomineDomain(project1.getId()));
        assertThat(projectService.listByAdmin(admin)).contains(new NamedCytomineDomain(project1.getId()));
        assertThat(projectService.listByAdmin(user)).doesNotContain(new NamedCytomineDomain(project1.getId()));
        assertThat(projectService.listByUser(user)).contains(new NamedCytomineDomain(project1.getId()));
    }




    @Test
    void add_project() {
        Project project = BasicInstanceBuilder.given_a_not_persisted_project();

        CommandResponse commandResponse = projectService.add(project.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(projectService.find(commandResponse.getObject().getId())).isPresent();
        Project projectCreated = projectService.find(commandResponse.getObject().getId()).get();
        assertThat(projectCreated.getName()).isEqualTo(project.getName());

        assertThat(securityACLService.getProjectUsers(projectCreated)).containsExactly(builder.given_superadmin().getUsername());

        assertThat(permissionService.hasACLPermission(projectCreated, builder.given_superadmin().getUsername(), ADMINISTRATION)).isTrue();

        assertThat(projectRepresentativeUserService.find(projectCreated, builder.given_superadmin())).isPresent();
    }

    @Test
    void add_project_with_users_and_admins() {
        Project project = BasicInstanceBuilder.given_a_not_persisted_project();
        project.setOntology(builder.given_an_ontology());
        User user = builder.given_a_user();
        User admin = builder.given_a_user();

        CommandResponse commandResponse = projectService.add(project.toJsonObject()
            .withChange("users", List.of(user.getId()))
            .withChange("admins", List.of(admin.getId()))
        );

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(projectService.find(commandResponse.getObject().getId())).isPresent();
        Project projectCreated = projectService.find(commandResponse.getObject().getId()).get();
        assertThat(projectCreated.getName()).isEqualTo(project.getName());

        assertThat(permissionService.hasACLPermission(projectCreated, builder.given_superadmin().getUsername(), ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(projectCreated, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(projectCreated, user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(projectCreated, admin.getUsername(), ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(projectCreated, admin.getUsername(), READ)).isTrue();

        // check ontology access
        assertThat(permissionService.hasACLPermission(projectCreated.getOntology(), user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(projectCreated.getOntology(), admin.getUsername(), READ)).isTrue();
    }

    @Test
    void update_project_name() {
        Project project = builder.given_a_project();
        User user = builder.given_a_user();
        builder.addUserToProject(project, user.getUsername());

        CommandResponse commandResponse = projectService.update(project, project.toJsonObject().withChange("name", "NEW NAME"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(projectService.find(commandResponse.getObject().getId())).isPresent();
        Project edited = projectService.find(commandResponse.getObject().getId()).get();
        assertThat(edited.getName()).isEqualTo("NEW NAME");
        assertThat(securityACLService.isUserInProject(user, project)).isTrue(); // no impact on users
    }

    @Test
    void update_project_with_another_ontology() {
        Project project = builder.given_a_project();
        Ontology anotherOntology = builder.given_an_ontology();

        CommandResponse commandResponse = projectService.update(project, project.toJsonObject()
                .withChange("ontology", anotherOntology.getId()));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(projectService.find(commandResponse.getObject().getId())).isPresent();
        Project edited = projectService.find(commandResponse.getObject().getId()).get();
        assertThat(edited.getOntology()).isEqualTo(anotherOntology);
    }

    @Test
    void update_project_with_another_ontology_with_already_existing_annotations() {
        Project project = builder.given_a_project();
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(project);
        builder.persistAndReturn(userAnnotation);
        builder.given_an_annotation_term(userAnnotation);
        Ontology anotherOntology = builder.given_an_ontology();

        Assertions.assertThrows(ForbiddenException.class, () -> {
            projectService.update(project, project.toJsonObject()
                    .withChange("ontology", anotherOntology.getId()));
        });
        assertThat(project.getOntology()).isNotEqualTo(anotherOntology);

        CommandResponse commandResponse = projectService.update(project, project.toJsonObject()
                .withChange("ontology", anotherOntology.getId())
                .withChange("forceOntologyUpdate", true));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(projectService.find(commandResponse.getObject().getId())).isPresent();
        Project edited = projectService.find(commandResponse.getObject().getId()).get();
        assertThat(edited.getOntology()).isEqualTo(anotherOntology);
        entityManager.refresh(userAnnotation);
        assertThat(userAnnotation.getTerms()).hasSize(0);
    }

    @Test
    void update_project_with_other_users() {
        Project project = builder.given_a_project();
        User previousUser = builder.given_a_user();
        User newUser = builder.given_a_user();
        builder.addUserToProject(project, previousUser.getUsername(), READ);

        CommandResponse commandResponse = projectService.update(project, project.toJsonObject()
                .withChange("users", List.of(newUser.getId())));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(projectService.find(commandResponse.getObject().getId())).isPresent();
        Project edited = projectService.find(commandResponse.getObject().getId()).get();

        assertThat(permissionService.hasACLPermission(project, previousUser.getUsername(), READ)).isFalse();
        assertThat(permissionService.hasACLPermission(project, newUser.getUsername(), READ)).isTrue();
    }

    @Test
    void update_project_with_other_admins() {
        Project project = builder.given_a_project();
        User previousUser = builder.given_a_user();
        User newUser = builder.given_a_user();
        builder.addUserToProject(project, previousUser.getUsername(), ADMINISTRATION);

        CommandResponse commandResponse = projectService.update(project, project.toJsonObject()
                .withChange("admins", List.of(newUser.getId())));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(projectService.find(commandResponse.getObject().getId())).isPresent();
        Project edited = projectService.find(commandResponse.getObject().getId()).get();
        System.out.println("User " + previousUser.getUsername() + " right " + ADMINISTRATION.getMask() + " in domain " + project + " => " + permissionService.hasACLPermission(project, previousUser.getUsername(), ADMINISTRATION));
        assertThat(permissionService.hasACLPermission(project, previousUser.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(project, previousUser.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(project, newUser.getUsername(), ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(project, newUser.getUsername(), READ)).isTrue();
    }

    @Test
    void update_project_with_other_representatives() {
        Project project = builder.given_a_project();
        User previousUser = builder.given_a_user();
        User newUser = builder.given_a_user();
        builder.addUserToProject(project, previousUser.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, newUser.getUsername(), ADMINISTRATION);

        builder.given_a_project_representative_user(project, previousUser);

        assertThat(projectRepresentativeUserService.find(project, previousUser)).isPresent();
        assertThat(projectRepresentativeUserService.find(project, newUser)).isEmpty();

        CommandResponse commandResponse = projectService.update(project, project.toJsonObject()
                .withChange("representatives", List.of(newUser.getId())));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(projectRepresentativeUserService.find(project, previousUser)).isEmpty();
        assertThat(projectRepresentativeUserService.find(project, newUser)).isPresent();
    }

    @Test
    void update_project_with_other_representatives_not_in_project() {
        Project project = builder.given_a_project();
        User userNotInProject = builder.given_a_user();

        Assertions.assertThrows(ConstraintException.class, () -> {
            projectService.update(project, project.toJsonObject()
                    .withChange("representatives", List.of(userNotInProject.getId())));
        });

    }

    @Test
    void list_active_projects() {
        User user1 = builder.given_superadmin();
        Project project1 = builder.given_a_project_with_user(user1);
        Project project2 = builder.given_a_project_with_user(user1);
        Project project3 = builder.given_a_project_with_user(user1);

        given_a_persistent_connection_in_project(user1, project1, DateUtils.addSeconds(new Date(), -300));
        given_a_persistent_connection_in_project(user1, project2, DateUtils.addSeconds(new Date(), -5));

        assertThat(projectService.getActiveProjects()).contains(project2.getId()).doesNotContain(project1.getId(), project3.getId());
    }

    @Test
    void list_active_projects_with_number_of_users() {
        User user1 = builder.given_superadmin();
        Project project1 = builder.given_a_project_with_user(user1);
        Project project2 = builder.given_a_project_with_user(user1);
        Project project3 = builder.given_a_project_with_user(user1);

        builder.addUserToProject(project2, user1.getUsername());
        builder.addUserToProject(project2, builder.given_superadmin().getUsername());

        given_a_persistent_connection_in_project(builder.given_superadmin(), project1, DateUtils.addSeconds(new Date(), -300));
        given_a_persistent_connection_in_project(user1, project2, DateUtils.addSeconds(new Date(), -5));

        assertThat(projectService.getActiveProjectsWithNumberOfUsers()).hasSize(1);
        assertThat(((JsonObject)projectService.getActiveProjectsWithNumberOfUsers().get(0).get("project")).getId()).isEqualTo(project2.getId());
        assertThat(projectService.getActiveProjectsWithNumberOfUsers().get(0).get("users")).isEqualTo(1);

        given_a_persistent_connection_in_project(builder.given_superadmin(), project1, DateUtils.addSeconds(new Date(), -10));
        given_a_persistent_connection_in_project(user1, project2, DateUtils.addSeconds(new Date(), -5));

        assertThat(projectService.getActiveProjectsWithNumberOfUsers()).hasSize(2);
    }

    @Test
    void delete_project() {
        Project project = builder.given_a_project();

        CommandResponse commandResponse = projectService.delete(project, null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(projectService.find(project.getId()).isEmpty());
    }



    @Test
    void delete_project_just_beeing_created() {
        Project project = BasicInstanceBuilder.given_a_not_persisted_project();

        CommandResponse commandResponse = projectService.add(project.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(projectService.find(commandResponse.getObject().getId())).isPresent();
        Project projectCreated = projectService.find(commandResponse.getObject().getId()).get();

        commandResponse = projectService.delete(projectCreated, null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
    }


    @Test
    void delete_project_with_dependencies() {
        Project project = builder.given_a_project();
        builder.given_an_image_instance(project);
        UserAnnotation annotation = builder.given_a_not_persisted_user_annotation(project);
        builder.persistAndReturn(annotation);

        Property property = builder.given_a_property(project, "mustbedeleted", "value");
        Description description = builder.given_a_description(project);
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), project);
        AttachedFile attachedFile = builder.given_a_attached_file(project);

        AssertionsForClassTypes.assertThat(entityManager.find(Property.class, property.getId())).isNotNull();
        AssertionsForClassTypes.assertThat(entityManager.find(Description.class, description.getId())).isNotNull();
        AssertionsForClassTypes.assertThat(entityManager.find(TagDomainAssociation.class, tagDomainAssociation.getId())).isNotNull();
        AssertionsForClassTypes.assertThat(entityManager.find(AttachedFile.class, attachedFile.getId())).isNotNull();

        CommandResponse commandResponse = projectService.delete(project, null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(projectService.find(project.getId()).isEmpty());

        AssertionsForClassTypes.assertThat(entityManager.find(Property.class, property.getId())).isNull();
        AssertionsForClassTypes.assertThat(entityManager.find(Description.class, description.getId())).isNull();
        AssertionsForClassTypes.assertThat(entityManager.find(TagDomainAssociation.class, tagDomainAssociation.getId())).isNull();
        AssertionsForClassTypes.assertThat(entityManager.find(AttachedFile.class, attachedFile.getId())).isNull();
    }

    PersistentProjectConnection given_a_persistent_connection_in_project(User user, Project project, Date created) {
        PersistentProjectConnection connection = projectConnectionService.add(user, project, "xxx", "linux", "chrome", "123", created);
        return connection;
    }
}
