package com.ga.api_gateway.dto.response;

/**
 * Represents the authenticated user's identity returned by User Service.
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