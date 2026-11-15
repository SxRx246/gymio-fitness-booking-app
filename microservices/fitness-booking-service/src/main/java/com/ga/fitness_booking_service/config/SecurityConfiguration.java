package com.ga.fitness_booking_service.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures security for the Fitness & Booking Service.
 *
 * <p>Authentication is established using trusted identity headers
 * forwarded by the API Gateway after JWT validation.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    /**
     * Creates the security configuration.
     *
     * @param gatewayAuthenticationFilter the filter that establishes
     *                                    authentication from trusted Gateway headers
     */
    public SecurityConfiguration(
            GatewayAuthenticationFilter gatewayAuthenticationFilter) {
        this.gatewayAuthenticationFilter = gatewayAuthenticationFilter;
    }

    /**
     * Configures the stateless security filter chain.
     *
     * @param http the HTTP security configuration
     * @return the configured security filter chain
     * @throws Exception if the security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info"
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
     * Prevents the Gateway authentication filter from being registered
     * twice by the servlet container and Spring Security.
     *
     * @return the disabled servlet filter registration
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
