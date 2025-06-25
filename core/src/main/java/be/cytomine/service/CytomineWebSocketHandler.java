package be.cytomine.service;

import be.cytomine.exceptions.ServerException;
import be.cytomine.utils.Lock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

@Slf4j
public abstract class CytomineWebSocketHandler extends TextWebSocketHandler {

    public Map<String, ConcurrentWebSocketSessionDecorator[]> afterConnectionEstablished(WebSocketSession session, Map<String, ConcurrentWebSocketSessionDecorator[]> sessions) {
        ConcurrentWebSocketSessionDecorator sessionDecorator = new ConcurrentWebSocketSessionDecorator(session, 1000, 8192);
        String userId = session.getAttributes().get("userId").toString();

        if(Lock.getInstance().lockUsedWsSession(userId)){
            try{
                addSessionToSessionsList(userId, sessionDecorator, sessions);
            } finally {
                Lock.getInstance().unlockUserWsSession(userId);
            }
        } else {
            throw new ServerException("Cannot acquire lock for websocket sessions of user " + userId  + " , tryLock return false");
        }

        return sessions;
    }

    protected ConcurrentWebSocketSessionDecorator[] addSession(ConcurrentWebSocketSessionDecorator[] oldSessions, ConcurrentWebSocketSessionDecorator newSession){
        ConcurrentWebSocketSessionDecorator[] newSessions = new ConcurrentWebSocketSessionDecorator[oldSessions.length + 1];
        for(int i=0; i < oldSessions.length; i++){
            newSessions[i] = oldSessions[i];
        }
        newSessions[newSessions.length - 1] = newSession;
        return newSessions;
    }

    protected void sendWebSocketMessage(WebSocketSession s, TextMessage message) throws ServerException {
        if(s.isOpen()){
            try {
                s.sendMessage(message);
                log.info("Has send WebSocket message to session : " + s.getId());
            } catch (IOException e) {
                throw new ServerException("Failed to send message to session : " + s.getId());
            }
        }
    }

    private void addSessionToSessionsList(String userId, ConcurrentWebSocketSessionDecorator sessionDecorator, Map<String, ConcurrentWebSocketSessionDecorator[]> sessions){
        if(sessions.containsKey(userId)){
            ConcurrentWebSocketSessionDecorator[] oldSessions = sessions.get(userId);
            ConcurrentWebSocketSessionDecorator[] newSessions = addSession(oldSessions, sessionDecorator);
            sessions.replace(userId, oldSessions, newSessions);
        }
        else{
            ConcurrentWebSocketSessionDecorator[] newSessions = {sessionDecorator};
            sessions.put(userId, newSessions);
        }
    }

}
