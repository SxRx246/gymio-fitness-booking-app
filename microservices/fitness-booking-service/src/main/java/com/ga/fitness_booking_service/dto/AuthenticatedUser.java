package com.ga.fitness_booking_service.dto;

/**
 * Represents the authenticated user's identity forwarded by the API Gateway.
 *
 * @param userId the authenticated user's ID
 * @param role the authenticated user's role
 * @param email the authenticated user's email address
 */
public record AuthenticatedUser(
        Long userId,
        String role,
        String email
) {
}
