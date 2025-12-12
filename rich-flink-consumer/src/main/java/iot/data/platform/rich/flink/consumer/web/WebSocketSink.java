package iot.data.platform.rich.flink.consumer.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import iot.data.platform.rich.flink.consumer.rich.RichHit;

import javax.websocket.*;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ClientEndpoint
public class WebSocketSink extends RichSinkFunction<RichHit> {

    private static final String DEFAULT_WS_URL = "ws://rich-websocket:8090/hits";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private transient Session session;
    private transient WebSocketContainer container;
    private transient CountDownLatch openLatch;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        String wsUrl = System.getenv().getOrDefault("WS_SERVER_URL", DEFAULT_WS_URL);

        this.container = ContainerProvider.getWebSocketContainer();
        this.openLatch = new CountDownLatch(1);

        container.connectToServer(this, new URI(wsUrl));

        if (!openLatch.await(10, TimeUnit.SECONDS)) {
            System.err.println("⚠️ WebSocketSink: timeout while waiting for connection to " + wsUrl);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("✅ WebSocketSink connected: " + session.getId());
        if (openLatch != null) {
            openLatch.countDown();
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("⚠️ WebSocketSink error: " + throwable.getMessage());
        throwable.printStackTrace();
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("ℹ️ WebSocketSink closed: " + closeReason);
        this.session = null;
    }

    @Override
    public void invoke(RichHit value, Context context) throws Exception {
        if (session == null || !session.isOpen()) {
            System.err.println("⚠️ WebSocketSink: session is not open, dropping message");
            return;
        }

        String json = OBJECT_MAPPER.writeValueAsString(value);
        session.getAsyncRemote().sendText(json);
    }

    @Override
    public void close() throws Exception {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } finally {
            session = null;
            container = null;
            openLatch = null;
            super.close();
        }
    }
}
