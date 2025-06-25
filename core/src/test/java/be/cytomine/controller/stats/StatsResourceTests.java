package be.cytomine.controller.stats;

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
import be.cytomine.domain.social.AnnotationAction;
import be.cytomine.domain.social.PersistentImageConsultation;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.repositorynosql.social.*;
import be.cytomine.service.social.AnnotationActionService;
import be.cytomine.service.social.ImageConsultationService;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class StatsResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ImageConsultationService imageConsultationService;

    @Autowired
    private ProjectConnectionService projectConnectionService;

    @Autowired
    private PersistentConnectionRepository persistentConnectionRepository;

    @Autowired
    private LastConnectionRepository lastConnectionRepository;

    @Autowired
    private PersistentImageConsultationRepository persistentImageConsultationRepository;

    @Autowired
    private PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    private ProjectConnectionRepository projectConnectionRepository;

    @Autowired
    private PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    private LastUserPositionRepository lastUserPositionRepository;

    @Autowired
    private AnnotationActionService annotationActionService;

    @Autowired
    private MockMvc restStatsControllerMockMvc;

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
    void stats_term() throws Exception {
        Project project = builder.given_a_project();

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/term.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))));

        AnnotationTerm annotationTerm = builder.given_an_annotation_term(builder.given_a_user_annotation(project));
        entityManager.refresh(project.getOntology());

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/term.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2))))
                .andExpect(jsonPath("$.collection[?(@.key=='"+annotationTerm.getTerm().getName()+"')].id").value(annotationTerm.getTerm().getId().intValue()))
                .andExpect(jsonPath("$.collection[?(@.key=='"+annotationTerm.getTerm().getName()+"')].value").value(1));

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/term.json", project.getId())
                        .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -10).getTime()))
                        .param("endDate", String.valueOf(new Date().getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2))));
    }



    @Test
    void stats_user() throws Exception {

        Project project = builder.given_a_project();
        builder.addUserToProject(project, "superadmin");
        UserAnnotation annotation1 = builder.given_a_user_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.persistAndReturn(annotation1);
        entityManager.refresh(annotation1);
        UserAnnotation annotation2 = builder.given_a_user_annotation(project);
        annotation2.setCreated(DateUtils.addDays(new Date(), -1));
        builder.persistAndReturn(annotation2);

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/user.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].id").value(builder.given_superadmin().getId().intValue()))
                .andExpect(jsonPath("$.collection[0].value").value(2));


        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/user.json", project.getId())
                    .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -20).getTime()))
                    .param("endDate", String.valueOf(DateUtils.addDays(new Date(), -10).getTime())))
                .andExpect(status().isOk());
    }

    @Test
    void stats_term_slide() throws Exception {
        Project project = builder.given_a_project();

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/termslide.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))));

        Term term = builder.given_a_term(project.getOntology());

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/termslide.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2))));

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/termslide.json", project.getId())
                        .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -20).getTime()))
                        .param("endDate", String.valueOf(DateUtils.addDays(new Date(), -10).getTime())))
                .andExpect(status().isOk());
    }

    @Test
    void stat_Per_term_and_image() throws Exception {
        Project project = builder.given_a_project();

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/termimage.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));

        AnnotationTerm annotationTerm = builder.given_an_annotation_term(builder.given_a_user_annotation(project));
        entityManager.refresh(project.getOntology());

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/termimage.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].countAnnotations").value(1));

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/termimage.json", project.getId())
                        .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -20).getTime()))
                        .param("endDate", String.valueOf(DateUtils.addDays(new Date(), -10).getTime())))
                .andExpect(status().isOk());
    }



    @Test
    void stats_uder_slide() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, "superadmin");

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/userslide.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].id").value(builder.given_superadmin().getId()))
                .andExpect(jsonPath("$.collection[0].value").value(0));
        ;

        UserAnnotation annotation1 = builder.given_a_user_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.persistAndReturn(annotation1);
        UserAnnotation annotation2 = builder.given_a_user_annotation(project);
        annotation2.setCreated(DateUtils.addDays(new Date(), -10));
        builder.persistAndReturn(annotation2);

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/userslide.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].id").value(builder.given_superadmin().getId()))
                .andExpect(jsonPath("$.collection[0].value").value(2));
        ;

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/userslide.json", project.getId())
                        .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -20).getTime()))
                        .param("endDate", String.valueOf(DateUtils.addDays(new Date(), -10).getTime())))
                .andExpect(status().isOk());
    }

    @Test
    void stats_user_annotation() throws Exception {
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

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/userannotations.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].id").value(builder.given_superadmin().getId()))
                .andExpect(jsonPath("$.collection[0].terms[0].value").value(2));

    }


    @Test
    void stats_user_annotation_evolution() throws Exception {
        Project project = builder.given_a_project();
        UserAnnotation annotation1 = builder.given_a_user_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.persistAndReturn(annotation1);
        UserAnnotation annotation2 = builder.given_a_user_annotation(project);
        annotation2.setCreated(DateUtils.addDays(new Date(), -10));
        builder.persistAndReturn(annotation2);

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/annotationevolution.json", project.getId())
                        .param("daysRange", "7")
                        .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -18).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))));

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/annotationevolution.json", project.getId())
                        .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -20).getTime()))
                        .param("endDate", String.valueOf(DateUtils.addDays(new Date(), -10).getTime())))
                .andExpect(status().isOk());
    }

    @Test
    void stats_reviewed_annotation_evolution() throws Exception {
        Project project = builder.given_a_project();
        ReviewedAnnotation annotation1 = builder.given_a_reviewed_annotation(project);
        annotation1.setCreated(DateUtils.addDays(new Date(), -1));
        builder.persistAndReturn(annotation1);
        ReviewedAnnotation annotation2 = builder.given_a_reviewed_annotation(project);
        annotation2.setCreated(DateUtils.addDays(new Date(), -10));
        builder.persistAndReturn(annotation2);

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/reviewedannotationevolution.json", project.getId()))
                .andExpect(status().isOk());

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/reviewedannotationevolution.json", project.getId())
                        .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -20).getTime()))
                        .param("endDate", String.valueOf(DateUtils.addDays(new Date(), -10).getTime())))
                .andExpect(status().isOk());
    }

    @Test
    void stats_annotation_term_by_project() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, "superadmin");
        AnnotationTerm annotationTerm = builder.given_an_annotation_term(builder.given_a_user_annotation(project));
        entityManager.refresh(annotationTerm.getUserAnnotation());

        restStatsControllerMockMvc.perform(get("/api/term/{id}/project/stat.json", annotationTerm.getTerm().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].key").value(project.getName()))
                .andExpect(jsonPath("$.collection[0].value").value(1L));
    }

    @Test
    void number_of_connections() throws Exception {
        restStatsControllerMockMvc.perform(get("/api/total/project/connections.json"))
                .andExpect(status().isOk());
    }

    @Test
    void stats_domain_count() throws Exception {
        UserAnnotation annotation = builder.given_a_user_annotation();

        restStatsControllerMockMvc.perform(get("/api/total/{domain}.json", annotation.getClass().getName()))
                .andExpect(status().isOk());
    }

    @Test
    void current_stats() throws Exception {
        restStatsControllerMockMvc.perform(get("/api/stats/currentStats.json"))
                .andExpect(status().isOk());
    }

    @Test
    void allGlobalStats() throws Exception {
        restStatsControllerMockMvc.perform(get("/api/stats/all.json"))
                .andExpect(status().isOk());
    }

    @Test
    void stats_connection_evolution() throws Exception {
        Project project = builder.given_a_project();
        given_a_persistent_connection_in_project(builder.given_superadmin(), project, DateUtils.addDays(new Date(), -15));
        given_a_persistent_connection_in_project(builder.given_superadmin(), project, DateUtils.addDays(new Date(), -15));
        given_a_persistent_connection_in_project(builder.given_superadmin(), project, DateUtils.addDays(new Date(), -5));

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/connectionsevolution.json", project.getId())
                        .param("daysRange", "7")
                        .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -18).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))));
    }

    @Test
    void stats_image_consultation_evolution() throws Exception {
        Project project = builder.given_a_project();
        ImageInstance imageInstance = builder.given_an_image_instance(project);
        given_a_persistent_image_consultation(builder.given_superadmin(), imageInstance, DateUtils.addDays(new Date(), -15));
        given_a_persistent_image_consultation(builder.given_superadmin(), imageInstance, DateUtils.addDays(new Date(), -15));
        given_a_persistent_image_consultation(builder.given_superadmin(), imageInstance, DateUtils.addDays(new Date(), -5));

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/imageconsultationsevolution.json", project.getId())
                        .param("daysRange", "7")
                        .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -18).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))));
    }

    @Test
    void stats_annotation_Action_evolution() throws Exception {
        Project project = builder.given_a_project();
        AnnotationDomain annotation = builder.given_a_user_annotation(project);
        given_a_persistent_annotation_action(DateUtils.addDays(new Date(), -15), annotation, builder.given_superadmin(), "select");
        given_a_persistent_annotation_action(DateUtils.addDays(new Date(), -15), annotation, builder.given_superadmin(), "move");
        given_a_persistent_annotation_action(DateUtils.addDays(new Date(), -15), annotation, builder.given_superadmin(), "select");
        given_a_persistent_annotation_action(DateUtils.addDays(new Date(), -5), annotation, builder.given_superadmin(), "select");

        restStatsControllerMockMvc.perform(get("/api/project/{project}/stats/annotationactionsevolution.json", project.getId())
                    .param("daysRange", "7")
                    .param("startDate", String.valueOf(DateUtils.addDays(new Date(), -18).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))));
    }
}
