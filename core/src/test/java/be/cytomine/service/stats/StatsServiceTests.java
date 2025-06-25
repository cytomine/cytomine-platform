package be.cytomine.service.stats;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.*;
import be.cytomine.repositorynosql.social.*;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.social.AnnotationActionService;
import be.cytomine.service.social.ImageConsultationService;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.service.social.UserPositionService;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.locationtech.jts.io.ParseException;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.service.middleware.ImageServerService.IMS_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class StatsServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    StatsService statsService;

    @Autowired
    ImageConsultationService imageConsultationService;

    @Autowired
    ProjectConnectionService projectConnectionService;

    @Autowired
    PersistentConnectionRepository persistentConnectionRepository;

    @Autowired
    LastConnectionRepository lastConnectionRepository;

    @Autowired
    PersistentImageConsultationRepository persistentImageConsultationRepository;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    ProjectConnectionRepository projectConnectionRepository;

    @Autowired
    PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    LastUserPositionRepository lastUserPositionRepository;

    @Autowired
    SequenceService sequenceService;

    @Autowired
    UserPositionService userPositionService;

    @Autowired
    AnnotationActionService annotationActionService;


    private static WireMockServer wireMockServer = new WireMockServer(8888);

    @BeforeAll
    public static void beforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        try {
            wireMockServer.stop();
        } catch (Exception e) {
        }
    }

    @BeforeEach
    public void init() {
        persistentConnectionRepository.deleteAll();
        lastConnectionRepository.deleteAll();
        persistentImageConsultationRepository.deleteAll();
        persistentProjectConnectionRepository.deleteAll();
        projectConnectionRepository.deleteAll();
        lastUserPositionRepository.deleteAll();
        persistentUserPositionRepository.deleteAll();
    }

    PersistentProjectConnection given_a_persistent_connection_in_project(User user, Project project, Date created) {
        PersistentProjectConnection connection = projectConnectionService.add(user, project, "xxx", "linux", "chrome", "123", created);
        return connection;
    }

    PersistentImageConsultation given_a_persistent_image_consultation(User user, ImageInstance imageInstance, Date created) {
        return imageConsultationService.add(user, imageInstance.getId(), "xxx", "mode", created);
    }

    AnnotationAction given_a_persistent_annotation_action(Date creation, AnnotationDomain annotationDomain, User user, String action) {
        AnnotationAction annotationAction =
                annotationActionService.add(
                        annotationDomain,
                        user,
                        action,
                        creation
                );
        return annotationAction;
    }


    @Test
    void stats_domain_count() {
        UserAnnotation annotation = builder.given_a_user_annotation();
        assertThat(statsService.total(annotation.getClass())).isGreaterThanOrEqualTo(1);
        assertThat(statsService.total(annotation.getProject().getClass())).isGreaterThanOrEqualTo(1);
    }

    @Test
    void current_user_count() {
        assertThat(statsService.numberOfCurrentUsers()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void active_projects_count() {
        assertThat(statsService.numberOfActiveProjects()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void most_active_project_count() {
        Project project = builder.given_a_project();
        given_a_persistent_connection_in_project(builder.given_superadmin(), project, new Date());
        assertThat(((JsonObject)statsService.mostActiveProjects().get().get("project")).getId()).isEqualTo(project.getId());
    }

    @Test
    void stats_annotation_term_by_project() {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, "superadmin");
        AnnotationTerm annotationTerm = builder.given_an_annotation_term(builder.given_a_user_annotation(project));
        entityManager.refresh(annotationTerm.getUserAnnotation());

        List<JsonObject> jsonObjects = statsService.statAnnotationTermedByProject(annotationTerm.getTerm());
        assertThat(jsonObjects).hasSize(1);
        assertThat(jsonObjects.get(0).get("key")).isEqualTo(project.getName());
        assertThat(jsonObjects.get(0).get("value")).isEqualTo(1L);

    }


    @Test
    void stats_user_annotation_evolution() {
        Project project = builder.given_a_project();
        UserAnnotation annotation1 = builder.given_a_user_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.persistAndReturn(annotation1);
        UserAnnotation annotation2 = builder.given_a_user_annotation(project);
        annotation2.setCreated(DateUtils.addDays(new Date(), -10));
        builder.persistAndReturn(annotation2);

        List<JsonObject> jsonObjects = statsService.statAnnotationEvolution(project, null, 7, DateUtils.addDays(new Date(), -30), DateUtils.addDays(new Date(), 0), true, false);

        assertThat(jsonObjects).hasSize(5);
        assertThat(jsonObjects.stream().filter(x -> x.getJSONAttrLong("size")==1).collect(Collectors.toList())).hasSize(2);

        statsService.statAnnotationEvolution(project, builder.given_a_term(project.getOntology()), 7, DateUtils.addDays(new Date(), -30), DateUtils.addDays(new Date(), 0), true, false);

    }

    @Test
    void stats_reviewed_annotation_evolution() throws ParseException {
        Project project = builder.given_a_project();
        ReviewedAnnotation annotation1 = builder.given_a_reviewed_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.persistAndReturn(annotation1);
        ReviewedAnnotation annotation2 = builder.given_a_reviewed_annotation(project);
        annotation2.setCreated(DateUtils.addDays(new Date(), -10));
        builder.persistAndReturn(annotation2);

        List<JsonObject> jsonObjects = statsService.statReviewedAnnotationEvolution(project, null, 7, DateUtils.addDays(new Date(), -30), DateUtils.addDays(new Date(), 0), true, false);

        assertThat(jsonObjects).hasSize(5);
        assertThat(jsonObjects.stream().filter(x -> x.getJSONAttrLong("size")==1).collect(Collectors.toList())).hasSize(2);

        statsService.statReviewedAnnotationEvolution(project, builder.given_a_term(project.getOntology()), 7, DateUtils.addDays(new Date(), -30), DateUtils.addDays(new Date(), 0), true, false);


    }

    @Test
    void stats_uder_slide() {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, "superadmin");

        List<JsonObject> results = statsService.statUserSlide(project, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(builder.given_superadmin().getId());
        assertThat(results.get(0).get("value")).isEqualTo(0);

        UserAnnotation annotation1 = builder.given_a_user_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.persistAndReturn(annotation1);
        UserAnnotation annotation2 = builder.given_a_user_annotation(project);
        annotation2.setCreated(DateUtils.addDays(new Date(), -10));
        builder.persistAndReturn(annotation2);

        results = statsService.statUserSlide(project, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(builder.given_superadmin().getId());
        assertThat(results.get(0).get("value")).isEqualTo(2L);

        builder.addUserToProject(project, builder.given_a_user().getUsername());

        results = statsService.statUserSlide(project, null, null);

        assertThat(results).hasSize(2);

        results = statsService.statUserSlide(project, DateUtils.addDays(new Date(), -40), DateUtils.addDays(new Date(), -20));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("value")).isEqualTo(0);
        assertThat(results.get(1).get("value")).isEqualTo(0);
    }


    @Test
    void stats_term_slide() {
        Project project = builder.given_a_project();

        List<JsonObject> results = statsService.statTermSlide(project, null, null);
        results.removeIf( x-> x.get("id")==null);
        assertThat(results).hasSize(0);

        Term term = builder.given_a_term(project.getOntology());

        results = statsService.statTermSlide(project, null, null);
        results.removeIf( x-> x.get("id")==null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(term.getId());
        assertThat(results.get(0).get("value")).isEqualTo(0);

        UserAnnotation annotation1 = builder.given_a_user_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.given_an_annotation_term(annotation1, term);
        builder.persistAndReturn(annotation1);

        results = statsService.statTermSlide(project, null, null);
        results.removeIf( x-> x.get("id")==null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(term.getId());
        assertThat(results.get(0).get("value")).isEqualTo(1L);

        builder.given_a_term(project.getOntology());

        results = statsService.statTermSlide(project, null, null);
        results.removeIf( x-> x.get("id")==null);
        assertThat(results).hasSize(2);

        results = statsService.statTermSlide(project, DateUtils.addDays(new Date(), -40), DateUtils.addDays(new Date(), -20));
        results.removeIf( x-> x.get("id")==null);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("value")).isEqualTo(0);
        assertThat(results.get(1).get("value")).isEqualTo(0);
    }


    @Test
    void stats_term() {
        Project project = builder.given_a_project();

        List<JsonObject> results = statsService.statTerm(project, null, null, false);

        assertThat(results).hasSize(1); //no term

        AnnotationTerm annotationTerm = builder.given_an_annotation_term(builder.given_a_user_annotation(project));
        entityManager.refresh(project.getOntology());
        results = statsService.statTerm(project, null, null, false);
        results.removeIf( x-> x.get("id")==null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(annotationTerm.getTerm().getId());
        assertThat(results.get(0).get("value")).isEqualTo(1L);

        UserAnnotation annotation1 = builder.given_a_user_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.given_an_annotation_term(annotation1, annotationTerm.getTerm());
        builder.persistAndReturn(annotation1);

        results = statsService.statTerm(project, null, null, false);
        results.removeIf( x-> x.get("id")==null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(annotationTerm.getTerm().getId());
        assertThat(results.get(0).get("value")).isEqualTo(2L);


        results = statsService.statTerm(project, DateUtils.addDays(new Date(), -40), DateUtils.addDays(new Date(), -20), false);
        results.removeIf( x-> x.get("id")==null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("value")).isEqualTo(0);
    }

    @Test
    void stat_per_term_and_image() {
        Project project = builder.given_a_project();

        List<JsonObject> results = statsService.statPerTermAndImage(project, null, null);

        assertThat(results).hasSize(0); //no annotations

        AnnotationTerm annotationTerm = builder.given_an_annotation_term(builder.given_a_user_annotation(project));
        entityManager.refresh(project.getOntology());

        results = statsService.statPerTermAndImage(project, null, null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("term")).isEqualTo(annotationTerm.getTerm().getId());
        assertThat(results.get(0).get("image")).isEqualTo(annotationTerm.getUserAnnotation().getImage().getId());
        assertThat(results.get(0).get("countAnnotations")).isEqualTo(1L);

        AnnotationTerm annotationTermWithSameImageAndSameTerm = builder.given_an_annotation_term(builder.given_a_user_annotation(project));
        annotationTermWithSameImageAndSameTerm.getUserAnnotation().setImage(annotationTerm.getUserAnnotation().getImage());
        annotationTermWithSameImageAndSameTerm.setTerm(annotationTerm.getTerm());

        results = statsService.statPerTermAndImage(project, null, null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("term")).isEqualTo(annotationTerm.getTerm().getId());
        assertThat(results.get(0).get("image")).isEqualTo(annotationTerm.getUserAnnotation().getImage().getId());
        assertThat(results.get(0).get("countAnnotations")).isEqualTo(2L);

        AnnotationTerm annotationTermWithSameImage = builder.given_an_annotation_term(builder.given_a_user_annotation(project));
        annotationTermWithSameImage.getUserAnnotation().setImage(annotationTerm.getUserAnnotation().getImage());

        results = statsService.statPerTermAndImage(project, null, null);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("term")).isEqualTo(annotationTerm.getTerm().getId());
        assertThat(results.get(0).get("image")).isEqualTo(annotationTerm.getUserAnnotation().getImage().getId());
        assertThat(results.get(0).get("countAnnotations")).isEqualTo(2L);
        assertThat(results.get(1).get("term")).isEqualTo(annotationTermWithSameImage.getTerm().getId());
        assertThat(results.get(1).get("image")).isEqualTo(annotationTerm.getUserAnnotation().getImage().getId());
        assertThat(results.get(1).get("countAnnotations")).isEqualTo(1L);


        results = statsService.statPerTermAndImage(project, DateUtils.addDays(new Date(), -40), DateUtils.addDays(new Date(), -20));
        assertThat(results).hasSize(0);
    }







    @Test
    void stats_user_annotation() {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, "superadmin");
        UserAnnotation annotation1 = builder.given_a_user_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.given_an_annotation_term(annotation1, builder.given_a_term(project.getOntology()));
        builder.persistAndReturn(annotation1);
        entityManager.refresh(annotation1);
        UserAnnotation annotation2 = builder.given_a_user_annotation(project);
        annotation2.setCreated(DateUtils.addDays(new Date(), -1));
        builder.given_an_annotation_term(annotation2, annotation1.getTerms().get(0));
        builder.persistAndReturn(annotation2);
        entityManager.refresh(annotation2);

        List<JsonObject> terms;

        List<JsonObject> results = statsService.statUserAnnotations(project);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(builder.given_superadmin().getId());
        terms = (List<JsonObject>) results.get(0).get("terms");
        assertThat(terms).hasSize(1);
        assertThat(terms.get(0).getJSONAttrLong("value")).isEqualTo(2);
    }



    @Test
    void stats_user() {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, "superadmin");
        UserAnnotation annotation1 = builder.given_a_user_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.persistAndReturn(annotation1);
        entityManager.refresh(annotation1);
        UserAnnotation annotation2 = builder.given_a_user_annotation(project);
        annotation2.setCreated(DateUtils.addDays(new Date(), -1));
        builder.persistAndReturn(annotation2);

        List<JsonObject> terms;

        List<JsonObject> results = statsService.statUser(project, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(builder.given_superadmin().getId());
        assertThat(results.get(0).getJSONAttrLong("value")).isEqualTo(2);
    }


    @Test
    void retrieve_storage_spaces() {
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/storage/size.json"))
                .willReturn(
                        aResponse().withBody("" + "{\"used\":193396892,\"available\":445132860,\"usedP\":0.302878435,\"hostname\":\"b52416f53249\",\"mount\":\"/data/images\",\"ip\":null}")
                )
        );

        JsonObject response = statsService.statUsedStorage();
        assertThat(response).isNotNull();
        // expected to be Greather than or eq because localhost:8888 may not be the only one
        assertThat(response.getJSONAttrLong("total")).isGreaterThanOrEqualTo(193396892+445132860);
        assertThat(response.getJSONAttrLong("available")).isGreaterThanOrEqualTo(445132860);
        assertThat(response.getJSONAttrLong("used")).isGreaterThanOrEqualTo(193396892);
        assertThat(response.getJSONAttrDouble("usedP")).isGreaterThan(0);

    }

    @Test
    void stats_connection_evolution() {
        Project project = builder.given_a_project();
        given_a_persistent_connection_in_project(builder.given_superadmin(), project, DateUtils.addDays(new Date(), -15));
        given_a_persistent_connection_in_project(builder.given_superadmin(), project, DateUtils.addDays(new Date(), -15));
        given_a_persistent_connection_in_project(builder.given_superadmin(), project, DateUtils.addDays(new Date(), -5));


        List<JsonObject> jsonObjects = statsService.statConnectionsEvolution(project, 7, DateUtils.addDays(new Date(), -18), null, false);
        assertThat(jsonObjects).hasSize(3);
        assertThat(jsonObjects.get(0).getJSONAttrLong("size")).isEqualTo(2);
        assertThat(jsonObjects.get(1).getJSONAttrLong("size")).isEqualTo(1);
        assertThat(jsonObjects.get(2).getJSONAttrLong("size")).isEqualTo(0);

        jsonObjects = statsService.statConnectionsEvolution(project, 7, DateUtils.addDays(new Date(), -18), null, true);
        assertThat(jsonObjects).hasSize(3);
        assertThat(jsonObjects.get(0).getJSONAttrLong("size")).isEqualTo(2);
        assertThat(jsonObjects.get(1).getJSONAttrLong("size")).isEqualTo(3);
        assertThat(jsonObjects.get(2).getJSONAttrLong("size")).isEqualTo(3);


        jsonObjects = statsService.statConnectionsEvolution(project, 7, DateUtils.addDays(new Date(), -18), DateUtils.addDays(new Date(), -6), true);
        assertThat(jsonObjects).hasSize(2);

    }

    @Test
    void stats_image_consultation_evolution() {
        Project project = builder.given_a_project();
        ImageInstance imageInstance = builder.given_an_image_instance(project);
        given_a_persistent_image_consultation(builder.given_superadmin(), imageInstance, DateUtils.addDays(new Date(), -15));
        given_a_persistent_image_consultation(builder.given_superadmin(), imageInstance, DateUtils.addDays(new Date(), -15));
        given_a_persistent_image_consultation(builder.given_superadmin(), imageInstance, DateUtils.addDays(new Date(), -5));


        List<JsonObject> jsonObjects = statsService.statImageConsultationsEvolution(project, 7, DateUtils.addDays(new Date(), -18), null, false);
        assertThat(jsonObjects).hasSize(3);
        assertThat(jsonObjects.get(0).getJSONAttrLong("size")).isEqualTo(2);
        assertThat(jsonObjects.get(1).getJSONAttrLong("size")).isEqualTo(1);
        assertThat(jsonObjects.get(2).getJSONAttrLong("size")).isEqualTo(0);

        jsonObjects = statsService.statImageConsultationsEvolution(project, 7, DateUtils.addDays(new Date(), -18), null, true);
        assertThat(jsonObjects).hasSize(3);
        assertThat(jsonObjects.get(0).getJSONAttrLong("size")).isEqualTo(2);
        assertThat(jsonObjects.get(1).getJSONAttrLong("size")).isEqualTo(3);
        assertThat(jsonObjects.get(2).getJSONAttrLong("size")).isEqualTo(3);
    }


    @Test
    void stats_annotation_Action_evolution() {
        Project project = builder.given_a_project();
        AnnotationDomain annotation = builder.given_a_user_annotation(project);
        given_a_persistent_annotation_action(DateUtils.addDays(new Date(), -15), annotation, builder.given_superadmin(), "select");
        given_a_persistent_annotation_action(DateUtils.addDays(new Date(), -15), annotation, builder.given_superadmin(), "move");
        given_a_persistent_annotation_action(DateUtils.addDays(new Date(), -15), annotation, builder.given_superadmin(), "select");
        given_a_persistent_annotation_action(DateUtils.addDays(new Date(), -5), annotation, builder.given_superadmin(), "select");

        List<JsonObject> jsonObjects = statsService.statAnnotationActionsEvolution(project, 7, DateUtils.addDays(new Date(), -18), null, false, "select");
        assertThat(jsonObjects).hasSize(3);
        assertThat(jsonObjects.get(0).getJSONAttrLong("size")).isEqualTo(2);
        assertThat(jsonObjects.get(1).getJSONAttrLong("size")).isEqualTo(1);
        assertThat(jsonObjects.get(2).getJSONAttrLong("size")).isEqualTo(0);

        jsonObjects = statsService.statAnnotationActionsEvolution(project, 7, DateUtils.addDays(new Date(), -18), null, true, "select");
        assertThat(jsonObjects).hasSize(3);
        assertThat(jsonObjects.get(0).getJSONAttrLong("size")).isEqualTo(2);
        assertThat(jsonObjects.get(1).getJSONAttrLong("size")).isEqualTo(3);
        assertThat(jsonObjects.get(2).getJSONAttrLong("size")).isEqualTo(3);
    }
    
    
}
