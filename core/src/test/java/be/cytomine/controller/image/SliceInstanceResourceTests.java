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
import be.cytomine.domain.image.SliceInstance;
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
public class SliceInstanceResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restSliceInstanceControllerMockMvc;

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
    public void list_slice_instance_by_image_instance() throws Exception {
        SliceInstance sliceInstance = builder.given_a_slice_instance();

        restSliceInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/sliceinstance.json", sliceInstance.getImage().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+sliceInstance.getId()+")]").exists());
    }

    @Test
    @Transactional
    public void get_an_slice_instance() throws Exception {
        SliceInstance image = given_test_slice_instance();

        restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.SliceInstance"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.image").value(image.getImage().getId()))
                .andExpect(jsonPath("$.mime").hasJsonPath())
                .andExpect(jsonPath("$.baseSlice").hasJsonPath())
                .andExpect(jsonPath("$.path").hasJsonPath())
                .andExpect(jsonPath("$.zStack").hasJsonPath())
                .andExpect(jsonPath("$.rank").hasJsonPath())
                .andExpect(jsonPath("$.time").hasJsonPath())
                .andExpect(jsonPath("$.updated").hasJsonPath())
                .andExpect(jsonPath("$.uploadedFile").hasJsonPath());
    }

    @Test
    @Transactional
    public void get_an_slice_instance_not_exist() throws Exception {
        restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}.json", 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.message").exists());
    }


    @Test
    @Transactional
    public void get_an_slice_instance_with_coordinates() throws Exception {
        SliceInstance image = given_test_slice_instance();

        restSliceInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/{channel}/{zStack}/{time}/sliceinstance.json",
                        image.getImage().getId(), image.getBaseSlice().getChannel(), image.getBaseSlice().getZStack(), image.getBaseSlice().getTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()));
    }

    @Test
    @Transactional
    public void add_valid_slice_instance() throws Exception {
        SliceInstance sliceInstance = builder.given_a_not_persisted_slice_instance();
        restSliceInstanceControllerMockMvc.perform(post("/api/sliceinstance.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sliceInstance.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.sliceinstanceID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.sliceinstance.id").exists());

    }

    @Test
    @Transactional
    public void edit_valid_slice_instance() throws Exception {
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        JsonObject jsonObject = sliceInstance.toJsonObject();
        restSliceInstanceControllerMockMvc.perform(put("/api/sliceinstance/{id}.json", sliceInstance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.sliceinstanceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditSliceInstanceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.sliceinstance.id").exists());


    }


    @Test
    @Transactional
    public void delete_slice_instance() throws Exception {
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        restSliceInstanceControllerMockMvc.perform(delete("/api/sliceinstance/{id}.json", sliceInstance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.sliceinstanceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteSliceInstanceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.sliceinstance.id").exists());


    }

    @Test
    @Transactional
    public void get_slice_instance_thumb() throws Exception {
        SliceInstance image = given_test_slice_instance();
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/thumb?z_slices=0&timepoints=0&length=512"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}/thumb.png?maxSize=512", image.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @Transactional
    public void get_slice_instance_thumb_if_image_not_exist() throws Exception {
        restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}/thumb.png", 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @Transactional
    public void get_slice_instance_tile() throws Exception {
        SliceInstance image = given_test_slice_instance();
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/normalized-tile/zoom/2/tx/4/ty/6?z_slices=0&timepoints=0&filters=binary"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}/normalized-tile/zoom/{z}/tx/{tx}/ty/{ty}.jpg?filters=binary", image.getId(), 2, 4, 6))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @Transactional
    public void get_slice_instance_tile_if_image_not_exist() throws Exception {
        restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}/normalized-tile/zoom/{z}/tx/{tx}/ty/{ty}.jpg?filters=binary", 0, 2, 4, 6))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @Transactional
    public void get_slice_instance_crop() throws Exception {
        SliceInstance image = given_test_slice_instance();

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

        MvcResult mvcResult = restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}/crop.png", image.getId())
                        .param("location", "POLYGON((1 1,50 10,50 50,10 50,1 1))"))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @Transactional
    public void get_slice_instance_window() throws Exception {
        SliceInstance image = given_test_slice_instance();

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        configureFor("localhost", 8888);
        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/window";
        String body = "{\"level\":0,\"z_slices\":0,\"annotations\":[{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\"}],\"timepoints\":0,\"background_transparency\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(equalTo(body))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );


        MvcResult mvcResult = restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}/window-10-20-30-40.png", image.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);

    }



    @Test
    @Transactional
    public void histograms() throws Exception {
        SliceInstance image = given_test_slice_instance();

        configureFor("localhost", 8888);
        System.out.println("/image/"+ URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0?n_bins=256&channels=0");
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0?n_bins=256&channels=0"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"items":[
                                    {"type":"FAST","minimum":6,"maximum":255,"first_bin":6,"last_bin":255,"n_bins":256,
                                    "histogram":[900,2701,2701,2701,3601,5402,4502,9904,9004,11705,15306,18007,32413,19808,27011,22509,
                                                34213,29712,35114,45918,43217,46818,42317,48619,42317,49519,54021,54921,59423,59423,62124,
                                                68427,68427,66626,69327,59423,67526,98138,68427,86434,76530,81032,74729,85533,92736,90936,
                                                104441,95437,95437,108943,120647,108042,129651,125149,148558,141355,135053,122448,154860,
                                                125149,166565,170166,145857,165665,187273,163864,181871,187273,189974,182771,212483,210682,
                                                204380,212483,241294,220586,241294,269205,253899,278209,287212,323226,323226,379048,398856,
                                                458279,489791,537510,602335,664460,725683,831925,891348,992188,1086725,1264994,1364933,1543203,
                                                1594523,1736778,1868230,2021290,2175250,2295897,2446256,2485871,2745172,2750574,2873022,2985566,
                                                3035986,3253871,3206152,3202551,3225960,3281782,3261074,3346607,3134124,3232263,3139526,3180943,
                                                3134124,3022481,2955855,2957655,2924342,2810898,2754176,2747873,2620924,2651536,2593013,2671343,
                                                2496675,2468764,2448056,2445355,2320206,2340914,2385932,2355320,2318406,2360722,2278790,2359822,
                                                2225669,2330110,2229271,2228370,2200460,2256281,2192356,2174349,2235573,2154542,2118528,2075311,
                                                2110424,2110424,2122129,2102321,2005984,2117627,1942959,1974471,1951062,1987076,1846621,1918649,
                                                1804305,1813308,1825013,1774593,1792600,1763789,1747583,1663850,1664750,1638640,1636839,1597224,
                                                1593623,1515292,1481979,1507189,1425257,1449566,1382040,1417154,1329819,1341524,1290204,1356830,
                                                1264094,1265894,1326218,1237083,1207372,1180361,1222678,1224478,1167756,1176760,1179461,1244286,
                                                1286603,1305510,1385641,1508089,1732277,1969069,2427348,3583400,4999653,7937501,14420933,20375859,
                                                42760403,101379601,116701787,117473388,74001707,10310828,1870030,1317215,957974,786907,598734,506898,
                                                408760,319625,263803,210682,151259,114345,91836,59423,45018,42317,27011,20708,10804,8103,6302,6302,4502,900,900,3601],
                                                "channel":0,"concrete_channel":0,"sample":0,"color":"#f00","z_slice":0,"timepoint":0}
                                    ],"size":1}                                   
                                """
                        )
                )
        );

        restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}/histogram.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].lastBin").value("255"))
                .andExpect(jsonPath("$.collection[0].color").value("#f00"))
                .andExpect(jsonPath("$.collection[0].histogram[0]").value(900));
    }


    @Test
    @Transactional
    public void histograms_bounds() throws Exception {
        SliceInstance image = given_test_slice_instance();


        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0/bounds?channels=0"))
                .willReturn(
                        aResponse().withBody(
                                """
                                    {"items":[{"type":"FAST","minimum":6,"maximum":255,"channel":0,"concrete_channel":0,"sample":0,"color":"#f00","z_slice":0,"timepoint":0}],"size":1}                                                                                             
                                """
                        )
                )
        );

        restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}/histogram/bounds.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].color").value("#f00"));
    }


    @Test
    @Transactional
    public void channel_histograms() throws Exception {
        SliceInstance image = given_test_slice_instance();

        configureFor("localhost", 8888);
        System.out.println("/image/"+ URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0?n_bins=256");
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0?n_bins=256"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"items":[{"type":"FAST","minimum":6,"maximum":255,"first_bin":6,"last_bin":255,"n_bins":256,"histogram":
                                [900,2701,2701,2701,3601,5402,4502,9904,9004,11705,15306,18007,32413,19808,27011,22509,34213,29712,35114,
                                45918,43217,46818,42317,48619,42317,49519,54021,54921,59423,59423,62124,68427,68427,66626,69327,59423,67526,
                                98138,68427,86434,76530,81032,74729,85533,92736,90936,104441,95437,95437,108943,120647,108042,129651,125149,
                                148558,141355,135053,122448,154860,125149,166565,170166,145857,165665,187273,163864,181871,187273,189974,182771,
                                212483,210682,204380,212483,241294,220586,241294,269205,253899,278209,287212,323226,323226,379048,398856,458279,
                                489791,537510,602335,664460,725683,831925,891348,992188,1086725,1264994,1364933,1543203,1594523,1736778,1868230,
                                2021290,2175250,2295897,2446256,2485871,2745172,2750574,2873022,2985566,3035986,3253871,3206152,3202551,3225960,
                                3281782,3261074,3346607,3134124,3232263,3139526,3180943,3134124,3022481,2955855,2957655,2924342,2810898,2754176,
                                2747873,2620924,2651536,2593013,2671343,2496675,2468764,2448056,2445355,2320206,2340914,2385932,2355320,2318406,
                                2360722,2278790,2359822,2225669,2330110,2229271,2228370,2200460,2256281,2192356,2174349,2235573,2154542,2118528,
                                2075311,2110424,2110424,2122129,2102321,2005984,2117627,1942959,1974471,1951062,1987076,1846621,1918649,1804305,
                                1813308,1825013,1774593,1792600,1763789,1747583,1663850,1664750,1638640,1636839,1597224,1593623,1515292,1481979,
                                1507189,1425257,1449566,1382040,1417154,1329819,1341524,1290204,1356830,1264094,1265894,1326218,1237083,1207372,
                                1180361,1222678,1224478,1167756,1176760,1179461,1244286,1286603,1305510,1385641,1508089,1732277,1969069,2427348,
                                3583400,4999653,7937501,14420933,20375859,42760403,101379601,116701787,117473388,74001707,10310828,1870030,1317215,
                                957974,786907,598734,506898,408760,319625,263803,210682,151259,114345,91836,59423,45018,42317,27011,20708,10804,8103,
                                6302,6302,4502,900,900,3601],"channel":0,"concrete_channel":0,"sample":0,"color":"#f00","z_slice":0,"timepoint":0}],"size":1}                          
                                """
                        )
                )
        );

        restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}/channelhistogram.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].type").value("FAST"));
    }


    @Test
    @Transactional
    public void channel_histograms_bounds() throws Exception {
        SliceInstance image = given_test_slice_instance();


        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0/bounds"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"items":[{"type":"FAST","minimum":6,"maximum":255,"channel":0,"concrete_channel":0,"sample":0,"color":"#f00","z_slice":0,"timepoint":0},
                                {"type":"FAST","minimum":0,"maximum":244,"channel":1,"concrete_channel":0,"sample":1,"color":"#0f0","z_slice":0,"timepoint":0},
                                {"type":"FAST","minimum":23,"maximum":255,"channel":2,"concrete_channel":0,"sample":2,"color":"#00f","z_slice":0,"timepoint":0}],"size":3}                                                                                                                                                                                                                                                                                                   
                                """
                        )
                )
        );

        restSliceInstanceControllerMockMvc.perform(get("/api/sliceinstance/{id}/channelhistogram/bounds.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection[0].color").value("#f00"));
    }

    private SliceInstance given_test_slice_instance() {
        AbstractSlice image = builder.given_an_abstract_slice();
        image.setMime(builder.given_a_mime("openslide/mrxs"));
        image.getImage().setWidth(109240);
        image.getImage().setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        SliceInstance sliceInstance = builder.given_a_slice_instance(builder.given_an_image_instance(image.getImage(), builder.given_a_project()),image);
        return sliceInstance;
    }

}
