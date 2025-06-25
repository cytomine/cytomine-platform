package be.cytomine.service.ontology;

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
import be.cytomine.domain.ontology.Track;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.TrackRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.utils.CommandResponse;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class TrackServiceTests {

    @Autowired
    TrackService trackService;

    @Autowired
    TrackRepository trackRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Test
    void get_track_with_success() {
        Track track = builder.given_a_track();
        assertThat(track).isEqualTo(trackService.get(track.getId()));
    }

    @Test
    void get_unexisting_track_return_null() {
        assertThat(trackService.get(0L)).isNull();
    }

    @Test
    void find_track_with_success() {
        Track track = builder.given_a_track();
        assertThat(trackService.find(track.getId()).isPresent());
        assertThat(track).isEqualTo(trackService.find(track.getId()).get());
    }

    @Test
    void find_unexisting_track_return_empty() {
        assertThat(trackService.find(0L)).isEmpty();
    }

    @Test
    void list_all_track_by_image() {
        Track track = builder.given_a_track();
        assertThat(track).isIn(trackService.list(track.getImage()));
        assertThat(trackService.list(builder.given_an_image_instance()).size()).isEqualTo(0);
    }

    @Test
    void list_all_track_by_project() {
        Track track = builder.given_a_track();
        assertThat(track).isIn(trackService.list(track.getProject()));
        assertThat(trackService.list(builder.given_a_project()).size()).isEqualTo(0);
    }


    @Test
    void count_by_project() {
        Track track = builder.given_a_track();
        assertThat(trackService.countByProject(track.getProject(), null, null))
                .isEqualTo(1);
        assertThat(trackService.countByProject(builder.given_a_project(), null, null))
                .isEqualTo(0);
    }

    @Test
    void count_by_project_with_date() {
        Track track = builder.given_a_track();

        assertThat(trackService.countByProject(
                track.getProject(),
                        DateUtils.addDays(track.getCreated(),-30),
                        DateUtils.addDays(track.getCreated(),30)))
                .isEqualTo(1);

        assertThat(trackService.countByProject(
                track.getProject(),
                        DateUtils.addDays(track.getCreated(),-30),
                        DateUtils.addDays(track.getCreated(),-15)))
                .isEqualTo(0);

        assertThat(trackService.countByProject(
                track.getProject(),
                        DateUtils.addDays(track.getCreated(),15),
                        DateUtils.addDays(track.getCreated(),30)))
                .isEqualTo(0);
    }


    @Test
    void add_valid_track_with_success() {
        Track track = builder.given_a_not_persisted_track();
        CommandResponse commandResponse = trackService.add(track.toJsonObject());
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(trackService.find(commandResponse.getObject().getId())).isPresent();
        Track created = trackService.find(commandResponse.getObject().getId()).get();
        assertThat(created.getName()).isEqualTo(track.getName());
    }

    @Test
    void add_track_with_null_image_fails() {
        Track track = builder.given_a_not_persisted_track();
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            trackService.add(track.toJsonObject().withChange("image", null));
        });
    }

    @Test
    void add_track_already_exists() {
        Track track = builder.given_a_track();
        Assertions.assertThrows(AlreadyExistException.class, () -> {
            trackService.add(track.toJsonObject().withChange("id", null));
        });
    }

    @Test
    void edit_valid_track_with_success() {
        Track track = builder.given_a_track();

        CommandResponse commandResponse = trackService.update(track, track.toJsonObject().withChange("name", "NEW NAME").withChange("color", "NEW COLOR"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(trackService.find(commandResponse.getObject().getId())).isPresent();
        Track edited = trackService.find(commandResponse.getObject().getId()).get();
        assertThat(edited.getName()).isEqualTo("NEW NAME");
        assertThat(edited.getColor()).isEqualTo("NEW COLOR");
    }

    @Test
    void undo_redo_track_edition_with_success() {
        Track track = builder.given_a_track();
        track.setName("OLD NAME");
        track = builder.persistAndReturn(track);

        trackService.update(track, track.toJsonObject().withChange("name", "NEW NAME"));

        assertThat(trackRepository.getById(track.getId()).getName()).isEqualTo("NEW NAME");

        commandService.undo();

        assertThat(trackRepository.getById(track.getId()).getName()).isEqualTo("OLD NAME");

        commandService.redo();

        assertThat(trackRepository.getById(track.getId()).getName()).isEqualTo("NEW NAME");

    }

    @Test
    void delete_track_with_success() {
        Track track = builder.given_a_track();

        CommandResponse commandResponse = trackService.delete(track, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(trackService.find(track.getId()).isEmpty());
    }

    @Test
    void delete_track_with_dependencies_with_success() {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        CommandResponse commandResponse = trackService.delete(annotationTrack.getTrack(), null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(trackService.find(annotationTrack.getId()).isEmpty());
    }


    @Test
    void undo_redo_track_deletion_with_success() {
        Track track = builder.given_a_track();

        trackService.delete(track, null, null, true);

        assertThat(trackService.find(track.getId()).isEmpty());

        commandService.undo();

        assertThat(trackService.find(track.getId()).isPresent());

        commandService.redo();

        assertThat(trackService.find(track.getId()).isEmpty());
    }
}
