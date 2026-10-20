package com.ga.gymio.controller;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Provides endpoints for receiving real-time notifications.
 *
 * Authenticated users can subscribe to a Server-Sent Events (SSE)
 * stream to receive notifications from the application.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Subscribes the authenticated user to a real-time notification stream.
     *
     * The user's ID is extracted from the authenticated user's details
     * and passed to the notification service.
     *
     * @param userDetails the details of the authenticated user
     * @return an SSE emitter used to send notifications to the user
     */
    @GetMapping(
            value = "/subscribe",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public SseEmitter subscribe(
            @AuthenticationPrincipal MyUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();

        return notificationService.subscribe(userId);
    }
}