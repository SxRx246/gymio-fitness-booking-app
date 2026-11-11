package com.ga.notification_service.config;

import jakarta.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures security for the Notification Service.
 *
 * <p>Protected notification endpoints rely on trusted authentication
 * information forwarded by the API Gateway.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    /**
     * Creates the security configuration.
     *
     * @param gatewayAuthenticationFilter filter that establishes
     *                                     authentication from Gateway headers
     */
    public SecurityConfiguration(
            GatewayAuthenticationFilter gatewayAuthenticationFilter) {
        this.gatewayAuthenticationFilter = gatewayAuthenticationFilter;
    }

    /**
     * Prevents Spring Boot from registering the authentication filter
     * as a separate servlet filter. The filter is registered only
     * inside the Spring Security filter chain.
     *
     * @return disabled servlet filter registration
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

    /**
     * Configures HTTP security and authorization rules.
     *
     * @param http HTTP security configuration
     * @return configured security filter chain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/notifications/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(
                        gatewayAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
