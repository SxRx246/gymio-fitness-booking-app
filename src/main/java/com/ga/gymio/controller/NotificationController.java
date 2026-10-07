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

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

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