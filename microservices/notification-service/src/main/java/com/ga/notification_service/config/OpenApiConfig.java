package com.ga.notification_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures OpenAPI documentation for the Gymio Notification API,
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
                        .title("Gymio Notification API")
                        .version("1.0")
                        .description(
                                "REST API for Gymio notification management, "
                                        + "in-app notifications, and "
                                        + "server-sent events (SSE)."
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
