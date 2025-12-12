package iot.data.platform.rich.websocket;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.server.ServerEndpointConfig;

public class WebSocketServerMain {

    public static void main(String[] args) throws Exception {

        int port = 8090;
        String portFromEnv = System.getenv("WS_SERVER_PORT");
        if (portFromEnv != null && !portFromEnv.isBlank()) {
            try {
                port = Integer.parseInt(portFromEnv.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        Server server = new Server(port);

        ServletContextHandler context =
                new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        ServerContainer wsContainer =
                WebSocketServerContainerInitializer.configureContext(context);

        wsContainer.addEndpoint(
                ServerEndpointConfig.Builder
                        .create(WebSocketServer.class, "/hits")
                        .build()
        );

        System.out.println("🚀 WebSocket server started on port " + port + ", path /hits");

        server.start();
        server.join();
    }
}