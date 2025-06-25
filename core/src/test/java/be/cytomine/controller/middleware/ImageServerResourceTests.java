package be.cytomine.controller.middleware;

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

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import be.cytomine.CytomineCoreApplication;

import static be.cytomine.service.middleware.ImageServerService.IMS_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class ImageServerResourceTests {

    @Autowired
    private MockMvc restImageserverControllerMockMvc;

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

    @Test
    @Transactional
    public void get_a_imageserver_format() throws Exception {
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/formats"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"items":[
                                    {"id":"BMP","name":"BMP","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"DICOM","name":"Dicom","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"PYROMETIFF","name":"Pyramidal OME-TIFF","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"PLANARTIFF","name":"Planar TIFF","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"IMAGEJTIFF","name":"ImageJ TIFF","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"JPEG","name":"JPEG","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"JPEG2000","name":"JPEG2000","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"OMETIFF","name":"OME-TIFF","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"PNG","name":"PNG","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"PPM","name":"PPM","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"SIS","name":"Olympus SIS TIFF","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"PYRTIFF","name":"Pyramidal TIFF","remarks":"","convertible":false,"readable":true,"writable":true,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"VIRTUALSTACK","name":"Virtual Stack","remarks":"","convertible":true,"readable":true,"writable":false,"importable":false,"plugin":"pims.formats.common"},
                                    {"id":"WEBP","name":"Web P","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"CZI","name":"Zeiss CZI","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_bioformats"},
                                    {"id":"LIF","name":"Leica LIF","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_bioformats"},
                                    {"id":"ND2","name":"Nikon ND2","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_bioformats"},
                                    {"id":"WSIDICOM","name":"WSI Dicom","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_dicom"},
                                    {"id":"ISYNTAX","name":"Philips ISyntax","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_isyntax"},
                                    {"id":"BIF","name":"Ventana BIF","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"MRXS","name":"3D Histech MIRAX","remarks":"One .mrxs file and one directory with same name with .dat and .ini files, packed in an archive. ","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"NDPI","name":"Hamamatsu NDPI","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"PHILIPS","name":"Philips TIFF","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"SCN","name":"Leica SCN","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"SVS","name":"Leica Aperio SVS","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"VMS","name":"Hamamatsu VMS","remarks":"One .vms file, one .opt optimization file and several .jpg with same name, packed in an archive. ","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"}
                                    ],"size":26}                                        
                                        """
                        )
                )
        );
        restImageserverControllerMockMvc.perform(get("/api/imageserver/format.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='MRXS')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='VMS')].remarks").value("One .vms file, one .opt optimization file and several .jpg with same name, packed in an archive. "));
    }
}
