package iot.data.platform.rich.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/hits")
public class WebSocketServer {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final CopyOnWriteArraySet<Session> sessions = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("✅ New WebSocket connection opened: "
                + session.getId() + ", total = " + sessions.size());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            RichHit hit = objectMapper.readValue(message, RichHit.class);
            System.out.println("⬅️ Received from client " + session.getId() + ": " + hit);
            broadcast(hit);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("❌ WebSocket connection closed: " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("⚠️ WebSocket error, session = " +
                (session != null ? session.getId() : "null"));
        throwable.printStackTrace();
    }

    public static void broadcast(RichHit hit) {
        try {
            String message = objectMapper.writeValueAsString(hit);
            System.out.println("📤 Broadcasting hit to " + sessions.size() + " sessions");
            for (Session session : sessions) {
                System.out.println("  -> " + session.getId() + " (open=" + session.isOpen() + ")");
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(message);
                } else {
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
