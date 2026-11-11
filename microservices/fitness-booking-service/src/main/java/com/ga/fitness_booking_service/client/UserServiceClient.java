package com.ga.fitness_booking_service.client;

import com.ga.fitness_booking_service.dto.response.TrainerValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Client responsible for communicating with the User Service
 * to retrieve and validate trainer information.
 */
@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final String internalAuthSecret;

    /**
     * Creates a User Service client.
     *
     * @param builder the RestClient builder
     * @param userServiceUrl the base URL of the User Service
     * @param internalAuthSecret the shared secret for internal communication
     */
    public UserServiceClient(
            RestClient.Builder builder,
            @Value("${app.user-service.base-url:http://localhost:8081}") String userServiceUrl,
            @Value("${app.internal-auth.secret}") String internalAuthSecret) {

        this.restClient = builder.baseUrl(userServiceUrl).build();
        this.internalAuthSecret = internalAuthSecret;
    }

    /**
     * Requests trainer validation from the User Service.
     *
     * @param email the trainer's email address
     * @return trainer details if the trainer is eligible, or empty if not found
     */
    public Optional<TrainerValidationResponse> validateTrainerByEmail(String email) {

        return Optional.ofNullable(
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/internal/auth/trainers/validate")
                                .queryParam("email", email)
                                .build())
                        .header("X-Internal-Secret", internalAuthSecret)
                        .retrieve()
                        .onStatus(status -> status.value() == 404, (request, response) -> {
                        })
                        .body(TrainerValidationResponse.class)
        );
    }
}
