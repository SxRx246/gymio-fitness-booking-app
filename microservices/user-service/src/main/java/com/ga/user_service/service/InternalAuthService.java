package com.ga.user_service.service;

import com.ga.user_service.authentication.JWTUtils;
import com.ga.user_service.dto.response.AuthValidationResponse;
import com.ga.user_service.dto.response.TrainerValidationResponse;
import com.ga.user_service.model.User;
import com.ga.user_service.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InternalAuthService {

    private final JWTUtils jwtUtils;
    private final UserRepository userRepository;

    /**
     * Validates a JWT and extracts the authenticated user's identity.
     *
     * @param token the JWT to validate
     * @return the authenticated user's identity
     * @throws JwtException if the token is invalid or expired
     */
    public AuthValidationResponse validateToken(String token) {

        if (!jwtUtils.validateJwtToken(token)) {
            throw new JwtException("Invalid or expired JWT");
        }

        Claims claims = jwtUtils.getClaimsFromJwtToken(token);

        Long userId = claims.get("userId", Long.class);
        String role = claims.get("role", String.class);
        String email = claims.getSubject();

        if (userId == null || role == null || email == null) {
            throw new JwtException("Required JWT claims are missing");
        }

        return new AuthValidationResponse(userId, role, email);
    }

    /**
     * Finds a trainer by email and verifies that the account is eligible
     * to create or manage fitness classes.
     *
     * @param email the trainer's email address
     * @return trainer details if the account is eligible, otherwise empty
     */
    public Optional<TrainerValidationResponse> validateTrainerByEmail(String email) {

        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email.trim())
                .filter(user -> user.getRole() == User.Role.TRAINER)
                .filter(user -> user.getStatus() == User.Status.ACTIVE)
                .filter(User::isEmailVerified)
                .map(user -> new TrainerValidationResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getStatus().name(),
                        user.isEmailVerified()
                ));
    }
}
