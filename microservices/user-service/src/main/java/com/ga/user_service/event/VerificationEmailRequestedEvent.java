package com.ga.user_service.event;

public record VerificationEmailRequestedEvent(
        String email,
        String token
) {
}