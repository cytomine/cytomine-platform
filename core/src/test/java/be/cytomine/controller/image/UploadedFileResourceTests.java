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
import be.cytomine.domain.image.*;
import be.cytomine.repository.image.UploadedFileRepository;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static be.cytomine.service.middleware.ImageServerService.IMS_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
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
public class UploadedFileResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restUploadedFileControllerMockMvc;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

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
    public void list_uploaded() throws Exception {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();

        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+uploadedFile.getId()+")]").exists());
    }

    @Test
    @Transactional
    public void list_uploaded_hirerachical_tree() throws Exception {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();

        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json").param("root", uploadedFile.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void list_uploaded_with_search() throws Exception {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        uploadedFile.setOriginalFilename("abracadabra");

        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("originalFilename[equals]", "abracadabra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+uploadedFile.getId()+")]").exists());

        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("originalFilename[equals]", "notFound"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+uploadedFile.getId()+")]").doesNotExist());
    }



    @Test
    @Transactional
    public void list_uploaded_file_with_pagination() throws Exception {

        UploadedFile image1 = builder.given_a_uploaded_file();
        UploadedFile image2 = builder.given_a_uploaded_file();
        UploadedFile image3 = builder.given_a_uploaded_file();

        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("offset", "0")
                        .param("max", "0")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.size", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalPages").value(1));


        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("offset", "0")
                        .param("max", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(3)));


        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("offset", "1")
                        .param("max", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(3)));

        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("offset", "1")
                        .param("max", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(2)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.size", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalPages").value(1));


        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("offset", "0")
                        .param("max", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.totalPages").value(1));


        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("offset", "500")
                        .param("max", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0)))) // default sorting must be created desc
                .andExpect(jsonPath("$.offset").value(500))
                .andExpect(jsonPath("$.perPage").value(0))
                .andExpect(jsonPath("$.size").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    List<Long> retrieveIds(MvcResult mvcResult) throws UnsupportedEncodingException {
        Map<String, Object> result = JsonObject.toMap(mvcResult.getResponse().getContentAsString());
        List<Map<String, Object>> collection = (List<Map<String, Object>>) result.get("collection");
        return collection.stream().map(x -> Long.valueOf(x.get("id").toString())).collect(Collectors.toList());
    }

    @Test
    @Transactional
    void sort_uploaded_file() throws Exception {

        //creation
        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        uploadedFile.setSize(1L);
        UploadedFile uploadedFileChild1 = builder.given_a_uploaded_file();
        uploadedFileChild1.setParent(uploadedFile);
        UploadedFile uploadedfileChild2 = builder.given_a_uploaded_file();
        uploadedfileChild2.setParent(uploadedFile);
        uploadedfileChild2.setSize(uploadedFile.getSize() + 200);
        uploadedfileChild2.setOriginalFilename(uploadedFile.getOriginalFilename() + "s");
        uploadedfileChild2.setStatus(9);
        UploadedFile uploadedFile2 = builder.given_a_uploaded_file();
        uploadedFile2.setSize(100000L);

        List<UploadedFile> uploadedFiles = null;

        MvcResult mvcResult;
        List<Long> ids;
        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "created")
                        .param("order", "asc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        Long first = ids.get(0);
        Long last = ids.get(ids.size()-1);

        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "created")
                        .param("order", "desc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        assertThat(ids.get(0)).isEqualTo(last);
        assertThat(ids.get(ids.size()-1)).isEqualTo(first);


        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "originalFilename")
                        .param("order", "desc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        first = ids.get(0);
        last = ids.get(ids.size()-1);

        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "originalFilename")
                        .param("order", "asc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        assertThat(ids.get(0)).isEqualTo(last);
        assertThat(ids.get(ids.size()-1)).isEqualTo(first);

        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "size")
                        .param("order", "asc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        first = ids.get(0);
        last = ids.get(ids.size()-1);

        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "size")
                        .param("order", "desc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        assertThat(ids.get(0)).isEqualTo(last);
        assertThat(ids.get(ids.size()-1)).isEqualTo(first);


        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "contentType")
                        .param("order", "asc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        first = ids.get(0);
        last = ids.get(ids.size()-1);

        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "contentType")
                        .param("order", "desc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        assertThat(ids.get(0)).isEqualTo(last);
        assertThat(ids.get(ids.size()-1)).isEqualTo(first);


        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "globalSize")
                        .param("order", "asc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        uploadedFiles = ids.stream().map(x -> uploadedFileRepository.getById(x)).collect(Collectors.toList());
        assertThat(uploadedFiles.get(0).getSize()).isLessThan(uploadedFiles.get(uploadedFiles.size()-1).getSize());

        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "globalSize")
                        .param("order", "desc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        uploadedFiles = ids.stream().map(x -> uploadedFileRepository.getById(x)).collect(Collectors.toList());
        assertThat(uploadedFiles.get(0).getSize()).isGreaterThan(uploadedFiles.get(uploadedFiles.size()-1).getSize());


        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "status")
                        .param("order", "asc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        first = ids.get(0);


        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "status")
                        .param("order", "desc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        assertThat(ids.get(0)).isNotEqualTo(last);

        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "parentFilename")
                        .param("order", "asc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        first = ids.get(0);


        mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile.json")
                        .param("onlyRootsWithDetails", "true")
                        .param("sort", "parentFilename")
                        .param("order", "desc"))
                .andExpect(status().isOk()).andReturn();
        ids = retrieveIds(mvcResult);
        assertThat(ids.get(0)).isNotEqualTo(first);

    }


    @Test
    @Transactional
    public void get_an_uploaded_file() throws Exception {
        UploadedFile image = builder.given_a_uploaded_file();
        

        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile/{id}.json", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.UploadedFile"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.projects").hasJsonPath())
                .andExpect(jsonPath("$.storage").hasJsonPath())
                .andExpect(jsonPath("$.path").hasJsonPath())
                .andExpect(jsonPath("$.filename").hasJsonPath())
                .andExpect(jsonPath("$.size").hasJsonPath())
                .andExpect(jsonPath("$.user").hasJsonPath())
                .andExpect(jsonPath("$.contentType").hasJsonPath())
                .andExpect(jsonPath("$.originalFilename").hasJsonPath())
                .andExpect(jsonPath("$.status").hasJsonPath());
    }

    @Test
    @Transactional
    public void get_an_uploaded_file_not_exist() throws Exception {
        restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile/{id}.json", 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.message").exists());
    }


    @Test
    @Transactional
    public void add_valid_uploaded_file() throws Exception {
        UploadedFile uploadedFile = builder.given_a_not_persisted_uploaded_file();
        restUploadedFileControllerMockMvc.perform(post("/api/uploadedfile.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadedFile.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.uploadedfileID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.uploadedfile.id").exists());

    }

    @Test
    @Transactional
    public void edit_valid_uploaded_file() throws Exception {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        JsonObject jsonObject = uploadedFile.toJsonObject();
        jsonObject.put("filename", "new");
        restUploadedFileControllerMockMvc.perform(put("/api/uploadedfile/{id}.json", uploadedFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.uploadedfileID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditUploadedFileCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.uploadedfile.id").exists())
                .andExpect(jsonPath("$.uploadedfile.filename").value("new"));


    }


    @Test
    @Transactional
    public void delete_uploaded_file() throws Exception {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        restUploadedFileControllerMockMvc.perform(delete("/api/uploadedfile/{id}.json", uploadedFile.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.uploadedfileID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteUploadedFileCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.uploadedfile.id").exists());


    }

    @Test
    public void download_uploaded_file() throws Exception {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        uploadedFile.setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        uploadedFile.setOriginalFilename("CMU-2.mrxs");
        uploadedFile.setContentType("MRXS");


        byte[] mockResponse = UUID.randomUUID().toString().getBytes();
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(IMS_API_BASE_PATH + "/file/" + URLEncoder.encode(uploadedFile.getPath(), StandardCharsets.UTF_8).replace("%2F", "/")+"/export?filename=" + URLEncoder.encode(uploadedFile.getOriginalFilename(), StandardCharsets.UTF_8)))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restUploadedFileControllerMockMvc.perform(get("/api/uploadedfile/{id}/download", uploadedFile.getId()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }
}
