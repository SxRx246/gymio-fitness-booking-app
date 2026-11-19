package com.ga.api_gateway.service;

import com.ga.api_gateway.dto.response.AuthValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AuthValidationService {

    private final WebClient.Builder webClientBuilder;

    public AuthValidationService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Value("${app.user-service.validation-url}")
    private String validationUrl;

    @Value("${app.internal-auth.secret}")
    private String internalAuthSecret;

    /**
     * Validates a JWT token through the User Service.
     *
     * @param token the JWT token to validate
     * @return the authenticated user's identity
     */
    public Mono<AuthValidationResponse> validateToken(String token) {

        return webClientBuilder.build()
                .post()
                .uri(validationUrl)
                .header("Authorization", "Bearer " + token)
                .header("X-Internal-Secret", internalAuthSecret)
                .retrieve()
                .bodyToMono(AuthValidationResponse.class);
    }
}