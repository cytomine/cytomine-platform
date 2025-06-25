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
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.transaction.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static be.cytomine.service.middleware.ImageServerService.IMS_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
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
public class AbstractSliceResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restAbstractSliceControllerMockMvc;

    private static WireMockServer wireMockServer = new WireMockServer(8888);
    
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
    public void list_abstract_slice_by_abstract_image() throws Exception {
        AbstractSlice abstractSlice = builder.given_an_abstract_slice();

        restAbstractSliceControllerMockMvc.perform(get("/api/abstractimage/{id}/abstractslice.json", abstractSlice.getImageId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+abstractSlice.getId()+")]").exists());
    }

    @Test
    @Transactional
    public void list_abstract_slice_by_uploaded_file() throws Exception {
        AbstractSlice abstractSlice = builder.given_an_abstract_slice();

        restAbstractSliceControllerMockMvc.perform(get("/api/uploadedfile/{id}/abstractslice.json", abstractSlice.getUploadedFile().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+abstractSlice.getId()+")]").exists());
    }

    @Test
    @Transactional
    public void get_an_abstract_slice() throws Exception {
        AbstractSlice image = given_test_abstract_slice();

        restAbstractSliceControllerMockMvc.perform(get("/api/abstractslice/{id}.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.AbstractSlice"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.channel").hasJsonPath())
                .andExpect(jsonPath("$.zStack").hasJsonPath())
                .andExpect(jsonPath("$.time").hasJsonPath())
                .andExpect(jsonPath("$.rank").hasJsonPath())
                .andExpect(jsonPath("$.image").hasJsonPath())
                .andExpect(jsonPath("$.path").hasJsonPath())
                .andExpect(jsonPath("$.uploadedFile").hasJsonPath());
    }

    @Test
    @Transactional
    public void get_an_abstract_slice_not_exist() throws Exception {
        restAbstractSliceControllerMockMvc.perform(get("/api/abstractslice/{id}.json", 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.message").exists());
    }


    @Test
    @Transactional
    public void get_an_abstract_slice_with_coordinates() throws Exception {
        AbstractSlice image = given_test_abstract_slice();

        restAbstractSliceControllerMockMvc.perform(get("/api/abstractimage/{id}/{channel}/{zStack}/{time}/abstractslice.json",
                        image.getImage().getId(), image.getChannel(), image.getZStack(), image.getTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()));
    }

    @Test
    @Transactional
    public void add_valid_abstract_slice() throws Exception {
        AbstractSlice abstractSlice = builder.given_a_not_persisted_abstract_slice();
        restAbstractSliceControllerMockMvc.perform(post("/api/abstractslice.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(abstractSlice.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.abstractsliceID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.abstractslice.id").exists());

    }

    @Test
    @Transactional
    public void edit_valid_abstract_slice() throws Exception {
        AbstractSlice abstractSlice = builder.given_an_abstract_slice();
        JsonObject jsonObject = abstractSlice.toJsonObject();
        jsonObject.put("time", 3);
        restAbstractSliceControllerMockMvc.perform(put("/api/abstractslice/{id}.json", abstractSlice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.abstractsliceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditAbstractSliceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.abstractslice.id").exists())
                .andExpect(jsonPath("$.abstractslice.time").value(3));


    }


    @Test
    @Transactional
    public void delete_abstract_slice() throws Exception {
        AbstractSlice abstractSlice = builder.given_an_abstract_slice();
        restAbstractSliceControllerMockMvc.perform(delete("/api/abstractslice/{id}.json", abstractSlice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.abstractsliceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteAbstractSliceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.abstractslice.id").exists());


    }


    @Test
    @Transactional
    public void get_abstract_slice_uploader() throws Exception {
        AbstractSlice image = builder.given_an_abstract_slice();

        restAbstractSliceControllerMockMvc.perform(get("/api/abstractslice/{id}/user.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(builder.given_superadmin().getId()));
    }

    @Test
    @Transactional
    public void get_abstract_slice_uploader_when_abstract_slice_does_not_exists() throws Exception {
        restAbstractSliceControllerMockMvc.perform(get("/api/abstractslice/{id}/user.json", 0))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void get_abstract_slice_thumb() throws Exception {
        AbstractSlice image = given_test_abstract_slice();

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/thumb?z_slices=0&timepoints=0&length=512"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restAbstractSliceControllerMockMvc.perform(get("/api/abstractslice/{id}/thumb.png?maxSize=512", image.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @Transactional
    public void get_abstract_slice_thumb_if_image_not_exist() throws Exception {
        restAbstractSliceControllerMockMvc.perform(get("/api/abstractslice/{id}/thumb.png", 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }


    @Test
    @Transactional
    public void get_abstract_slice_tile() throws Exception {
        AbstractSlice image = given_test_abstract_slice();
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/normalized-tile/zoom/2/tx/4/ty/6?channels=0&z_slices=0&timepoints=0&filters=binary"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restAbstractSliceControllerMockMvc.perform(get("/api/abstractslice/{id}/normalized-tile/zoom/{z}/tx/{tx}/ty/{ty}.jpg?filters=binary&channels=0", image.getId(), 2, 4, 6))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @Transactional
    public void get_abstract_slice_tile_if_image_not_exist() throws Exception {
        restAbstractSliceControllerMockMvc.perform(get("/api/abstractslice/{id}/normalized-tile/zoom/{z}/tx/{tx}/ty/{ty}.jpg?filters=binary", 0, 2, 4, 6))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @Transactional
    public void get_abstract_slice_crop() throws Exception {
        AbstractSlice image = given_test_abstract_slice();

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/annotation/crop";
        String body = "{\"level\":0,\"z_slices\":0,\"annotations\":[{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\"}],\"timepoints\":0,\"background_transparency\":0}";
        System.out.println(url);
        System.out.println(body);

        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(equalTo(
                                body
                        ))
                        .willReturn(
                                aResponse().withBody(mockResponse)
                        )
        );

        MvcResult mvcResult = restAbstractSliceControllerMockMvc.perform(get("/api/abstractslice/{id}/crop.png", image.getId())
                        .param("location", "POLYGON((1 1,50 10,50 50,10 50,1 1))"))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @Transactional
    public void get_abstract_slice_window() throws Exception {
        AbstractSlice image = given_test_abstract_slice();

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        configureFor("localhost", 8888);
        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/window";
        String body = "{\"level\":0,\"z_slices\":0,\"timepoints\":0,\"region\":{\"left\":10,\"top\":20,\"width\":30,\"height\":40}}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(equalTo(body))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restAbstractSliceControllerMockMvc.perform(get("/api/abstractslice/{id}/window-10-20-30-40.png", image.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }


    private AbstractSlice given_test_abstract_slice() {
        AbstractSlice image = builder.given_an_abstract_slice();
        image.setMime(builder.given_a_mime("openslide/mrxs"));
        image.getImage().setWidth(109240);
        image.getImage().setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        return image;
    }

}
