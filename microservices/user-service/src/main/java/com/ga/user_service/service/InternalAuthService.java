package com.ga.user_service.service;

import com.ga.user_service.authentication.JWTUtils;
import com.ga.user_service.dto.response.AuthValidationResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternalAuthService {

    private final JWTUtils jwtUtils;

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
}