package be.cytomine.service.social;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.exceptions.ServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@ExtendWith(MockitoExtension.class)
public class WebSocketUserPositionTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    WebSocketUserPositionHandler webSocketUserPositionHandler;

    @AfterEach
    public void cleanSessions(){
        WebSocketUserPositionHandler.sessions = new ConcurrentHashMap<>();
        WebSocketUserPositionHandler.sessionsTracked = new ConcurrentHashMap<>();
        WebSocketUserPositionHandler.sessionsBroadcast = new ConcurrentHashMap<>();
    }

    @Test
    public void create_session_for_not_connected_user() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes("54", "imageId", "false"));

        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get("54/imageId")).isNull();
        assertThat(WebSocketUserPositionHandler.sessions.get("54")).isNull();
        webSocketUserPositionHandler.afterConnectionEstablished(session);
        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get("54/imageId")).isNull();
        assertThat(WebSocketUserPositionHandler.sessions.get("54")).isNotEmpty();
    }

    @Test
    public void create_broadcast_session_for_not_connected_user() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes("54", "imageId", "true"));

        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get("54/imageId")).isNull();
        assertThat(WebSocketUserPositionHandler.sessions.get("54")).isNull();
        webSocketUserPositionHandler.afterConnectionEstablished(session);
        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get("54/imageId")).isNotNull();
        assertThat(WebSocketUserPositionHandler.sessions.get("54")).isNotEmpty();
    }

    @Test
    public void add_session_for_already_connected_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectSession(sessionDecorator, "54", "imageId", "false");
        connectSession(sessionDecorator, "89", "imageId", "false");
        assertThat(WebSocketUserPositionHandler.sessions.get("54").length).isEqualTo(1);
        assertThat(WebSocketUserPositionHandler.sessions.get("89").length).isEqualTo(1);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes("54", "imageId", "false"));

        webSocketUserPositionHandler.afterConnectionEstablished(session);
        assertThat(WebSocketUserPositionHandler.sessions.get("54").length).isEqualTo(2);
        assertThat(WebSocketUserPositionHandler.sessions.get("89").length).isEqualTo(1);
    }

    @Test
    public void add_track_session_to_not_tracked_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        connectSession(sessionDecorator, userId, imageInstanceId,"true");

        // Should have created a broadcast session
        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId)).isNotNull();

        // Ask for follow the broadcast session
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "false"));
        when(session.getId()).thenReturn("1234");
        when(sessionDecorator.getId()).thenReturn("1234");
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId));

        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(1);
    }

    @Test
    public void add_track_session_to_already_tracked_user() {
        ConcurrentWebSocketSessionDecorator followerSession = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        connectSession(followerSession, userId, imageInstanceId,"true");
        initFollowingSession(userAndImageId, broadcastSession, followerSession);

        //Should have added session to sessions tracked
        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(1);

        WebSocketSession session = mock(WebSocketSession.class);
        connectSession(session, userId, imageInstanceId,"false");

        when(session.getId()).thenReturn("1234");
        when(followerSession.getId()).thenReturn("5678");

        // Ask a new follow on the broadcast session
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId));

        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(2);
    }

    @Test
    public void add_track_session_who_is_already_tracking() {
        ConcurrentWebSocketSessionDecorator followerSession = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);
        when(followerSession.getId()).thenReturn("5678");

        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        connectSession(followerSession, userId, imageInstanceId,"true");
        initFollowingSession(userAndImageId, broadcastSession, followerSession);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("1234");
        connectSession(session, userId, imageInstanceId,"false");

        // Ask a new follow on the broadcast session
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId));

        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(2);

        // Ask a follow on the broadcast session with already tracking session
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId));
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(2);
    }

    @Test
    public void add_some_track_sessions_to_already_tracked_user() {
        ConcurrentWebSocketSessionDecorator followerSession1 = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator followerSession2 = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator followerSession3 = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId1 = builder.given_a_user().getId().toString();
        String userId2 = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId1+"/"+imageInstanceId;
        initFollowingSession(userAndImageId, broadcastSession, followerSession1);

        //Should have added session to sessions tracked
        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(1);

        // Simulate that user is connected to Cytomine with 2 sessions
        connectSession(followerSession2, userId2, imageInstanceId,"false");
        connectSession(followerSession3, userId2, imageInstanceId,"false");
        when(followerSession1.getId()).thenReturn("1");
        when(followerSession2.getId()).thenReturn("2");
        when(followerSession3.getId()).thenReturn("3");

        // Ask for session 2 only to follow the broadcast session
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes(userId2, imageInstanceId, "false"));
        when(session.getId()).thenReturn("2");

        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId1));
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(2);

        // Ask for session 3 only to follow the broadcast session
        when(session.getId()).thenReturn("3");
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId1));
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(3);
    }

    @Test
    public void remove_tracking_sessions_from_tracked_sessions() throws Exception {
        ConcurrentWebSocketSessionDecorator followerSession = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        connectSession(followerSession, userId,imageInstanceId, "false");
        initFollowingSession(userAndImageId, broadcastSession, followerSession);

        // Broadcast session should be followed by follower session
        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(1);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "false"));
        when(session.getId()).thenReturn("1234");
        when(followerSession.getId()).thenReturn("1234");

        // Close the followerSession (by calling afterConnectionClosed with mock session with same session id)
        webSocketUserPositionHandler.afterConnectionClosed(session, CloseStatus.NO_STATUS_CODE);

        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(0);
    }

    @Test
    public void remove_broadcasting_sessions_from_tracked_sessions() throws Exception {
        ConcurrentWebSocketSessionDecorator followerSession = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        connectSession(followerSession, userId, imageInstanceId,"false");
        initFollowingSession(userAndImageId, broadcastSession, followerSession);

        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(1);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "true"));
        when(session.getId()).thenReturn("1234");
        when(followerSession.getId()).thenReturn("5678");
        when(broadcastSession.getId()).thenReturn("1234");

        // Close the broadcastSession (by calling afterConnectionClosed with mock session with same session id)
        webSocketUserPositionHandler.afterConnectionClosed(session, CloseStatus.NO_STATUS_CODE);

        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId)).isNull();
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession)).isNull();
    }

    @Test
    public void update_position_of_tracked_user_send_message_works() throws IOException {
        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();

        WebSocketSession session = mock(WebSocketSession.class);
        connectSession(session, userId, imageInstanceId, "true");

        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "false"));
        when(session.getId()).thenReturn("1234");
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId));

        when(session.isOpen()).thenReturn(true);
        doNothing().when(session).sendMessage(new TextMessage("position"));
        assertDoesNotThrow(() -> webSocketUserPositionHandler.sendPositionToFollowers(userId, imageInstanceId, "position"));
        verify(session, Mockito.timeout(2000).times(1)).sendMessage(new TextMessage("position"));
    }

    @Test
    public void update_position_of_not_tracked_user_do_nothing(){
        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        assertDoesNotThrow(() -> webSocketUserPositionHandler.sendPositionToFollowers(userId, imageInstanceId,"position"));
    }

    @Test
    public void remove_session_if_connection_closed() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        ConcurrentWebSocketSessionDecorator followerSession = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String userAndImageId = userId+"/imageId";
        connectSession(session, userId, "imageId", "false");
        initFollowingSession(userAndImageId, broadcastSession, followerSession);

        assertThat(WebSocketUserPositionHandler.sessions.get(userId).length).isEqualTo(1);
        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(1);

        when(session.getId()).thenReturn("1");
        when(followerSession.getId()).thenReturn("1");
        // Close the session (by calling afterConnectionClosed with mock session with same session id)
        webSocketUserPositionHandler.afterConnectionClosed(session, CloseStatus.NO_STATUS_CODE);

        assertThat(WebSocketUserPositionHandler.sessions.get(userId).length).isEqualTo(0);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(0);
    }


    private void connectSession(WebSocketSession session, String userId, String imageId, String broadcast){
        when(session.getAttributes()).thenReturn(Map.of("userId", userId, "imageId", imageId, "broadcast", broadcast));
        webSocketUserPositionHandler.afterConnectionEstablished(session);
    }

    private void initFollowingSession(String userAndImageId, ConcurrentWebSocketSessionDecorator broadcastSession, ConcurrentWebSocketSessionDecorator followerSession){
        WebSocketUserPositionHandler.sessionsBroadcast.put(userAndImageId, broadcastSession);
        WebSocketUserPositionHandler.sessionsTracked.put(broadcastSession, new ConcurrentWebSocketSessionDecorator[]{followerSession});
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(broadcastSession).length).isEqualTo(1);
    }

    private Map<String, Object> sessionAttributes(String userId, String imageId, String broadcast){
        return Map.of("userId", userId, "imageId", imageId, "broadcast", broadcast);
    }
}