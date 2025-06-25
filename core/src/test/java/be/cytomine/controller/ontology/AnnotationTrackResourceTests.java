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
import be.cytomine.domain.ontology.AnnotationTrack;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class AnnotationTrackResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restAnnotationTrackControllerMockMvc;

    @Test
    @Transactional
    public void list_all_annotationTracks_by_track() throws Exception {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        restAnnotationTrackControllerMockMvc.perform(get("/api/track/{id}/annotationtrack.json", annotationTrack.getTrack().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))));
    }

    @Test
    @Transactional
    public void list_all_annotationTracks_by_track_not_exists() throws Exception {
        restAnnotationTrackControllerMockMvc.perform(get("/api/track/{id}/annotationtrack.json", 0))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void list_all_annotationTracks_by_annotation() throws Exception {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        restAnnotationTrackControllerMockMvc.perform(get("/api/annotation/{id}/annotationtrack.json", annotationTrack.getAnnotationIdent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))));
    }

    @Test
    @Transactional
    public void get_a_annotationTrack() throws Exception {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        restAnnotationTrackControllerMockMvc.perform(get("/api/annotationtrack/{annotation}/{track}.json", annotationTrack.getAnnotationIdent(), annotationTrack.getTrack().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(annotationTrack.getId().intValue()));
    }

    @Test
    @Transactional
    public void get_a_annotationTrack_annotation_not_exists() throws Exception {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        restAnnotationTrackControllerMockMvc.perform(get("/api/annotationtrack/{annotation}/{track}.json", 0, annotationTrack.getTrack().getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void get_a_annotationTrack_track_not_exists() throws Exception {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        restAnnotationTrackControllerMockMvc.perform(get("/api/annotationtrack/{annotation}/{track}.json", annotationTrack.getAnnotationIdent(), 0))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void get_a_annotationTrack_not_exists() throws Exception {
        AnnotationTrack annotationTrack = builder.given_a_not_persisted_annotation_track();
        restAnnotationTrackControllerMockMvc.perform(get("/api/annotationtrack/{annotation}/{track}.json", annotationTrack.getAnnotationIdent(), annotationTrack.getTrack().getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void add_valid_annotationTrack() throws Exception {
        AnnotationTrack annotationTrack = builder.given_a_not_persisted_annotation_track();
        restAnnotationTrackControllerMockMvc.perform(post("/api/annotationtrack.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationTrack.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.annotationtrackID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddAnnotationTrackCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotationtrack.id").exists());
    }

    @Test
    @Transactional
    public void delete_annotationTrack() throws Exception {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        restAnnotationTrackControllerMockMvc.perform(delete("/api/annotationtrack/{annotation}/{track}.json", annotationTrack.getAnnotationIdent(), annotationTrack.getTrack().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationTrack.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.annotationtrackID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteAnnotationTrackCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotationtrack.id").exists());
    }

    @Test
    @Transactional
    public void delete_annotationTrack_not_exist_fails() throws Exception {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        restAnnotationTrackControllerMockMvc.perform(delete("/api/annotationtrack/{annotation}/{track}.json", 0, 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationTrack.toJSON()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }
}
