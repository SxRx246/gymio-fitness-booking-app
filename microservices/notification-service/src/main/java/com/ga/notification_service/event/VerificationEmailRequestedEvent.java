package com.ga.notification_service.event;

public record VerificationEmailRequestedEvent(
        String email,
        String token
) {
}