package com.ga.api_gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures OpenAPI documentation for the Gymio API Gateway,
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
                        .title("Gymio API Gateway")
                        .version("1.0")
                        .description(
                                "Central entry point for the Gymio platform, "
                                        + "providing routing and access "
                                        + "to the platform's microservices."
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
