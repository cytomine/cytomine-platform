package be.cytomine.controller.meta;

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.project.Project;
import be.cytomine.repository.meta.AttachedFileRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class AttachedFileResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restTermControllerMockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AttachedFileRepository attachedFileRepository;

    @Test
    @Transactional
    public void list_all_attached_file() throws Exception {
        AttachedFile attachedFile = builder.given_a_attached_file(builder.given_a_project());
        restTermControllerMockMvc.perform(get("/api/attachedfile.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.domainIdent=='" + attachedFile.getDomainIdent() + "')]").exists());
    }

    @Test
    @Transactional
    public void get_an_attached_file() throws Exception {
        AttachedFile attachedFile = builder.given_a_attached_file(builder.given_a_project());
        restTermControllerMockMvc.perform(get("/api/attachedfile/{id}.json", attachedFile.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(attachedFile.getId().intValue()));
    }

    @Test
    @Transactional
    public void get_an_attached_file_does_not_exists() throws Exception {
        restTermControllerMockMvc.perform(get("/api/attachedfile/{id}.json", 0L))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void list_attached_files_by_domain() throws Exception {
        AttachedFile attachedFile = builder.given_a_attached_file(builder.given_a_project());
        restTermControllerMockMvc.perform(get("/api/domain/{domainClassName}/{domainIdent}/attachedfile.json", attachedFile.getDomainClassName(), attachedFile.getDomainIdent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[0].id").value(attachedFile.getId().intValue()));
    }

    @Test
    @Transactional
    public void list_attached_files_by_domain_does_not_exists() throws Exception {
        AttachedFile attachedFile = builder.given_a_attached_file(builder.given_a_project());
        restTermControllerMockMvc.perform(get("/api/domain/{domainClassName}/{domainIdent}/attachedfile.json", attachedFile.getDomainClassName(), 0))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void download_attached_file() throws Exception {

        AttachedFile attachedFile = builder.given_a_attached_file(builder.given_a_project());
        attachedFile.setFilename("test.txt");
        attachedFile.setData("hello".getBytes());

        MvcResult mvcResult = restTermControllerMockMvc.perform(get("/api/attachedfile/{id}/download", attachedFile.getId()))
                .andExpect(status().isOk()).andReturn();
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo("hello".getBytes());
    }

    @Test
    @Transactional
    public void upload_attached_file() throws Exception {

        Project project = builder.given_a_project();

        MockMultipartFile file
                = new MockMultipartFile(
                "files[]",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockMvc.perform(multipart("/api/attachedfile.json").file(file)
                        .param("domainClassName", project.getClass().getName())
                        .param("domainIdent", project.getId().toString())
                        .param("key", "test")
                )
                .andExpect(status().isOk());

        assertThat(attachedFileRepository.findAllByDomainClassNameAndDomainIdent(project.getClass().getName(), project.getId()))
                .isNotEmpty();
        AttachedFile attachedFile = attachedFileRepository.findAllByDomainClassNameAndDomainIdent(project.getClass().getName(), project.getId()).get(0);
        assertThat(attachedFile.getData()).isEqualTo("hello".getBytes());
    }


    @Test
    @Transactional
    public void delete_attached_file() throws Exception {
        Project project = builder.given_a_project();
        AttachedFile attachedFile = builder.given_a_attached_file(project);
        restTermControllerMockMvc.perform(delete("/api/attachedfile/{id}.json", attachedFile.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(attachedFileRepository.findAllByDomainClassNameAndDomainIdent(project.getClass().getName(), project.getId()))
                .isEmpty();
    }
}
