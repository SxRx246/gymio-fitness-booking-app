package com.ga.gymio.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages real-time user notifications using Server-Sent Events (SSE).
 */
@Service
public class NotificationService {

    private final Map<Long, SseEmitter> emitters =
            new ConcurrentHashMap<>();

    /**
     * Subscribes a user to real-time notifications.
     *
     * @param userId the ID of the user subscribing
     * @return the SSE emitter used to send notifications to the user
     */
    public SseEmitter subscribe(Long userId) {

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(error -> emitters.remove(userId));

        return emitter;
    }

    /**
     * Sends a real-time notification to a subscribed user.
     * Does nothing if the user has no active subscription.
     *
     * @param userId the ID of the recipient
     * @param message the notification message
     */
    public void sendNotification(Long userId, String message) {

        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("notification")
                            .data(message)
            );
        } catch (IOException e) {
            emitters.remove(userId);
        }
    }
}