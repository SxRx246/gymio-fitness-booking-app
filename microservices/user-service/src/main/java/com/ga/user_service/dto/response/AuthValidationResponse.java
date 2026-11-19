package com.ga.user_service.dto.response;

/**
 * Represents the authenticated user's identity extracted from a valid JWT.
 *
 * @param userId the authenticated user's ID
 * @param role the authenticated user's role
 * @param email the authenticated user's email
 */
public record AuthValidationResponse(
        Long userId,
        String role,
        String email
) {
}