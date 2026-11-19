package com.ga.notification_service.controller;

import com.ga.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Provides REST endpoints for real-time user notifications.
 *
 * <p>Authenticated users can subscribe to a Server-Sent Events (SSE)
 * stream to receive real-time notifications.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    /**
     * Service responsible for managing SSE connections
     * and delivering real-time notifications.
     */
    private final NotificationService notificationService;

    /**
     * Subscribes the authenticated user to a real-time notification stream.
     *
     * <p>The user's ID is extracted from the authenticated principal
     * and used to establish an SSE connection.</p>
     *
     * @param authentication the authentication details of the current user
     * @return an SSE emitter used to deliver real-time notifications
     */
    @GetMapping(
            value = "/subscribe",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public SseEmitter subscribe(Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return notificationService.subscribe(userId);
    }
}
