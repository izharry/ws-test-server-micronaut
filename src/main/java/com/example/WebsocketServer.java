package com.example;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationShutdownEvent;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashSet;

@ServerWebSocket("/ws")
@RequiredArgsConstructor
public class WebsocketServer implements ApplicationEventListener<ApplicationShutdownEvent> {

    public static final String SESSION = "session";
    private final ApplicationContext context;
    private final HashSet<CollectorSession> sessions = new HashSet<>();

    @OnOpen
    public void onOpen(WebSocketSession session) {
        System.out.println("New connection opened");
        Mono.fromSupplier(() -> context.createBean(CollectorSession.class, session))
                .doOnSuccess(newSession -> {
                    session.put(SESSION, newSession);
                    sessions.add(newSession);
                })
                .subscribe();
    }

    @OnMessage
    public void onMessage(String message, WebSocketSession session) {
        System.out.println("New message received");
        session.get(SESSION, CollectorSession.class)
                .ifPresentOrElse(collectorSession -> collectorSession.handleMessage(message),
                        () -> System.out.println("Message received but Collector Session not present"));
    }

    @OnClose
    public void onClose(WebSocketSession session, CloseReason closeReason) {
        System.out.println("Connection closed: " + closeReason.getReason());
        session.get(SESSION, CollectorSession.class)
                .ifPresentOrElse(sessions::remove, () -> System.out.println("Collector Session not present for closing"));
    }

    @Override
    public void onApplicationEvent(ApplicationShutdownEvent event) {
        System.out.println("Websocket server is shutting down...");
        System.out.println("Number of collector sessions to close: " + sessions.size());
        Flux.fromIterable(sessions)
                .flatMap(collectorSession -> {
                    System.out.println("Closing collector session");
                    return collectorSession.close();
                })
                .doOnComplete(() -> System.out.println("All collector sessions closed"))
                .blockLast(Duration.ofSeconds(10));
    }

    @Override
    public boolean supports(ApplicationShutdownEvent event) {
        return ApplicationEventListener.super.supports(event);
    }
}
