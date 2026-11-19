package com.ga.notification_service.event;

public record PasswordResetEmailRequestedEvent(
        String email,
        String token
) {
}