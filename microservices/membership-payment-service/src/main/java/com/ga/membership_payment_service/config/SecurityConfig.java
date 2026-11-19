package com.ga.membership_payment_service.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures security for the Gymio Membership and Payment Service.
 * Requests are authenticated through the API Gateway authentication filter.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    /**
     * Creates the security configuration.
     *
     * @param gatewayAuthenticationFilter filter that authenticates Gateway requests
     */
    public SecurityConfig(
            GatewayAuthenticationFilter gatewayAuthenticationFilter) {
        this.gatewayAuthenticationFilter = gatewayAuthenticationFilter;
    }

    /**
     * Configures the application's HTTP security rules.
     *
     * @param http the HTTP security configuration
     * @return the configured security filter chain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        gatewayAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Prevents Spring Boot from registering the Gateway filter twice.
     *
     * @return a disabled servlet filter registration
     */
    @Bean
    public FilterRegistrationBean<GatewayAuthenticationFilter>
    gatewayFilterRegistration() {

        FilterRegistrationBean<GatewayAuthenticationFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(gatewayAuthenticationFilter);
        registration.setEnabled(false);

        return registration;
    }
}
