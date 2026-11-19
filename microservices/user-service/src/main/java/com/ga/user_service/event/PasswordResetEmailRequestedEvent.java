package com.ga.user_service.event;

public record PasswordResetEmailRequestedEvent(
        String email,
        String token
) {
}