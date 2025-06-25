package be.cytomine.service.social;

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
import be.cytomine.dto.image.Point;
import be.cytomine.repositorynosql.social.LastUserPositionRepository;
import be.cytomine.repositorynosql.social.PersistentUserPositionRepository;
import be.cytomine.service.database.SequenceService;

import com.mongodb.client.MongoClient;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import jakarta.transaction.Transactional;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class UserPositionServiceTests {

    @Autowired
    UserPositionService userPositionService;

    @Autowired
    SequenceService sequenceService;

    @Autowired
    LastUserPositionRepository lastUserPositionRepository;

    @Autowired
    PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    MongoClient mongoClient;

    @BeforeEach
    public void cleanDB() {
        lastUserPositionRepository.deleteAll();
        persistentUserPositionRepository.deleteAll();

        WebSocketUserPositionHandler.sessionsTracked = new ConcurrentHashMap<>();
        WebSocketUserPositionHandler.sessions = new ConcurrentHashMap<>();
        UserPositionService.followers = new ConcurrentHashMap<>();
        UserPositionService.broadcasters = new ConcurrentHashMap<>();
    }

    public static final AreaDTO USER_VIEW = new AreaDTO(
            new be.cytomine.dto.image.Point(1000d, 1000d),
            new be.cytomine.dto.image.Point(4000d, 1000d),
            new be.cytomine.dto.image.Point(4000d, 4000d),
            new be.cytomine.dto.image.Point(1000d, 4000d)
    );

    public static final AreaDTO ANOTHER_USER_VIEW = new AreaDTO(
            new be.cytomine.dto.image.Point(3000d, 3000d),
            new be.cytomine.dto.image.Point(9000d, 3000d),
            new be.cytomine.dto.image.Point(9000d, 9000d),
            new be.cytomine.dto.image.Point(3000d, 9000d)
    );

    PersistentUserPosition given_a_persistent_user_position(Date creation, User user, SliceInstance sliceInstance) {
        return given_a_persistent_user_position(creation, user, sliceInstance, USER_VIEW);
    }

    PersistentUserPosition given_a_persistent_user_position(Date creation, User user, SliceInstance sliceInstance, AreaDTO areaDTO) {
        PersistentUserPosition connection =
                userPositionService.add(
                        creation,
                        user,
                        sliceInstance,
                        sliceInstance.getImage(),
                        areaDTO,
                        1,
                        5.0,
                        false
                );
        return connection;
    }

    @Test
    void user_position_create_persistent_and_expired_position() {
        PersistentUserPosition persistentUserPosition = given_a_persistent_user_position(new Date(), builder.given_superadmin(), builder.given_a_slice_instance());
        assertThat(lastUserPositionRepository.count()).isEqualTo(1);
        assertThat(persistentUserPositionRepository.count()).isEqualTo(1);
    }


    @Test
    void retrieve_last_position_for_user() {
        User mainUser = builder.given_superadmin();
        User anotherUser = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();

        PersistentUserPosition persistentUserPosition = given_a_persistent_user_position(new Date(), mainUser, sliceInstance);
        PersistentUserPosition persistentUserPositionForAnotherUserSameSlice
                = given_a_persistent_user_position(new Date(), anotherUser, sliceInstance);
        PersistentUserPosition persistentUserPositionForAnotherUserAnotherSlice
                = given_a_persistent_user_position(new Date(), anotherUser, builder.given_a_slice_instance());

        Optional<LastUserPosition> lastUserPosition
                = userPositionService.lastPositionByUser(sliceInstance.getImage(), sliceInstance, mainUser, false);
        assertThat(lastUserPosition).isPresent();
        assertThat(lastUserPosition.get().getUser()).isEqualTo(mainUser.getId());
        assertThat(lastUserPosition.get().getLocation()).isEqualTo(USER_VIEW.toMongodbLocation().getCoordinates());

        lastUserPosition
                = userPositionService.lastPositionByUser(sliceInstance.getImage(), sliceInstance, anotherUser, false);
        assertThat(lastUserPosition).isPresent();
        assertThat(lastUserPosition.get().getUser()).isEqualTo(anotherUser.getId());
        assertThat(lastUserPosition.get().getSlice()).isEqualTo(sliceInstance.getId());
    }

    @Test
    public void list_users_online_on_image() {
        User mainUser = builder.given_superadmin();
        User anotherUser = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();

        PersistentUserPosition persistentUserPosition = given_a_persistent_user_position(new Date(), mainUser, sliceInstance);

        assertThat(userPositionService.listOnlineUsersByImage(sliceInstance.getImage(), sliceInstance, false))
                .containsExactlyInAnyOrder(mainUser.getId());

        // add a new user
        PersistentUserPosition persistentUserPositionForAnotherUserSameSlice
                = given_a_persistent_user_position(new Date(), anotherUser, sliceInstance);

        assertThat(userPositionService.listOnlineUsersByImage(sliceInstance.getImage(), sliceInstance, false))
                .containsExactlyInAnyOrder(mainUser.getId(), anotherUser.getId());
    }


    @Test
    public void list_users_position() {
        User mainUser = builder.given_superadmin();
        User anotherUser = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();


        Date freshPosition = DateUtils.addSeconds(new Date(), -1);
        Date oldPosition = DateUtils.addMonths(freshPosition, -1);

        Date beforeFirstPosition = DateUtils.addSeconds(oldPosition, -1);
        Date afterLastPosition = new Date();

        given_a_persistent_user_position(oldPosition, mainUser, sliceInstance);
        given_a_persistent_user_position(oldPosition, anotherUser, sliceInstance);
        given_a_persistent_user_position(freshPosition, mainUser, sliceInstance);

        List<PersistentUserPosition> results;

        // all filters
        results = userPositionService.list(
                sliceInstance.getImage(),
                mainUser,
                sliceInstance,
                beforeFirstPosition.getTime(),
                afterLastPosition.getTime(),
                100,
                0
        );
        assertThat(results).hasSize(2);

        // no user filters
        results = userPositionService.list(
                sliceInstance.getImage(),
                null,
                sliceInstance,
                beforeFirstPosition.getTime(),
                afterLastPosition.getTime(),
                100,
                0
        );
        assertThat(results).hasSize(3);

        // no date before filters
        results = userPositionService.list(
                sliceInstance.getImage(),
                null,
                sliceInstance,
                null,
                afterLastPosition.getTime(),
                100,
                0
        );
        assertThat(results).hasSize(3);

        // no date filters
        results = userPositionService.list(
                sliceInstance.getImage(),
                null,
                sliceInstance,
                null,
                null,
                100,
                0
        );
        assertThat(results).hasSize(3);

        // date restriction but in range
        results = userPositionService.list(
                sliceInstance.getImage(),
                null,
                sliceInstance,
                beforeFirstPosition.getTime(),
                afterLastPosition.getTime(),
                100,
                0
        );
        assertThat(results).hasSize(3);

        // date restriction but only old
        results = userPositionService.list(
                sliceInstance.getImage(),
                null,
                sliceInstance,
                DateUtils.addDays(oldPosition, -1).getTime(),
                DateUtils.addDays(oldPosition, 1).getTime(),
                100,
                0
        );
        assertThat(results).hasSize(2); // only old position
    }

    @Test
    public void summerize() {
        User mainUser = builder.given_superadmin();
        User anotherUser = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();


        Date freshPosition = DateUtils.addSeconds(new Date(), -1);
        Date oldPosition = DateUtils.addMonths(freshPosition, -1);

        Date beforeFirstPosition = DateUtils.addSeconds(oldPosition, -1);
        Date afterLastPosition = new Date();

        given_a_persistent_user_position(oldPosition, mainUser, sliceInstance);
        given_a_persistent_user_position(oldPosition, anotherUser, sliceInstance);
        given_a_persistent_user_position(freshPosition, mainUser, sliceInstance);

        List<Map<String, Object>> summarize = userPositionService.summarize(
                sliceInstance.getImage(),
                mainUser,
                sliceInstance,
                beforeFirstPosition.getTime(),
                afterLastPosition.getTime()
        );
        assertThat(summarize).isNotEmpty();


    }

    @Test
    public void summerize_location() {
        User mainUser = builder.given_superadmin();
        User anotherUser = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();


        Date freshPosition = DateUtils.addSeconds(new Date(), -1);
        Date oldPosition = DateUtils.addMonths(freshPosition, -1);

        Date beforeFirstPosition = DateUtils.addSeconds(oldPosition, -1);
        Date afterLastPosition = new Date();

        given_a_persistent_user_position(DateUtils.addSeconds(oldPosition, 1), mainUser, sliceInstance, USER_VIEW);
        given_a_persistent_user_position(DateUtils.addSeconds(oldPosition, 2), mainUser, sliceInstance, USER_VIEW);
        given_a_persistent_user_position(DateUtils.addSeconds(oldPosition, 3), mainUser, sliceInstance, USER_VIEW);
        given_a_persistent_user_position(DateUtils.addSeconds(oldPosition, 4), mainUser, sliceInstance, ANOTHER_USER_VIEW);

        List<Map<String, Object>> summarize = userPositionService.summarize(
                sliceInstance.getImage(),
                mainUser,
                sliceInstance,
                beforeFirstPosition.getTime(),
                afterLastPosition.getTime()
        );
        assertThat(summarize).hasSize(2);
        Optional<Map<String, Object>> first = summarize.stream().filter(x -> ((List<List<Double>>) x.get("location")).get(0).contains(USER_VIEW.toList().get(0).get(0))).findFirst();
        assertThat(first).isPresent();
        assertThat(first.get().get("frequency")).isEqualTo(3);
        Optional<Map<String, Object>> second = summarize.stream().filter(x -> ((List<List<Double>>) x.get("location")).get(0).contains(ANOTHER_USER_VIEW.toList().get(0).get(0))).findFirst();
        assertThat(second).isPresent();
        assertThat(second.get().get("frequency")).isEqualTo(1);


    }


    @Test
    public void summerize_after_than() {
        User mainUser = builder.given_superadmin();
        User anotherUser = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();

        Date freshPosition = DateUtils.addSeconds(new Date(), -1);
        Date oldPosition = DateUtils.addMonths(freshPosition, -1);

        Date beforeFirstPosition = DateUtils.addSeconds(oldPosition, -1);
        Date afterLastPosition = new Date();

        given_a_persistent_user_position(DateUtils.addDays(oldPosition, 1), mainUser, sliceInstance, USER_VIEW);
        given_a_persistent_user_position(DateUtils.addDays(oldPosition, 3), mainUser, sliceInstance, USER_VIEW);
        given_a_persistent_user_position(DateUtils.addDays(oldPosition, 5), mainUser, sliceInstance, USER_VIEW);
        given_a_persistent_user_position(DateUtils.addDays(oldPosition, 7), mainUser, sliceInstance, USER_VIEW);

        List<Map<String, Object>> summarize = userPositionService.summarize(
                sliceInstance.getImage(),
                mainUser,
                sliceInstance,
                beforeFirstPosition.getTime(),
                afterLastPosition.getTime()
        );
        assertThat(summarize.get(0).get("frequency")).isEqualTo(4);

        summarize = userPositionService.summarize(
                sliceInstance.getImage(),
                mainUser,
                sliceInstance,
                DateUtils.addDays(oldPosition, 2).getTime(),
                afterLastPosition.getTime()
        );
        assertThat(summarize.get(0).get("frequency")).isEqualTo(3);

        summarize = userPositionService.summarize(
                sliceInstance.getImage(),
                mainUser,
                sliceInstance,
                DateUtils.addDays(oldPosition, 4).getTime(),
                afterLastPosition.getTime()
        );
        assertThat(summarize.get(0).get("frequency")).isEqualTo(2);
    }

    @Test
    void adding_a_position_with_new_location() {
        User user = builder.given_a_user();
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = builder.given_an_image_instance();
        AreaDTO area = new AreaDTO(new Point((double)0, (double)0), new Point((double)0, (double)0), new Point((double)0, (double)0), new Point((double)0, (double)0));
        Date date = new Date();

        userPositionService.add(date, user, sliceInstance, imageInstance, area, 0, (double)0, true);
    }

    @Test
    public void list_followers() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("1234");

        ConcurrentWebSocketSessionDecorator sessionDecorator = new ConcurrentWebSocketSessionDecorator(session, 0, 0);

        User user = builder.given_a_user();

        WebSocketUserPositionHandler.sessionsBroadcast.put(user.getId().toString()+"/514", sessionDecorator);
        WebSocketUserPositionHandler.sessionsTracked.put(sessionDecorator, new ConcurrentWebSocketSessionDecorator[]{new ConcurrentWebSocketSessionDecorator(session, 0, 0)});
        WebSocketUserPositionHandler.sessions.put(user.getId().toString(), new ConcurrentWebSocketSessionDecorator[]{new ConcurrentWebSocketSessionDecorator(session, 0, 0)});

        List<String> users = userPositionService.listFollowers(user.getId(), 514L);

        assertThat(users.size()).isEqualTo(1);
        assertThat(users).contains(user.getId().toString());
    }

    @Test
    public void list_distinct_followers() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("1234");

        ConcurrentWebSocketSessionDecorator sessionDecorator = new ConcurrentWebSocketSessionDecorator(session, 0, 0);

        User user = builder.given_a_user();

        WebSocketUserPositionHandler.sessionsBroadcast.put(user.getId().toString()+"/514", sessionDecorator);
        WebSocketUserPositionHandler.sessionsTracked.put(sessionDecorator, new ConcurrentWebSocketSessionDecorator[]{new ConcurrentWebSocketSessionDecorator(session, 0, 0)});
        WebSocketUserPositionHandler.sessions.put(user.getId().toString(), new ConcurrentWebSocketSessionDecorator[]{new ConcurrentWebSocketSessionDecorator(session, 0, 0)});
        UserPositionService.broadcasters.put("89/514", List.of(user));

        List<String> users = userPositionService.listFollowers(89L, 514L);

        assertThat(users.size()).isEqualTo(1);
        assertThat(users).contains(user.getId().toString());
    }

    @Test
    public void list_followers_for_not_followed_user() {
        User user = builder.given_a_user();
        ImageInstance imageInstance = builder.given_an_image_instance();
        List<String> users = userPositionService.listFollowers(user.getId(), imageInstance.getId());
        assertThat(users.size()).isEqualTo(0);
    }

    @Test
    public void adding_users_as_followers(){
        User broadcaster = builder.given_a_user();
        User follower = builder.given_a_user();
        ImageInstance imageInstance = builder.given_an_image_instance();
        String followerAndImageId = follower.getId().toString() + "/" + imageInstance.getId().toString();

        Assertions.assertThat(UserPositionService.followers.get(followerAndImageId)).isNull();
        userPositionService.addAsFollower(broadcaster, follower, imageInstance);
        Assertions.assertThat(UserPositionService.followers.get(followerAndImageId)).isNotNull();
    }

    @Test
    public void updating_users_followers(){
        User broadcaster = builder.given_a_user();
        User follower = builder.given_a_user();
        ImageInstance imageInstance = builder.given_an_image_instance();

        String followerAndImageId = follower.getId().toString() + "/" + imageInstance.getId().toString();
        UserPositionService.followers.put(followerAndImageId, false);

        Assertions.assertThat(UserPositionService.followers.get(followerAndImageId)).isEqualTo(false);
        userPositionService.addAsFollower(broadcaster, follower, imageInstance);
        Assertions.assertThat(UserPositionService.followers.get(followerAndImageId)).isEqualTo(true);
    }

    @Test
    public void remove_users_followers_that_did_not_fetch_position(){
        User broadcaster = builder.given_a_user();
        User follower = builder.given_a_user();
        ImageInstance imageInstance = builder.given_an_image_instance();

        String followerAndImageId = follower.getId().toString() + "/" + imageInstance.getId().toString();
        String trackerAndImageId = broadcaster.getId().toString() + "/" + imageInstance.getId().toString();
        UserPositionService.followers.put(followerAndImageId, false);
        UserPositionService.broadcasters.put(trackerAndImageId, List.of(follower));

        Assertions.assertThat(UserPositionService.followers.get(followerAndImageId)).isEqualTo(false);
        Assertions.assertThat(UserPositionService.broadcasters.get(trackerAndImageId).size()).isEqualTo(1);

        userPositionService.removeFollower(UserPositionService.followers.entrySet().iterator().next());

        Assertions.assertThat(UserPositionService.broadcasters.get(trackerAndImageId).size()).isEqualTo(0);
    }

}