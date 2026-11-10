package com.ga.fitness_booking_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Extracts the claims from a JWT after validating its signature.
     *
     * @param token the JWT token
     * @return the token claims
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validates the JWT signature and expiration.
     *
     * @param token the JWT token
     * @return true if the token is valid
     */
    public boolean validateJwtToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extracts the user ID from the JWT.
     *
     * @param token the JWT token
     * @return the authenticated user's ID
     */
    public Long getUserIdFromJwtToken(String token) {
        Number userId = getClaims(token).get("userId", Number.class);
        return userId.longValue();
    }

    /**
     * Extracts the user's role from the JWT.
     *
     * @param token the JWT token
     * @return the authenticated user's role
     */
    public String getRoleFromJwtToken(String token) {
        return getClaims(token).get("role", String.class);
    }
}
