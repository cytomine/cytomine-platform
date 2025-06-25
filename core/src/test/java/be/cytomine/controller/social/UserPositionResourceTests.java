package be.cytomine.controller.social;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.LastUserPosition;
import be.cytomine.domain.social.PersistentUserPosition;
import be.cytomine.dto.image.AreaDTO;
import be.cytomine.repositorynosql.social.LastUserPositionRepository;
import be.cytomine.repositorynosql.social.PersistentUserPositionRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.social.UserPositionService;
import be.cytomine.service.social.UserPositionServiceTests;
import be.cytomine.service.social.WebSocketUserPositionHandler;
import be.cytomine.utils.JsonObject;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.*;

import static be.cytomine.service.social.UserPositionServiceTests.ANOTHER_USER_VIEW;
import static be.cytomine.service.social.UserPositionServiceTests.USER_VIEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@ExtendWith(MockitoExtension.class)
public class UserPositionResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restUserPositionControllerMockMvc;

    @Autowired
    private LastUserPositionRepository lastUserPositionRepository;

    @Autowired
    private PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    private UserPositionService userPositionService;

    @Autowired
    private CurrentUserService currentUserService;

    @BeforeEach
    public void cleanDB() {
        lastUserPositionRepository.deleteAll();
        persistentUserPositionRepository.deleteAll();
    }

    PersistentUserPosition given_a_persistent_user_position(Date creation, User user, SliceInstance sliceInstance, boolean broadcast) {
        return given_a_persistent_user_position(creation, user, sliceInstance, UserPositionServiceTests.USER_VIEW, broadcast);
    }

    PersistentUserPosition given_a_persistent_user_position(Date creation, User user, SliceInstance sliceInstance, AreaDTO areaDTO, boolean broadcast) {
        PersistentUserPosition connection =
                userPositionService.add(
                        creation,
                        user,
                        sliceInstance,
                        sliceInstance.getImage(),
                        areaDTO,
                        1,
                        5.0,
                        broadcast
                );
        return connection;
    }

    @Test
    @Transactional
    public void list_last_user_on_image() throws Exception {
        User user = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        given_a_persistent_user_position(new Date(), user, sliceInstance, false);

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/online.json", imageInstance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.users[0]").value(user.getId()));
    }

    @Test
    @Transactional
    public void list_last_broadcast_user_on_image() throws Exception {
        User user = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        given_a_persistent_user_position(new Date(), user, sliceInstance, true);

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/online.json", imageInstance.getId())
                        .param("broadcast", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.users[0]").value(user.getId()));
    }


    @Test
    @Transactional
    public void list_empty_last_broadcast_user_on_image() throws Exception {
        User user = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        given_a_persistent_user_position(new Date(), user, sliceInstance, false);

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/online.json", imageInstance.getId())
                        .param("broadcast", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(equalTo(0))));
    }

    @Test
    @Transactional
    public void list_user_position() throws Exception {
        User user = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        given_a_persistent_user_position(new Date(), user, sliceInstance, false);

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/positions.json", imageInstance.getId())
                        .param("showDetails", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].user").value(user.getId()));

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/positions.json", imageInstance.getId())
                        .param("showDetails", "true")
                        .param("user", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].user").value(user.getId()));

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/positions.json", imageInstance.getId())
                        .param("showDetails", "true")
                        .param("user", builder.given_a_user().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));
    }


    @Test
    @Transactional
    public void list_after_than() throws Exception {
        User user = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        given_a_persistent_user_position(DateUtils.addDays(new Date(), -5), user, sliceInstance, false);

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/positions.json", imageInstance.getId())
                        .param("afterThan", String.valueOf(DateUtils.addDays(new Date(), -10).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))));

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/positions.json", imageInstance.getId())
                        .param("afterThan", String.valueOf(DateUtils.addDays(new Date(), -3).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));


    }

    @Test
    @Transactional
    public void list_before_than() throws Exception {
        User user = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        given_a_persistent_user_position(DateUtils.addDays(new Date(), -5), user, sliceInstance, false);

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/positions.json", imageInstance.getId())
                        .param("beforeThan", String.valueOf(DateUtils.addDays(new Date(), -3).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))));

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/positions.json", imageInstance.getId())
                        .param("beforeThan", String.valueOf(DateUtils.addDays(new Date(), -10).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));
    }

    @Test
    @Transactional
    public void list_followers() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("1234");
        ConcurrentWebSocketSessionDecorator sessionDecoratorA = new ConcurrentWebSocketSessionDecorator(session, 0, 0);
        ConcurrentWebSocketSessionDecorator sessionDecoratorB = new ConcurrentWebSocketSessionDecorator(session, 0, 0);

        User userA = builder.given_a_user();
        User userB = builder.given_a_user();

        ImageInstance imageInstance = builder.given_an_image_instance();
        Long imageId = imageInstance.getId();
        Long currentUserId = currentUserService.getCurrentUser().getId();
        String currentUserAndImageId = currentUserId.toString()+"/"+imageId.toString();

        WebSocketUserPositionHandler.sessionsBroadcast.put(currentUserAndImageId, sessionDecoratorA);
        WebSocketUserPositionHandler.sessionsTracked.put(sessionDecoratorA, new ConcurrentWebSocketSessionDecorator[]{sessionDecoratorB});
        WebSocketUserPositionHandler.sessions.put(userA.getId().toString(), new ConcurrentWebSocketSessionDecorator[]{sessionDecoratorA});
        UserPositionService.broadcasters.put(currentUserAndImageId, new ArrayList<>(Collections.singleton(userB)));

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/followers/{user}.json", imageId, currentUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2)))).andReturn();
    }

    @Test
    @Transactional
    public void list_followers_for_forbidden_user() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        Long imageId = imageInstance.getId();
        User user = builder.given_a_user();
        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/followers/{user}.json", imageId, user.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    public void summarize() throws Exception {
        User user = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        given_a_persistent_user_position(DateUtils.addMinutes(new Date(), -5), user, sliceInstance, USER_VIEW, false);
        given_a_persistent_user_position(DateUtils.addMinutes(new Date(), -4), user, sliceInstance, USER_VIEW, false);
        given_a_persistent_user_position(DateUtils.addMinutes(new Date(), -3), user, sliceInstance, USER_VIEW, false);
        given_a_persistent_user_position(DateUtils.addMinutes(new Date(), -2), user, sliceInstance, ANOTHER_USER_VIEW, false);


        MvcResult mvcResult = restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/positions.json", imageInstance.getId())
                        .param("user", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2)))).andReturn();
        List<Map<String, Object>> response = (List<Map<String, Object>>)(JsonObject.toMap(mvcResult.getResponse().getContentAsString()).get("collection"));

        Optional<Map<String, Object>> first = response.stream().filter(x -> ((List<List<Double>>) x.get("location")).get(0).contains(USER_VIEW.toList().get(0).get(0))).findFirst();
        assertThat(first).isPresent();
        assertThat(first.get().get("frequency")).isEqualTo(3);
        Optional<Map<String, Object>> second = response.stream().filter(x -> ((List<List<Double>>) x.get("location")).get(0).contains(ANOTHER_USER_VIEW.toList().get(0).get(0))).findFirst();
        assertThat(second).isPresent();
        assertThat(second.get().get("frequency")).isEqualTo(1);

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/positions.json", imageInstance.getId())
                        .param("user", builder.given_a_user().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));

    }


    @Test
    @Transactional
    public void add_position() throws Exception {
        User user = builder.given_superadmin();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        //{"image":6836067,"zoom":1,"rotation":0,"bottomLeftX":-2344,"bottomLeftY":1032,
        // "bottomRightX":6784,"bottomRightY":1032,"topLeftX":-2344,"topLeftY":2336,"topRightX":6784,"topRightY":2336,"broadcast":false}

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/online.json", imageInstance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(equalTo(0))));

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("image", imageInstance.getId());
        jsonObject.put("zoom", 1);
        jsonObject.put("rotation", 0);
        jsonObject.put("bottomLeftX", -2344);
        jsonObject.put("bottomLeftY", 1032);
        jsonObject.put("bottomRightX", 6784);
        jsonObject.put("bottomRightY", 1032);
        jsonObject.put("topLeftX", -2344);
        jsonObject.put("topLeftY", 2336);
        jsonObject.put("topRightX", 6784);
        jsonObject.put("topRightY", 2336);
        jsonObject.put("broadcast", false);

        restUserPositionControllerMockMvc.perform(post("/api/imageinstance/{image}/position.json", imageInstance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk());

        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/online.json", imageInstance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.users[0]").value(user.getId()));

        List<PersistentUserPosition> persisted = persistentUserPositionRepository.findAll(Sort.by(Sort.Direction.DESC, "created"));
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getLocation().get(0).get(0)).isEqualTo(-2344);
        assertThat(persisted.get(0).getLocation().get(0).get(1)).isEqualTo(2336);
        assertThat(persisted.get(0).getLocation().get(1).get(0)).isEqualTo(6784);
        assertThat(persisted.get(0).getLocation().get(1).get(1)).isEqualTo(2336);
        assertThat(persisted.get(0).getLocation().get(2).get(0)).isEqualTo(6784);
        assertThat(persisted.get(0).getLocation().get(2).get(1)).isEqualTo(1032);
        assertThat(persisted.get(0).getLocation().get(3).get(0)).isEqualTo(-2344);
        assertThat(persisted.get(0).getLocation().get(3).get(1)).isEqualTo(1032);

        List<LastUserPosition> latest = lastUserPositionRepository.findAll(Sort.by(Sort.Direction.DESC, "created"));
        assertThat(persisted).hasSize(1);
    }



    @Test
    @Transactional
    public void add_position_with_slice_instance() throws Exception {
        User user = builder.given_superadmin();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        //{"image":6836067,"zoom":1,"rotation":0,"bottomLeftX":-2344,"bottomLeftY":1032,
        // "bottomRightX":6784,"bottomRightY":1032,"topLeftX":-2344,"topLeftY":2336,"topRightX":6784,"topRightY":2336,"broadcast":false}

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("zoom", 1);
        jsonObject.put("rotation", 0);
        jsonObject.put("bottomLeftX", -2344);
        jsonObject.put("bottomLeftY", 1032);
        jsonObject.put("bottomRightX", 6784);
        jsonObject.put("bottomRightY", 1032);
        jsonObject.put("topLeftX", -2344);
        jsonObject.put("topLeftY", 2336);
        jsonObject.put("topRightX", 6784);
        jsonObject.put("topRightY", 2336);
        jsonObject.put("broadcast", false);

        restUserPositionControllerMockMvc.perform(post("/api/sliceinstance/{image}/position.json", sliceInstance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void get_last_user_position_of_not_followed_user() throws Exception {
        User user = builder.given_superadmin();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();
        String userAndImageId = user.getId().toString() + "/" + imageInstance.getId().toString();

        assertThat(UserPositionService.broadcasters.get(userAndImageId)).isNull();
        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/position/{user}.json", imageInstance.getId(), user.getId()))
                .andExpect(status().isOk());
        assertThat(UserPositionService.broadcasters.get(userAndImageId).size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void get_last_user_position_of_user_already_followed_by_current_user() throws Exception {
        User admin = builder.given_superadmin();
        User user = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();
        String userAndImageId = user.getId().toString() + "/" + imageInstance.getId().toString();
        String followerAndImageId = admin.getId().toString() + "/" + imageInstance.getId().toString();

        UserPositionService.broadcasters.put(userAndImageId, new ArrayList<>(Collections.singleton(admin)));
        UserPositionService.followers.put(followerAndImageId, false);
        assertThat(UserPositionService.broadcasters.get(userAndImageId).size()).isEqualTo(1);
        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/position/{user}.json", imageInstance.getId(), user.getId()))
                .andExpect(status().isOk());
        assertThat(UserPositionService.broadcasters.get(userAndImageId).size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void get_last_user_position_of_user_already_followed_but_not_by_current_user() throws Exception {
        User user = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();
        String userAndImageId = user.getId().toString() + "/" + imageInstance.getId().toString();

        UserPositionService.broadcasters.put(userAndImageId, new ArrayList<>(Collections.singleton(user)));
        assertThat(UserPositionService.broadcasters.get(userAndImageId).size()).isEqualTo(1);
        restUserPositionControllerMockMvc.perform(get("/api/imageinstance/{image}/position/{user}.json", imageInstance.getId(), user.getId()))
                .andExpect(status().isOk());
        assertThat(UserPositionService.broadcasters.get(userAndImageId).size()).isEqualTo(2);
    }
}
