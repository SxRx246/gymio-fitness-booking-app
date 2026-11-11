package com.ga.notification_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages real-time customer notifications using Server-Sent Events (SSE).
 *
 * Supports multiple active connections for each customer.
 */
@Service
public class NotificationService {

    private final Map<Long, Set<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    /**
     * Subscribes a customer to real-time notifications.
     *
     * @param userId the ID of the customer subscribing
     * @return the SSE emitter used to receive notifications
     */
    public SseEmitter subscribe(Long userId) {

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitters.computeIfAbsent(
                userId,
                id -> ConcurrentHashMap.newKeySet()
        ).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));

        return emitter;
    }

    /**
     * Removes a disconnected emitter from the customer's connections.
     *
     * @param userId the ID of the customer
     * @param emitter the disconnected SSE emitter
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {

        emitters.computeIfPresent(userId, (id, connections) -> {
            connections.remove(emitter);

            return connections.isEmpty() ? null : connections;
        });
    }

    /**
     * Sends a notification to all active connections of a customer.
     *
     * @param userId the ID of the notification recipient
     * @param message the notification message
     */
    public void sendNotification(Long userId, String message) {

        Set<SseEmitter> connections = emitters.get(userId);

        if (connections == null || connections.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : connections) {

            try {
                emitter.send(
                        SseEmitter.event()
                                .name("notification")
                                .data(message)
                );

            } catch (IOException | IllegalStateException e) {
                removeEmitter(userId, emitter);
            }
        }
    }
}
