package com.ga.user_service.dto.response;

/**
 * Represents the trainer information returned to an authorized internal service.
 *
 * @param id the trainer's user ID
 * @param email the trainer's email address
 * @param role the trainer's role
 * @param status the trainer's account status
 * @param emailVerified whether the trainer's email is verified
 */
public record TrainerValidationResponse(
        Long id,
        String email,
        String role,
        String status,
        boolean emailVerified
) {
}