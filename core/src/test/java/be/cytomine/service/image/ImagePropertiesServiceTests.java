package be.cytomine.service.image;

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
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.meta.Property;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.service.middleware.ImageServerService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static be.cytomine.service.middleware.ImageServerService.IMS_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ImagePropertiesServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    ImageServerService imageServerService;

    @Autowired
    ImagePropertiesService imagePropertiesService;

    @Autowired
    PropertyRepository propertyRepository;

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
    void extract_populated_properties_to_abstract_image() throws IOException, IllegalAccessException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");


        image.setWidth(1);
        image.setPhysicalSizeX(2d);
        image.setColorspace("empty");

        configureFor("localhost", 8888); //       /image/upload1644425985928451/LUNG1_pyr.tif/info
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/info"))
                .willReturn(
                        aResponse().withBody(
                            """
                            {"image":
                                {
                                    "original_format":"PYRTIFF",
                                    "width":30720,
                                    "height":25600,
                                    "depth":1,
                                    "duration":1,
                                    "physical_size_x":100000.00617,
                                    "physical_size_y":100000.00617,
                                    "physical_size_z":null,
                                    "frame_rate":null,
                                    "n_channels":3,
                                    "n_concrete_channels":1,
                                    "n_samples":3,
                                    "n_planes":1,
                                    "are_rgb_planes":true,
                                    "n_distinct_channels":1,
                                    "acquired_at":null,
                                    "description":"",
                                    "pixel_type":"uint8",
                                    "significant_bits":8,"bits":8},
                                    "instrument":
                                        {"microscope":{"model":null},
                                         "objective":{"nominal_magnification":null,"calibrated_magnification":null}},
                                         "associated":[],
                                         "channels":[
                                            {"index":0,"suggested_name":"R","emission_wavelength":null,"excitation_wavelength":null,"color":"#f00"},
                                            {"index":1,"suggested_name":"G","emission_wavelength":null,"excitation_wavelength":null,"color":"#0f0"},
                                            {"index":2,"suggested_name":"B","emission_wavelength":null,"excitation_wavelength":null,"color":"#00f"}
                                         ],
                                         "representations":[
                                            {"role":"UPLOAD",
                                             "file":
                                                {"file_type":"SINGLE",
                                                "filepath":"/data/images/upload1644425985928451/LUNG1_pyr.tif",
                                                "stem":"LUNG1_pyr",
                                                "extension":".tif",
                                                "created_at":"2022-05-05T22:16:23.318839",
                                                "size":126616954,"is_symbolic":false,"role":"UPLOAD"}
                                                },
                                                {"role":"ORIGINAL",
                                                "file":
                                                    {"file_type":"SINGLE","filepath":"/data/images/upload1644425985928451/processed/original.PYRTIFF","stem":"original","extension":".PYRTIFF","created_at":"2022-05-05T22:16:23.318839","size":126616954,"is_symbolic":true,"role":"ORIGINAL"}
                                                    },
                                                    {"role":"SPATIAL","file":{"file_type":"SINGLE","filepath":"/data/images/upload1644425985928451/processed/visualisation.PYRTIFF","stem":"visualisation","extension":".PYRTIFF","created_at":"2022-05-05T22:16:23.318839","size":126616954,"is_symbolic":true,"role":"SPATIAL"},"pyramid":{"n_tiers":8,"tiers":[{"zoom":7,"level":0,"width":30720,"height":25600,"tile_width":256,"tile_height":256,"downsampling_factor":1.0,"n_tiles":12000,"n_tx":120,"n_ty":100},{"zoom":6,"level":1,"width":15360,"height":12800,"tile_width":256,"tile_height":256,"downsampling_factor":2.0,"n_tiles":3000,"n_tx":60,"n_ty":50},{"zoom":5,"level":2,"width":7680,"height":6400,"tile_width":256,"tile_height":256,"downsampling_factor":4.0,"n_tiles":750,"n_tx":30,"n_ty":25},{"zoom":4,"level":3,"width":3840,"height":3200,"tile_width":256,"tile_height":256,"downsampling_factor":8.0,"n_tiles":195,"n_tx":15,"n_ty":13},{"zoom":3,"level":4,"width":1920,"height":1600,"tile_width":256,"tile_height":256,"downsampling_factor":16.0,"n_tiles":56,"n_tx":8,"n_ty":7},{"zoom":2,"level":5,"width":960,"height":800,"tile_width":256,"tile_height":256,"downsampling_factor":32.0,"n_tiles":16,"n_tx":4,"n_ty":4},{"zoom":1,"level":6,"width":480,"height":400,"tile_width":256,"tile_height":256,"downsampling_factor":64.0,"n_tiles":4,"n_tx":2,"n_ty":2},{"zoom":0,"level":7,"width":240,"height":200,"tile_width":256,"tile_height":256,"downsampling_factor":128.0,"n_tiles":1,"n_tx":1,"n_ty":1}]}}]}

                            """
                        )
                )
        );


        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/metadata"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"size":14,"items":
                                [{"key":"ImageWidth","value":30720,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"ImageLength","value":25600,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"BitsPerSample","value":"(8, 8, 8)","type":"UNKNOWN","namespace":"TIFF"},
                                {"key":"Compression","value":"JPEG","type":"STRING","namespace":"TIFF"},
                                {"key":"PhotometricInterpretation","value":"YCBCR","type":"STRING","namespace":"TIFF"},
                                {"key":"Orientation","value":"TOPLEFT","type":"STRING","namespace":"TIFF"},
                                {"key":"SamplesPerPixel","value":3,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"XResolution","value":"(429496703, 4294967295)","type":"UNKNOWN","namespace":"TIFF"},
                                {"key":"YResolution","value":"(429496703, 4294967295)","type":"UNKNOWN","namespace":"TIFF"},
                                {"key":"PlanarConfiguration","value":"CONTIG","type":"STRING","namespace":"TIFF"},
                                {"key":"ResolutionUnit","value":"CENTIMETER","type":"STRING","namespace":"TIFF"},
                                {"key":"TileWidth","value":256,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"TileLength","value":256,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"ReferenceBlackWhite","value":"(0, 1, 255, 1, 128, 1, 255, 1, 128, 1, 255, 1)","type":"UNKNOWN","namespace":"TIFF"}]
                                }
                                """
                        )
                )
        );

        imagePropertiesService.extractUseful(image);

        assertThat(image.getWidth()).isEqualTo(30720);
        assertThat(image.getHeight()).isEqualTo(25600);
        assertThat(image.getColorspace()).isEqualTo("empty");
    }

}
