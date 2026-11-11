package com.ga.user_service.controller;

import com.ga.user_service.dto.response.AuthValidationResponse;
import com.ga.user_service.dto.response.TrainerValidationResponse;
import com.ga.user_service.service.InternalAuthService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final InternalAuthService internalAuthService;

    @Value("${internal.auth.secret}")
    private String internalAuthSecret;

    /**
     * Validates a JWT received from the API Gateway.
     *
     * @param authorization the Bearer token
     * @param internalSecret the shared secret used to authenticate the Gateway
     * @return the authenticated user's identity, or an unauthorized response
     */
    @PostMapping("/validate")
    public ResponseEntity<AuthValidationResponse> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalSecret) {

        if (!isValidInternalSecret(internalSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String token = authorization.substring(7);
            return ResponseEntity.ok(internalAuthService.validateToken(token));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Validates a trainer's account using their email address.
     *
     * @param email the trainer's email address
     * @param internalSecret the shared secret used to authenticate the calling service
     * @return trainer details if valid, or HTTP 404 if the trainer is not eligible
     */
    @GetMapping("/trainers/validate")
    public ResponseEntity<TrainerValidationResponse> validateTrainer(
            @RequestParam String email,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalSecret) {

        if (!isValidInternalSecret(internalSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return internalAuthService.validateTrainerByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Compares the supplied internal secret with the configured secret.
     *
     * @param suppliedSecret the secret supplied by the caller
     * @return true if the secrets match
     */
    private boolean isValidInternalSecret(String suppliedSecret) {
        if (suppliedSecret == null || internalAuthSecret == null) {
            return false;
        }

        return MessageDigest.isEqual(
                internalAuthSecret.getBytes(StandardCharsets.UTF_8),
                suppliedSecret.getBytes(StandardCharsets.UTF_8)
        );
    }
}