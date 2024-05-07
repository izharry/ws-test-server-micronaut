package com.example;

import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Prototype
public class CollectorSession {
    private final WebSocketSession session;

    public CollectorSession(@Parameter WebSocketSession session) {
        this.session = session;
    }

    public Mono<String> close() {
        System.out.println("Closing session");
        return Mono.from(session.send("bye"))
                .doOnError(s -> System.out.println("OnError: " + s))
                .doOnSuccess(s -> {
                    System.out.println("OnSuccess: " + s);
                    session.close(CloseReason.NORMAL);
                })
                .doFinally(s -> System.out.println("Finally"));
    }

    public void handleMessage(String message) {
        System.out.println("Handling message: " + message);
        Mono.just(message)
                .delayElement(Duration.ofSeconds(2))
                .flatMap(this::sendMessage)
                .subscribe();
    }

    public <T> Mono<Void> sendMessage(T message) {
        System.out.println("Sending message: " + message);
        return Mono.from(session.send(message))
                .then();
    }
}
