package be.cytomine.controller.ontology;

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
import be.cytomine.TestUtils;
import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.repository.ontology.ReviewedAnnotationRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.locationtech.jts.io.ParseException;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static be.cytomine.service.middleware.ImageServerService.IMS_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class ReviewedAnnotationResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restReviewedAnnotationControllerMockMvc;

    @Autowired
    private ReviewedAnnotationRepository reviewedAnnotationRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    private static WireMockServer wireMockServer = new WireMockServer(8888);

    private Project project;
    private ImageInstance image;
    private SliceInstance slice;
    private Term term;
    private User me;
    private ReviewedAnnotation reviewedAnnotation;

    @BeforeAll
    public static void beforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        try {
            wireMockServer.stop();
        } catch (Exception e) {}
    }

    @Test
    @Transactional
    public void get_a_reviewed_annotation() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        reviewedAnnotation.getTerms().add(builder.given_a_term());
        restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation/{id}.json", reviewedAnnotation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewedAnnotation.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.ontology.ReviewedAnnotation"))
                .andExpect(jsonPath("$.created").value(reviewedAnnotation.getCreated().getTime()))
                .andExpect(jsonPath("$.location").value(reviewedAnnotation.getWktLocation()))
                .andExpect(jsonPath("$.image").value(reviewedAnnotation.getImage().getId().intValue()))
                .andExpect(jsonPath("$.project").value(reviewedAnnotation.getProject().getId().intValue()))
                .andExpect(jsonPath("$.user").value(reviewedAnnotation.getUser().getId()))
                .andExpect(jsonPath("$.parentIdent").exists())
                .andExpect(jsonPath("$.parentClassName").exists())
                .andExpect(jsonPath("$.centroid.x").exists())
                .andExpect(jsonPath("$.centroid.y").exists())
                .andExpect(jsonPath("$.term", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.term[0]").value(reviewedAnnotation.getTerms().get(0).getId().intValue()));
    }

    @Test
    @Transactional
    public void get_a_reviewed_annotation_not_exists() throws Exception {
        restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation/{id}.json", 0))
                .andExpect(status().isNotFound());
    }


    @Test
    @Transactional
    public void list_annotations() throws Exception {
        restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation.json"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void stats_annotations() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(get("/api/imageinstance/{image}/reviewedannotation/stats.json", reviewedAnnotation.getImage().getId()))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void count_annotations_by_project() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idUser}/reviewedannotation/count.json", reviewedAnnotation.getProject().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThan(0)));

        Project projectWithoutAnnotation = builder.given_a_project();
        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idUser}/reviewedannotation/count.json", projectWithoutAnnotation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }


    @Test
    @Transactional
    public void count_annotations_by_project_with_dates() throws Exception {
        ReviewedAnnotation oldReviewedAnnotation = builder.given_a_reviewed_annotation();
        oldReviewedAnnotation.setCreated(DateUtils.addDays(new Date(), -1));

        ReviewedAnnotation newReviewedAnnotation =
                builder.persistAndReturn(builder.given_a_not_persisted_reviewed_annotation(oldReviewedAnnotation.getProject()));


        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("startDate", String.valueOf(oldReviewedAnnotation.getCreated().getTime()))
                        .param("endDate", String.valueOf(newReviewedAnnotation.getCreated().getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("startDate", String.valueOf(DateUtils.addSeconds(oldReviewedAnnotation.getCreated(),-1).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("endDate", String.valueOf(DateUtils.addSeconds(newReviewedAnnotation.getCreated(),1).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("startDate", String.valueOf(DateUtils.addSeconds(newReviewedAnnotation.getCreated(),-1).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("endDate", String.valueOf(DateUtils.addSeconds(oldReviewedAnnotation.getCreated(),1).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("endDate", String.valueOf(DateUtils.addDays(oldReviewedAnnotation.getCreated(), -2).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }


    @Test
    @Transactional
    public void download_reviewed_annotation_csv_document() throws Exception {
        buildDownloadContext();
        MvcResult mvcResult = performDownload("csv");
        checkResult(";", mvcResult);
    }

    @Test
    @Transactional
    public void download_reviewed_annotation_xls_document() throws Exception {
        buildDownloadContext();
        MvcResult mvcResult = performDownload("xls");
        checkXLSResult( mvcResult);
    }

    @Test
    @Transactional
    public void download_reviewed_annotation_pdf_document() throws Exception {
        this.buildDownloadContext();
        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{project}/reviewedannotation/download", this.project.getId())
                        .param("format", "pdf")
                        .param("reviewUsers", "")
                        .param("terms", this.term.getId().toString())
                        .param("images", this.image.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();
    }

    private void buildDownloadContext() throws ParseException {
        this.project = builder.given_a_project();
        this.image = builder.given_an_image_instance(this.project);
        this.slice = builder.given_a_slice_instance(this.image, 0, 0, 0);
        this.term = builder.given_a_term(this.project.getOntology());
        this.me = builder.given_superadmin();
        this.reviewedAnnotation = builder.given_a_reviewed_annotation(this.slice, "POLYGON((1 1,5 1,5 5,1 5,1 1))", this.me, this.term);
        builder.addUserToProject(project, this.me.getUsername());
    }

    private MvcResult performDownload(String format) throws Exception {
        return restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{project}/reviewedannotation/download", this.project.getId())
                        .param("format", format)
                        .param("reviewUsers", "")
                        .param("terms", this.term.getId().toString())
                        .param("images", this.image.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
    }

    private void checkResult(String delimiter, MvcResult result) throws UnsupportedEncodingException {
        TestUtils.checkSpreadsheetAnnotationResult(delimiter, result, this.reviewedAnnotation, this.project, this.image, this.me, this.term, "reviewedannotation", applicationProperties.getServerURL());
    }

    private void checkXLSResult( MvcResult result) throws IOException {
        TestUtils.checkSpreadsheetXLSAnnotationResult( result, this.reviewedAnnotation, this.project, this.image, this.me, this.term, "reviewedannotation", applicationProperties.getServerURL());
    }

    @Test
    @Transactional
    public void add_valid_reviewed_annotation() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_not_persisted_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(post("/api/reviewedannotation.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewedAnnotation.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.reviewedannotationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddReviewedAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists());

    }

    @Test
    @Transactional
    public void edit_valid_reviewed_annotation() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(put("/api/reviewedannotation/{id}.json", reviewedAnnotation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewedAnnotation.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.reviewedannotationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditReviewedAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists());

    }


    @Test
    @Transactional
    public void delete_reviewed_annotation() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(delete("/api/reviewedannotation/{id}.json", reviewedAnnotation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewedAnnotation.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.reviewedannotationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteReviewedAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists());

    }


    @Test
    @Transactional
    public void delete_reviewed_annotation_not_exist_fails() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(delete("/api/reviewedannotation/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewedAnnotation.toJSON()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());

    }



    @Test
    @Transactional
    public void start_image_review() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        assertThat(imageInstance.getReviewStart()).isNull();
        restReviewedAnnotationControllerMockMvc.perform(post("/api/imageinstance/{image}/review.json", imageInstance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk());
        assertThat(imageInstance.getReviewStart()).isNotNull();
    }


    @Test
    @Transactional
    public void stop_image_review() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        imageInstance.setReviewStart(new Date());
        imageInstance.setReviewUser(imageInstance.getUser());
        assertThat(imageInstance.getReviewStop()).isNull();
        restReviewedAnnotationControllerMockMvc.perform(delete("/api/imageinstance/{image}/review.json", imageInstance.getId()))
                .andExpect(status().isOk());
        assertThat(imageInstance.getReviewStop()).isNotNull();
    }

    @Test
    @Transactional
    public void cancel_image_review() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        imageInstance.setReviewStart(new Date());
        imageInstance.setReviewUser(imageInstance.getUser());
        assertThat(imageInstance.getReviewStop()).isNull();
        restReviewedAnnotationControllerMockMvc.perform(delete("/api/imageinstance/{image}/review.json", imageInstance.getId())
                        .param("cancel", "true"))
                .andExpect(status().isOk());
        assertThat(imageInstance.getReviewStart()).isNull();
        assertThat(imageInstance.getReviewStop()).isNull();
    }

    @Test
    @Transactional
    public void stop_image_review_refuse_if_image_not_started_to_review() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        assertThat(imageInstance.getReviewStart()).isNull();
        restReviewedAnnotationControllerMockMvc.perform(delete("/api/imageinstance/{image}/review.json", imageInstance.getId()))
                .andExpect(status().isBadRequest());
    }


    @Test
    @Transactional
    public void add_annotation_review() throws Exception {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.getImage().setReviewStart(new Date());
        userAnnotation.getImage().setReviewUser(userAnnotation.getUser());

        AnnotationTerm annotationTerm = builder.given_an_annotation_term(userAnnotation);
        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isEmpty();
        em.refresh(userAnnotation);
        restReviewedAnnotationControllerMockMvc.perform(post("/api/annotation/{annotation}/review.json", userAnnotation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk());


        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isPresent();
        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId()).get().getTerms()).containsExactly(annotationTerm.getTerm());
    }



    @Test
    @Transactional
    public void add_annotation_review_with_terms_change() throws Exception {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.getImage().setReviewStart(new Date());
        userAnnotation.getImage().setReviewUser(userAnnotation.getUser());
        AnnotationTerm annotationTerm = builder.given_an_annotation_term(userAnnotation);
        Term anotherTerm = builder.given_a_term(userAnnotation.getProject().getOntology());

        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isEmpty();

        restReviewedAnnotationControllerMockMvc.perform(post("/api/annotation/{annotation}/review.json", userAnnotation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("").param("terms", anotherTerm.getId().toString()))
                .andExpect(status().isOk());


        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isPresent();
        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId()).get().getTerms()).containsExactly(anotherTerm);
    }


    @Test
    @Transactional
    public void remove_annotation_review() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();

        assertThat(reviewedAnnotationRepository.findByParentIdent(reviewedAnnotation.getParentIdent())).isPresent();

        restReviewedAnnotationControllerMockMvc.perform(delete("/api/annotation/{annotation}/review.json", reviewedAnnotation.getParentIdent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk());

        assertThat(reviewedAnnotationRepository.findByParentIdent(reviewedAnnotation.getParentIdent())).isEmpty();
       }



    @Test
    @Transactional
    public void review_full_layer() throws Exception {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.getImage().setReviewStart(new Date());
        userAnnotation.getImage().setReviewUser(userAnnotation.getUser());
        AnnotationTerm annotationTerm = builder.given_an_annotation_term(userAnnotation);
        em.refresh(userAnnotation);

        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isEmpty();

        restReviewedAnnotationControllerMockMvc.perform(post("/api/imageinstance/{image}/annotation/review.json", userAnnotation.getImage().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("").param("users", userAnnotation.getUser().getId().toString()))
                .andExpect(status().isOk());


        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isPresent();
    }


    @Test
    @Transactional
    public void unreview_full_layer() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        reviewedAnnotation.getImage().setReviewStart(new Date());
        reviewedAnnotation.getImage().setReviewUser(reviewedAnnotation.getUser());

        assertThat(reviewedAnnotationRepository.findByParentIdent(reviewedAnnotation.getParentIdent())).isPresent();

        restReviewedAnnotationControllerMockMvc.perform(delete("/api/imageinstance/{image}/annotation/review.json", reviewedAnnotation.getImage().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("").param("users", reviewedAnnotation.getUser().getId().toString()))
                .andExpect(status().isOk());


        assertThat(reviewedAnnotationRepository.findByParentIdent(reviewedAnnotation.getParentIdent())).isEmpty();
    }



    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @jakarta.transaction.Transactional
    public void get_reviewed_annotation_crop() throws Exception {
        ReviewedAnnotation annotation = given_a_reviewed_annotation_with_valid_image_server(builder);

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/"+ URLEncoder.encode("1636379100999/CMU-2/CMU-2.mrxs", StandardCharsets.UTF_8).replace("%2F", "/") + "/annotation/crop";
        String body =  "{\"length\":512,\"z_slices\":0,\"annotations\":[{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\"}],\"timepoints\":0,\"background_transparency\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(WireMock.equalTo(
                                body
                        ))
                        .willReturn(
                                aResponse().withBody(mockResponse)
                        )
        );


        MvcResult mvcResult = restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation/{id}/crop.png?maxSize=512", annotation.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @jakarta.transaction.Transactional
    public void get_reviewed_annotation_crop_mask() throws Exception {
        ReviewedAnnotation annotation = given_a_reviewed_annotation_with_valid_image_server(builder);

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/"+ URLEncoder.encode("1636379100999/CMU-2/CMU-2.mrxs", StandardCharsets.UTF_8).replace("%2F", "/") + "/annotation/mask";
        String body = "{\"level\":0,\"z_slices\":0,\"annotations\":[{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\",\"fill_color\":\"#fff\"}],\"timepoints\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(WireMock.equalTo(
                                body
                        ))
                        .willReturn(
                                aResponse().withBody(mockResponse)
                        )
        );


        MvcResult mvcResult = restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation/{id}/mask.png", annotation.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @jakarta.transaction.Transactional
    public void get_reviewed_annotation_alpha_mask() throws Exception {
        ReviewedAnnotation annotation = given_a_reviewed_annotation_with_valid_image_server(builder);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content


        configureFor("localhost", 8888);
        String url = "/image/"+ URLEncoder.encode(annotation.getImage().getBaseImage().getPath() , StandardCharsets.UTF_8).replace("%2F", "/") + "/annotation/crop";
        String body = "{\"level\":0,\"z_slices\":0,\"annotations\":[{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\"}],\"timepoints\":0,\"background_transparency\":100}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(WireMock.equalTo(
                                body
                        ))
                        .willReturn(
                                aResponse().withBody(mockResponse)
                        )
        );

        MvcResult mvcResult = restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation/{id}/alphamask.png", annotation.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    public static ReviewedAnnotation given_a_reviewed_annotation_with_valid_image_server(BasicInstanceBuilder builder) throws ParseException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        ImageInstance imageInstance = builder.given_an_image_instance(image, builder.given_a_project());
        imageInstance.setInstanceFilename("CMU-2");
        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());
        SliceInstance sliceInstance = builder.given_a_slice_instance(imageInstance, slice);
        ReviewedAnnotation reviewedAnnotation
                = builder.given_a_reviewed_annotation(
                        sliceInstance,
                "POLYGON((1 1,50 10,50 50,10 50,1 1))", builder.given_superadmin(), null);
        return reviewedAnnotation;
    }






}
