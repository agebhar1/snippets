package io.github.agebhar1.snippets.quarkus.boundary;

import io.quarkus.vertx.ConsumeEvent;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.smallrye.mutiny.Uni.join;

@WebSocket(path = "/changes")
public class Changes {

    private static final Logger logger = LoggerFactory.getLogger(Changes.class);

    private final OpenConnections openConnections;

    public Changes(OpenConnections openConnections) {
        this.openConnections = openConnections;
    }

    @OnOpen
    public Uni<String> onOpen(WebSocketConnection connection, HandshakeRequest request) {
        logger.info("Client connected: id={} remoteAddress={}", connection.id(), request.remoteAddress());
        return Uni.createFrom().item(connection.id());
    }

    @OnClose
    public void onClose(WebSocketConnection connection, HandshakeRequest request) {
        logger.info("Client disconnected: id={} remoteAddress={}", connection.id(), request.remoteAddress());
    }

    @ConsumeEvent(value = "StateChange", local = false)
    Uni<Void> broadcast(String message) {
        logger.info("Received message from event bus: {}", message);

        var unis = openConnections.stream()
                .filter(WebSocketConnection::isOpen)
                .map(connection -> connection.sendText(message))
                .toList();

        return join().all(unis).andCollectFailures().replaceWithVoid();
    }

}
