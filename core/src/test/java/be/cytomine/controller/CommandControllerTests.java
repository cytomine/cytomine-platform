package be.cytomine.controller;

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
import be.cytomine.domain.command.Command;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.repository.command.CommandRepository;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.service.image.UploadedFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
public class CommandControllerTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private OntologyRepository ontologyRepository;

    @Autowired
    private MockMvc restCommandControllerMockMvc;

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private UploadedFileService uploadedFileService;


    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void list_delete_command() throws Exception {

        Long start = System.currentTimeMillis();

        int initialSize = (int) commandRepository.findAll().stream().filter(x -> x instanceof DeleteCommand).count();
        int initialSizeUploadedFileDeleteCommand = (int) commandRepository.findAll().stream().filter(x -> x instanceof DeleteCommand && x.getServiceName().equals("UploadedFileService")).count();

        restCommandControllerMockMvc.perform(get("/api/deletecommand.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(initialSize))));

        restCommandControllerMockMvc.perform(get("/api/deletecommand.json")
                        .param("domain", "uploadedFile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(initialSizeUploadedFileDeleteCommand))));

        UploadedFile uploadedFile = builder.given_a_uploaded_file();

        Command c = new DeleteCommand(builder.given_superadmin(), null);
        uploadedFileService.executeCommand(c, uploadedFile, null);

        restCommandControllerMockMvc.perform(get("/api/deletecommand.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(initialSize+1))));

        restCommandControllerMockMvc.perform(get("/api/deletecommand.json")
                        .param("domain", "uploadedFile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(initialSizeUploadedFileDeleteCommand+1))));

        restCommandControllerMockMvc.perform(get("/api/deletecommand.json")
                        .param("domain", "uploadedFile").param("after", String.valueOf(new Date().getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));

        restCommandControllerMockMvc.perform(get("/api/deletecommand.json")
                        .param("domain", "uploadedFile").param("after", String.valueOf(start)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))));

    }

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void undo_redo() throws Exception {

        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        ontology.setName("undo_redo");
        restCommandControllerMockMvc.perform(post("/api/ontology.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ontology.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ontology.name").value(ontology.getName()));

        ontology = ontologyRepository.findAll().stream().filter(x -> x.getName().equals("undo_redo")).findFirst().get();

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ontology.getId().intValue()));

        restCommandControllerMockMvc.perform(get("/command/undo.json")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andExpect(status().isNotFound());

        restCommandControllerMockMvc.perform(get("/command/redo.json")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ontology.getId().intValue()));
    }

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void undo_redo_with_command_id() throws Exception {

        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        ontology.setName("undo_redo");
        restCommandControllerMockMvc.perform(post("/api/ontology.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ontology.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ontology.name").value(ontology.getName()));

        ontology = ontologyRepository.findAll().stream().filter(x -> x.getName().equals("undo_redo")).findFirst().get();

        Command command = commandRepository.findAll(Sort.by(Sort.Direction.DESC, "created")).get(0);

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ontology.getId().intValue()));

        restCommandControllerMockMvc.perform(get("/command/{id}/undo.json", command.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andExpect(status().isNotFound());

        restCommandControllerMockMvc.perform(get("/command/{id}/redo.json", command.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ontology.getId().intValue()));
    }
}
