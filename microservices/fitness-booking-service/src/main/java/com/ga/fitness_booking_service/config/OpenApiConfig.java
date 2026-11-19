package com.ga.fitness_booking_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures OpenAPI documentation for the Gymio Fitness and Booking API,
 * including API information and JWT bearer authentication.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates the OpenAPI configuration with API information
     * and JWT bearer authentication.
     *
     * @return the configured OpenAPI documentation object
     */
    @Bean
    public OpenAPI gymioOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gymio Fitness & Booking API")
                        .version("1.0")
                        .description(
                                "REST API for Gymio fitness classes, "
                                        + "trainer management, bookings, "
                                        + "waitlists, and class reviews."
                        )
                )
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(
                        new SecurityRequirement().addList("bearerAuth")
                );
    }
}
