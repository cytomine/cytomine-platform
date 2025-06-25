package be.cytomine.controller.image;

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
import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.nimbusds.jose.crypto.RSASSASigner;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.transaction.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import static be.cytomine.service.middleware.ImageServerService.IMS_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class ImageInstanceResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restImageInstanceControllerMockMvc;

    @Autowired
    private ApplicationProperties applicationProperties;

    private static WireMockServer wireMockServer = new WireMockServer(8888);

    private static RSAKey rsaKey;

    private static final String KEY_ID = "some random string";

    @BeforeAll
    public static void beforeAll() throws JOSEException {
        configureWireMock(wireMockServer);
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        try {
            wireMockServer.stop();
        } catch (Exception e) {}
    }

    private ImageInstance given_test_image_instance() {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.setOriginalFilename("CMU-2.mrxs");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setOriginalFilename("CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        ImageInstance imageInstance = builder.given_an_image_instance(image, builder.given_a_project());
        imageInstance.setInstanceFilename("CMU-2");
        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());
        SliceInstance sliceInstance = builder.given_a_slice_instance(imageInstance, slice);
        return imageInstance;
    }

    @Test
    @Transactional
    public void get_an_image_instance() throws Exception {
        ImageInstance image = given_test_image_instance();
        String serverUrl = applicationProperties.getServerURL();
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.ImageInstance"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.preview").value(serverUrl + "/api/imageinstance/"+image.getId()+"/thumb.png?maxSize=1024"))
                .andExpect(jsonPath("$.thumb").value(serverUrl + "/api/imageinstance/"+image.getId()+"/thumb.png?maxSize=512"))
                .andExpect(jsonPath("$.macroURL").value(serverUrl + "/api/imageinstance/"+image.getId()+"/associated/macro.png?maxWidth=512"))
                .andExpect(jsonPath("$.reviewStop").hasJsonPath())
                .andExpect(jsonPath("$.baseImage").value(image.getBaseImage().getId()))
                .andExpect(jsonPath("$.project").value(image.getProject().getId()))

                .andExpect(jsonPath("$.path").value("1636379100999/CMU-2/CMU-2.mrxs"))
                .andExpect(jsonPath("$.blindedName").hasJsonPath())
                .andExpect(jsonPath("$.instanceFilename").value("CMU-2"))
                .andExpect(jsonPath("$.originalFilename").exists())
                .andExpect(jsonPath("$.blindedName").hasJsonPath())
                .andExpect(jsonPath("$.height").value(220696))
                .andExpect(jsonPath("$.width").value(109240))
                .andExpect(jsonPath("$.physicalSizeY").hasJsonPath())
                .andExpect(jsonPath("$.physicalSizeX").hasJsonPath())
                .andExpect(jsonPath("$.physicalSizeZ").hasJsonPath())
                .andExpect(jsonPath("$.fps").hasJsonPath())
                .andExpect(jsonPath("$.zoom").hasJsonPath())
                .andExpect(jsonPath("$.filename").hasJsonPath());
    }

    @Test
    @Transactional
    @WithMockUser(username = "get_blind_image_instance")
    public void get_blind_image_instance() throws Exception {
        ImageInstance image = given_test_image_instance();

        User user = builder.given_a_user("get_blind_image_instance");
        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE); // contributor

        image.getProject().setBlindMode(true);

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.instanceFilename").doesNotExist())
                .andExpect(jsonPath("$.originalFilename").doesNotExist())
                .andExpect(jsonPath("$.blindedName").hasJsonPath())
                .andExpect(jsonPath("$.height").value(220696))
                .andExpect(jsonPath("$.width").value(109240));
    }


    @Test
    @Transactional
    public void get_an_image_instance_not_exist() throws Exception {
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}.json", 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.message").exists());
    }


    @Test
    @Transactional
    public void get_slice_instance_reference() throws Exception {
        ImageInstance image = given_test_image_instance();
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/sliceinstance/reference.json", image.getId()))
                .andExpect(status().isOk());
    }


    @Test
    @Transactional
    public void get_image_instance_bounds() throws Exception {
        ImageInstance image = given_test_image_instance();
        image.setReviewStart(new Date(1636702044534L));
        image.setMagnification(123);
        image.setPhysicalSizeX(null);
        image.setPhysicalSizeY(0.5);
        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/bounds/imageinstance.json", image.getProject().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStart.min").value(1636702044534L))
                .andExpect(jsonPath("$.reviewStart.max").value(1636702044534L))
                .andExpect(jsonPath("$.reviewStop.min").isEmpty())
                .andExpect(jsonPath("$.reviewStop.max").isEmpty())
                .andExpect(jsonPath("$.magnification.min").value(123))
                .andExpect(jsonPath("$.magnification.max").value(123))
                .andExpect(jsonPath("$.magnification.list", contains(123)))
                .andExpect(jsonPath("$.physicalSizeY.min").value(0.5))
                .andExpect(jsonPath("$.physicalSizeX.min").isEmpty())
                .andExpect(jsonPath("$.format.list", contains(image.getBaseImage().getContentType())));

    }

    @Autowired
    UserRepository userRepository;

    @WithMockUser(username = "list_image_instance_by_user")
    @Test
    @Transactional
    public void list_image_instance_by_user() throws Exception {
        ImageInstance image = builder.given_an_image_instance();
        image.getBaseImage().setWidth(500);
        ImageInstance imageFromOtherProjectNotAccessibleForUser = builder.given_an_image_instance();
        User user = builder.given_a_user("list_image_instance_by_user");
        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE); // contributor

        restImageInstanceControllerMockMvc.perform(get("/api/user/{id}/imageinstance.json", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+image.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+imageFromOtherProjectNotAccessibleForUser.getId()+")]").doesNotExist());

        restImageInstanceControllerMockMvc.perform(get("/api/user/{id}/imageinstance.json", user.getId()).param("width[lte]", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+image.getId()+")]").exists());

        restImageInstanceControllerMockMvc.perform(get("/api/user/{id}/imageinstance.json", user.getId()).param("width[gte]", "501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+image.getId()+")]").doesNotExist());
    }


    @WithMockUser(username = "list_image_instance_light_by_user")
    @Test
    @Transactional
    public void list_image_instance_light_by_user() throws Exception {
        ImageInstance image = builder.given_an_image_instance();
        image.getBaseImage().setWidth(500);
        ImageInstance imageFromOtherProjectNotAccessibleForUser = builder.given_an_image_instance();
        User user = builder.given_a_user("list_image_instance_light_by_user");
        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE); // contributor


        restImageInstanceControllerMockMvc.perform(get("/api/user/{id}/imageinstance/light.json", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+image.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+imageFromOtherProjectNotAccessibleForUser.getId()+")]").doesNotExist());

    }


    @Test
    @Transactional
    public void list_image_instance_by_projects() throws Exception {
        Project project1 = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        ImageInstance imageInProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);
        ImageInstance imageInAnotherProject = builder.given_an_image_instance(builder.given_an_abstract_image(), anotherProject);


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInAnotherProject.getId() + ")]").doesNotExist());

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
                        .param("light", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInAnotherProject.getId() + ")]").doesNotExist());


    }


    @Test
    @Transactional
    public void list_image_instance_light() throws Exception {
        Project project1 = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        ImageInstance imageInProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);
        ImageInstance imageInAnotherProject = builder.given_an_image_instance(builder.given_an_abstract_image(), anotherProject);


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
                        .param("light", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")].instanceFilename").value(imageInProject1.getBlindInstanceFilename()))
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInAnotherProject.getId() + ")]").doesNotExist());
    }


    @Test
    @Transactional
    @WithMockUser("list_image_instance_by_projects_blind_filenames")
    public void list_image_instance_by_projects_blind_filenames() throws Exception {
        User user = builder.given_a_user("list_image_instance_by_projects_blind_filenames");
        ImageInstance image = given_test_image_instance();

        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE); // contributor

        image.getProject().setBlindMode(true);

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", image.getProject().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + image.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[0].filename").isEmpty());
    }


    @Test
    @Transactional
    public void list_image_instance_by_projects_tree() throws Exception {
        Project project1 = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        ImageInstance image1InProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);
        ImageInstance image2InProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
                        .param("tree", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.children", hasSize(2)));

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
                    .param("tree", "true").param("max", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.children", hasSize(1)));
    }


    @Test
    @Transactional
    public void list_image_instance_by_project_with_annotation_filter() throws Exception {
        Project project = builder.given_a_project();
        // we add width filter to get only the image set defined in this test
        ImageInstance image1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
        image1.setCountImageAnnotations(2L);
        ImageInstance image2 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
        image2.setCountImageAnnotations(4L);

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "0")
                        .param("sort", "created")
                        .param("order", "desc")
                        .param("numberOfAnnotations[lte]", "5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").exists());

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "0")
                        .param("sort", "created")
                        .param("order", "desc")
                        .param("numberOfAnnotations[lte]", "3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").doesNotExist());

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "0")
                        .param("sort", "created")
                        .param("order", "desc")
                        .param("numberOfAnnotations[lte]", "4")
                        .param("numberOfAnnotations[gte]", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").exists());

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "0")
                        .param("sort", "created")
                        .param("order", "desc")
                        .param("numberOfAnnotations[lte]", "4")
                        .param("numberOfAnnotations[gte]", "3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").exists());
    }


    @Test
    @Transactional
    public void list_image_instance_by_project_with_pagination() throws Exception {
        Project project = builder.given_a_project();
        int width = Math.abs(new Random().nextInt());
        // we add width filter to get only the image set defined in this test
        ImageInstance image1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
        image1.getBaseImage().setWidth(width);
        ImageInstance image2 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
        image2.getBaseImage().setWidth(width);
        ImageInstance image3 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
        image3.getBaseImage().setWidth(width);

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "0")
                        .param("width[equals]",  String.valueOf(width))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(3))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "1")
                        .param("width[equals]",  String.valueOf(width))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "1")
                        .param("max", "1")
                        .param("width[equals]",  String.valueOf(width))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "1")
                        .param("max", "0")
                        .param("width[equals]", String.valueOf(width)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.perPage").value(2))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "500")
                        .param("width[equals]", String.valueOf(width)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(3))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "500")
                        .param("max", "0")
                        .param("width[equals]", String.valueOf(width)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0)))) // default sorting must be created desc
                .andExpect(jsonPath("$.offset").value(500))
                .andExpect(jsonPath("$.perPage").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }





    @Test
    @Transactional
    public void get_next_image_instance() throws Exception {
        Project project = builder.given_a_project();
        ImageInstance imageInstance1 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );
        ImageInstance imageInstance2 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/next.json", imageInstance2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageInstance1.getId().intValue()));

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/next.json", imageInstance1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    public void get_previous_image_instance() throws Exception {
        Project project = builder.given_a_project();
        ImageInstance imageInstance1 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );
        ImageInstance imageInstance2 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/previous.json", imageInstance1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageInstance2.getId().intValue()));

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/previous.json", imageInstance2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }



    @Test
    @Transactional
    public void add_valid_image_instance() throws Exception {
        ImageInstance imageInstance = builder.given_a_not_persisted_image_instance();
        restImageInstanceControllerMockMvc.perform(post("/api/imageinstance.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imageInstance.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imageinstanceID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imageinstance.id").exists());

    }

    @Test
    @Transactional
    public void edit_valid_image_instance() throws Exception {
        Project project = builder.given_a_project();
        ImageInstance imageInstance = builder.given_an_image_instance();
        JsonObject jsonObject = imageInstance.toJsonObject();
        jsonObject.put("project", project.getId());
        restImageInstanceControllerMockMvc.perform(put("/api/imageinstance/{id}.json", imageInstance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imageinstanceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditImageInstanceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imageinstance.id").exists())
                .andExpect(jsonPath("$.imageinstance.project").value(project.getId()));


    }


    @Test
    @Transactional
    public void delete_image_instance() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        restImageInstanceControllerMockMvc.perform(delete("/api/imageinstance/{id}.json", imageInstance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imageinstanceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteImageInstanceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imageinstance.id").exists());


    }


    @Test
    @Transactional
    public void get_image_instance_thumb() throws Exception {
        ImageInstance image = given_test_image_instance();
        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/thumb?z_slices=0&timepoints=0&length=512"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/thumb.png?maxSize=512&Authorization=Bearer "+getSignedNotExpiredJwt(), image.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @Transactional
    public void get_image_instance_thumb_if_image_not_exist() throws Exception {
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/thumb.png?Authorization=Bearer " + getSignedNotExpiredJwt(), 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @Transactional
    public void get_image_instance_preview() throws Exception {
        ImageInstance image = given_test_image_instance();
        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/thumb?z_slices=0&timepoints=0&length=1024"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/preview.png?maxSize=1024", image.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }


    @Test
    @Transactional
    public void get_image_instance_associeted_label() throws Exception {
        ImageInstance image = given_test_image_instance();
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode("1636379100999/CMU-2/CMU-2.mrxs", StandardCharsets.UTF_8).replace("%2F", "/")+ "/info/associated"))
                .willReturn(
                        aResponse().withBody("{\"items\": [{\"name\":\"macro\"},{\"name\":\"thumbnail\"},{\"name\":\"label\"}], \"size\": 0}")
                )
        );
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/associated.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection", containsInAnyOrder("label","macro","thumbnail")));
    }


    @Test
    @Transactional
    public void get_image_instance_associeted_label_macro() throws Exception {
        ImageInstance image = given_test_image_instance();
        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/associated/macro?length=512";

        System.out.println(url);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/associated/{label}.png", image.getId(), "macro")
                        .param("maxSize", "512"))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }


    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @Transactional
    public void get_image_instance_crop() throws Exception {
        ImageInstance image = given_test_image_instance();

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/annotation/crop";
        String body = "{\"length\":512,\"z_slices\":0,\"annotations\":[{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\"}],\"timepoints\":0,\"background_transparency\":0}";

        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(WireMock.equalTo(
                                body
                        ))
                        .willReturn(
                                aResponse().withBody(mockResponse)
                        )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/crop.png?maxSize=512", image.getId())
                        .param("location", "POLYGON((1 1,50 10,50 50,10 50,1 1))"))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @Transactional
    public void get_image_instance_window() throws Exception {
        ImageInstance image = given_test_image_instance();

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/window";
        String body = "{\"level\":0,\"z_slices\":0,\"timepoints\":0,\"region\":{\"left\":10,\"top\":20,\"width\":30,\"height\":40}}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(WireMock.equalTo(body))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/window-10-20-30-40.png", image.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);

    }


    @Test
    @Transactional
    public void get_image_instance_metadata() throws Exception {
        ImageInstance image = given_test_image_instance();
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/metadata"))
                .willReturn(
                        aResponse().withBody("{\"size\":11,\"items\":[{\"key\":\"JFIFVersion\",\"value\":1.01,\"type\":\"DECIMAL\",\"namespace\":\"JFIF\"}," +
                                "{\"key\":\"ResolutionUnit\",\"value\":\"inches\",\"type\":\"STRING\",\"namespace\":\"JFIF\"},{\"key\":\"XResolution\"," +
                                "\"value\":300,\"type\":\"INTEGER\",\"namespace\":\"JFIF\"},{\"key\":\"YResolution\",\"value\":300,\"type\":\"INTEGER\"," +
                                "\"namespace\":\"JFIF\"},{\"key\":\"ProfileCMMType\",\"value\":\"Little CMS\",\"type\":\"STRING\",\"namespace\":\"ICC_PROFILE\"}," +
                                "{\"key\":\"ProfileVersion\",\"value\":\"4.3.0\",\"type\":\"STRING\",\"namespace\":\"ICC_PROFILE\"}," +
                                "{\"key\":\"ProfileClass\",\"value\":\"Display Device Profile\",\"type\":\"STRING\",\"namespace\":\"ICC_PROFILE\"}," +
                                "{\"key\":\"ColorSpaceData\",\"value\":\"RGB\",\"type\":\"STRING\",\"namespace\":\"ICC_PROFILE\"}," +
                                "{\"key\":\"ProfileConnectionSpace\",\"value\":\"XYZ\",\"type\":\"STRING\",\"namespace\":\"ICC_PROFILE\"}," +
                                "{\"key\":\"ProfileDateTime\",\"value\":\"2021:03:02 20:40:36\",\"type\":\"STRING\",\"namespace\":\"ICC_PROFILE\"}," +
                                "{\"key\":\"ProfileFileSignature\",\"value\":\"acsp\",\"type\":\"STRING\",\"namespace\":\"ICC_PROFILE\"}]}")
                )
        );
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/metadata.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(11))))
                .andExpect(jsonPath("$.collection[?(@.key==\"ProfileClass\")]").exists());
    }



    @Test
    public void download_image_instance() throws Exception {
        ImageInstance image = given_test_image_instance();

        byte[] mockResponse = UUID.randomUUID().toString().getBytes();
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/")+"/export?filename=" + URLEncoder.encode(image.getBaseImage().getOriginalFilename(), StandardCharsets.UTF_8)))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/download?Authorization=Bearer " + getSignedNotExpiredJwt(), image.getId()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }



    @Test
    @WithMockUser("download_image_instance_cannot_download")
    public void download_image_instance_cannot_download() throws Exception {
        User user = builder.given_a_user("download_image_instance_cannot_download");

        ImageInstance image = given_test_image_instance();
        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE);
        image.getProject().setAreImagesDownloadable(true);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes();
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/")+"/export?filename=" + URLEncoder.encode(image.getBaseImage().getOriginalFilename(), StandardCharsets.UTF_8)))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/download?Authorization=Bearer " + getSignedNotExpiredJwt(), image.getId()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @Transactional
    public void histograms() throws Exception {
        ImageInstance image = given_test_image_instance();

        configureFor("localhost", 8888);
        System.out.println("/image/"+ URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0?n_bins=256&channels=0");
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-image?n_bins=256"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"type":"FAST","minimum":0,"maximum":255,"first_bin":0,"last_bin":255,"n_bins":256,
                                "histogram":[168366,69327,58523,61224,76530,72928,63925,68427,57622,76530,73828,63925,79231,77430,87335,
                                78331,98138,82832,103541,81932,114345,108042,131451,118846,134153,149458,162063,157561,159363,153060,136854,
                                174668,186373,194475,194475,191775,189073,214284,205280,210682,239493,234091,239493,280910,251198,276408,250298,
                                272807,288112,325027,326827,317825,352938,377247,370045,401557,407859,416862,482589,515002,562720,583428,606837,
                                653655,743690,809417,857135,903953,1051611,1161454,1255991,1301008,1436961,1591822,1626034,1749383,1874532,
                                2049201,2048301,2236474,2436352,2521885,2628126,2825303,2838809,2952254,3117017,3194448,3220558,3293487,3390725,
                                3453749,3511371,3547386,3614011,3693243,3734658,3796783,3807587,3783278,3936338,4054284,4249660,4360404,4290176,
                                4319887,4519765,4608900,4628708,4863700,4898814,4900614,5165317,5123901,5177922,5303072,5349890,5541664,5496647,
                                5527259,5572277,5576779,5545266,5611892,5429120,5656910,5639803,5570476,5649707,5606490,5649707,5710930,5714532,
                                5777557,5759550,5917111,5887400,6045862,6058466,6340277,6395198,6429412,6716623,6912000,6839071,6996633,7318059,
                                7445008,7529641,7692605,7953707,8008629,8177894,8252624,8301243,8371469,8357065,8532632,8431794,8441697,8357965,
                                8196802,8186898,8226513,7961810,7829458,7767334,7697106,7538645,7600769,7229824,7247831,7054255,6926405,6539254,
                                6692314,6252043,6120591,6118790,6124192,5962129,5809069,5683020,5474139,5390406,5301270,5259854,5039269,5088788,
                                4901515,4900614,4708839,4611602,4630509,4489154,4471147,4303680,4211846,4097501,4264965,3990359,3939938,3951644,
                                3861608,3876915,3783278,3733759,3765270,3683339,3677937,3724755,3808488,3925534,4095700,4282072,4672825,5382302,
                                6388896,7618776,10436878,15088093,25878810,65904844,153326293,264905079,212651367,181677468,228504759,204774190,
                                143072188,36629008,4463044,3138626,2549795,2030294,1769191,1577416,1322617,1176760,1016497,841829,664460,596934,
                                509598,389852,358340,282711,216084,188173,195376,135052,99939,86434,70227,37814,180970]}                           
                                """
                        )
                )
        );
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/histogram.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastBin").value("255"))
                .andExpect(jsonPath("$.histogram[0]").value(168366));
    }

    @Test
    @Transactional
    public void histograms_bounds() throws Exception {
        ImageInstance image = given_test_image_instance();

        configureFor("localhost", 8888);

        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-image/bounds?n_bins=256"))
                .willReturn(
                        aResponse().withBody(
                                """
                                 {"type":"FAST","minimum":0,"maximum":255}                                                                                   
                                """
                        )
                )
        );
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/histogram/bounds.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FAST"));
    }


    @Test
    @Transactional
    public void channel_histograms() throws Exception {
        ImageInstance image = given_test_image_instance();


        configureFor("localhost", 8888);
        System.out.println("/image/"+ URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-channels?n_bins=256");
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-channels?n_bins=256"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"items":[{"type":"FAST","minimum":6,"maximum":255,"first_bin":6,"last_bin":255,"n_bins":256,"histogram":[900,2701,2701,2701,3601,5402,4502,9904,9004,11705,15306,18007,32413,19808,27011,22509,34213,29712,35114,45918,43217,46818,42317,48619,42317,49519,54021,54921,59423,59423,62124,68427,68427,66626,69327,59423,67526,98138,68427,86434,76530,81032,74729,85533,92736,90936,104441,95437,95437,108943,120647,108042,129651,125149,148558,141355,135053,122448,154860,125149,166565,170166,145857,165665,187273,163864,181871,187273,189974,182771,212483,210682,204380,212483,241294,220586,241294,269205,253899,278209,287212,323226,323226,379048,398856,458279,489791,537510,602335,664460,725683,831925,891348,992188,1086725,1264994,1364933,1543203,1594523,1736778,1868230,2021290,2175250,2295897,2446256,2485871,2745172,2750574,2873022,2985566,3035986,3253871,3206152,3202551,3225960,3281782,3261074,3346607,3134124,3232263,3139526,3180943,3134124,3022481,2955855,2957655,2924342,2810898,2754176,2747873,2620924,2651536,2593013,2671343,2496675,2468764,2448056,2445355,2320206,2340914,2385932,2355320,2318406,2360722,2278790,2359822,2225669,2330110,2229271,2228370,2200460,2256281,2192356,2174349,2235573,2154542,2118528,2075311,2110424,2110424,2122129,2102321,2005984,2117627,1942959,1974471,1951062,1987076,1846621,1918649,1804305,1813308,1825013,1774593,1792600,1763789,1747583,1663850,1664750,1638640,1636839,1597224,1593623,1515292,1481979,1507189,1425257,1449566,1382040,1417154,1329819,1341524,1290204,1356830,1264094,1265894,1326218,1237083,1207372,1180361,1222678,1224478,1167756,1176760,1179461,1244286,1286603,1305510,1385641,1508089,1732277,1969069,2427348,3583400,4999653,7937501,14420933,20375859,42760403,101379601,116701787,117473388,74001707,10310828,1870030,1317215,957974,786907,598734,506898,408760,319625,263803,210682,151259,114345,91836,59423,45018,42317,27011,20708,10804,8103,6302,6302,4502,900,900,3601],"channel":0,"concrete_channel":0,"sample":0,"color":"#f00"},{"type":"FAST","minimum":0,"maximum":244,"first_bin":0,"last_bin":244,"n_bins":256,"histogram":[168366,69327,58523,61224,76530,72928,63025,65726,54921,73829,70227,58523,74729,67526,78331,66626,82832,64825,71128,62124,87334,85533,97238,88234,99039,103540,118846,110743,117046,104441,94537,125149,132352,138654,131451,130551,126049,144056,133252,138654,167465,170166,163864,168366,174668,174668,161163,177369,197177,212483,213383,201679,225988,245796,242195,262002,252098,269205,297116,339433,363742,396155,415062,476286,530307,621243,641951,677965,847231,918359,997590,1066917,1174959,1312713,1363132,1499086,1575615,1762889,1757487,1929454,2105923,2206762,2301299,2484070,2490373,2558800,2714560,2748774,2787489,2802795,2855916,2860417,2892830,2855916,2864919,2878424,2870321,2815400,2775784,2629027,2682148,2623625,2712760,2620924,2520985,2398537,2471465,2371526,2261683,2365224,2213965,2180652,2168047,2121229,2027592,2047400,2032094,1977172,1991578,1993379,1975372,1921351,1879934,1851123,1856525,1915048,1950162,1833116,1893440,1920450,1933055,1915048,1892539,1898842,1864628,1950162,1865529,1887137,1849322,1862828,1905144,1857426,1887137,1888038,1853824,1815109,1837618,1795301,1777294,1726875,1812408,1766490,1746682,1729576,1734978,1736778,1720572,1700764,1683658,1689960,1611630,1628736,1664750,1606227,1458570,1490082,1522495,1469374,1467573,1526096,1520694,1436061,1396445,1374837,1297407,1382040,1286603,1251489,1297407,1264094,1228980,1181261,1126340,1146148,1122739,1129941,1129041,1051611,1078621,1080422,1057913,988586,1007494,990387,1046209,994889,978682,928263,947170,1004792,897651,963376,901252,952572,919259,907555,911156,902152,912957,889547,904853,880544,970579,992188,1093027,1208272,1409050,1673754,1861927,2297698,3060295,4056985,6141299,9782321,17007644,24281585,47741149,106594438,128127250,130554598,32725984,1292905,547414,317824,139555,71128,39615,17107,17107,9904,2701,2701,1801,900,900],"channel":1,"concrete_channel":0,"sample":1,"color":"#0f0"},{"type":"FAST","minimum":23,"maximum":255,"first_bin":23,"last_bin":255,"n_bins":256,"histogram":[900,0,0,0,0,0,0,0,0,0,900,3601,1801,900,1801,3601,5402,2701,4502,8103,14406,8103,15306,12605,14406,16206,27011,20708,25210,22509,36014,32413,30612,35114,39615,55822,50420,50420,45918,56722,54921,58523,63025,48619,55822,58523,77430,71128,70227,80131,91836,72928,67526,86434,75630,86434,94537,89135,94537,85533,72028,94537,115245,115245,122448,109843,111644,135953,135053,128750,153960,146757,150359,138654,149458,140455,162063,167465,165665,171967,196277,174668,184572,180070,216084,191775,202579,238593,234091,252098,252098,277308,270106,281810,310621,298917,331329,370945,373646,404258,414162,438471,509599,550115,556417,622143,663559,760797,838227,897651,1067817,1140746,1219076,1400947,1507189,1616131,1806106,1993379,2103222,2381430,2578607,2665041,2840610,3094509,3294387,3433941,3605008,3862509,3882317,4205543,4192938,4336994,4406321,4436033,4575587,4555780,4577388,4510762,4413524,4403620,4544975,4392816,4228952,4122710,4125411,4065088,3957046,3766171,3837299,3706748,3564492,3395226,3391625,3161135,3055794,2996370,3085505,2940549,2864019,2809097,2664141,2602917,2532689,2493974,2390434,2416544,2305801,2360722,2213064,2178851,2190556,2060905,2059104,1995179,1942059,1860127,1903343,1828614,1710668,1724174,1671953,1750284,1695362,1599925,1638640,1602626,1611630,1640441,1683658,1668352,1798002,1803404,1956464,2240975,2746073,3329501,4555780,7028145,13884324,45342612,123168113,205137032,86990181,17234532,4436933,2645233,2206762,2032994,1852924,1633238,1445064,1292005,1191165,1129041,985885,895850,795911,687869,547414,503297,449275,343934,316023,255700,195376,177369,187273,128750,93637,81932,69327,36914,177369],"channel":2,"concrete_channel":0,"sample":2,"color":"#00f"}],"size":3}                         
                                """
                        )
                )
        );

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/channelhistogram.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection[0].lastBin").value(255));
    }


    @Test
    @Transactional
    public void channel_histograms_bounds() throws Exception {
        ImageInstance image = given_test_image_instance();



        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(image.getBaseImage().getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-channels/bounds"))
                .willReturn(
                        aResponse().withBody(
                                """
                                    {"items":[
                                        {"type":"FAST","minimum":6,"maximum":255,"channel":0,"concrete_channel":0,"sample":0,"color":"#f00"},
                                        {"type":"FAST","minimum":0,"maximum":244,"channel":1,"concrete_channel":0,"sample":1,"color":"#0f0"},
                                        {"type":"FAST","minimum":23,"maximum":255,"channel":2,"concrete_channel":0,"sample":2,"color":"#00f"}],
                                    "size":3}                                                                                                                                                                                                                                                                                             
                                """
                        )
                )
        );

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/channelhistogram/bounds.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection[0].color").value("#f00"));
    }

    private String getSignedNotExpiredJwt() throws Exception {
        return getSignedJwt(Instant.now().plus(10, ChronoUnit.MINUTES));
    }

    private String getSignedJwt(Instant expiresAt) throws Exception {

        RSASSASigner signer = new RSASSASigner(rsaKey);
        Instant issuedAt = Instant.now();
        Map<String, Object> resourceAccessClaim = new HashMap<>();
        Map<String, Object> resource = new HashMap<>();
        List<String> resourceRoles = List.of("ADMIN");
        resource.put("roles", resourceRoles);
        resourceAccessClaim.put("core" , resource);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer("http://localhost:8888/")
                .expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(issuedAt))
                .claim("iss", "http://localhost:8888/")
                .claim("sub", UUID.randomUUID())
                .claim("name", "Some User")
                .claim("preferred_username", "superadmin")
                .claim("resource_access" , resourceAccessClaim)
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .build(), claimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public static void configureWireMock(WireMockServer wireMockServer) throws JOSEException {
        rsaKey = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(new Algorithm("RS256"))
                .keyID(KEY_ID)
                .generate();

        RSAKey rsaPublicJWK = rsaKey.toPublicJWK();
        String jwkResponse = String.format("{\"keys\": [%s]}", rsaPublicJWK.toJSONString());

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlMatching("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jwkResponse)));


    }

}
