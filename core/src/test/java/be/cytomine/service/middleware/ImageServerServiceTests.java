package be.cytomine.service.middleware;

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
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.dto.StorageStats;
import be.cytomine.dto.image.CropParameter;
import be.cytomine.dto.image.ImageParameter;
import be.cytomine.dto.image.LabelParameter;
import be.cytomine.dto.image.TileParameters;
import be.cytomine.dto.image.WindowParameter;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.locationtech.jts.io.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static be.cytomine.service.middleware.ImageServerService.IMS_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ImageServerServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    ImageServerService imageServerService;

    @Autowired
    ApplicationProperties applicationProperties;

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
    void retrieve_storage_spaces() throws IOException {
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/storage/size.json"))
                .willReturn(
                        aResponse().withBody("" + "{\"used\":193396892,\"available\":445132860,\"usedP\":0.302878435,\"hostname\":\"b52416f53249\",\"mount\":\"/data/images\",\"ip\":null}")
                )
        );

        StorageStats response = imageServerService.storageSpace();
        assertThat(response).isNotNull();
        assertThat(response.getUsed()).isEqualTo(193396892);
        assertThat(response.getHostname()).isEqualTo("b52416f53249");

    }


    @Test
    void retrieve_formats() throws IOException {
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

        List<Map<String, Object>> formats = imageServerService.formats();
        printLastRequest();
        System.out.println(formats.stream().map(x -> x.get("id")).collect(Collectors.toList()));

        assertThat(formats).isNotNull();
        assertThat(formats.size()).isEqualTo(26);
        assertThat(formats.stream().map(x -> x.get("id"))).contains("PNG", "CZI", "SVS");
        assertThat(formats.stream().filter(x -> x.get("id").equals("PNG")).findFirst().get()).containsAllEntriesOf(Map.of("id", "PNG", "name", "PNG", "plugin", "pims.formats.common"));
    }


    //http://localhost-ims/image/download?fif=%2Fdata%2Fimages%2F58%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs

    // TODO
    @Test
    void retrieve_abstract_image_download_uri() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setOriginalFilename("CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");

        configureFor("localhost", 8888);
        byte[] mockResponse = UUID.randomUUID().toString().getBytes();
        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/")
                + "/export?filename=" + URLEncoder.encode(image.getOriginalFilename(), StandardCharsets.UTF_8);

        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        byte[] data = imageServerService.download(image, null).getBody();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse);

        url = "/file/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/")
            + "/export?filename=" + URLEncoder.encode(image.getUploadedFile().getOriginalFilename(), StandardCharsets.UTF_8);

        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );
        data = imageServerService.download(image.getUploadedFile(), null).getBody();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse);
    }

    @Test
    void retrieve_uploaded_file_download_uri() throws IOException {
        UploadedFile uploadedFile = builder.given_a_not_persisted_uploaded_file();
        uploadedFile.setFilename("1636379100999/CMU-2.zip");
        uploadedFile.setOriginalFilename("CMU-2.zip");
        uploadedFile.setContentType("ZIP");

        configureFor("localhost", 8888);
        byte[] mockResponse = UUID.randomUUID().toString().getBytes();
        String url = "/file/" + URLEncoder.encode(uploadedFile.getPath(), StandardCharsets.UTF_8).replace("%2F", "/")
                + "/export?filename=" + URLEncoder.encode(uploadedFile.getOriginalFilename(), StandardCharsets.UTF_8);

        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        byte[] data = imageServerService.download(uploadedFile, null).getBody();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse);
    }

    @Test
    void extract_properties_from_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");


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
        assertThat(((Map<String, Object>)imageServerService.properties(image).get("image")).size()).isEqualTo(20);
        printLastRequest();
        assertThat(((Map<String, Object>)imageServerService.properties(image).get("image")).get("width")).isEqualTo(30720);

    }

    @Test
    void get_associated_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/info/associated"))
                .willReturn(
                        aResponse().withBody("{\"items\": [{\"name\":\"macro\"},{\"name\":\"thumbnail\"},{\"name\":\"label\"}], \"size\": 0}")
                )
        );
        int size = imageServerService.associated(image).size();
        printLastRequest();
        assertThat(size).isEqualTo(3);

    }

    @Test
    void get_label_abstract_image_macro() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        configureFor("localhost", 8888);
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/associated/macro?length=512";

        System.out.println(url);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );
        LabelParameter labelParameter = new LabelParameter();
        labelParameter.setMaxSize(512);
        labelParameter.setLabel("macro");
        labelParameter.setFormat("png");
        byte[] data = imageServerService.label(image, labelParameter, null, null).getBody();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse);
    }

    @Test
    void get_thumb_for_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content


        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/thumb?z_slices=0&timepoints=0&length=256"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );
        ImageParameter imageParameter = new ImageParameter();
        imageParameter.setMaxSize(256);
        imageParameter.setFormat("png");

        byte[] data = imageServerService.thumb(slice, imageParameter, null, null).getBody();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse);


        byte[] mockResponse2 = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/thumb?z_slices=0&timepoints=0&length=512"))
                .willReturn(
                        aResponse().withBody(mockResponse2)
                )
        );

        imageParameter.setMaxSize(512);
        imageParameter.setFormat("png");
        data = imageServerService.thumb(slice, imageParameter, null, null).getBody();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse2);
    }

    @Test
    void get_normalized_tile_for_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content


        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/normalized-tile/zoom/2/tx/4/ty/6?z_slices=0&timepoints=0&filters=binary"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );
        TileParameters tileParameters = new TileParameters();
        tileParameters.setZoom(2L);
        tileParameters.setTx(4L);
        tileParameters.setTy(6L);
        tileParameters.setFormat("webp");
        tileParameters.setFilters("binary");

        byte[] data = imageServerService.normalizedTile(slice, tileParameters, null, null).getBody();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse);


        byte[] mockResponse2 = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/normalized-tile/zoom/2/tx/4/ty/6?channels=1&z_slices=0&timepoints=3&filters=otsu"))
                .willReturn(
                        aResponse().withBody(mockResponse2)
                )
        );

        tileParameters.setFormat("webp");
        tileParameters.setFilters("otsu");
        tileParameters.setTimepoints("3");
        tileParameters.setChannels("1");
        data = imageServerService.normalizedTile(slice, tileParameters, null, null).getBody();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse2);
    }


    @Test
    void get_crop_for_abstract_image() throws IOException, ParseException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/annotation/drawing";
        String geometry = new WKTReader().read("POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))").norm().toString();
        String body = "{\"level\":0,\"z_slices\":0,\"annotations\":[{\"geometry\":\"" + geometry +"\",\"stroke_color\":null,\"stroke_width\":null}],\"timepoints\":0,\"context_factor\":1.25}";
        System.out.println(url);
        System.out.println(body);
        stubFor(post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(equalTo(
                body
                        ))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        CropParameter cropParameter = new CropParameter();
        cropParameter.setLocation("POLYGON((1 1,50 10,50 50,10 50,1 1))");
        cropParameter.setFormat("png");
        cropParameter.setDraw(true);
        cropParameter.setIncreaseArea(1.25);
        cropParameter.setComplete(true);

        byte[] crop = null;
        try {
            crop = imageServerService.crop(slice, cropParameter, null, null).getBody();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        printLastRequest();
        assertThat(crop).isEqualTo(mockResponse);

    }


    @Test
    void get_window_for_abstract_image() throws UnsupportedEncodingException, ParseException {

        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        configureFor("localhost", 8888);
        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/window";
        String body = "{\"level\":0,\"z_slices\":0,\"timepoints\":0,\"region\":{\"left\":10,\"top\":20,\"width\":30,\"height\":40}}";
        System.out.println(url);
        System.out.println(body);
        stubFor(post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(equalTo(body))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(10);
        windowParameter.setY(20);
        windowParameter.setW(30);
        windowParameter.setH(40);
        windowParameter.setFormat("png");
        byte[] crop = imageServerService.window(slice, windowParameter, null, null).getBody();
        printLastRequest();
        assertThat(crop).isEqualTo(mockResponse);
    }


    @Test
    void image_histograms() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);


        configureFor("localhost", 8888);
        System.out.println("/image/"+ URLEncoder.encode(slice.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-image?n_bins=256");
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(slice.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-image?n_bins=256"))
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

        Map<String, Object> reponse = imageServerService.imageHistogram(image, 256);
        printLastRequest();

        assertThat(reponse).isNotNull();
        assertThat(reponse.get("lastBin")).isEqualTo(255);
        assertThat(reponse.get("type")).isEqualTo("FAST");
        assertThat(((List)reponse.get("histogram")).get(0)).isEqualTo(168366);
        assertThat(((List)reponse.get("histogram")).get(1)).isEqualTo(69327);
    }

    @Test
    void image_histograms_bounds() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);


        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(slice.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-image/bounds?n_bins=256"))
                .willReturn(
                        aResponse().withBody(
                                """
                                 {"type":"FAST","minimum":0,"maximum":255}                                                                                   
                                """
                        )
                )
        );

        Map<String, Object> response = imageServerService.imageHistogramBounds(image);
        printLastRequest();

        assertThat(response).isNotNull();
        assertThat(response.get("minimum")).isEqualTo(0);
        assertThat(response.get("maximum")).isEqualTo(255);
    }


    @Test
    void plane_histograms() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);


        configureFor("localhost", 8888);
        System.out.println("/image/"+ URLEncoder.encode(slice.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0?n_bins=256&channels=0");
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(slice.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0?n_bins=256&channels=0"))
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

        List<Map<String, Object>> reponse = imageServerService.planeHistograms(slice, 256, false);
        printLastRequest();

        assertThat(reponse).isNotNull();
        assertThat(reponse.size()).isEqualTo(1);
        assertThat(reponse.get(0).get("lastBin")).isEqualTo(255);
        assertThat(reponse.get(0).get("color")).isEqualTo("#f00");
        assertThat(((List)reponse.get(0).get("histogram")).get(0)).isEqualTo(900);
        assertThat(((List)reponse.get(0).get("histogram")).get(1)).isEqualTo(2701);
    }

    @Test
    void plane_histograms_bounds() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);


        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(slice.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0/bounds?channels=0"))
                .willReturn(
                        aResponse().withBody(
                                """
                                    {"items":[{"type":"FAST","minimum":6,"maximum":255,"channel":0,"concrete_channel":0,"sample":0,"color":"#f00","z_slice":0,"timepoint":0}],"size":1}                                                                                             
                                """
                        )
                )
        );

        List<Map<String, Object>> reponse = imageServerService.planeHistogramBounds(slice, false);
        printLastRequest();

        assertThat(reponse).isNotNull();
        assertThat(reponse.size()).isEqualTo(1);
        assertThat(reponse.get(0).get("channel")).isEqualTo(0);
        assertThat(reponse.get(0).get("color")).isEqualTo("#f00");
    }


    @Test
    void channel_histograms() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);



        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(slice.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0?n_bins=256"))
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

        List<Map<String, Object>> reponse = imageServerService.planeHistograms(slice, 256, true);
        printLastRequest();

        assertThat(reponse).isNotNull();
        assertThat(reponse.size()).isEqualTo(1);
        assertThat(reponse.get(0).get("channel")).isEqualTo(0);
        assertThat(reponse.get(0).get("color")).isEqualTo("#f00");
        assertThat(((List)reponse.get(0).get("histogram")).get(0)).isEqualTo(900);
        assertThat(((List)reponse.get(0).get("histogram")).get(1)).isEqualTo(2701);
    }

    @Test
    void chanel_histograms_bounds() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);


        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/image/"+ URLEncoder.encode(slice.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") +"/histogram/per-plane/z/0/t/0/bounds"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"items":[
                                {"type":"FAST","minimum":6,"maximum":255,"channel":0,"concrete_channel":0,"sample":0,"color":"#f00","z_slice":0,"timepoint":0},
                                {"type":"FAST","minimum":0,"maximum":244,"channel":1,"concrete_channel":0,"sample":1,"color":"#0f0","z_slice":0,"timepoint":0},
                                {"type":"FAST","minimum":23,"maximum":255,"channel":2,"concrete_channel":0,"sample":2,"color":"#00f","z_slice":0,"timepoint":0}],
                                "size":3}                                                                                    
                                """
                        )
                )
        );

        List<Map<String, Object>> reponse = imageServerService.planeHistogramBounds(slice, true);
        printLastRequest();

        assertThat(reponse).isNotNull();
        assertThat(reponse.size()).isEqualTo(3);
        assertThat(reponse.get(0).get("channel")).isEqualTo(0); //concreteChannel
        assertThat(reponse.get(1).get("channel")).isEqualTo(0); //concreteChannel
        assertThat(reponse.get(2).get("channel")).isEqualTo(0); //concreteChannel
        assertThat(reponse.get(0).get("apparentChannel")).isEqualTo(0); //channel
        assertThat(reponse.get(1).get("apparentChannel")).isEqualTo(1); //channel
        assertThat(reponse.get(2).get("apparentChannel")).isEqualTo(2); //channel
        assertThat(reponse.get(0).get("minimum")).isEqualTo(6);
        assertThat(reponse.get(0).get("maximum")).isEqualTo(255);
    }








    private void printLastRequest() {
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        all.subList(Math.max(all.size() - 3, 0), all.size()).forEach(x -> System.out.println(x.getMethod() + " " + x.getAbsoluteUrl() + " " + x.getBodyAsString()));
    }


}
