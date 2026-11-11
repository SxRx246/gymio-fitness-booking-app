package com.ga.fitness_booking_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the RestClient builder used for synchronous
 * communication with other microservices.
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates a RestClient builder bean.
     *
     * @return a new RestClient builder
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
